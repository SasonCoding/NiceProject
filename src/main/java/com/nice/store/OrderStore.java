package com.nice.store;

import com.nice.model.Dish;
import com.nice.model.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Repository responsible for holding all orders waiting to be delivered, in
 * first-come-first-served order, and for selecting delivery batches that
 * group same-city orders together.
 *
 * <p>This class is a pure data/algorithm layer: it has no printing or other
 * I/O side effects. Callers (e.g. {@link com.nice.restaurant.Restaurant})
 * decide what to do with the {@link Order} data it returns.
 *
 * <p>Data structures:
 * <ul>
 *   <li>{@code ordersById}: a {@link LinkedHashMap} keyed by order id. Iteration
 *       order always matches arrival order and is unaffected by removals,
 *       giving O(1) add/get/remove while preserving FIFO semantics.</li>
 *   <li>{@code ordersByCity}: a map from normalized city -&gt; the ids of the
 *       orders waiting for that city (in arrival order), giving O(1) lookup of
 *       "does this city have other waiting orders" plus O(1) removal from the
 *       bucket.</li>
 * </ul>
 *
 * <p>Every mutating/reading operation is guarded by a single {@link ReentrantLock}.
 * Because each critical section is O(1) (or, for {@link #getCurrentOrders()},
 * O(n) over currently waiting orders), lock hold times are negligible.
 */
public final class OrderStore {

    private static final int BATCH_CAPACITY = 3;

    private final LinkedHashMap<Long, Order> ordersById = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<Long>> ordersByCity = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Registers a new order, received first-come-first-served.
     *
     * @param customerName the customer's name
     * @param city          the destination city (address), as typed by the customer
     * @param dish          the dish ordered (a {@link com.nice.model.Pasta} or {@link com.nice.model.Pizza})
     * @return the newly created {@link Order}
     */
    public Order addOrder(String customerName, String city, Dish dish) {
        lock.lock();
        try {
            long id = idGenerator.getAndIncrement();
            String normalizedCity = normalizeCity(city);
            Order order = new Order(id, customerName, city, normalizedCity, dish);

            ordersById.put(id, order);
            ordersByCity.computeIfAbsent(normalizedCity, key -> new LinkedHashSet<>()).add(id);

            return order;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return every order currently waiting to be dispatched, oldest first.
     *         An immutable snapshot; does not reflect later mutations.
     */
    public List<Order> getCurrentOrders() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(ordersById.values()));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Selects up to {@value #BATCH_CAPACITY} waiting orders for the next
     * delivery batch, removing them from the waiting store.
     *
     * <p>Selection order: walk the waiting orders in FIFO order; the first
     * not-yet-selected order becomes a "pivot" and is added to the batch;
     * every other still-waiting order for that same city is then greedily
     * pulled in (in their own arrival order) until either that city's orders
     * are exhausted or the batch is full. If the batch still has room, the
     * FIFO walk continues to the next not-yet-selected order as the next
     * pivot. This repeats until the batch reaches capacity or there are no
     * more waiting orders.
     *
     * @return the orders selected for this batch, in the order they should be delivered
     */
    public List<Order> selectNextBatch() {
        lock.lock();
        try {
            return selectAndRemoveBatch();
        } finally {
            lock.unlock();
        }
    }

    private List<Order> selectAndRemoveBatch() {
        List<Order> batch = new ArrayList<>(BATCH_CAPACITY);
        Set<Long> usedIds = new LinkedHashSet<>();

        for (Long id : ordersById.keySet()) {
            if (batch.size() >= BATCH_CAPACITY) {
                break;
            }
            if (usedIds.contains(id)) {
                continue;
            }

            Order pivot = ordersById.get(id);
            batch.add(pivot);
            usedIds.add(id);
            if (batch.size() >= BATCH_CAPACITY) {
                break;
            }

            LinkedHashSet<Long> cityBucket = ordersByCity.get(pivot.getNormalizedCity());
            if (cityBucket != null) {
                for (Long otherId : cityBucket) {
                    if (batch.size() >= BATCH_CAPACITY) {
                        break;
                    }
                    if (otherId.equals(id) || usedIds.contains(otherId)) {
                        continue;
                    }
                    batch.add(ordersById.get(otherId));
                    usedIds.add(otherId);
                }
            }
        }

        for (Long id : usedIds) {
            Order removed = ordersById.remove(id);
            LinkedHashSet<Long> cityBucket = ordersByCity.get(removed.getNormalizedCity());
            if (cityBucket != null) {
                cityBucket.remove(id);
                if (cityBucket.isEmpty()) {
                    ordersByCity.remove(removed.getNormalizedCity());
                }
            }
        }

        return batch;
    }

    private static String normalizeCity(String city) {
        return city == null ? "" : city.trim().toLowerCase();
    }
}

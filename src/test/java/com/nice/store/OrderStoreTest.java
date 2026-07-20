package com.nice.store;

import com.nice.model.Dish;
import com.nice.model.Order;
import com.nice.model.Pasta;
import com.nice.model.PastaType;
import com.nice.model.Pizza;
import com.nice.model.SauceType;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStoreTest {

    private static final Dish ANY_DISH = new Pasta(PastaType.PENNE, SauceType.TOMATO);

    private static List<String> cities(List<Order> orders) {
        return orders.stream().map(Order::getRawCity).collect(Collectors.toList());
    }

    private static List<String> nextBatchCities(OrderStore store) {
        return cities(store.selectNextBatch());
    }

    @Test
    void example1_allDistinctCities_firstThreeInFifoOrder() {
        OrderStore store = new OrderStore();
        store.addOrder("A", "TLV", ANY_DISH);
        store.addOrder("B", "Kfar Saba", ANY_DISH);
        store.addOrder("C", "Netanya", ANY_DISH);
        store.addOrder("D", "Raanana", ANY_DISH);

        List<Order> batch = store.selectNextBatch();

        assertEquals(List.of("TLV", "Kfar Saba", "Netanya"), cities(batch));
        // Raanana remains waiting -> next dispatch returns it alone
        assertEquals(List.of("Raanana"), nextBatchCities(store));
    }

    @Test
    void example2_lastOrderSameCityAsSecond_groupedTogether() {
        OrderStore store = new OrderStore();
        store.addOrder("A", "TLV", ANY_DISH);
        store.addOrder("B", "Kfar Saba", ANY_DISH);
        store.addOrder("C", "Netanya", ANY_DISH);
        store.addOrder("D", "Kfar Saba", ANY_DISH);

        List<Order> batch = store.selectNextBatch();

        assertEquals(List.of("TLV", "Kfar Saba", "Kfar Saba"), cities(batch));
        assertEquals(List.of("Netanya"), nextBatchCities(store));
    }

    @Test
    void example3_lastOrderSameCityAsFirst_groupedTogether() {
        OrderStore store = new OrderStore();
        store.addOrder("A", "TLV", ANY_DISH);
        store.addOrder("B", "Kfar Saba", ANY_DISH);
        store.addOrder("C", "Netanya", ANY_DISH);
        store.addOrder("D", "TLV", ANY_DISH);

        List<Order> batch = store.selectNextBatch();

        assertEquals(List.of("TLV", "TLV", "Kfar Saba"), cities(batch));
        assertEquals(List.of("Netanya"), nextBatchCities(store));
    }

    @Test
    void example4_twoAdditionalSameCityOrders_fillWholeBatch() {
        OrderStore store = new OrderStore();
        store.addOrder("A", "TLV", ANY_DISH);
        store.addOrder("B", "Kfar Saba", ANY_DISH);
        store.addOrder("C", "Netanya", ANY_DISH);
        store.addOrder("D", "TLV", ANY_DISH);
        store.addOrder("E", "TLV", ANY_DISH);

        List<Order> batch = store.selectNextBatch();

        assertEquals(List.of("TLV", "TLV", "TLV"), cities(batch));
        // Kfar Saba and Netanya remain, in original arrival order
        assertEquals(List.of("Kfar Saba", "Netanya"), nextBatchCities(store));
    }

    @Test
    void dispatchOnEmptyStore_returnsEmptyBatch() {
        OrderStore store = new OrderStore();
        assertTrue(store.selectNextBatch().isEmpty());
    }

    @Test
    void dispatchWithFewerThanCapacity_returnsWhatIsAvailable() {
        OrderStore store = new OrderStore();
        store.addOrder("A", "TLV", ANY_DISH);
        store.addOrder("B", "Netanya", ANY_DISH);

        List<Order> batch = store.selectNextBatch();

        assertEquals(2, batch.size());
        assertTrue(store.selectNextBatch().isEmpty());
    }

    @Test
    void cityGroupingIsCaseAndWhitespaceInsensitive() {
        OrderStore store = new OrderStore();
        store.addOrder("A", " TLV ", ANY_DISH);
        store.addOrder("B", "Netanya", ANY_DISH);
        store.addOrder("C", "tlv", ANY_DISH);

        List<Order> batch = store.selectNextBatch();

        // Both TLV variants should be grouped together, ahead of Netanya
        assertEquals(3, batch.size());
        assertEquals("A", batch.get(0).getCustomerName());
        assertEquals("C", batch.get(1).getCustomerName());
        assertEquals("B", batch.get(2).getCustomerName());
    }

    @Test
    void moreThanCapacityOrdersForOneCity_leavesRestForNextBatch() {
        OrderStore store = new OrderStore();
        for (int i = 0; i < 5; i++) {
            store.addOrder("Customer" + i, "TLV", ANY_DISH);
        }

        List<Order> firstBatch = store.selectNextBatch();
        assertEquals(3, firstBatch.size());

        List<Order> secondBatch = store.selectNextBatch();
        assertEquals(2, secondBatch.size());
    }

    @Test
    void pastaPriceIsFixedRegardlessOfTypeAndSauce() {
        assertEquals(50, new Pasta(PastaType.SPAGHETTI, SauceType.ROSA).getPrice());
        assertEquals(50, new Pasta(PastaType.TORTELLINI, SauceType.MUSHROOM_CREAM).getPrice());
    }

    @Test
    void pizzaPriceDependsOnlyOnWhetherToppingsArePresent() {
        assertEquals(30, new Pizza(Collections.emptyList()).getPrice());
        assertEquals(45, new Pizza(List.of("mushroom")).getPrice());
        assertEquals(45, new Pizza(List.of("mushroom", "onion", "olives")).getPrice());
    }

    @Test
    void getCurrentOrdersReflectsWaitingOrdersInFifoOrder() {
        OrderStore store = new OrderStore();
        assertTrue(store.getCurrentOrders().isEmpty());

        store.addOrder("A", "TLV", ANY_DISH);
        store.addOrder("B", "Netanya", ANY_DISH);

        assertEquals(List.of("TLV", "Netanya"), cities(store.getCurrentOrders()));
    }

    @Test
    void getCurrentOrdersSnapshotIsUnaffectedByLaterMutation() {
        OrderStore store = new OrderStore();
        store.addOrder("A", "TLV", ANY_DISH);

        List<Order> snapshot = store.getCurrentOrders();
        store.addOrder("B", "Netanya", ANY_DISH);

        assertEquals(1, snapshot.size());
    }

    @Test
    void concurrentAddOrderAndSelectNextBatchNeverCorruptOrLoseOrders() throws InterruptedException {
        OrderStore store = new OrderStore();
        int writerThreads = 8;
        int ordersPerThread = 50;
        int totalOrders = writerThreads * ordersPerThread;
        String[] cityPool = {"TLV", "Kfar Saba", "Netanya", "Raanana"};

        ExecutorService writers = Executors.newFixedThreadPool(writerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(writerThreads);
        Set<Long> seenIds = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicateIds = new AtomicInteger();

        for (int t = 0; t < writerThreads; t++) {
            final int threadIndex = t;
            writers.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < ordersPerThread; i++) {
                        String city = cityPool[(threadIndex + i) % cityPool.length];
                        Order order = store.addOrder("Customer" + threadIndex + "-" + i, city, ANY_DISH);
                        if (!seenIds.add(order.getId())) {
                            duplicateIds.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // A concurrent reader/dispatcher, hammering selectNextBatch while orders are still arriving.
        AtomicInteger dispatchedCount = new AtomicInteger();
        Thread dispatcherThread = new Thread(() -> {
            while (doneLatch.getCount() > 0) {
                dispatchedCount.addAndGet(store.selectNextBatch().size());
            }
        });
        dispatcherThread.start();

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "writer threads did not finish in time");
        writers.shutdown();
        dispatcherThread.join(10_000);

        // Drain anything left waiting after the writers/dispatcher loop finished.
        List<Order> remaining;
        do {
            remaining = store.selectNextBatch();
            dispatchedCount.addAndGet(remaining.size());
        } while (!remaining.isEmpty());

        assertEquals(0, duplicateIds.get(), "no order id should ever be handed out twice");
        assertEquals(totalOrders, seenIds.size(), "every submitted order must have been accepted exactly once");
        assertEquals(totalOrders, dispatchedCount.get(), "every accepted order must eventually be dispatched exactly once");
        assertTrue(store.getCurrentOrders().isEmpty());
    }
}

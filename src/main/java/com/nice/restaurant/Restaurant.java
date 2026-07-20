package com.nice.restaurant;

import com.nice.model.Dish;
import com.nice.model.Order;
import com.nice.store.OrderStore;

import java.util.List;
import java.util.Objects;

/**
 * Public-facing API for the restaurant's ordering system.
 *
 * <p>This is the facade a caller (a CLI, a test, a future scheduler) talks
 * to: placing orders, viewing what's currently waiting, and dispatching the
 * next delivery batch. All storage and batch-selection concerns are
 * delegated to {@link OrderStore}, so callers of {@code Restaurant} never
 * need to know how orders are stored, indexed, or grouped by city — and if
 * that internal representation ever changes, this public API doesn't have
 * to.
 */
public final class Restaurant {

    private final OrderStore orderStore;

    public Restaurant() {
        this(new OrderStore());
    }

    public Restaurant(OrderStore orderStore) {
        this.orderStore = Objects.requireNonNull(orderStore, "orderStore");
    }

    /**
     * Receives a new order, first-come-first-served.
     *
     * @param customerName the customer's name
     * @param city          the destination city (address)
     * @param dish          the dish ordered (a {@link com.nice.model.Pasta} or {@link com.nice.model.Pizza})
     * @return the newly created {@link Order}
     */
    public Order placeOrder(String customerName, String city, Dish dish) {
        return orderStore.addOrder(customerName, city, dish);
    }

    /** Prints every order currently waiting to be dispatched, oldest first. */
    public void printCurrentOrders() {
        List<Order> orders = orderStore.getCurrentOrders();
        if (orders.isEmpty()) {
            System.out.println("No current orders.");
            return;
        }
        System.out.println("Current orders (" + orders.size() + "):");
        for (Order order : orders) {
            System.out.println("  " + order);
        }
    }

    /**
     * Selects the next delivery batch (up to 3 orders, FIFO with same-city
     * grouping — see {@link OrderStore#selectNextBatch()}), removes those
     * orders from the waiting list, and prints them.
     *
     * @return the orders included in this batch, in delivery order
     */
    public List<Order> dispatchNextBatch() {
        List<Order> batch = orderStore.selectNextBatch();
        if (batch.isEmpty()) {
            System.out.println("No orders to dispatch.");
            return batch;
        }
        System.out.println("Dispatching delivery batch (" + batch.size() + " orders):");
        for (Order order : batch) {
            System.out.println("  " + order);
        }
        return batch;
    }
}

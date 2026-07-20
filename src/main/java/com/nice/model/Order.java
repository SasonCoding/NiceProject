package com.nice.model;

import java.util.Objects;

/** A single customer order: who, where, and what dish. */
public final class Order {

    private final long id;
    private final String customerName;
    private final String rawCity;
    private final String normalizedCity;
    private final Dish dish;

    public Order(long id, String customerName, String rawCity, String normalizedCity, Dish dish) {
        this.id = id;
        this.customerName = Objects.requireNonNull(customerName, "customerName");
        this.rawCity = Objects.requireNonNull(rawCity, "rawCity");
        this.normalizedCity = Objects.requireNonNull(normalizedCity, "normalizedCity");
        this.dish = Objects.requireNonNull(dish, "dish");
    }

    public long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    /** The city exactly as the customer typed it, for display. */
    public String getRawCity() {
        return rawCity;
    }

    /** Trimmed + case-folded city, used as the grouping key for delivery batches. */
    public String getNormalizedCity() {
        return normalizedCity;
    }

    public Dish getDish() {
        return dish;
    }

    @Override
    public String toString() {
        return "Order #" + id + " [" + customerName + ", " + rawCity + "] " + dish.describe()
                + " ($" + dish.getPrice() + ")";
    }
}

package com.nice.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A pizza dish. Toppings are free text; price only depends on whether any
 * toppings were requested, not on which specific toppings.
 */
public final class Pizza extends Dish {

    public static final double PRICE_NO_TOPPINGS = 30;
    public static final double PRICE_WITH_TOPPINGS = 45;

    private final List<String> toppings;

    public Pizza(List<String> toppings) {
        Objects.requireNonNull(toppings, "toppings");
        this.toppings = Collections.unmodifiableList(toppings);
    }

    public List<String> getToppings() {
        return toppings;
    }

    @Override
    public double getPrice() {
        return toppings.isEmpty() ? PRICE_NO_TOPPINGS : PRICE_WITH_TOPPINGS;
    }

    @Override
    public String describe() {
        return toppings.isEmpty()
                ? "Pizza"
                : "Pizza with " + String.join(", ", toppings);
    }
}

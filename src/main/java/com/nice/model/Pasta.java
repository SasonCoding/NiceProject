package com.nice.model;

import java.util.Objects;

/** A pasta dish: a pasta type plus a sauce. Price is fixed regardless of the combination chosen. */
public final class Pasta extends Dish {

    public static final double PRICE = 50;

    private final PastaType type;
    private final SauceType sauce;

    public Pasta(PastaType type, SauceType sauce) {
        this.type = Objects.requireNonNull(type, "type");
        this.sauce = Objects.requireNonNull(sauce, "sauce");
    }

    public PastaType getType() {
        return type;
    }

    public SauceType getSauce() {
        return sauce;
    }

    @Override
    public double getPrice() {
        return PRICE;
    }

    @Override
    public String describe() {
        return "Pasta: " + type + " with " + sauce + " sauce";
    }
}

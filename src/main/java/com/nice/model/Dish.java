package com.nice.model;

/** Base type for anything a customer can order (Pasta, Pizza, ...). */
public abstract class Dish {

    /** Price of this dish, in whole currency units. */
    public abstract double getPrice();

    /** Human readable description, used when printing orders. */
    public abstract String describe();

    @Override
    public String toString() {
        return describe();
    }
}

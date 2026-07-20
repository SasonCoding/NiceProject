package com.nice.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PizzaTest {

    @Test
    void noToppingsPricedAtThirty() {
        Pizza pizza = new Pizza(List.of());
        assertEquals(Pizza.PRICE_NO_TOPPINGS, pizza.getPrice());
        assertEquals("Pizza", pizza.describe());
    }

    @Test
    void withToppingsPricedAtFortyFive() {
        Pizza pizza = new Pizza(List.of("mushroom", "olives"));
        assertEquals(Pizza.PRICE_WITH_TOPPINGS, pizza.getPrice());
        assertEquals("Pizza with mushroom, olives", pizza.describe());
    }

    @Test
    void mutatingOriginalListAfterConstructionDoesNotAffectPizza() {
        List<String> toppings = new ArrayList<>(List.of("olives"));
        Pizza pizza = new Pizza(toppings);

        toppings.add("mushroom");
        toppings.clear();

        assertEquals(List.of("olives"), pizza.getToppings());
    }

    @Test
    void getToppingsIsUnmodifiable() {
        Pizza pizza = new Pizza(List.of("olives"));
        assertThrows(UnsupportedOperationException.class, () -> pizza.getToppings().add("mushroom"));
    }
}

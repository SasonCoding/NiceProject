package com.nice.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void gettersReturnConstructorValues() {
        Dish dish = new Pasta(PastaType.PENNE, SauceType.TOMATO);
        Order order = new Order(1L, "Alice", " TLV ", "tlv", dish);

        assertEquals(1L, order.getId());
        assertEquals("Alice", order.getCustomerName());
        assertEquals(" TLV ", order.getRawCity());
        assertEquals("tlv", order.getNormalizedCity());
        assertEquals(dish, order.getDish());
    }

    @Test
    void toStringIncludesIdCustomerCityAndDishDescription() {
        Dish dish = new Pizza(List.of("mushroom"));
        Order order = new Order(7L, "Bob", "Netanya", "netanya", dish);

        String text = order.toString();

        assertTrue(text.contains("7"));
        assertTrue(text.contains("Bob"));
        assertTrue(text.contains("Netanya"));
        assertTrue(text.contains(dish.describe()));
    }

    @Test
    void constructorRejectsNullRequiredFields() {
        Dish dish = new Pasta(PastaType.SPAGHETTI, SauceType.TOMATO);

        assertThrows(NullPointerException.class, () -> new Order(1L, null, "TLV", "tlv", dish));
        assertThrows(NullPointerException.class, () -> new Order(1L, "Alice", null, "tlv", dish));
        assertThrows(NullPointerException.class, () -> new Order(1L, "Alice", "TLV", null, dish));
        assertThrows(NullPointerException.class, () -> new Order(1L, "Alice", "TLV", "tlv", null));
    }

    @Test
    void mutatingListPassedIntoPizzaDoesNotAffectOrderDish() {
        List<String> toppings = new ArrayList<>(List.of("olives"));
        Pizza pizza = new Pizza(toppings);
        Order order = new Order(1L, "Alice", "TLV", "tlv", pizza);

        toppings.add("mushroom");

        assertEquals(List.of("olives"), ((Pizza) order.getDish()).getToppings());
    }
}

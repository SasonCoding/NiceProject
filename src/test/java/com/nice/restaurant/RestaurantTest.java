package com.nice.restaurant;

import com.nice.model.Dish;
import com.nice.model.Order;
import com.nice.model.Pasta;
import com.nice.model.PastaType;
import com.nice.model.SauceType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestaurantTest {

    private static final Dish ANY_DISH = new Pasta(PastaType.PENNE, SauceType.TOMATO);

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void captureStdout() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void placeOrderReturnsOrderWithGivenDetails() {
        Restaurant restaurant = new Restaurant();

        Order order = restaurant.placeOrder("Alice", "TLV", ANY_DISH);

        assertEquals("Alice", order.getCustomerName());
        assertEquals("TLV", order.getRawCity());
        assertEquals(ANY_DISH, order.getDish());
    }

    @Test
    void printCurrentOrdersPrintsEveryWaitingOrder() {
        Restaurant restaurant = new Restaurant();
        restaurant.placeOrder("Alice", "TLV", ANY_DISH);
        restaurant.placeOrder("Bob", "Netanya", ANY_DISH);

        restaurant.printCurrentOrders();

        String output = capturedOut.toString();
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
    }

    @Test
    void printCurrentOrdersHandlesEmptyStoreGracefully() {
        Restaurant restaurant = new Restaurant();

        restaurant.printCurrentOrders();

        assertTrue(capturedOut.toString().contains("No current orders"));
    }

    @Test
    void dispatchNextBatchGroupsSameCityAndRemovesFromWaitingList() {
        Restaurant restaurant = new Restaurant();
        restaurant.placeOrder("A", "TLV", ANY_DISH);
        restaurant.placeOrder("B", "Kfar Saba", ANY_DISH);
        restaurant.placeOrder("C", "Netanya", ANY_DISH);
        restaurant.placeOrder("D", "TLV", ANY_DISH);

        List<Order> batch = restaurant.dispatchNextBatch();

        assertEquals(3, batch.size());
        assertEquals("TLV", batch.get(0).getRawCity());
        assertEquals("TLV", batch.get(1).getRawCity());
        assertEquals("Kfar Saba", batch.get(2).getRawCity());
        assertTrue(capturedOut.toString().contains("Dispatching delivery batch"));
    }

    @Test
    void dispatchNextBatchOnEmptyStorePrintsNoOrdersMessage() {
        Restaurant restaurant = new Restaurant();

        List<Order> batch = restaurant.dispatchNextBatch();

        assertTrue(batch.isEmpty());
        assertTrue(capturedOut.toString().contains("No orders to dispatch"));
    }

    @Test
    void constructorRejectsNullOrderStore() {
        assertThrows(NullPointerException.class, () -> new Restaurant(null));
    }
}

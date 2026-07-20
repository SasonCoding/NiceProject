package com.nice.dispatch;

import com.nice.model.Pasta;
import com.nice.model.PastaType;
import com.nice.model.SauceType;
import com.nice.restaurant.Restaurant;
import com.nice.store.OrderStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryDispatcherTest {

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
    void startPeriodicallyDispatchesWaitingOrders() throws InterruptedException {
        OrderStore store = new OrderStore();
        Restaurant restaurant = new Restaurant(store);
        restaurant.placeOrder("Alice", "TLV", new Pasta(PastaType.PENNE, SauceType.TOMATO));

        DeliveryDispatcher dispatcher = new DeliveryDispatcher(restaurant);
        try {
            dispatcher.start(50, TimeUnit.MILLISECONDS);
            Thread.sleep(250);

            assertTrue(store.getCurrentOrders().isEmpty(), "the waiting order should have been dispatched");
            assertTrue(capturedOut.toString().contains("Dispatching delivery batch"));
        } finally {
            dispatcher.stop();
        }
    }

    @Test
    void startTwiceThrowsIllegalStateException() {
        Restaurant restaurant = new Restaurant();
        DeliveryDispatcher dispatcher = new DeliveryDispatcher(restaurant);
        try {
            dispatcher.start(1, TimeUnit.HOURS);
            assertThrows(IllegalStateException.class, () -> dispatcher.start(1, TimeUnit.HOURS));
        } finally {
            dispatcher.stop();
        }
    }

    @Test
    void stopIsSafeToCallWithoutHavingStarted() {
        Restaurant restaurant = new Restaurant();
        DeliveryDispatcher dispatcher = new DeliveryDispatcher(restaurant);
        dispatcher.stop();
    }
}

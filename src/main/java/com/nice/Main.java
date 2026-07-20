package com.nice;

import com.nice.dispatch.DeliveryDispatcher;
import com.nice.model.Pasta;
import com.nice.model.PastaType;
import com.nice.model.Pizza;
import com.nice.model.SauceType;
import com.nice.restaurant.Restaurant;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Small runnable demo of the restaurant order/dispatch system, split into a
 * few independent scenarios:
 * <ol>
 *   <li>The "TLV, KfarSaba, Netanya, TLV, TLV" worked example from the
 *       requirements, dispatched manually until nothing is left.</li>
 *   <li>City-key normalization: "TLV" and " tlv " are grouped into the same
 *       batch even though they differ in case/whitespace.</li>
 *   <li>A single city with more waiting orders than the batch capacity,
 *       showing the leftovers correctly carrying over to a second cycle.</li>
 *   <li>The real {@link DeliveryDispatcher} scheduler, actually running on a
 *       timer (a short interval here, just to keep the demo fast) instead of
 *       being dispatched manually.</li>
 * </ol>
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("NICEProject up and running on Java " + System.getProperty("java.version"));

        runWorkedExampleScenario();
        runCityNormalizationScenario();
        runCityOverflowScenario();
        runLiveSchedulerScenario();
    }

    /** Manual dispatch of the requirements' worked example, until the store is empty. */
    private static void runWorkedExampleScenario() {
        System.out.println();
        System.out.println("=== Scenario 1: worked example (manual dispatch) ===");

        Restaurant restaurant = new Restaurant();
        restaurant.placeOrder("Alice", "TLV", new Pasta(PastaType.PENNE, SauceType.ROSA));
        restaurant.placeOrder("Bob", "Kfar Saba", new Pizza(List.of()));
        restaurant.placeOrder("Carol", "Netanya", new Pasta(PastaType.SPAGHETTI, SauceType.TOMATO));
        restaurant.placeOrder("Dan", "TLV", new Pizza(List.of("mushroom", "olives")));
        restaurant.placeOrder("Eve", "TLV", new Pasta(PastaType.TORTELLINI, SauceType.MUSHROOM_CREAM));

        System.out.println();
        restaurant.printCurrentOrders();

        System.out.println();
        dispatchUntilEmpty(restaurant);
    }

    /** Orders for the same city typed with different case/whitespace should still group together. */
    private static void runCityNormalizationScenario() {
        System.out.println();
        System.out.println("=== Scenario 2: city-key normalization (\"TLV\" vs \" tlv \") ===");

        Restaurant restaurant = new Restaurant();
        restaurant.placeOrder("Frank", "TLV", new Pasta(PastaType.PENNE, SauceType.TOMATO));
        restaurant.placeOrder("Grace", "Netanya", new Pizza(List.of()));
        restaurant.placeOrder("Heidi", " tlv ", new Pizza(List.of("olives")));

        System.out.println();
        restaurant.printCurrentOrders();

        System.out.println();
        dispatchUntilEmpty(restaurant);
    }

    /** A city with more waiting orders than the batch cap (3) spans two dispatch cycles. */
    private static void runCityOverflowScenario() {
        System.out.println();
        System.out.println("=== Scenario 3: one city with more orders than the batch cap ===");

        Restaurant restaurant = new Restaurant();
        for (int i = 1; i <= 5; i++) {
            restaurant.placeOrder("Customer" + i, "TLV", new Pasta(PastaType.SPAGHETTI, SauceType.TOMATO));
        }

        System.out.println();
        restaurant.printCurrentOrders();

        System.out.println();
        dispatchUntilEmpty(restaurant);
    }

    /** Runs the real DeliveryDispatcher scheduler on a short interval, instead of dispatching manually. */
    private static void runLiveSchedulerScenario() {
        System.out.println();
        System.out.println("=== Scenario 4: live DeliveryDispatcher scheduler (1-second interval) ===");

        Restaurant restaurant = new Restaurant();
        restaurant.placeOrder("Ivan", "Raanana", new Pasta(PastaType.PENNE, SauceType.MUSHROOM_CREAM));
        restaurant.placeOrder("Judy", "Raanana", new Pizza(List.of("pepperoni")));
        restaurant.placeOrder("Mallory", "Netanya", new Pasta(PastaType.TORTELLINI, SauceType.ROSA));

        System.out.println();
        restaurant.printCurrentOrders();

        DeliveryDispatcher dispatcher = new DeliveryDispatcher(restaurant);
        try {
            System.out.println();
            dispatcher.start(1, TimeUnit.SECONDS);
            Thread.sleep(3_500); // let the scheduler fire a couple of times
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            dispatcher.stop();
        }

        System.out.println();
        restaurant.printCurrentOrders();
    }

    private static void dispatchUntilEmpty(Restaurant restaurant) {
        int batchNumber = 1;
        List<?> lastBatch;
        do {
            System.out.println("--- Dispatch cycle " + batchNumber + " ---");
            lastBatch = restaurant.dispatchNextBatch();
            batchNumber++;
        } while (!lastBatch.isEmpty());
    }
}

package com.nice;

import com.nice.model.Pasta;
import com.nice.model.PastaType;
import com.nice.model.Pizza;
import com.nice.model.SauceType;
import com.nice.restaurant.Restaurant;

import java.util.List;

/**
 * Small runnable demo of the restaurant order/dispatch system: places a
 * handful of orders (matching the "TLV, KfarSaba, Netanya, TLV, TLV"
 * worked example from the requirements, plus a couple of dish variations),
 * prints what's currently waiting, then repeatedly dispatches batches until
 * nothing is left. This calls {@link Restaurant#dispatchNextBatch()}
 * directly/manually — it does not start the real {@code DeliveryDispatcher}
 * scheduler, so the demo runs instantly instead of waiting for a timer.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("NICEProject up and running on Java " + System.getProperty("java.version"));
        System.out.println();

        Restaurant restaurant = new Restaurant();

        restaurant.placeOrder("Alice", "TLV", new Pasta(PastaType.PENNE, SauceType.ROSA));
        restaurant.placeOrder("Bob", "Kfar Saba", new Pizza(List.of()));
        restaurant.placeOrder("Carol", "Netanya", new Pasta(PastaType.SPAGHETTI, SauceType.TOMATO));
        restaurant.placeOrder("Dan", "TLV", new Pizza(List.of("mushroom", "olives")));
        restaurant.placeOrder("Eve", "TLV", new Pasta(PastaType.TORTELLINI, SauceType.MUSHROOM_CREAM));

        System.out.println();
        restaurant.printCurrentOrders();

        System.out.println();
        int batchNumber = 1;
        List<?> lastBatch;
        do {
            System.out.println("--- Dispatch cycle " + batchNumber + " ---");
            lastBatch = restaurant.dispatchNextBatch();
            batchNumber++;
        } while (!lastBatch.isEmpty());
    }
}

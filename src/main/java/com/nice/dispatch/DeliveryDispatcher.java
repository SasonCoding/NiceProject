package com.nice.dispatch;

import com.nice.restaurant.Restaurant;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodically asks a {@link Restaurant} to dispatch its next delivery
 * batch — the "every five minutes" cadence described in the requirements.
 *
 * <p>The requirements only ask for the dispatch function itself (see
 * {@link Restaurant#dispatchNextBatch()}); this scheduler is an optional
 * convenience for running the system continuously (e.g. from {@code main})
 * and is not exercised by the core dispatch-selection tests. It is
 * decoupled from {@link com.nice.store.OrderStore}'s locking: each tick
 * simply calls the same public {@code dispatchNextBatch()} method any other
 * caller would use, so incoming orders and scheduled dispatches remain
 * correctly synchronized via the store's own lock.
 */
public final class DeliveryDispatcher {

    private final Restaurant restaurant;
    private final ScheduledExecutorService executor;
    private volatile ScheduledFuture<?> scheduledTask;

    public DeliveryDispatcher(Restaurant restaurant) {
        this(restaurant, Executors.newSingleThreadScheduledExecutor());
    }

    /** Package-visible constructor allowing tests to inject a controllable executor. */
    DeliveryDispatcher(Restaurant restaurant, ScheduledExecutorService executor) {
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Starts dispatching a batch every {@code period} of the given
     * {@code unit}, with the first dispatch happening after one full period
     * elapses.
     *
     * @throws IllegalStateException if already running
     */
    public synchronized void start(long period, TimeUnit unit) {
        if (scheduledTask != null) {
            throw new IllegalStateException("DeliveryDispatcher is already running");
        }
        scheduledTask = executor.scheduleAtFixedRate(restaurant::dispatchNextBatch, period, period, unit);
    }

    /**
     * Stops future scheduled dispatches. A dispatch already in progress, if
     * any, is allowed to finish. Safe to call even if not running.
     */
    public synchronized void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        executor.shutdown();
    }
}

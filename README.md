# NICEProject — Restaurant Order & Delivery Dispatch

A small Java 17 / Maven application modelling a restaurant's ordering system: customers
place orders first-come-first-served, and deliveries go out in batches of up to **3 orders**,
grouping orders headed to the same city so a courier doesn't drive to the same place twice.

## Domain model

| Type | Description |
| --- | --- |
| `Dish` | Abstract base for anything orderable — exposes `getPrice()` and `describe()`. |
| `Pasta` | A `PastaType` + `SauceType`. Fixed price of 50 regardless of combination. |
| `Pizza` | A list of free-text toppings. 30 without toppings, 45 with. Toppings are defensively copied. |
| `Order` | Who (customer), where (city), what (dish), plus an arrival id. Keeps both the raw city as typed and a normalized key. |

## How batching works

`OrderStore.selectNextBatch()` walks the waiting orders in arrival (FIFO) order:

1. The first not-yet-selected order becomes the **pivot** and joins the batch.
2. Every other still-waiting order for that **same city** is greedily pulled in, in its own
   arrival order, until that city is exhausted or the batch is full.
3. If there's still room, the FIFO walk continues to the next pivot.

Cities are matched on a normalized key (trimmed + lower-cased), so `"TLV"` and `" tlv "`
group together. Orders that don't fit carry over to the next dispatch cycle.

### Worked example

Orders arriving as `TLV, Kfar Saba, Netanya, TLV, TLV` dispatch as:

- **Batch 1** — TLV, TLV, TLV (the pivot plus both later TLV orders pulled forward)
- **Batch 2** — Kfar Saba, Netanya

## Architecture

```
com.nice
├── Main                        — runnable demo of four scenarios
├── model/                      — Dish, Pasta, Pizza, Order, PastaType, SauceType
├── store/OrderStore            — storage + batch selection (pure data/algorithm, no I/O)
├── restaurant/Restaurant       — public facade: placeOrder, printCurrentOrders, dispatchNextBatch
└── dispatch/DeliveryDispatcher — optional scheduler, dispatches on a fixed interval
```

`Restaurant` is the facade callers talk to; it delegates all storage and selection to
`OrderStore`, so the internal representation can change without breaking the public API.

**Data structures.** `OrderStore` keeps a `LinkedHashMap` of orders by id (O(1) add/get/remove
while preserving arrival order across removals) and a city → ordered set of ids index (O(1)
lookup of a city's other waiting orders). All operations are guarded by a single
`ReentrantLock`; every critical section is O(1), so hold times are negligible.

`DeliveryDispatcher` wraps a `ScheduledExecutorService` to call `dispatchNextBatch()` on a
fixed cadence (the "every five minutes" behaviour from the requirements). It goes through the
same public method any other caller would, so incoming orders and scheduled dispatches stay
correctly synchronized by the store's own lock.

## Requirements

- JDK 17+
- Maven 3.6+

## Build, test, run

```bash
mvn test              # run the JUnit 5 test suite
mvn package           # build an executable jar into target/
java -jar target/NICEProject-1.0.0.jar
```

## The demo

`Main` runs four independent scenarios:

1. The worked example above, dispatched manually until the store is empty.
2. City-key normalization — `"TLV"` and `" tlv "` landing in the same batch.
3. A single city with 5 waiting orders, showing leftovers carrying into a second cycle.
4. The real `DeliveryDispatcher` running on a live 1-second timer (shortened for the demo).

## Tests

JUnit 5 tests cover the model, the store's batch-selection algorithm, the `Restaurant`
facade, the scheduler (with an injected executor), and the demo entry point.

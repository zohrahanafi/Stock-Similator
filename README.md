# Stock-Simulator

This implementation simulates a stock exchange where buy and sell orders for various tickers (stocks) are added, matched, and processed concurrently.

## Requirments: 
- **Java 17+** (for record amd virtual threads).

## Dependencies
 - **Java Standard Library**: Utilizes Java's built-in `ConcurrentSkipListSet` and `ExecutorService`.

## How to Run
1. Navigate into the project directory
2. Compile and run the Java program:
   
   On a Terminal, if you have Java 17 or later installed, use the following commands to compile and run the program:
   
   `javac StockTradingSimulator.java`
   
    `java StockTradingSimulator`
   
## Components

### 1. `Order` (Record)
A `record` class that defines an order in the system. Each order contains:
- `OrderType`: Type of order (`BUY` or `SELL`).
- `ticker`: Stock symbol (e.g., AAPL, GOOG ).
- `quantity`: Number of shares being bought or sold.
- `price`: The price at which the order is placed.
- `timestamp`: Time the order was created, automatically set during creation.

The `Order` class implements `Comparable<Order>`, ensuring that orders are sorted by ticker, price, and timestamp.

### 2. `StockExchange` (Class)
The core class responsible for handling the stock exchange operations:
- It maintains two `ConcurrentSkipListSet` arrays: one for `buyOrders` and one for `sellOrders`, sorted by price and timestamp.
- It supports adding new orders and matching buy and sell orders based on price and timestamp.

### 3. `StockTradingSimulator` (Class)
This class simulates the generation of orders and the operation of the stock exchange:
- It uses a `ExecutorService` to simulate concurrent order generation.
- Random orders are created and added to the exchange for a set of stock tickers (`AAPL`, `GOOG`, etc.).

## How It Works

1. **Adding Orders**: 
   - Buy and sell orders are added to the system with randomly generated data (ticker, order type, quantity, price).
   
2. **Order Matching**: 
   - After each new order is added, the system attempts to match buy and sell orders:
     - For a `BUY` order, the highest prices are matched.
     - For a `SELL` order, the lowest prices are matched.
     - If a match is found, the order is either partially or fully filled, and the remaining quantity is updated.

3. **Concurrency**:
   - The simulation uses virtual threads to handle the order generation process concurrently. This mimics the behavior of multiple users submitting orders at the same time.

4. **Output**:
   - The system prints details about matched orders, such as:
     - Matched quantity of shares.
     - Matched ticker symbol.
     - Matched price.

5. **Active Orders Count**:
   - The total number of active orders in the system is tracked and printed at the end of the simulation.

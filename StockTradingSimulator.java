import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentSkipListSet;

// we will use record for immutability
 record Order(OrderType type, String ticker, int quantity, double price, long timestamp) implements Comparable<Order> {
    // enum to define the type of the order: BUY or SELL
    public enum OrderType { BUY, SELL }

    // constructor
    public Order {
        // to ensure that the timestamp is set when creating the order
        timestamp = System.nanoTime();
    }

    @Override
    public int compareTo(Order other) {
        // compare by ticker first (grouping orders)
        int tickerComparison = this.ticker.compareTo(other.ticker);
        if (tickerComparison != 0) return tickerComparison;

        // if it's a BUY order, prioritize higher price and earlier timestamp
        if (this.type == OrderType.BUY) {
            int priceComparison = Double.compare(other.price, this.price); // higher price for BUY orders
            return priceComparison != 0 ? priceComparison : Long.compare(this.timestamp, other.timestamp);
        } else { // for SELL orders, prioritize lower price and earlier timestamp
            int priceComparison = Double.compare(this.price, other.price); // lower price for SELL orders
            return priceComparison != 0 ? priceComparison : Long.compare(this.timestamp, other.timestamp);
        }
    }
}

class StockExchange {
    private static final int MAX_TICKERS = 1024; // max number of tickers to handle
    private final ConcurrentSkipListSet<Order>[] buyOrders; // array of buy orders for each ticker
    private final ConcurrentSkipListSet<Order>[] sellOrders; // array of sell orders for each ticker
    private final AtomicInteger activeOrders = new AtomicInteger(); // keeps track of active orders

    // constructor to initialize the order books
    @SuppressWarnings("unchecked")
    public StockExchange() {
        buyOrders = new ConcurrentSkipListSet[MAX_TICKERS];
        sellOrders = new ConcurrentSkipListSet[MAX_TICKERS];

        // initialize order books for each ticker
        for (int i = 0; i < MAX_TICKERS; i++) {
            buyOrders[i] = new ConcurrentSkipListSet<>();
            sellOrders[i] = new ConcurrentSkipListSet<>();
        }
    }

    // calculates an index for the ticker (to map to the correct order book)
    private int getTickerIndex(String ticker) {
        return Math.abs(ticker.hashCode() % MAX_TICKERS); // hash the ticker and mod by MAX_TICKERS for efficient mapping
    }

    // to add an order to the appropriate order book (buy or sell)
    public void addOrder(Order.OrderType type, String ticker, int quantity, double price) {
        int index = getTickerIndex(ticker); // Get the order book index based on ticker
        Order order = new Order(type, ticker, quantity, price, 0); // Create the order (timestamp is auto-generated)

        // increment the active order count
        activeOrders.incrementAndGet();

        // add the order to the appropriate book based on its type (BUY or SELL)
        try {
            if (type == Order.OrderType.BUY) {
                buyOrders[index].add(order);
            } else {
                sellOrders[index].add(order);
            }

            // try to match orders after adding a new one
            matchOrders(index);
        } catch (Exception e) {
            // catch any exception that might occur and print an error message
            System.err.println("Error adding order: " + e.getMessage());
        }
    }

    // to match buy and sell orders at the given ticker index
    private void matchOrders(int index) {
        try {
            // loop until there are no more matching buy and sell orders
            while (!buyOrders[index].isEmpty() && !sellOrders[index].isEmpty()) {
                Order buy = buyOrders[index].first(); // get the highest priority buy order
                Order sell = sellOrders[index].first(); // get the lowest priority sell order

                // if the buy price is greater than or equal to the sell price, they can be matched
                if (buy.price() >= sell.price()) {
                    int matchedQty = Math.min(buy.quantity(), sell.quantity()); // the max quantity that can be matched

                    // print match details
                    System.out.println("Matched: " + matchedQty + " shares of " + buy.ticker() + " at $" + sell.price());

                    // update the buy order if it wasn't fully matched
                    if (buy.quantity() > matchedQty) {
                        buyOrders[index].pollFirst(); // remove the matched buy order
                        buyOrders[index].add(new Order(buy.type(), buy.ticker(), buy.quantity() - matchedQty, buy.price(), buy.timestamp())); // add the remaining portion
                    } else {
                        buyOrders[index].pollFirst(); // remove the fully matched buy order
                    }

                    // same thing with sell
                    if (sell.quantity() > matchedQty) {
                        sellOrders[index].pollFirst(); 
                        sellOrders[index].add(new Order(sell.type(), sell.ticker(), sell.quantity() - matchedQty, sell.price(), sell.timestamp())); 
                    } else {
                        sellOrders[index].pollFirst(); 
                    }

                    // decrement the active orders count
                    activeOrders.decrementAndGet();
                } else {
                    break; // no more orders can be matched if buy price < sell price
                }
            }
        } catch (Exception e) {
            System.err.println("Error matching orders: " + e.getMessage());
        }
    }

    // returns the number of active orders
    public int getActiveOrders() {
        return activeOrders.get();
    }
}

public class StockTradingSimulator {
    public static void main(String[] args) {
        StockExchange exchange = new StockExchange(); 
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Executor to handle orders in parallel

        // Runnable task to simulate order generation
        Runnable orderGenerator = () -> {
            String[] tickers = {"AAPL", "GOOG", "AMZN", "MSFT", "TSLA"}; // list of tickers to simulate
            for (int i = 0; i < 100; i++) {
                String ticker = tickers[(int) (Math.random() * tickers.length)]; // randomly select a ticker
                Order.OrderType type = Math.random() > 0.5 ? Order.OrderType.BUY : Order.OrderType.SELL; // randomly select order type (BUY/SELL)
                int quantity = (int) (Math.random() * 100) + 1; // random order quantity (1 to 100)
                double price = 100 + Math.random() * 50; // random order price (100 to 150)

                // add the generated order to the exchange
                exchange.addOrder(type, ticker, quantity, price);
            }
        };

        // submit multiple order generator tasks to simulate concurrent orders
        executor.submit(orderGenerator);
        executor.submit(orderGenerator);
        executor.submit(orderGenerator);

        // shut down the executor service after order generation
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // force shutdown if tasks exceed the time limit
            }
        } catch (InterruptedException e) {
            executor.shutdownNow(); // handle interruption gracefully
        }

        // print the number of active orders at the end of simulation
        System.out.println("Total Active Orders: " + exchange.getActiveOrders());
    }
}

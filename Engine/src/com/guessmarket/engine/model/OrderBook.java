package com.guessmarket.engine.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The open orders of a single outcome, kept sorted by price-time priority.
 * The matching algorithm itself lives in {@link OrderMatcher}; this class only stores and orders the data.
 */
public class OrderBook {
    private final List<Order> bids = new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();
    private Double lastTradePrice = null;

    public List<Order> getBids() {
        return new ArrayList<>(bids);
    }

    public List<Order> getAsks() {
        return new ArrayList<>(asks);
    }

    // Bids are kept sorted from the highest price, so the best bid is always first (null when empty).
    public Double getBestBidPrice() {
        Order bestBid = getBestBid();
        return (bestBid == null) ? null : bestBid.getPrice();
    }

    // Asks are kept sorted from the lowest price, so the best ask is always first (null when empty).
    public Double getBestAskPrice() {
        Order bestAsk = getBestAsk();
        return (bestAsk == null) ? null : bestAsk.getPrice();
    }

    public Double getLastTradePrice() {
        return lastTradePrice;
    }

    // Shares this user already promised to sell in open orders; they cannot be offered again.
    public double getOpenSellShares(String userName) {
        double total = 0.0;
        for (Order ask : asks) {
            if (ask.getUserName().equals(userName)) {
                total += ask.getSharesCount();
            }
        }
        return total;
    }

    // Removes every open order (used when the event closes) and returns them, so reservations can be released.
    public List<Order> cancelAllOrders() {
        List<Order> cancelled = new ArrayList<>(bids);
        cancelled.addAll(asks);
        bids.clear();
        asks.clear();
        return cancelled;
    }

    // --- Package-private operations, used only by OrderMatcher (same package) ---

    Order getBestBid() {
        return bids.isEmpty() ? null : bids.get(0);
    }

    Order getBestAsk() {
        return asks.isEmpty() ? null : asks.get(0);
    }

    void addOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.add(order);
            sortBids();
        } else {
            asks.add(order);
            sortAsks();
        }
    }

    void removeFilledOrders() {
        bids.removeIf(Order::isFilled);
        asks.removeIf(Order::isFilled);
    }

    void recordTradePrice(double price) {
        this.lastTradePrice = price;
    }

    // Price priority first; for equal prices, the order that arrived first (lower sequence number) wins.
    private void sortBids() {
        bids.sort(Comparator.comparingDouble(Order::getPrice).reversed()
                .thenComparingLong(Order::getSequenceNumber));
    }

    private void sortAsks() {
        asks.sort(Comparator.comparingDouble(Order::getPrice)
                .thenComparingLong(Order::getSequenceNumber));
    }
}

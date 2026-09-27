package com.guessmarket.engine.model;

import java.io.Serializable;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String userName;
    private final String eventName;
    private final String outcomeTitle;
    private final OrderSide side;         // BUY / SELL
    private final double price;
    private double sharesCount;
    private final double originalSharesCount;
    private final long timestamp;

    public Order(String id, String userName, String eventName, String outcomeTitle,
                 OrderSide side, double price, double sharesCount) {
        this.id = id;
        this.userName = userName;
        this.eventName = eventName;
        this.outcomeTitle = outcomeTitle;
        this.side = side;
        this.price = price;
        this.sharesCount = sharesCount;
        this.originalSharesCount = sharesCount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getEventName() { return eventName; }
    public String getOutcomeTitle() { return outcomeTitle; }
    public OrderSide getSide() { return side; }
    public double getPrice() { return price; }
    public double getSharesCount() { return sharesCount; }
    public double getOriginalSharesCount() { return originalSharesCount; }
    public long getTimestamp() { return timestamp; }

    public void reduceShares(double amount) {
        this.sharesCount -= amount;
    }

    public boolean isFilled() {
        return this.sharesCount <= 0.00001;
    }
}
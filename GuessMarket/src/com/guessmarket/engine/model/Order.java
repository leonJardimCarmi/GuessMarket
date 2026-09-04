package com.guessmarket.engine.model;

import java.io.Serializable;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String userName;
    private final String eventId;
    private final String outcomeTitle;
    private final OrderSide side;         // BUY / SELL
    private final OrderActionType actionType; // LMT / FOK / IOC
    private final double price;
    private double sharesCount;
    private final double originalSharesCount;
    private final long timestamp;

    public Order(String id, String userName, String eventId, String outcomeTitle,
                 OrderSide side, OrderActionType actionType, double price, double sharesCount) {
        this.id = id;
        this.userName = userName;
        this.eventId = eventId;
        this.outcomeTitle = outcomeTitle;
        this.side = side;
        this.actionType = actionType;
        this.price = price;
        this.sharesCount = sharesCount;
        this.originalSharesCount = sharesCount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getEventId() { return eventId; }
    public String getOutcomeTitle() { return outcomeTitle; }
    public OrderSide getSide() { return side; }
    public OrderActionType getActionType() { return actionType; }
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
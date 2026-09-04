package com.guessmarket.engine.dto;

import java.io.Serializable;

public class OrderDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String userName;
    private final String eventId;
    private final String outcomeTitle;
    private final String side;       // "BUY" / "SELL"
    private final String actionType; // "LMT" / "FOK" / "IOC"
    private final double price;
    private final double remainingShares;
    private final double originalShares;
    private final long timestamp;

    public OrderDto(String id, String userName, String eventId, String outcomeTitle,
                    String side, String actionType, double price,
                    double remainingShares, double originalShares, long timestamp) {
        this.id = id;
        this.userName = userName;
        this.eventId = eventId;
        this.outcomeTitle = outcomeTitle;
        this.side = side;
        this.actionType = actionType;
        this.price = price;
        this.remainingShares = remainingShares;
        this.originalShares = originalShares;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getEventId() { return eventId; }
    public String getOutcomeTitle() { return outcomeTitle; }
    public String getSide() { return side; }
    public String getActionType() { return actionType; }
    public double getPrice() { return price; }
    public double getRemainingShares() { return remainingShares; }
    public double getOriginalShares() { return originalShares; }
    public long getTimestamp() { return timestamp; }
}
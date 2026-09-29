package com.guessmarket.dto;

public class OrderDto {
    private final String id;
    private final String userName;
    private final String eventName;
    private final String outcomeTitle;
    private final String side;       // "BUY" / "SELL"
    private final double price;
    private final double remainingShares;
    private final double originalShares;
    private final long timestamp;

    public OrderDto(String id, String userName, String eventName, String outcomeTitle,
                    String side, double price,
                    double remainingShares, double originalShares, long timestamp) {
        this.id = id;
        this.userName = userName;
        this.eventName = eventName;
        this.outcomeTitle = outcomeTitle;
        this.side = side;
        this.price = price;
        this.remainingShares = remainingShares;
        this.originalShares = originalShares;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getEventName() { return eventName; }
    public String getOutcomeTitle() { return outcomeTitle; }
    public String getSide() { return side; }
    public double getPrice() { return price; }
    public double getRemainingShares() { return remainingShares; }
    public double getOriginalShares() { return originalShares; }
    public long getTimestamp() { return timestamp; }
}
package com.guessmarket.engine.dto;

import java.io.Serializable;
import java.util.List;

public class OrderBookDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String eventName;
    private final String outcomeTitle;
    private final List<OrderDto> buyOrders;  // Bids
    private final List<OrderDto> sellOrders; // Asks

    public OrderBookDto(String eventName, String outcomeTitle,
                        List<OrderDto> buyOrders, List<OrderDto> sellOrders) {
        this.eventName = eventName;
        this.outcomeTitle = outcomeTitle;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
    }

    public String getEventName() { return eventName; }
    public String getOutcomeTitle() { return outcomeTitle; }
    public List<OrderDto> getBuyOrders() { return buyOrders; }
    public List<OrderDto> getSellOrders() { return sellOrders; }
}
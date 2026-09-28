package com.guessmarket.engine.dto;

import java.io.Serializable;
import java.util.List;

public class OrderBookDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String eventName;
    private final String outcomeTitle;
    private final List<OrderDto> buyOrders;  // Bids
    private final List<OrderDto> sellOrders; // Asks

    // Market statistics; null means "no value" (e.g. no trade yet, or an empty side of the book)
    private final Double lastPrice;
    private final Double bestBid;
    private final Double bestAsk;
    private final Double midPrice;
    private final Double spread;

    public OrderBookDto(String eventName, String outcomeTitle,
                        List<OrderDto> buyOrders, List<OrderDto> sellOrders,
                        Double lastPrice, Double bestBid, Double bestAsk, Double midPrice, Double spread) {
        this.eventName = eventName;
        this.outcomeTitle = outcomeTitle;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
        this.lastPrice = lastPrice;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.midPrice = midPrice;
        this.spread = spread;
    }

    public String getEventName() { return eventName; }
    public String getOutcomeTitle() { return outcomeTitle; }
    public List<OrderDto> getBuyOrders() { return buyOrders; }
    public List<OrderDto> getSellOrders() { return sellOrders; }
    public Double getLastPrice() { return lastPrice; }
    public Double getBestBid() { return bestBid; }
    public Double getBestAsk() { return bestAsk; }
    public Double getMidPrice() { return midPrice; }
    public Double getSpread() { return spread; }
}

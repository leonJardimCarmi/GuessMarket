package com.guessmarket.dto;

import java.util.Map;

/**
 * One participant of an event, as every user sees it: how many shares of each outcome they hold and what they are
 * worth at the current price. A participant is anyone who acted in the event (an order - even one that was never
 * executed -, a purchase, or opening it as market maker).
 */
public class ParticipantDto {
    private final String name;
    private final boolean marketMaker;
    private final Map<String, Double> holdings;       // outcome -> shares held
    private final Map<String, Double> holdingValues;  // outcome -> shares * current price
    private final double totalValue;

    public ParticipantDto(String name, boolean marketMaker, Map<String, Double> holdings,
                          Map<String, Double> holdingValues, double totalValue) {
        this.name = name;
        this.marketMaker = marketMaker;
        this.holdings = holdings;
        this.holdingValues = holdingValues;
        this.totalValue = totalValue;
    }

    public String getName() {
        return name;
    }

    public boolean isMarketMaker() {
        return marketMaker;
    }

    public Map<String, Double> getHoldings() {
        return holdings;
    }

    public Map<String, Double> getHoldingValues() {
        return holdingValues;
    }

    public double getTotalValue() {
        return totalValue;
    }
}

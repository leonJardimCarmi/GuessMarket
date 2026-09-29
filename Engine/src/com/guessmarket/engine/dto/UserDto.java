package com.guessmarket.engine.dto;

import java.util.List;
import java.util.Map;

public class UserDto {
    private final String name;
    private final double balance;
    private final double reservedBalance;
    private final Map<String, Map<String, Double>> holdings;
    private final List<String> marketMakerEvents;
    private final List<String> participatedEvents;

    public UserDto(String name, double balance, double reservedBalance, Map<String, Map<String, Double>> holdings,
                   List<String> marketMakerEvents, List<String> participatedEvents) {
        this.name = name;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.holdings = holdings;
        this.marketMakerEvents = marketMakerEvents;
        this.participatedEvents = participatedEvents;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public double getReservedBalance() {
        return reservedBalance;
    }

    public double getAvailableBalance() {
        return balance - reservedBalance;
    }

    public Map<String, Map<String, Double>> getHoldings() {
        return holdings;
    }

    public List<String> getMarketMakerEvents() {
        return marketMakerEvents;
    }

    public boolean isMarketMaker() {
        return !marketMakerEvents.isEmpty();
    }

    public List<String> getParticipatedEvents() {
        return participatedEvents;
    }
}

package com.guessmarket.engine.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Account account;
    private final Map<String, Map<String, Double>> userHoldings = new HashMap<>();
    // One position per event the user takes part in, in the order they joined (key = event name)
    private final Map<String, Position> positions = new LinkedHashMap<>();

    public User(String name, double initialCash) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty.");
        }
        if (initialCash < 0) {
            throw new IllegalArgumentException("Initial cash cannot be negative.");
        }

        this.name = name.trim();
        this.account = new Account(initialCash);
    }

    public String getName() {
        return name;
    }

    public Account getAccount() {
        return account;
    }

    public double getSharesCount(String eventName, String outcomeTitle) {
        return userHoldings
                .getOrDefault(eventName, new HashMap<>())
                .getOrDefault(outcomeTitle, 0.0);
    }

    public void addShares(String eventName, String outcomeTitle, double shares) {
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares amount to add must be positive.");
        }
        userHoldings.computeIfAbsent(eventName, k -> new HashMap<>()).merge(outcomeTitle, shares, Double::sum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(name.toLowerCase(), user.name.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    public void deductShares(String eventName, String outcomeTitle, double shares) {
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares amount to deduct must be positive.");
        }

        double currentShares = getSharesCount(eventName, outcomeTitle);
        if (currentShares < shares) {
            throw new IllegalStateException("Insufficient shares to deduct. Available: " + currentShares + ", Requested: " + shares);
        }

        Map<String, Double> eventHoldings = userHoldings.get(eventName);
        double remainingShares = currentShares - shares;

        if (remainingShares > 0) {
            eventHoldings.put(outcomeTitle, remainingShares);
        } else {
            eventHoldings.remove(outcomeTitle);
            if (eventHoldings.isEmpty()) {
                userHoldings.remove(eventName);
            }
        }
    }

    public double getBalance() {
        return this.account.getBalance();
    }

    public Position getOrCreatePosition(String eventName) {
        return positions.computeIfAbsent(eventName, key -> new Position());
    }

    // Null when the user never took part in this event
    public Position getPosition(String eventName) {
        return positions.get(eventName);
    }

    public List<String> getParticipatedEventNames() {
        return List.copyOf(positions.keySet());
    }
}
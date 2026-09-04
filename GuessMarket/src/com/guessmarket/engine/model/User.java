package com.guessmarket.engine.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Account account;
    private final Map<String, Map<String, Double>> userHoldings = new HashMap<>();

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

    public double getSharesCount(String eventId, String outcomeTitle) {
        return userHoldings
                .getOrDefault(eventId, new HashMap<>())
                .getOrDefault(outcomeTitle, 0.0);
    }

    public void addShares(String eventId, String outcomeTitle, double shares) {
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares amount to add must be positive.");
        }
        userHoldings.computeIfAbsent(eventId, k -> new HashMap<>()).merge(outcomeTitle, shares, Double::sum);
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

    public void deductShares(String eventId, String outcomeTitle, double shares) {
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares amount to deduct must be positive.");
        }

        double currentShares = getSharesCount(eventId, outcomeTitle);
        if (currentShares < shares) {
            throw new IllegalStateException("Insufficient shares to deduct. Available: " + currentShares + ", Requested: " + shares);
        }

        Map<String, Double> eventHoldings = userHoldings.get(eventId);
        double remainingShares = currentShares - shares;

        if (remainingShares > 0) {
            eventHoldings.put(outcomeTitle, remainingShares);
        } else {
            eventHoldings.remove(outcomeTitle);
            if (eventHoldings.isEmpty()) {
                userHoldings.remove(eventId);
            }
        }
    }
}
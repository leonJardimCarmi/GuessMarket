package com.guessmarket.engine.model;

import java.io.Serializable;

public class AccountEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String description;
    private final double amount;
    private final double balanceAfter;
    private final long timestamp;

    public AccountEntry(String description, double amount, double balanceAfter) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Account entry description cannot be empty.");
        }
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = System.currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

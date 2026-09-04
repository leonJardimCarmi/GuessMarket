package com.guessmarket.engine.model;

import java.io.Serializable;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userName;
    private final String outcomeTitle;
    private final double shareAmount;
    private final double cost;
    private final double feePaid;

    public Transaction(String userName, String outcomeTitle, double shareAmount, double cost, double feePaid) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty.");
        }
        if (outcomeTitle == null || outcomeTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Outcome title cannot be empty.");
        }
        if (shareAmount <= 0) {
            throw new IllegalArgumentException("Share amount must be positive.");
        }
        if (cost < 0 || feePaid < 0) {
            throw new IllegalArgumentException("Cost and fee paid cannot be negative.");
        }

        this.userName = userName.trim();
        this.outcomeTitle = outcomeTitle.trim();
        this.shareAmount = shareAmount;
        this.cost = cost;
        this.feePaid = feePaid;
    }

    public String getUserName() {
        return userName;
    }

    public String getOutcomeTitle() {
        return outcomeTitle;
    }

    public double getShareAmount() {
        return shareAmount;
    }

    public double getCost() {
        return cost;
    }

    public double getFeePaid() {
        return feePaid;
    }

    public double getTotalPaid() {
        return cost + feePaid;
    }
}
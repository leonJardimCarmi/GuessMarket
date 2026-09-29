package com.guessmarket.dto;

public class AccountEntryDto {
    private final String description;
    private final double amount;
    private final double balanceAfter;
    private final long timestamp;

    public AccountEntryDto(String description, double amount, double balanceAfter, long timestamp) {
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
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

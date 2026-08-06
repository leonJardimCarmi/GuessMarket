package com.guessmarket.engine.dto;

import java.util.List;

public class MarketEventDto {
    private final String id;
    private final String title;
    private final String description;
    private final boolean isActive;
    private final String winningOutcome;
    private final double accountBalance;
    private final double totalFeesCollected;
    private final double feePercentage;
    private final String feeType;

    private final List<OutcomeDto> outcomes;
    private final List<TransactionDto> transactions;


    public MarketEventDto(String id, String title, String description, boolean isActive, String winningOutcome, double accountBalance,
                          double totalFeesCollected, double feePercentage, String feeType, List<OutcomeDto> outcomes,
                          List<TransactionDto> transactions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isActive = isActive;
        this.winningOutcome = winningOutcome;
        this.accountBalance = accountBalance;
        this.totalFeesCollected = totalFeesCollected;
        this.feePercentage = feePercentage;
        this.feeType = feeType;
        this.outcomes = outcomes;
        this.transactions = transactions;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getWinningOutcome() {
        return winningOutcome;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public double getTotalFeesCollected() {
        return totalFeesCollected;
    }

    public double getFeePercentage() {
        return feePercentage;
    }

    public String getFeeType() {
        return feeType;
    }

    public List<OutcomeDto> getOutcomes() {
        return outcomes;
    }

    public List<TransactionDto> getTransactions() {
        return transactions;
    }
}


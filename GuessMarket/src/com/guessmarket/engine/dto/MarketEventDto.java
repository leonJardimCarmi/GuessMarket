package com.guessmarket.engine.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class MarketEventDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String title;
    private final String description;
    private final boolean isActive;
    private final String winningOutcome;

    private final String marketMakerName;
    private final double marketMakerBalance;
    private final double totalFeesCollected;
    private final double feePercentage;
    private final String feeType;
    private final String tradingMethod;

    private final List<OutcomeDto> outcomes;
    private final List<TransactionDto> transactions;

    public MarketEventDto(String id, String title, String description, boolean isActive,
                          String winningOutcome, String marketMakerName, double marketMakerBalance,
                          double totalFeesCollected, double feePercentage, String feeType,
                          String tradingMethod, List<OutcomeDto> outcomes,
                          List<TransactionDto> transactions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isActive = isActive;
        this.winningOutcome = winningOutcome;
        this.marketMakerName = marketMakerName;
        this.marketMakerBalance = marketMakerBalance;
        this.totalFeesCollected = totalFeesCollected;
        this.feePercentage = feePercentage;
        this.feeType = feeType;
        this.tradingMethod = tradingMethod;
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

    public String getMarketMakerName() {
        return marketMakerName;
    }

    public double getMarketMakerBalance() {
        return marketMakerBalance;
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

    public String getTradingMethod() {
        return tradingMethod;
    }

    public List<OutcomeDto> getOutcomes() {
        return outcomes != null ? Collections.unmodifiableList(outcomes) : Collections.emptyList();
    }

    public List<TransactionDto> getTransactions() {
        return transactions != null ? Collections.unmodifiableList(transactions) : Collections.emptyList();
    }
}
package com.guessmarket.engine.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class MarketEventDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String description;
    private final String status;
    private final String winningOutcome;

    private final String marketMakerName;
    private final double eventBalance;
    private final double totalFeesCollected;
    private final double feePercentage;
    private final String feeType;
    private final String tradingMethod;

    private final List<OutcomeDto> outcomes;
    private final List<TransactionDto> transactions;

    private final double b;
    private final double d;

    public MarketEventDto(String name, String description, String status,
                          String winningOutcome, String marketMakerName, double eventBalance,
                          double totalFeesCollected, double feePercentage, String feeType,
                          String tradingMethod, List<OutcomeDto> outcomes,
                          List<TransactionDto> transactions, double B, double D) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.winningOutcome = winningOutcome;
        this.marketMakerName = marketMakerName;
        this.eventBalance = eventBalance;
        this.totalFeesCollected = totalFeesCollected;
        this.feePercentage = feePercentage;
        this.feeType = feeType;
        this.tradingMethod = tradingMethod;
        this.outcomes = outcomes;
        this.transactions = transactions;
        this.b =B;
        this.d = D;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getWinningOutcome() {
        return winningOutcome;
    }

    public String getMarketMakerName() {
        return marketMakerName;
    }

    public double getEventBalance() {return eventBalance;}

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

    public double getBParameter() {
        return b;
    }

}
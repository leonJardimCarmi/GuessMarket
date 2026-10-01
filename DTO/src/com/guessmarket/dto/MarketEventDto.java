package com.guessmarket.dto;

import java.util.Collections;
import java.util.List;

public class MarketEventDto {
    // The values of status, tradingMethod and feeType, as the server sends them (the names of the engine's enums).
    // static fields are not part of the JSON (Gson skips them), so the DTO stays plain data.
    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String METHOD_LMSR = "LMSR";
    public static final String METHOD_ORDER_BOOK = "ORDER_BOOK";
    public static final String FEE_AT_PURCHASE = "AT_PURCHASE";
    public static final String FEE_AT_CLOSE = "AT_RESOLUTION";

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

    public double getDParameter() {
        return d;
    }

}
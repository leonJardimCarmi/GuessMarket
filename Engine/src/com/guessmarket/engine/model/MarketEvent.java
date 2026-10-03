package com.guessmarket.engine.model;

import java.util.*;

public class MarketEvent {
    public enum FeeType {
        AT_PURCHASE,
        AT_RESOLUTION
    }

    public enum TradingMethod {
        LMSR,
        ORDER_BOOK
    }

    public enum EventStatus {
        NOT_STARTED,
        ACTIVE,
        CLOSED
    }

    private static final double LMSR_PAYOUT_PER_SHARE = 1.0;

    private final String name;
    private final String description;
    private EventStatus status = EventStatus.NOT_STARTED;
    private String winningOutcome = null;
    private double totalFeesCollected = 0.0;
    private final Account eventAccount = new Account(0.0);

    private final double feePercentage;
    private final FeeType feeType;

    private String marketMakerName;
    private final TradingMethod tradingMethod;

    // LMSR only
    private final double b;

    // Order Book only
    private final double initialInvestment;
    private final boolean allowMint;
    private final double d;

    private final List<Outcome> outcomes;
    private final List<Transaction> transactions;
    private final Map<String, OrderBook> orderBooks = new HashMap<>();

    public MarketEvent(String name, String description, double feePercentage,
                       FeeType feeType, TradingMethod tradingMethod, double b,
                       double initialInvestment, boolean allowMint, double d) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be empty.");
        }
        if (tradingMethod == TradingMethod.LMSR && b <= 0) {
            throw new IllegalArgumentException("LMSR parameter B must be strictly positive.");
        }
        if (tradingMethod == TradingMethod.ORDER_BOOK) {
            // The market maker opens the event by buying its first shares, so there must be something to buy.
            if (initialInvestment <= 0) {
                throw new IllegalArgumentException("Order Book parameter 'initial' must be strictly positive.");
            }
            if (d <= 0) {
                throw new IllegalArgumentException("Order Book parameter 'd' must be strictly positive.");
            }
        }
        if (feePercentage < 0 || feePercentage > 90) {
            throw new IllegalArgumentException("Fee percentage must be between 0 and 90.");
        }

        this.name = name.trim();
        this.description = description;
        this.feePercentage = feePercentage;
        this.feeType = feeType;
        this.tradingMethod = tradingMethod;

        if (tradingMethod == TradingMethod.LMSR) {
            this.b = b;
            this.initialInvestment = 0.0;
            this.allowMint = false;
            this.d = 0.0;
        } else { // ORDER_BOOK
            this.b = 0.0;
            this.initialInvestment = initialInvestment;
            this.allowMint = allowMint;
            this.d = d;
        }

        this.outcomes = new ArrayList<>();
        this.transactions = new ArrayList<>();
    }

    // --- Getters & Setters ---

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getWinningOutcome() {
        return winningOutcome;
    }

    public double getFeePercentage() {
        return feePercentage;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public String getMarketMakerName() {
        return marketMakerName;
    }

    public Account getEventAccount() {
        return eventAccount;
    }

    public void setMarketMakerName(String marketMakerName) {
        if (marketMakerName == null || marketMakerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Market Maker name cannot be null or empty.");
        }
        this.marketMakerName = marketMakerName.trim();
    }

    public TradingMethod getTradingMethod() {
        return tradingMethod;
    }

    public double getB() {
        return b;
    }

    public double getInitialInvestment() {
        return initialInvestment;
    }

    public boolean isAllowMint() {
        return allowMint;
    }

    public double getD() {
        return d;
    }

    public double getPayoutPerShare() {
        return (tradingMethod == TradingMethod.LMSR) ? LMSR_PAYOUT_PER_SHARE : d;
    }

    public List<Outcome> getOutcomes() {
        return Collections.unmodifiableList(outcomes);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void addOutcome(Outcome outcome) {
        if (outcome != null) {
            this.outcomes.add(outcome);
        }
    }

    public Outcome getOutcomeByTitle(String title) {
        for (Outcome outcome : outcomes) {
            if (outcome.getTitle().equalsIgnoreCase(title)) {
                return outcome;
            }
        }
        return null;
    }

    // Events are binary, so every outcome has exactly one opposite outcome.
    public Outcome getOtherOutcome(Outcome outcome) {
        for (Outcome candidate : outcomes) {
            if (candidate != outcome) {
                return candidate;
            }
        }
        throw new IllegalStateException("Event '" + name + "' has no outcome other than '" + outcome.getTitle() + "'.");
    }

    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            this.transactions.add(transaction);
        }
    }

    public void open() {
        if (status != EventStatus.NOT_STARTED) {
            throw new IllegalStateException("Event '" + name + "' has already been opened.");
        }
        this.status = EventStatus.ACTIVE;
    }

    public void close(String winningOutcomeTitle) {
        if (status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Only an active event can be closed. Event '" + name + "' is " + status + ".");
        }
        this.status = EventStatus.CLOSED;
        this.winningOutcome = winningOutcomeTitle;
    }

    public double getTotalFeesCollected() {
        return totalFeesCollected;
    }

    public void addFeeCollected(double fee) {
        this.totalFeesCollected += fee;
    }

    public OrderBook getOrCreateOrderBook(String outcomeTitle) {
        return orderBooks.computeIfAbsent(outcomeTitle, k -> new OrderBook());
    }

    public OrderBook getOrderBook(String outcomeTitle) {
        return orderBooks.get(outcomeTitle);
    }
}
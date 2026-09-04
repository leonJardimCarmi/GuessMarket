package com.guessmarket.engine.model;

import java.io.Serializable;
import java.util.*;

public class MarketEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum FeeType {
        AT_PURCHASE,
        AT_RESOLUTION
    }

    public enum TradingMethod {
        LMSR,
        ORDER_BOOK
    }

    private final String id;
    private final String title;
    private final String description;
    private boolean isActive = true;
    private String winningOutcome = null;
    private double totalFeesCollected = 0.0;
    private final Account eventAccount = new Account(0.0);

    private final double feePercentage;
    private final FeeType feeType;

    private String marketMakerName;
    private final TradingMethod tradingMethod;

    // שדות ייעודיים ל-LMSR
    private final double b;

    // שדות ייעודיים ל-Order Book
    private final double initialShares;
    private final boolean allowMint;
    private final double d;

    private final List<Outcome> outcomes;
    private final List<Transaction> transactions;
    private final Map<String, OrderBook> orderBooks = new HashMap<>();

    /**
     * בנאי מלא עבור MarketEvent (תומך גם ב-LMSR וגם ב-Order Book)
     */
    public MarketEvent(String id, String title, String description, double feePercentage,
                       FeeType feeType, TradingMethod tradingMethod, double b,
                       double initialShares, boolean allowMint, double d) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty.");
        }
        if (tradingMethod == TradingMethod.LMSR && b <= 0) {
            throw new IllegalArgumentException("LMSR parameter B must be strictly positive.");
        }
        if (tradingMethod == TradingMethod.ORDER_BOOK) {
            if (initialShares < 0) {
                throw new IllegalArgumentException("Initial shares for Order Book cannot be negative.");
            }
            if (d <= 0) {
                throw new IllegalArgumentException("Order Book parameter 'd' must be strictly positive.");
            }
        }
        if (feePercentage < 0 || feePercentage > 90) {
            throw new IllegalArgumentException("Fee percentage must be between 0 and 90.");
        }

        this.id = id.trim();
        this.title = title;
        this.description = description;
        this.feePercentage = feePercentage;
        this.feeType = feeType;
        this.tradingMethod = tradingMethod;

        // הגדרת משתנים בהתאם לשיטת המסחר
        if (tradingMethod == TradingMethod.LMSR) {
            this.b = b;
            this.initialShares = 0.0;
            this.allowMint = false;
            this.d = 0.0;
        } else { // ORDER_BOOK
            this.b = 0.0;
            this.initialShares = initialShares;
            this.allowMint = allowMint;
            this.d = d;
        }

        this.isActive = true;
        this.winningOutcome = null;

        this.outcomes = new ArrayList<>();
        this.transactions = new ArrayList<>();
    }

    /**
     * בנאי מקוצר עבור LMSR (לצורך תאימות לאחור בקוד קיים)
     */
    public MarketEvent(String id, String title, String description, double feePercentage,
                       FeeType feeType, TradingMethod tradingMethod, double b) {
        this(id, title, description, feePercentage, feeType, tradingMethod, b, 0.0, false, 0.0);
    }

    /**
     * בנאי מקוצר שברירת המחדל שלו היא LMSR
     */
    public MarketEvent(String id, String title, String description, double feePercentage,
                       FeeType feeType, double b) {
        this(id, title, description, feePercentage, feeType, TradingMethod.LMSR, b, 0.0, false, 0.0);
    }

    // --- Getters & Setters ---

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

    public double getInitialShares() {
        return initialShares;
    }

    public boolean isAllowMint() {
        return allowMint;
    }

    public double getD() {
        return d;
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

    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            this.transactions.add(transaction);
        }
    }

    public void closeEvent(String winningOutcomeTitle) {
        if (!isActive) {
            throw new IllegalStateException("Event is already closed.");
        }
        this.isActive = false;
        this.winningOutcome = winningOutcomeTitle;
    }

    public double getTotalFeesCollected() {
        return totalFeesCollected;
    }

    public void addFeeCollected(double fee) {
        this.totalFeesCollected += fee;
    }

    public synchronized OrderBook getOrCreateOrderBook(String outcomeTitle) {
        return orderBooks.computeIfAbsent(outcomeTitle, k -> new OrderBook());
    }

    public synchronized OrderBook getOrderBook(String outcomeTitle) {
        return orderBooks.get(outcomeTitle);
    }

    public void addMarketMakerFund(double initialShares) {
        this.getEventAccount().deposit(initialShares);
    }
}
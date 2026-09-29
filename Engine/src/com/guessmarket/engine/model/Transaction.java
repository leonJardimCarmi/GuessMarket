package com.guessmarket.engine.model;

/**
 * One purchase of shares in an event. The seller is null when nobody sold the shares:
 * an LMSR purchase (bought from the event) or minted Order Book shares.
 */
public class Transaction {
    private final String buyerName;
    private final String sellerName;
    private final String outcomeTitle;
    private final double shareAmount;
    private final double cost;
    private final double feePaid;

    public Transaction(String buyerName, String sellerName, String outcomeTitle,
                       double shareAmount, double cost, double feePaid) {
        if (buyerName == null || buyerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer name cannot be empty.");
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

        this.buyerName = buyerName.trim();
        this.sellerName = sellerName;
        this.outcomeTitle = outcomeTitle.trim();
        this.shareAmount = shareAmount;
        this.cost = cost;
        this.feePaid = feePaid;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public boolean involves(String userName) {
        return buyerName.equals(userName) || userName.equals(sellerName);
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

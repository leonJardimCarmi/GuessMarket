package com.guessmarket.engine.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A user's money flows with one event, recorded at the moment they happen.
 * A position is created on the user's first action in the event (an order, a purchase, or opening it as market maker).
 */
public class Position {
    // Paid into the event: shares bought (without fees) and the market maker's opening funds
    private double invested;
    private final Map<String, Double> investedByOutcome = new LinkedHashMap<>();
    // Commissions paid by this user (on purchase and on close)
    private double feesPaid;
    // Got back from the event: shares sold, winning payouts, the market maker's remaining balance
    private double received;
    // Commissions received as the event's market maker
    private double commissionsEarned;

    public void addInvestment(double amount) {
        invested += amount;
    }

    public void addShareInvestment(String outcomeTitle, double amount) {
        addInvestment(amount);
        investedByOutcome.merge(outcomeTitle, amount, Double::sum);
    }

    public void addFeePaid(double fee) {
        feesPaid += fee;
    }

    public void addReceived(double amount) {
        received += amount;
    }

    public void addCommissionEarned(double commission) {
        commissionsEarned += commission;
    }

    public double getInvested() {
        return invested;
    }

    public Map<String, Double> getInvestedByOutcome() {
        return Collections.unmodifiableMap(investedByOutcome);
    }

    public double getFeesPaid() {
        return feesPaid;
    }

    public double getReceived() {
        return received;
    }

    public double getCommissionsEarned() {
        return commissionsEarned;
    }

    // Final once the event is closed; while it is active this is only the result so far.
    public double getProfitLoss() {
        return received + commissionsEarned - invested - feesPaid;
    }
}

package com.guessmarket.engine.model;

public class Transaction {
    private final String outcomeTitle;
    private final double shareAmount;
    private final double cost;
    private final double feePaid;


    public Transaction(String outcomeTitle, double shareAmount, double cost, double feePaid) {
        this.outcomeTitle = outcomeTitle;
        this.shareAmount = shareAmount;
        this.cost = cost;
        this.feePaid = feePaid;
    }

    public String getOutcomeTitle() {
        return outcomeTitle;
    }

    public double getShareAmount(){
        return shareAmount;
    }

    public double getCost() {
        return cost;
    }

    public double getFeePaid() {
        return feePaid;
    }

    public double getTotalPaid(){
        return cost+ feePaid;
    }
}

package com.guessmarket.engine.dto;

public class TransactionDto {

    private final String outcomeTitle;
    private final double sharesBought;
    private final double amountPaid;
    private final double feePaid;

    public TransactionDto(String outcomeTitle, double sharesBought, double amountPaid, double feePaid) {
        this.outcomeTitle = outcomeTitle;
        this.sharesBought = sharesBought;
        this.amountPaid = amountPaid;
        this.feePaid = feePaid;
    }

    public String getOutcomeTitle() {
        return outcomeTitle;
    }

    public double getSharesBought() {
        return sharesBought;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public double getFeePaid() {
        return feePaid;
    }
}

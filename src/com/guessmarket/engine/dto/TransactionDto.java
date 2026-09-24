package com.guessmarket.engine.dto;

import java.io.Serializable;

public class TransactionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userName;
    private final String outcomeTitle;
    private final double sharesBought;
    private final double amountPaid;
    private final double feePaid;

    public TransactionDto(String userName, String outcomeTitle, double sharesBought, double amountPaid, double feePaid) {
        this.userName = userName;
        this.outcomeTitle = outcomeTitle;
        this.sharesBought = sharesBought;
        this.amountPaid = amountPaid;
        this.feePaid = feePaid;
    }

    public String getUserName() {
        return userName;
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
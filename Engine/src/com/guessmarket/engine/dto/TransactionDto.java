package com.guessmarket.engine.dto;

public class TransactionDto {
    private final String buyerName;
    private final String sellerName; // null when the shares were bought from the event (LMSR) or minted
    private final String outcomeTitle;
    private final double sharesBought;
    private final double amountPaid;
    private final double feePaid;

    public TransactionDto(String buyerName, String sellerName, String outcomeTitle,
                          double sharesBought, double amountPaid, double feePaid) {
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.outcomeTitle = outcomeTitle;
        this.sharesBought = sharesBought;
        this.amountPaid = amountPaid;
        this.feePaid = feePaid;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getSellerName() {
        return sellerName;
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

package com.guessmarket.engine.dto;

public class OutcomeDto {

    private final String title;
    private final double sharesCount;
    private final double currentPrice;


    public OutcomeDto(String title, double sharesCount, double currentPrice) {
        this.title = title;
        this.sharesCount = sharesCount;
        this.currentPrice = currentPrice;
    }

    public String getTitle() {
        return title;
    }

    public double getSharesCount() {
        return sharesCount;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }
}

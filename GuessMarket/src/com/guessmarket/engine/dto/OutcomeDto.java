package com.guessmarket.engine.dto;

import java.io.Serializable;

public class OutcomeDto implements Serializable {
    private static final long serialVersionUID = 1L;

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

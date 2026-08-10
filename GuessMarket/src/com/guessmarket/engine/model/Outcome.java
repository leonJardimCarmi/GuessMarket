package com.guessmarket.engine.model;

public class Outcome {
    private final String title;
    private double sharesBought;


    public Outcome(String title) {
        this.title = title;
        this.sharesBought =0;
    }

    public String getTitle() {
        return title;
    }

    public double getSharesBought() {
        return sharesBought;
    }

    public void addShares(double count){
        if (count < 0){
            throw new IllegalArgumentException("cannot add negative shares count");
        }
        this.sharesBought += count;
    }

}

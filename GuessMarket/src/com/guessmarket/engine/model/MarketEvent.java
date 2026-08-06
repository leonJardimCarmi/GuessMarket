package com.guessmarket.engine.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarketEvent {

    public enum FeeType{
        AT_PURCHASE,
        AT_RESOLUTION
    }

    private final String id;
    private final String title;
    private final String description;
    private boolean isActive;
    private String winningOutcome;

    private final double feePercentage;
    private final FeeType feeType;

    private final Account account;
    private final List<Outcome> outcomes;
    private final List<Transaction> transactions;

    public MarketEvent(String id, String title, String description, double initialAccountBalance,double feePercentage, FeeType feeType ){
        this.id = id;
        this.title = title;
        this.description = description;
        this.isActive = true;
        this.winningOutcome = null;

        this.feePercentage =feePercentage;
        this.feeType = feeType;

        this.account = new Account(initialAccountBalance);
        this.outcomes = new ArrayList<>();
        this.transactions = new ArrayList<>();
    }

    public String getId(){
        return  id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive(){
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

    public Account getAccount() {
        return account;
    }

    public List<Outcome> getOutcomes() {
        return Collections.unmodifiableList(outcomes);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void addOutcome(Outcome outcome){
        this.outcomes.add(outcome);
    }

    public Outcome getOutcomeByTitle(String title){
        for (Outcome outcome: outcomes){
            if(outcome.getTitle().equalsIgnoreCase(title)){
                return outcome;
            }
        }
        return null;
    }

    public void addTransaction(Transaction transaction){
        this.transactions.add(transaction);
    }

    public void closeEvent(String winningOutcomeTitle){
        if(!isActive){
            throw new IllegalStateException("Event is already closed. ");
        }
        this.isActive = false;
        this.winningOutcome = winningOutcomeTitle;
    }
}

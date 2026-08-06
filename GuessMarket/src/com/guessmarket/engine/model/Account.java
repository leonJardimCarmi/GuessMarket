package com.guessmarket.engine.model;

public class Account {
    private  double balance;
    private  double totalFees;

    public Account (double initialBalance){
        this.balance = initialBalance;
        this.totalFees = 0.0;
    }

    public double getBalance() {
        return  balance;
    }

    public double getTotalFees() {
        return totalFees;
    }

    public  void deposit(double amount) {
        if(amount < 0 ){
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance += amount;
    }

    public void addFee(double feeAmount ){
        if (feeAmount < 0){
            throw new IllegalArgumentException("Fee amoount must be positive");
        }
        this.totalFees+= feeAmount;
        this.balance += feeAmount;
    }

    public void withdraw(double amount){
        if (amount < 0 || amount > balance){
            throw new IllegalArgumentException("Invalid withdrawal amount");
        }
        this.balance -= amount;
    }

}

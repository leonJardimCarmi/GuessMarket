package com.guessmarket.engine.model;

import java.io.Serializable;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private  double balance;
    private  double totalFees;

    public Account(double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
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
        if(amount <= 0 ){
            throw new IllegalArgumentException("Deposit amount must be strictly positive");
        }
        this.balance += amount;
    }

    public void addFee(double feeAmount ){
        if (feeAmount <= 0){
            throw new IllegalArgumentException("Fee amoount must be strictly positive");
        }
        this.totalFees+= feeAmount;
        this.balance += feeAmount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly positive.");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds. Current balance: " + balance);
        }
        this.balance -= amount;
    }

    public void forceWithdraw(double amount){
        if (amount<= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly positive.");
        }
        this.balance -= amount;
    }
}

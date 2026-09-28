package com.guessmarket.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private double balance;
    private final List<AccountEntry> entries = new ArrayList<>();

    public Account(double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        this.balance = initialBalance;
        if (initialBalance > 0) {
            entries.add(new AccountEntry("Initial balance", initialBalance, balance));
        }
    }

    public double getBalance() {
        return balance;
    }

    public List<AccountEntry> getEntries() {return Collections.unmodifiableList(entries);
    }

    public void deposit(double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly positive.");
        }
        AccountEntry entry = new AccountEntry(description, amount, balance + amount);
        this.balance += amount;
        entries.add(entry);
    }

    public void withdraw(double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly positive.");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds. Current balance: " + balance);
        }
        AccountEntry entry = new AccountEntry(description, -amount, balance - amount);
        this.balance -= amount;
        entries.add(entry);
    }
}

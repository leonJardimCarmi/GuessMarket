package com.guessmarket.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account {

    private double balance;
    // Money promised to open buy orders: still part of the balance, but it cannot be spent elsewhere.
    private double reserved;
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

    public double getReserved() {
        return reserved;
    }

    public double getAvailableBalance() {
        return balance - reserved;
    }

    public List<AccountEntry> getEntries() {
        return Collections.unmodifiableList(entries);
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
        if (amount > getAvailableBalance() + Amounts.EPSILON) {
            throw new IllegalArgumentException("Insufficient funds. Available balance: " + getAvailableBalance());
        }
        AccountEntry entry = new AccountEntry(description, -amount, balance - amount);
        this.balance -= amount;
        entries.add(entry);
    }

    public void reserve(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Reserved amount must be strictly positive.");
        }
        if (amount > getAvailableBalance() + Amounts.EPSILON) {
            throw new IllegalArgumentException("Insufficient funds to reserve. Available balance: " + getAvailableBalance());
        }
        this.reserved += amount;
    }

    public void release(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Released amount must be strictly positive.");
        }
        if (amount > reserved + Amounts.EPSILON) {
            throw new IllegalStateException("Cannot release more than the reserved amount (" + reserved + ").");
        }
        this.reserved = clampToZero(reserved - amount);
    }

    // Spends money that was reserved earlier (an open buy order that is now being executed).
    public void withdrawReserved(double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly positive.");
        }
        if (amount > reserved + Amounts.EPSILON) {
            throw new IllegalStateException("Cannot spend more than the reserved amount (" + reserved + ").");
        }
        AccountEntry entry = new AccountEntry(description, -amount, balance - amount);
        this.reserved = clampToZero(reserved - amount);
        this.balance -= amount;
        entries.add(entry);
    }

    // Removes floating-point leftovers such as 1e-16, so a fully released reservation is exactly 0.
    private static double clampToZero(double value) {
        return (value < Amounts.EPSILON) ? 0.0 : value;
    }
}

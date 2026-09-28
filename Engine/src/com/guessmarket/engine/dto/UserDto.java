package com.guessmarket.engine.dto;

import java.io.Serializable;
import java.util.Map;

public class UserDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final double balance;
    private final double reservedBalance;
    private final Map<String, Map<String, Double>> holdings;

    public UserDto(String name, double balance, double reservedBalance, Map<String, Map<String, Double>> holdings) {
        this.name = name;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.holdings = holdings;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public double getReservedBalance() {
        return reservedBalance;
    }

    public double getAvailableBalance() {
        return balance - reservedBalance;
    }

    public Map<String, Map<String, Double>> getHoldings() {
        return holdings;
    }
}

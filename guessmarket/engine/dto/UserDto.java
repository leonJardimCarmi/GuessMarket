package com.guessmarket.engine.dto;

import java.io.Serializable;
import java.util.Map;

public class UserDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final double balance;
    private final Map<String, Map<String, Double>> holdings;

    public UserDto(String name, double balance, Map<String, Map<String, Double>> holdings) {
        this.name = name;
        this.balance = balance;
        this.holdings = holdings;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public Map<String, Map<String, Double>> getHoldings() {
        return holdings;
    }
}
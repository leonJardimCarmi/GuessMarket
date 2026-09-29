package com.guessmarket.engine.dto;

import java.io.Serializable;

/**
 * The public view of a user, as shown to other users: name, balance and whether they are a market maker.
 * A user's full details (holdings, own events) are available only through {@link UserDto}.
 */
public class UserSummaryDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final double balance;
    private final boolean marketMaker;

    public UserSummaryDto(String name, double balance, boolean marketMaker) {
        this.name = name;
        this.balance = balance;
        this.marketMaker = marketMaker;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isMarketMaker() {
        return marketMaker;
    }
}

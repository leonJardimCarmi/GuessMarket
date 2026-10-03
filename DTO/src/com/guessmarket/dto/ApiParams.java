package com.guessmarket.dto;

/**
 * The names of the request parameters, shared by the server and the client.
 */
public abstract class ApiParams {
    public static final String USERNAME = "username";
    public static final String EVENT = "event";
    public static final String OUTCOME = "outcome";
    public static final String WINNER = "winner";
    public static final String SHARES = "shares";
    public static final String PRICE = "price";
    public static final String SIDE = "side";
    public static final String AMOUNT = "amount";
    public static final String FROM = "from";
    public static final String FILE = "file";
    public static final String TEXT = "text";

    private ApiParams() {
    }
}

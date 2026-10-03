package com.guessmarket.dto;

/**
 * The server's addresses, relative to the application's context path (/GuessMarket).
 * Shared by the server (servlet mappings) and the client (requests), so both always use the same address.
 */
public abstract class ApiPaths {
    public static final String PING = "/ping";

    public static final String LOGIN = "/login";
    public static final String LOGOUT = "/logout";
    public static final String USERS = "/users";
    public static final String ME = "/me";
    public static final String MY_EVENT = "/me/event";
    public static final String MY_ACCOUNT = "/me/account";
    public static final String DEPOSIT = "/me/deposit";

    public static final String EVENTS = "/events";
    public static final String UPLOAD = "/events/upload";
    public static final String EVENT = "/event";
    public static final String OPEN_EVENT = "/event/open";
    public static final String CLOSE_EVENT = "/event/close";
    public static final String ORDER_BOOK = "/event/orderbook";
    public static final String PARTICIPANTS = "/event/participants";
    public static final String BUY = "/event/buy";
    public static final String ORDER = "/event/order";

    // Bonus: GET = the messages from index 'from' on, POST = send a message ('text')
    public static final String CHAT = "/chat";

    private ApiPaths() {
    }
}

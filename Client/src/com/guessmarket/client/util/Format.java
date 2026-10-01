package com.guessmarket.client.util;

import com.guessmarket.dto.MarketEventDto;
import com.guessmarket.dto.OrderDto;

/**
 * Turns the server's values into the text the screens show.
 * One place for it, so every screen shows the same value the same way.
 */
public abstract class Format {
    private static final String NO_VALUE = "-";

    private Format() {
    }

    public static String money(double amount) {
        return String.format("%.2f", amount);
    }

    // For values that may be missing, like order book statistics before the first trade.
    public static String optional(Double value) {
        return value == null ? NO_VALUE : money(value);
    }

    public static String status(String status) {
        return switch (status) {
            case MarketEventDto.STATUS_NOT_STARTED -> "Not started";
            case MarketEventDto.STATUS_ACTIVE -> "Active";
            case MarketEventDto.STATUS_CLOSED -> "Closed";
            default -> status;
        };
    }

    public static String method(String tradingMethod) {
        return switch (tradingMethod) {
            case MarketEventDto.METHOD_LMSR -> "LMSR";
            case MarketEventDto.METHOD_ORDER_BOOK -> "Order Book";
            default -> tradingMethod;
        };
    }

    public static String feeType(String feeType) {
        return switch (feeType) {
            case MarketEventDto.FEE_AT_PURCHASE -> "on purchase";
            case MarketEventDto.FEE_AT_CLOSE -> "on close";
            default -> feeType;
        };
    }

    // e.g. "2.00% on purchase"
    public static String fee(MarketEventDto event) {
        return money(event.getFeePercentage()) + "% " + feeType(event.getFeeType());
    }

    public static String side(String side) {
        return switch (side) {
            case OrderDto.SIDE_BUY -> "Buy";
            case OrderDto.SIDE_SELL -> "Sell";
            default -> side;
        };
    }
}

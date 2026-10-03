package com.guessmarket.dto;

/**
 * The rules for amounts users enter - money (deposits, prices) and share quantities - shared by the server and the client:
 * more than 0, whole cents (at most 2 decimal places, like everything the screens show), and at most MAX.
 */
public final class AmountLimits {
    public static final int MAX_DECIMALS = 2;
    // A sanity limit: keeps every balance and cost a finite number (a double overflows to Infinity above ~1.8e308).
    public static final double MAX = 1_000_000_000;

    private AmountLimits() {
    }
}

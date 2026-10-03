package com.guessmarket.engine.model;

import com.guessmarket.dto.AmountLimits;

import java.math.BigDecimal;

/**
 * How the engine compares amounts of money and shares.
 * Users enter whole cents, but computing with doubles leaves tiny leftovers (0.3 - 0.1 - 0.1 is 0.09999999999999998),
 * so amounts are compared with one tolerance for the whole engine: far below a cent, far above those leftovers.
 */
public final class Amounts {
    public static final double EPSILON = 1e-5;

    private Amounts() {
    }

    // Double.toString gives the shortest decimal that is exactly this double (0.1 -> "0.1"), so its scale is exact.
    public static boolean hasAllowedDecimals(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().scale() <= AmountLimits.MAX_DECIMALS;
    }
}

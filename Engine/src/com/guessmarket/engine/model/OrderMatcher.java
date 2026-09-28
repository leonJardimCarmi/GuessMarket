package com.guessmarket.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches an incoming order against the order books of a binary event.
 * It only moves shares between orders and produces {@link Fill}s; the money is settled by the engine.
 */
public final class OrderMatcher {
    private static final double EPSILON = 1e-9;

    private OrderMatcher() {
    }

    /**
     * @param ownBook  the book of the order's outcome
     * @param mintBook the book of the opposite outcome when minting is allowed, otherwise null
     * @param d        the base value: a pair of opposite shares is worth exactly d
     * @return the fills, in execution order; any unfilled remainder of the order is left resting in ownBook
     */
    public static List<Fill> match(Order incoming, OrderBook ownBook, OrderBook mintBook, double d) {
        List<Fill> fills = new ArrayList<>();
        if (incoming.getSide() == OrderSide.BUY) {
            matchBuyOrder(incoming, ownBook, mintBook, d, fills);
        } else {
            matchSellOrder(incoming, ownBook, fills);
        }
        if (!incoming.isFilled()) {
            ownBook.addOrder(incoming);
        }
        return fills;
    }

    private static void matchBuyOrder(Order buy, OrderBook ownBook, OrderBook mintBook, double d, List<Fill> fills) {
        while (!buy.isFilled()) {
            Order ask = ownBook.getBestAsk();
            Order mintPartner = (mintBook == null) ? null : mintBook.getBestBid();
            boolean canTrade = (ask != null) && (ask.getPrice() <= buy.getPrice() + EPSILON);
            boolean canMint = (mintPartner != null) && (buy.getPrice() + mintPartner.getPrice() >= d - EPSILON);
            if (!canTrade && !canMint) {
                return;
            }
            // Best execution: take whichever is cheaper for the incoming buyer; on a tie, prefer the existing shares.
            if (canTrade && (!canMint || ask.getPrice() <= d - mintPartner.getPrice() + EPSILON)) {
                tradeWithAsk(buy, ask, ownBook, fills);
            } else {
                mint(buy, mintPartner, ownBook, mintBook, d, fills);
            }
        }
    }

    private static void matchSellOrder(Order sell, OrderBook ownBook, List<Fill> fills) {
        while (!sell.isFilled()) {
            Order bid = ownBook.getBestBid();
            if (bid == null || bid.getPrice() < sell.getPrice() - EPSILON) {
                return;
            }
            // A trade executes at the price of the order that was already waiting in the book.
            double shares = Math.min(sell.getSharesCount(), bid.getSharesCount());
            fills.add(new Fill(bid.getUserName(), sell.getUserName(), bid.getOutcomeTitle(),
                    bid.getPrice(), shares, bid.getPrice()));
            sell.reduceShares(shares);
            bid.reduceShares(shares);
            ownBook.recordTradePrice(bid.getPrice());
            ownBook.removeFilledOrders();
        }
    }

    private static void tradeWithAsk(Order buy, Order ask, OrderBook ownBook, List<Fill> fills) {
        double shares = Math.min(buy.getSharesCount(), ask.getSharesCount());
        fills.add(new Fill(buy.getUserName(), ask.getUserName(), buy.getOutcomeTitle(),
                ask.getPrice(), shares, buy.getPrice()));
        buy.reduceShares(shares);
        ask.reduceShares(shares);
        ownBook.recordTradePrice(ask.getPrice());
        ownBook.removeFilledOrders();
    }

    // Mint: two buy orders on opposite outcomes together offer at least d, so new pairs of shares are created.
    // The order that was waiting pays its own price; the incoming order pays the complement to d.
    private static void mint(Order buy, Order partner, OrderBook ownBook, OrderBook partnerBook,
                             double d, List<Fill> fills) {
        double shares = Math.min(buy.getSharesCount(), partner.getSharesCount());
        double incomingPrice = d - partner.getPrice();
        fills.add(new Fill(buy.getUserName(), null, buy.getOutcomeTitle(), incomingPrice, shares, buy.getPrice()));
        fills.add(new Fill(partner.getUserName(), null, partner.getOutcomeTitle(),
                partner.getPrice(), shares, partner.getPrice()));
        buy.reduceShares(shares);
        partner.reduceShares(shares);
        ownBook.recordTradePrice(incomingPrice);
        partnerBook.recordTradePrice(partner.getPrice());
        partnerBook.removeFilledOrders();
    }
}

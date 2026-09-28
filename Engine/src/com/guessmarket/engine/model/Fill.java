package com.guessmarket.engine.model;

/**
 * One execution produced by the matching: a buyer received shares of one outcome at a price.
 * A regular trade has a seller; a minted fill has no seller (sellerName is null) - the shares are newly created
 * and the payment goes to the event account.
 *
 * @param buyerLimitPrice the price limit of the buyer's order; its money was reserved at this price
 */
public record Fill(String buyerName, String sellerName, String outcomeTitle,
                   double price, double shares, double buyerLimitPrice) {

    public boolean isMinted() {
        return sellerName == null;
    }
}

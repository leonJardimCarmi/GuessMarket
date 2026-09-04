package com.guessmarket.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class OrderBook implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Order> bids =new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();

    public List<Order> getBids() {
        return new ArrayList<>(bids);
    }

    public List<Order> getAsks() {
        return new ArrayList<>(asks);
    }

    private void sortBids() {
        bids.sort(Comparator.comparingDouble(Order::getPrice).reversed()
                .thenComparingLong(Order::getTimestamp));
    }

    private  void sortAsks() {
        asks.sort(Comparator.comparingDouble(Order::getPrice)
                .thenComparingLong(Order::getTimestamp));
    }

    public boolean canFullyFill(Order newOrder) {
        double neededShares = newOrder.getSharesCount();
        double limitPrice = newOrder.getPrice();

        if (newOrder.getSide() == OrderSide.BUY) {
            for (Order ask : asks) {
                if (ask.getPrice() <= limitPrice) {
                    neededShares -= ask.getSharesCount();
                    if (neededShares <= 0.00001) {
                        return true;
                    }
                } else {
                    break;
                }
            }
        } else { // SELL
            for (Order bid : bids) {
                if (bid.getPrice() >= limitPrice) {
                    neededShares -= bid.getSharesCount();
                    if (neededShares <= 0.00001) {
                        return true;
                    }
                } else {
                    break;
                }
            }
        }

        return false;
    }

    public List<TradeResult> processOrder(Order newOrder) {
        List<TradeResult> trades = new ArrayList<>();

        if (newOrder.getSide() == OrderSide.BUY) {
            matchBuyOrder(newOrder, trades);
            if (!newOrder.isFilled()) {
                bids.add(newOrder);
                sortBids();
            }
        } else { // SELL
            matchSellOrder(newOrder, trades);
            if (!newOrder.isFilled() ) {
                asks.add(newOrder);
                sortAsks();
            }
        }

        return trades;
    }

    private void matchBuyOrder(Order buyOrder, List<TradeResult> trades) {
        Iterator<Order> iterator = asks.iterator();

        while (iterator.hasNext() && !buyOrder.isFilled()) {
            Order ask = iterator.next();

            if (ask.getPrice() > buyOrder.getPrice()) {
                break;
            }

            double tradePrice = ask.getPrice();
            double tradeShares = Math.min(buyOrder.getSharesCount(), ask.getSharesCount());

            buyOrder.reduceShares(tradeShares);
            ask.reduceShares(tradeShares);

            trades.add(new TradeResult(buyOrder.getUserName(), ask.getUserName(), tradePrice, tradeShares));

            if (ask.isFilled()) {
                iterator.remove();
            }
        }
    }

    private void matchSellOrder(Order sellOrder, List<TradeResult> trades) {
        Iterator<Order> iterator = bids.iterator();

        while (iterator.hasNext() && !sellOrder.isFilled()) {
            Order bid = iterator.next();

            // אם מחיר הקנייה בספר נמוך ממחיר המכירה המינימלי - אין התאמה
            if (bid.getPrice() < sellOrder.getPrice()) {
                break;
            }

            // המחיר שבו תבוצע העסקה הוא המחיר של הפקודה הממתינה בספר (bid.getPrice())
            double tradePrice = bid.getPrice();
            double tradeShares = Math.min(sellOrder.getSharesCount(), bid.getSharesCount());

            sellOrder.reduceShares(tradeShares);
            bid.reduceShares(tradeShares);

            trades.add(new TradeResult(buyOrderOwner(bid), sellOrder.getUserName(), tradePrice, tradeShares));

            if (bid.isFilled()) {
                iterator.remove();
            }
        }
    }

    private String buyOrderOwner(Order bid) {
        return bid.getUserName();
    }


    public static class TradeResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String buyerName;
        private final String sellerName;
        private final double price;
        private final double shares;

        public TradeResult(String buyerName, String sellerName, double price, double shares) {
            this.buyerName = buyerName;
            this.sellerName = sellerName;
            this.price = price;
            this.shares = shares;
        }

        public String getBuyerName() { return buyerName; }
        public String getSellerName() { return sellerName; }
        public double getPrice() { return price; }
        public double getShares() { return shares; }
    }
}

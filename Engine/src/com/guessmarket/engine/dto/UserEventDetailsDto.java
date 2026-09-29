package com.guessmarket.engine.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Everything about one user's participation in one event: holdings, money flows, own trades and open orders.
 * profitLoss is final once the event is CLOSED; while it is active it is only the result so far.
 */
public class UserEventDetailsDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String eventName;
    private final String status;
    private final String tradingMethod;
    private final String winningOutcome;
    private final boolean marketMaker;

    private final Map<String, Double> holdings;          // outcome -> shares held
    private final Map<String, Double> investedByOutcome; // outcome -> money paid for its shares (without fees)
    private final double invested;
    private final double feesPaid;
    private final double received;
    private final double commissionsEarned;
    private final double profitLoss;

    private final List<TransactionDto> trades;            // newest first
    private final List<OrderDto> openOrders;

    public UserEventDetailsDto(String eventName, String status, String tradingMethod, String winningOutcome,
                               boolean marketMaker, Map<String, Double> holdings, Map<String, Double> investedByOutcome,
                               double invested, double feesPaid, double received, double commissionsEarned,
                               double profitLoss, List<TransactionDto> trades, List<OrderDto> openOrders) {
        this.eventName = eventName;
        this.status = status;
        this.tradingMethod = tradingMethod;
        this.winningOutcome = winningOutcome;
        this.marketMaker = marketMaker;
        this.holdings = holdings;
        this.investedByOutcome = investedByOutcome;
        this.invested = invested;
        this.feesPaid = feesPaid;
        this.received = received;
        this.commissionsEarned = commissionsEarned;
        this.profitLoss = profitLoss;
        this.trades = trades;
        this.openOrders = openOrders;
    }

    public String getEventName() { return eventName; }
    public String getStatus() { return status; }
    public String getTradingMethod() { return tradingMethod; }
    public String getWinningOutcome() { return winningOutcome; }
    public boolean isMarketMaker() { return marketMaker; }
    public Map<String, Double> getHoldings() { return holdings; }
    public Map<String, Double> getInvestedByOutcome() { return investedByOutcome; }
    public double getInvested() { return invested; }
    public double getFeesPaid() { return feesPaid; }
    public double getReceived() { return received; }
    public double getCommissionsEarned() { return commissionsEarned; }
    public double getProfitLoss() { return profitLoss; }
    public List<TransactionDto> getTrades() { return trades; }
    public List<OrderDto> getOpenOrders() { return openOrders; }
}

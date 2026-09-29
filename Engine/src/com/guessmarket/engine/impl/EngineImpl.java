package com.guessmarket.engine.impl;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.dto.*;
import com.guessmarket.engine.model.*;
import com.guessmarket.engine.schema.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.util.*;

/**
 * The engine is shared by all the server's request threads.
 * Thread safety: every public method is synchronized on this engine, so exactly one request works on the engine
 * at a time and each operation runs as one uninterrupted unit. Every returned DTO is a fresh copy (a snapshot),
 * so callers can use it after the lock is released while other requests keep changing the engine.
 */
public class EngineImpl implements EngineApi {
    private static final int REQUIRED_OPTIONS_COUNT = 2;
    private static final String COMMISSION_ON_CLOSE = "on-close";
    private static final String COMMISSION_ON_PURCHASE = "on-purchase";
    private static final double EPSILON = 1e-9;
    private static final double PRICE_TICK = 0.01;

    // LinkedHashMap keeps insertion order: events are listed in upload order, users in registration order.
    private final Map<String, MarketEvent> eventsMap = new LinkedHashMap<>();
    private final Map<String, User> usersMap = new LinkedHashMap<>();
    private long nextOrderNumber = 1;

    private MarketEventDto createDtoEvent(MarketEvent event) {
        List<OutcomeDto> outcomeDtos = new ArrayList<>();
        for (Outcome outcome : event.getOutcomes()) {
            double currentPrice = 0.0;
            if (event.getTradingMethod() == MarketEvent.TradingMethod.LMSR) {
                currentPrice = LmsrCalculator.calculatePrice(event.getOutcomes(), outcome.getTitle(), event.getB());
            } else if (event.getTradingMethod() == MarketEvent.TradingMethod.ORDER_BOOK) {
                OrderBook orderBook = event.getOrderBook(outcome.getTitle());
                if (orderBook != null && orderBook.getLastTradePrice() != null) {
                    currentPrice = orderBook.getLastTradePrice();
                }
            }
            outcomeDtos.add(new OutcomeDto(outcome.getTitle(), outcome.getSharesBought(), currentPrice));
        }

        List<TransactionDto> transactionDtos = new ArrayList<>();
        for (Transaction transaction : event.getTransactions()) {
            transactionDtos.add(toTransactionDto(transaction));
        }

        String mmName = event.getMarketMakerName();
        double eventBalance = event.getEventAccount().getBalance();

        return new MarketEventDto(
                event.getName(),
                event.getDescription(),
                event.getStatus().name(),
                event.getWinningOutcome(),
                mmName,
                eventBalance,
                event.getTotalFeesCollected(),
                event.getFeePercentage(),
                event.getFeeType().name(),
                event.getTradingMethod().name(),
                outcomeDtos,
                transactionDtos,
                event.getB(),
                event.getD()
        );
    }

    private UserDto createDtoUser(User user) {
        Map<String, Map<String, Double>> holdings = new LinkedHashMap<>();
        for (MarketEvent event : eventsMap.values()) {
            Map<String, Double> eventHoldings = new LinkedHashMap<>();
            for (Outcome outcome : event.getOutcomes()) {
                double shares = user.getSharesCount(event.getName(), outcome.getTitle());
                if (shares > 0) {
                    eventHoldings.put(outcome.getTitle(), shares);
                }
            }
            if (!eventHoldings.isEmpty()) {
                holdings.put(event.getName(), eventHoldings);
            }
        }
        return new UserDto(user.getName(), user.getAccount().getBalance(), user.getAccount().getReserved(),
                holdings, getMarketMakerEventNames(user), user.getParticipatedEventNames());
    }

    private static TransactionDto toTransactionDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getBuyerName(),
                transaction.getSellerName(),
                transaction.getOutcomeTitle(),
                transaction.getShareAmount(),
                transaction.getTotalPaid(),
                transaction.getFeePaid()
        );
    }

    private List<String> getMarketMakerEventNames(User user) {
        List<String> eventNames = new ArrayList<>();
        for (MarketEvent event : eventsMap.values()) {
            if (user.getName().equals(event.getMarketMakerName())) {
                eventNames.add(event.getName());
            }
        }
        return eventNames;
    }

    private OrderDto toOrderDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getUserName(),
                order.getEventName(),
                order.getOutcomeTitle(),
                order.getSide().name(),
                order.getPrice(),
                order.getSharesCount(),          // Maps to remainingShares
                order.getOriginalSharesCount(),  // Maps to originalShares
                order.getTimestamp()
        );
    }

    @Override
    public synchronized List<String> loadEventsFromXml(InputStream xmlContent, String uploaderName) {
        User uploader = getUserByNameInternal(uploaderName);
        if (uploader == null) {
            throw new IllegalArgumentException("User '" + uploaderName + "' was not found.");
        }

        GuessMarket guessMarket = parseXml(xmlContent);
        if (guessMarket.getGMEvents() == null || guessMarket.getGMEvents().getGMEvent().isEmpty()) {
            throw new IllegalArgumentException("The file contains no events.");
        }
        List<MarketEvent> newEvents = buildAllEvents(guessMarket.getGMEvents().getGMEvent());
        List<String> loadedNames = new ArrayList<>();

        for (MarketEvent event : newEvents) {
            event.setMarketMakerName(uploader.getName());
            eventsMap.put(toEventKey(event.getName()), event);
            loadedNames.add(event.getName());
        }

        return loadedNames;
    }

    private GuessMarket parseXml(InputStream xmlContent) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (GuessMarket) unmarshaller.unmarshal(xmlContent);
        } catch (JAXBException e) {
            String reason = (e.getLinkedException() != null) ? e.getLinkedException().getMessage() : e.getMessage();
            throw new IllegalArgumentException("The file is not a valid Guess Market XML file: " + reason, e);
        }
    }

    private List<MarketEvent> buildAllEvents(List<GMEvent> xmlEvents) {
        List<MarketEvent> newEvents = new ArrayList<>();
        Set<String> namesInFile = new HashSet<>();

        for (GMEvent xmlEvent : xmlEvents) {
            MarketEvent event;
            try {
                event = buildEvent(xmlEvent);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Event '" + xmlEvent.getName() + "': " + e.getMessage(), e);
            }

            String key = toEventKey(event.getName());
            if (!namesInFile.add(key)) {
                throw new IllegalArgumentException("Event name '" + event.getName() + "' appears more than once in the file.");
            }
            if (eventsMap.containsKey(key)) {
                throw new IllegalArgumentException("An event named '" + event.getName() + "' already exists in the system.");
            }
            newEvents.add(event);
        }
        return newEvents;
    }

    private MarketEvent buildEvent(GMEvent xmlEvent) {
        GMMethod method = xmlEvent.getGMMethod();
        if (method == null || (method.getGMLMSR() == null && method.getGMOrderBook() == null)) {
            throw new IllegalArgumentException("No trading method defined.");
        }
        if (xmlEvent.getCommission() == null) {
            throw new IllegalArgumentException("No commission defined.");
        }

        MarketEvent.TradingMethod tradingMethod;
        double b = 0.0;
        double initialInvestment = 0.0;
        double d = 0.0;
        boolean allowMint = false;

        if (method.getGMLMSR() != null) {
            tradingMethod = MarketEvent.TradingMethod.LMSR;
            b = method.getGMLMSR().getB();
        } else {
            GMOrderBook orderBook = method.getGMOrderBook();
            tradingMethod = MarketEvent.TradingMethod.ORDER_BOOK;
            initialInvestment = orderBook.getInitial();
            d = orderBook.getD();
            allowMint = "true".equalsIgnoreCase(orderBook.getAllowMint());
        }

        MarketEvent event = new MarketEvent(
                xmlEvent.getName(),
                xmlEvent.getDescription(),
                xmlEvent.getCommission().getValue(),
                parseFeeType(xmlEvent.getCommission().getType()),
                tradingMethod,
                b,
                initialInvestment,
                allowMint,
                d
        );

        addOutcomes(event, xmlEvent.getGMOptions());
        return event;
    }

    private MarketEvent.FeeType parseFeeType(String type) {
        String normalizedType = (type == null) ? "" : type.trim();
        if (normalizedType.equalsIgnoreCase(COMMISSION_ON_CLOSE)) {
            return MarketEvent.FeeType.AT_RESOLUTION;
        }
        if (normalizedType.equalsIgnoreCase(COMMISSION_ON_PURCHASE)) {
            return MarketEvent.FeeType.AT_PURCHASE;
        }
        throw new IllegalArgumentException("Unknown commission type: '" + type + "'. Expected '"
                + COMMISSION_ON_CLOSE + "' or '" + COMMISSION_ON_PURCHASE + "'.");
    }

    private void addOutcomes(MarketEvent event, GMOptions xmlOptions) {
        List<String> optionTitles = (xmlOptions == null) ? List.of() : xmlOptions.getGMOption();
        if (optionTitles.size() != REQUIRED_OPTIONS_COUNT) {
            throw new IllegalArgumentException("Event must have exactly " + REQUIRED_OPTIONS_COUNT
                    + " options, found " + optionTitles.size() + ".");
        }

        for (String title : optionTitles) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Option name cannot be empty.");
            }
            String trimmedTitle = title.trim();
            if (event.getOutcomeByTitle(trimmedTitle) != null) {
                throw new IllegalArgumentException("Duplicate option '" + trimmedTitle + "'.");
            }
            event.addOutcome(new Outcome(trimmedTitle));
        }
    }

    @Override
    public synchronized List<MarketEventDto> getAllMarketEvents() {
        List<MarketEventDto> dtos = new ArrayList<>();
        for (MarketEvent event : eventsMap.values()) {
            dtos.add(createDtoEvent(event));
        }
        return dtos;
    }

    @Override
    public synchronized MarketEventDto getMarketEventByName(String eventName) {
        MarketEvent event = getEventByNameInternal(eventName);
        return (event == null) ? null : createDtoEvent(event);
    }

    @Override
    public synchronized void registerUser(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("User name cannot be empty.");
        }
        String key = toUserKey(userName);
        if (usersMap.containsKey(key)) {
            throw new IllegalArgumentException("The user name '" + userName.trim() + "' is already taken.");
        }
        // New users start with an empty account; they add money with depositFunds.
        usersMap.put(key, new User(userName, 0.0));
    }

    @Override
    public synchronized boolean isUserRegistered(String userName) {
        return getUserByNameInternal(userName) != null;
    }

    @Override
    public synchronized List<UserSummaryDto> getAllUsers() {
        List<UserSummaryDto> summaries = new ArrayList<>();
        for (User user : usersMap.values()) {
            boolean isMarketMaker = !getMarketMakerEventNames(user).isEmpty();
            summaries.add(new UserSummaryDto(user.getName(), user.getBalance(), isMarketMaker));
        }
        return summaries;
    }

    @Override
    public synchronized UserDto getUserByName(String userName) {
        User user = getUserByNameInternal(userName);
        return (user != null) ? createDtoUser(user) : null;
    }

    @Override
    public synchronized UserEventDetailsDto getUserEventDetails(String userName, String eventName) {
        User user = requireUser(userName);
        MarketEvent event = requireEvent(eventName);
        Position position = user.getPosition(event.getName());
        if (position == null) {
            position = new Position(); // never took part: every amount is zero
        }

        Map<String, Double> holdings = new LinkedHashMap<>();
        for (Outcome outcome : event.getOutcomes()) {
            holdings.put(outcome.getTitle(), user.getSharesCount(event.getName(), outcome.getTitle()));
        }

        return new UserEventDetailsDto(event.getName(), event.getStatus().name(), event.getTradingMethod().name(),
                event.getWinningOutcome(), user.getName().equals(event.getMarketMakerName()),
                holdings, new LinkedHashMap<>(position.getInvestedByOutcome()), position.getInvested(), position.getFeesPaid(),
                position.getReceived(), position.getCommissionsEarned(), position.getProfitLoss(),
                getTradesOf(user, event), getOpenOrdersOf(user, event));
    }

    // The user's trades in the event (as buyer or seller), newest first
    private static List<TransactionDto> getTradesOf(User user, MarketEvent event) {
        List<Transaction> transactions = event.getTransactions();
        List<TransactionDto> trades = new ArrayList<>();
        for (int i = transactions.size() - 1; i >= 0; i--) {
            if (transactions.get(i).involves(user.getName())) {
                trades.add(toTransactionDto(transactions.get(i)));
            }
        }
        return trades;
    }

    private List<OrderDto> getOpenOrdersOf(User user, MarketEvent event) {
        List<OrderDto> openOrders = new ArrayList<>();
        for (Outcome outcome : event.getOutcomes()) {
            OrderBook orderBook = event.getOrderBook(outcome.getTitle());
            if (orderBook == null) {
                continue;
            }
            List<Order> orders = new ArrayList<>(orderBook.getBids());
            orders.addAll(orderBook.getAsks());
            for (Order order : orders) {
                if (order.getUserName().equals(user.getName())) {
                    openOrders.add(toOrderDto(order));
                }
            }
        }
        return openOrders;
    }

    @Override
    public synchronized List<AccountEntryDto> getAccountEntries(String userName, int fromIndex) {
        User user = requireUser(userName);
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex cannot be negative.");
        }

        List<AccountEntry> entries = user.getAccount().getEntries();
        if (fromIndex >= entries.size()) {
            return List.of();
        }
        return entries.subList(fromIndex, entries.size()).stream()
                .map(entry -> new AccountEntryDto(entry.getDescription(), entry.getAmount(),
                        entry.getBalanceAfter(), entry.getTimestamp()))
                .toList();
    }

    @Override
    public synchronized void depositFunds(String userName, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        requireUser(userName).getAccount().deposit(amount, "Deposit");
    }

    @Override
    public synchronized void buySharesLMSR(String userName, String eventName, String outcomeTitle, double sharesToBuy) {
        User user = requireUser(userName);
        MarketEvent event = requireEvent(eventName);
        requireActive(event);
        if (event.getTradingMethod() != MarketEvent.TradingMethod.LMSR) {
            throw new IllegalArgumentException("Event '" + event.getName() + "' does not use LMSR trading.");
        }
        Outcome outcome = requireOutcome(event, outcomeTitle);

        double rawCost = LmsrCalculator.calculatePurchaseCost(event.getOutcomes(), outcome.getTitle(), sharesToBuy, event.getB());
        double feePaid = 0.0;
        if (event.getFeeType() == MarketEvent.FeeType.AT_PURCHASE) {
            feePaid = LmsrCalculator.calculateFee(rawCost, event.getFeePercentage(), event.getB());
        }

        double totalCost = rawCost + feePaid;

        double available = user.getAccount().getAvailableBalance();
        if (available < totalCost) {
            throw new IllegalStateException("This purchase costs " + formatAmount(totalCost) + ", but only "
                    + formatAmount(available) + " is available" + reservedSuffix(user) + ".");
        }

        String purchase = formatAmount(sharesToBuy) + " '" + outcome.getTitle() + "' shares";
        user.getAccount().withdraw(totalCost,
                "Bought " + purchase + " in '" + event.getName() + "'" + feeSuffix(feePaid));
        event.getEventAccount().deposit(rawCost, "'" + user.getName() + "' bought " + purchase);
        positionOf(user, event).addShareInvestment(outcome.getTitle(), rawCost);
        payCommission(event, user, feePaid);

        outcome.addShares(sharesToBuy);
        user.addShares(event.getName(), outcome.getTitle(), sharesToBuy);

        event.addTransaction(new Transaction(user.getName(), null, outcome.getTitle(), sharesToBuy, rawCost, feePaid));
    }

    @Override
    public synchronized void openEvent(String userName, String eventName) {
        User marketMaker = requireUser(userName);
        MarketEvent event = requireEvent(eventName);
        requireMarketMaker(event, marketMaker, "open");
        if (event.getStatus() != MarketEvent.EventStatus.NOT_STARTED) {
            throw new IllegalStateException("Event '" + event.getName() + "' has already been opened.");
        }

        double openingCost = calculateOpeningCost(event);
        double available = marketMaker.getAccount().getAvailableBalance();
        if (available < openingCost) {
            throw new IllegalStateException("Opening '" + event.getName() + "' costs " + formatAmount(openingCost)
                    + ", but market maker '" + marketMaker.getName() + "' has only " + formatAmount(available)
                    + " available" + reservedSuffix(marketMaker) + ".");
        }

        event.open();
        if (openingCost > 0) {
            marketMaker.getAccount().withdraw(openingCost, "Opened event '" + event.getName() + "'");
            event.getEventAccount().deposit(openingCost, "Opening funds from market maker '" + marketMaker.getName() + "'");
        }
        positionOf(marketMaker, event).addInvestment(openingCost);
        if (event.getTradingMethod() == MarketEvent.TradingMethod.ORDER_BOOK) {
            giveInitialSharePairs(event, marketMaker);
        }
    }

    // LMSR: the subsidy is the cost function before any purchase, b * ln(number of outcomes).
    // Order Book: the market maker invests the 'initial' amount and receives initial / d pairs of shares.
    private static double calculateOpeningCost(MarketEvent event) {
        if (event.getTradingMethod() == MarketEvent.TradingMethod.LMSR) {
            return LmsrCalculator.calculateCostFunction(event.getOutcomes(), event.getB());
        }
        return event.getInitialInvestment();
    }

    private static void giveInitialSharePairs(MarketEvent event, User marketMaker) {
        double pairs = event.getInitialInvestment() / event.getD();
        if (pairs <= 0) {
            return;
        }
        for (Outcome outcome : event.getOutcomes()) {
            marketMaker.addShares(event.getName(), outcome.getTitle(), pairs);
        }
    }

    @Override
    public synchronized void closeEvent(String userName, String eventName, String winningOutcomeTitle) {
        User marketMaker = requireUser(userName);
        MarketEvent event = requireEvent(eventName);
        requireMarketMaker(event, marketMaker, "close");
        requireActive(event);
        Outcome winningOutcome = requireOutcome(event, winningOutcomeTitle);

        double totalPayout = countHeldShares(event, winningOutcome) * event.getPayoutPerShare();
        double eventBalance = event.getEventAccount().getBalance();
        if (totalPayout > eventBalance + EPSILON) {
            throw new IllegalStateException("Event '" + event.getName() + "' cannot cover the payouts: needs "
                    + formatAmount(totalPayout) + " but holds " + formatAmount(eventBalance) + ".");
        }

        event.close(winningOutcome.getTitle());
        cancelOpenOrders(event);
        payWinners(event, winningOutcome);
        returnRemainingBalance(event, marketMaker);
    }

    private double countHeldShares(MarketEvent event, Outcome outcome) {
        double total = 0.0;
        for (User user : usersMap.values()) {
            total += user.getSharesCount(event.getName(), outcome.getTitle());
        }
        return total;
    }

    private void payWinners(MarketEvent event, Outcome winningOutcome) {
        for (User holder : usersMap.values()) {
            double winningShares = holder.getSharesCount(event.getName(), winningOutcome.getTitle());
            if (winningShares <= 0) {
                continue;
            }
            // Math.min only absorbs floating-point rounding; coverage was verified before closing.
            double grossPayout = Math.min(winningShares * event.getPayoutPerShare(), event.getEventAccount().getBalance());
            double fee = (event.getFeeType() == MarketEvent.FeeType.AT_RESOLUTION)
                    ? grossPayout * (event.getFeePercentage() / 100.0)
                    : 0.0;

            String winnings = formatAmount(winningShares) + " winning '" + winningOutcome.getTitle() + "' shares";
            event.getEventAccount().withdraw(grossPayout, "Payout to '" + holder.getName() + "' for " + winnings);
            holder.getAccount().deposit(grossPayout - fee, "Payout for " + winnings + " in '" + event.getName() + "'"
                    + ((fee > 0) ? " (fee " + formatAmount(fee) + " deducted)" : ""));
            positionOf(holder, event).addReceived(grossPayout);
            payCommission(event, holder, fee);
        }
    }

    private static void returnRemainingBalance(MarketEvent event, User marketMaker) {
        double remainingBalance = event.getEventAccount().getBalance();
        if (remainingBalance > 0) {
            event.getEventAccount().withdraw(remainingBalance,
                    "Remaining balance returned to market maker '" + marketMaker.getName() + "'");
            marketMaker.getAccount().deposit(remainingBalance,
                    "Remaining balance of closed event '" + event.getName() + "'");
            positionOf(marketMaker, event).addReceived(remainingBalance);
        }
    }

    @Override
    public synchronized void addOrder(String userName, String eventName, String outcomeTitle,
                         String sideStr, double price, double shares) {
        if (price <= 0 || shares <= 0) {
            throw new IllegalArgumentException("Price and shares must be positive.");
        }

        User user = requireUser(userName);
        MarketEvent event = requireEvent(eventName);
        requireActive(event);
        if (event.getTradingMethod() != MarketEvent.TradingMethod.ORDER_BOOK) {
            throw new IllegalArgumentException("Event '" + event.getName() + "' does not use ORDER_BOOK trading.");
        }
        Outcome outcome = requireOutcome(event, outcomeTitle);

        double maxPrice = event.getD() - PRICE_TICK;
        if (price > maxPrice + EPSILON) {
            throw new IllegalArgumentException("Price " + formatAmount(price) + " is too high: the maximum price in '"
                    + event.getName() + "' is " + formatAmount(maxPrice) + " (d - " + formatAmount(PRICE_TICK) + ").");
        }

        OrderSide side = parseOrderSide(sideStr);
        if (side == OrderSide.BUY) {
            reserveFundsForBuyOrder(event, user, price, shares);
        } else {
            requireAvailableShares(event, outcome, user, shares);
        }
        positionOf(user, event); // placing an order is enough to count as taking part in the event

        Order order = new Order(nextOrderNumber++, user.getName(), event.getName(), outcome.getTitle(), side, price, shares);
        OrderBook ownBook = event.getOrCreateOrderBook(outcome.getTitle());
        OrderBook mintBook = event.isAllowMint()
                ? event.getOrCreateOrderBook(event.getOtherOutcome(outcome).getTitle())
                : null;
        for (Fill fill : OrderMatcher.match(order, ownBook, mintBook, event.getD())) {
            executeFill(event, fill);
        }
    }

    private static OrderSide parseOrderSide(String sideStr) {
        if (sideStr != null) {
            for (OrderSide side : OrderSide.values()) {
                if (side.name().equalsIgnoreCase(sideStr.trim())) {
                    return side;
                }
            }
        }
        throw new IllegalArgumentException("Order side must be BUY or SELL, got '" + sideStr + "'.");
    }

    // The most a buy order can ever cost: every share at its limit price, plus the purchase fee (if any).
    private double calculateBuyReservation(MarketEvent event, double price, double shares) {
        double amount = price * shares;
        return amount + calculateTradeFee(event, amount);
    }

    private void reserveFundsForBuyOrder(MarketEvent event, User buyer, double price, double shares) {
        double required = calculateBuyReservation(event, price, shares);
        double available = buyer.getAccount().getAvailableBalance();
        if (available < required) {
            throw new IllegalStateException("This buy order needs " + formatAmount(required) + " (including fees), but only "
                    + formatAmount(available) + " is available" + reservedSuffix(buyer) + ".");
        }
        buyer.getAccount().reserve(required);
    }

    private static void requireAvailableShares(MarketEvent event, Outcome outcome, User seller, double shares) {
        double held = seller.getSharesCount(event.getName(), outcome.getTitle());
        OrderBook orderBook = event.getOrderBook(outcome.getTitle());
        double alreadyOffered = (orderBook == null) ? 0.0 : orderBook.getOpenSellShares(seller.getName());
        double available = held - alreadyOffered;
        if (shares > available + EPSILON) {
            throw new IllegalStateException("You hold " + formatAmount(held) + " '" + outcome.getTitle() + "' shares and "
                    + formatAmount(alreadyOffered) + " of them are already offered in open sell orders, so at most "
                    + formatAmount(Math.max(available, 0.0)) + " can be sold.");
        }
    }

    private void executeFill(MarketEvent event, Fill fill) {
        User buyer = requireUser(fill.buyerName());
        Outcome outcome = requireOutcome(event, fill.outcomeTitle());

        double amount = fill.price() * fill.shares();
        double fee = calculateTradeFee(event, amount);
        String traded = formatAmount(fill.shares()) + " '" + outcome.getTitle() + "' shares in '"
                + event.getName() + "' at " + formatAmount(fill.price());

        // The buyer's money was reserved at the order's limit price; any saving from a cheaper price is released.
        double reservedForFill = calculateBuyReservation(event, fill.buyerLimitPrice(), fill.shares());
        buyer.getAccount().withdrawReserved(amount + fee,
                "Bought " + traded + (fill.isMinted() ? " (newly minted)" : "") + feeSuffix(fee));
        double unusedReservation = reservedForFill - (amount + fee);
        if (unusedReservation > EPSILON) {
            buyer.getAccount().release(unusedReservation);
        }
        buyer.addShares(event.getName(), outcome.getTitle(), fill.shares());
        positionOf(buyer, event).addShareInvestment(outcome.getTitle(), amount);

        if (fill.isMinted()) {
            // New shares were created: the payment backs them in the event account, to be paid out when it closes.
            event.getEventAccount().deposit(amount, "'" + buyer.getName() + "' paid for minted " + traded);
        } else {
            User seller = requireUser(fill.sellerName());
            seller.getAccount().deposit(amount, "Sold " + traded);
            seller.deductShares(event.getName(), outcome.getTitle(), fill.shares());
            positionOf(seller, event).addReceived(amount);
        }

        payCommission(event, buyer, fee);
        event.addTransaction(new Transaction(buyer.getName(), fill.sellerName(), outcome.getTitle(), fill.shares(), amount, fee));
    }

    // When an event closes, open orders can no longer execute: they are removed and their reserved money is released.
    private void cancelOpenOrders(MarketEvent event) {
        for (Outcome outcome : event.getOutcomes()) {
            OrderBook orderBook = event.getOrderBook(outcome.getTitle());
            if (orderBook == null) {
                continue;
            }
            for (Order order : orderBook.cancelAllOrders()) {
                if (order.getSide() == OrderSide.BUY) {
                    requireUser(order.getUserName()).getAccount()
                            .release(calculateBuyReservation(event, order.getPrice(), order.getSharesCount()));
                }
            }
        }
    }

    @Override
    public synchronized OrderBookDto getOrderBook(String eventName, String outcomeTitle) {
        MarketEvent event = getEventByNameInternal(eventName);
        if (event == null) {
            return null;
        }
        Outcome outcome = event.getOutcomeByTitle(outcomeTitle);
        if (outcome == null) {
            return null;
        }

        OrderBook orderBook = event.getOrderBook(outcome.getTitle());
        if (orderBook == null) {
            return new OrderBookDto(event.getName(), outcome.getTitle(), List.of(), List.of(),
                    null, null, null, null, null);
        }

        List<OrderDto> buyDtos = orderBook.getBids().stream()
                .map(this::toOrderDto)
                .toList();

        List<OrderDto> sellDtos = orderBook.getAsks().stream()
                .map(this::toOrderDto)
                .toList();

        // MID and SPREAD are meaningful only when both sides of the book have orders.
        Double bestBid = orderBook.getBestBidPrice();
        Double bestAsk = orderBook.getBestAskPrice();
        boolean hasBothSides = (bestBid != null && bestAsk != null);
        Double midPrice = hasBothSides ? (bestBid + bestAsk) / 2 : null;
        Double spread = hasBothSides ? bestAsk - bestBid : null;

        return new OrderBookDto(event.getName(), outcome.getTitle(), buyDtos, sellDtos,
                orderBook.getLastTradePrice(), bestBid, bestAsk, midPrice, spread);
    }

    private double calculateTradeFee(MarketEvent event, double tradeAmount) {
        if (event.getFeeType() == MarketEvent.FeeType.AT_PURCHASE) {
            return tradeAmount * (event.getFeePercentage() / 100.0);
        }
        return 0.0;
    }

    private User getUserByNameInternal(String name) {
        return (name != null) ? usersMap.get(toUserKey(name)) : null;
    }

    private static String toUserKey(String userName) {
        return userName.trim().toLowerCase();
    }

    private MarketEvent getEventByNameInternal(String name) {
        return (name != null) ? eventsMap.get(toEventKey(name)) : null;
    }

    private static String toEventKey(String eventName) {
        return eventName.trim().toLowerCase();
    }

    private User requireUser(String userName) {
        User user = getUserByNameInternal(userName);
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' was not found.");
        }
        return user;
    }

    private MarketEvent requireEvent(String eventName) {
        MarketEvent event = getEventByNameInternal(eventName);
        if (event == null) {
            throw new IllegalArgumentException("Event '" + eventName + "' was not found.");
        }
        return event;
    }

    private static Outcome requireOutcome(MarketEvent event, String outcomeTitle) {
        Outcome outcome = event.getOutcomeByTitle(outcomeTitle);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome '" + outcomeTitle + "' does not exist in event '" + event.getName() + "'.");
        }
        return outcome;
    }

    private static void requireActive(MarketEvent event) {
        switch (event.getStatus()) {
            case NOT_STARTED -> throw new IllegalStateException(
                    "Event '" + event.getName() + "' has not been opened by its market maker yet.");
            case CLOSED -> throw new IllegalStateException("Event '" + event.getName() + "' is closed.");
            case ACTIVE -> { }
        }
    }

    private static void requireMarketMaker(MarketEvent event, User user, String action) {
        if (!user.getName().equals(event.getMarketMakerName())) {
            throw new IllegalArgumentException("Only the market maker of '" + event.getName() + "' ("
                    + event.getMarketMakerName() + ") can " + action + " it.");
        }
    }

    private User getMarketMaker(MarketEvent event) {
        User marketMaker = getUserByNameInternal(event.getMarketMakerName());
        if (marketMaker == null) {
            throw new IllegalStateException("Market maker of event '" + event.getName() + "' was not found.");
        }
        return marketMaker;
    }

    // The payer's account was already charged (the fee is part of what they paid); this delivers it to the market maker.
    private void payCommission(MarketEvent event, User payer, double fee) {
        if (fee <= 0) {
            return;
        }
        User marketMaker = getMarketMaker(event);
        marketMaker.getAccount().deposit(fee, "Commission from '" + payer.getName() + "' in '" + event.getName() + "'");
        event.addFeeCollected(fee);
        positionOf(payer, event).addFeePaid(fee);
        positionOf(marketMaker, event).addCommissionEarned(fee);
    }

    private static Position positionOf(User user, MarketEvent event) {
        return user.getOrCreatePosition(event.getName());
    }

    private static String formatAmount(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String feeSuffix(double fee) {
        return (fee > 0) ? " (incl. fee " + formatAmount(fee) + ")" : "";
    }

    private static String reservedSuffix(User user) {
        double reserved = user.getAccount().getReserved();
        return (reserved > 0) ? " (" + formatAmount(reserved) + " is held for open buy orders)" : "";
    }
}
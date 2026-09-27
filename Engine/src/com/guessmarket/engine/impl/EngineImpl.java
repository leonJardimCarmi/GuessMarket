package com.guessmarket.engine.impl;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.*;
import com.guessmarket.engine.model.*;
import com.guessmarket.engine.schema.*;

import java.io.*;
import java.util.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

public class EngineImpl implements EngineApi, Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, MarketEvent> eventsMap = new HashMap<>();
    private final Map<String, User> usersMap = new HashMap<>();

    private MarketEventDto createDtoEvent(MarketEvent event) {
        List<OutcomeDto> outcomeDtos = new ArrayList<>();
        for (Outcome outcome : event.getOutcomes()) {
            double currentPrice = 0.0;
            if (event.getTradingMethod() == MarketEvent.TradingMethod.LMSR) {
                currentPrice = LmsrCalculator.calculatePrice(event.getOutcomes(), outcome.getTitle(), event.getB());
            } else if (event.getTradingMethod() == MarketEvent.TradingMethod.ORDER_BOOK) {
                // Determine current price based on the last transaction price for this outcome
                currentPrice = event.getTransactions().stream()
                        .filter(tx -> tx.getOutcomeTitle().equalsIgnoreCase(outcome.getTitle()))
                        .reduce((first, second) -> second) // Get last transaction
                        .map(tx -> tx.getShareAmount() > 0 ? (tx.getTotalPaid() / tx.getShareAmount()) : 0.0)
                        .orElse(0.0);
            }
            outcomeDtos.add(new OutcomeDto(outcome.getTitle(), outcome.getSharesBought(), currentPrice));
        }

        List<TransactionDto> transactionDtos = new ArrayList<>();
        for (Transaction transaction : event.getTransactions()) {
            transactionDtos.add(new TransactionDto(
                    transaction.getUserName(),
                    transaction.getOutcomeTitle(),
                    transaction.getShareAmount(),
                    transaction.getTotalPaid(),
                    transaction.getFeePaid()
            ));
        }

        String mmName = event.getMarketMakerName();
        double eventBalance = event.getEventAccount().getBalance();

        return new MarketEventDto(
                event.getName(),
                event.getDescription(),
                event.isActive(),
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
        Map<String, Map<String, Double>> holdings = new HashMap<>();
        for (MarketEvent event : eventsMap.values()) {
            Map<String, Double> eventHoldings = new HashMap<>();
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
        return new UserDto(user.getName(), user.getAccount().getBalance(), holdings);
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

    /*@Override
    public void loadMarketDataFromXml(String filePath) throws Exception {
        File file = new File(filePath);
        if (!filePath.toLowerCase().endsWith(".xml") || !file.exists()) {
            throw new IllegalArgumentException("Invalid XML file path: " + filePath);
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        GuessMarket guessMarket = (GuessMarket) unmarshaller.unmarshal(file);

        Map<String, MarketEvent> tempEventsMap = new HashMap<>();
        Map<String, User> tempUsersMap = new HashMap<>();

        if (guessMarket.getGMEvents() != null && guessMarket.getGMEvents().getGMEvent() != null) {
            for (GMEvent xmlEvent : guessMarket.getGMEvents().getGMEvent()) {
                String id = String.valueOf(xmlEvent.getId());
                if (tempEventsMap.containsKey(id)) {
                    throw new IllegalArgumentException("Duplicate event ID: " + id);
                }

                String title = (xmlEvent.getName() != null) ? String.join(" ", xmlEvent.getName()) : "";
                String description = xmlEvent.getDescription();

                double feePercentage = 0.0;
                MarketEvent.FeeType feeType = MarketEvent.FeeType.AT_PURCHASE;
                if (xmlEvent.getCommission() != null) {
                    feePercentage = xmlEvent.getCommission().getValue();
                    if ("on-resolution".equalsIgnoreCase(xmlEvent.getCommission().getType())) {
                        feeType = MarketEvent.FeeType.AT_RESOLUTION;
                    }
                }

                double b = 0.0;
                double initialShares = 0.0;
                boolean allowMint = false;
                double d = 0.0;

                MarketEvent.TradingMethod tradingMethod = MarketEvent.TradingMethod.LMSR;
                if (xmlEvent.getGMMethod() != null) {
                    if (xmlEvent.getGMMethod().getGMLMSR() != null) {
                        b = xmlEvent.getGMMethod().getGMLMSR().getB();
                        tradingMethod = MarketEvent.TradingMethod.LMSR;
                    } else if (xmlEvent.getGMMethod().getGMOrderBook() != null) {
                        tradingMethod = MarketEvent.TradingMethod.ORDER_BOOK;

                        GMOrderBook ob = xmlEvent.getGMMethod().getGMOrderBook();
                        initialShares = ob.getInitial();
                        allowMint = ob.isAllowMint();
                        d = ob.getD();
                    }
                }

                MarketEvent event = new MarketEvent(id, title, description, feePercentage, feeType, tradingMethod, b, initialShares, allowMint, d);

                if (xmlEvent.getGMOptions() != null && xmlEvent.getGMOptions().getGMOption() != null) {
                    for (String optionTitle : xmlEvent.getGMOptions().getGMOption()) {
                        event.addOutcome(new Outcome(optionTitle));
                    }
                }

                if (event.getOutcomes().size() < 2) {
                    throw new IllegalArgumentException("Event " + id + " must have at least 2 outcomes.");
                }

                tempEventsMap.put(id, event);
            }
        }

        if (guessMarket.getGMUsers() != null && guessMarket.getGMUsers().getGMUser() != null) {
            for (GMUser xmlUser : guessMarket.getGMUsers().getGMUser()) {
                String userName = xmlUser.getName();
                double initialCash = xmlUser.getInitialCash();

                if (tempUsersMap.containsKey(userName.toLowerCase())) {
                    throw new IllegalArgumentException("Duplicate user name: " + userName);
                }
                if (initialCash < 0) {
                    throw new IllegalArgumentException("Negative initial cash for user: " + userName);
                }

                User user = new User(userName, initialCash);
                tempUsersMap.put(userName.toLowerCase(), user);

                if (xmlUser.getGMMarketMaker() != null && xmlUser.getGMMarketMaker().getEvent() != null) {
                    for (Event mmEvent : xmlUser.getGMMarketMaker().getEvent()) {
                        String eventId = String.valueOf(mmEvent.getId());
                        MarketEvent targetEvent = tempEventsMap.get(eventId);

                        if (targetEvent == null) {
                            throw new IllegalArgumentException("Market Maker " + userName + " referenced non-existing event ID: " + eventId);
                        }
                        if (targetEvent.getMarketMakerName() != null) {
                            throw new IllegalArgumentException("Event " + eventId + " already has a Market Maker assigned!");
                        }

                        targetEvent.setMarketMakerName(userName);

                        if (targetEvent.getTradingMethod() == MarketEvent.TradingMethod.ORDER_BOOK) {
                            double initialShares = targetEvent.getInitialShares();
                            double d = targetEvent.getD();

                            double totalInitialCost = initialShares * d;

                            if (user.getBalance() < totalInitialCost) {
                                throw new IllegalArgumentException("Market Maker " + userName +
                                        " has insufficient cash (" + user.getBalance() + ") to fund initial shares cost (" + totalInitialCost + ") for event " + eventId);
                            }

                            user.withdraw(totalInitialCost);
                            targetEvent.addMarketMakerFund(totalInitialCost);

                            for (Outcome outcome : targetEvent.getOutcomes()) {
                                user.addShares(targetEvent.getId(), outcome.getTitle(), initialShares);
                            }
                        }
                    }
                }
            }
        }

        for (MarketEvent event : tempEventsMap.values()) {
            if (event.getMarketMakerName() == null) {
                throw new IllegalArgumentException("Event ID " + event.getId() + " has no Market Maker assigned.");
            }
        }

        this.eventsMap.clear();
        this.eventsMap.putAll(tempEventsMap);

        this.usersMap.clear();
        this.usersMap.putAll(tempUsersMap);
    }
*/

    @Override
    public List<String> loadEventsFromXml(InputStream xmlContent, String uploaderName) {
        User uploader = getUserByNameInternal(uploaderName);
        if (uploader == null) {
            throw new IllegalArgumentException("User '" + uploaderName + "' was not found.");
        }

        GuessMarket guessMarket = parseXml(xmlContent);
        if (guessMarket.getGMEvents() == null || guessMarket.getGMEvents().getGMEvent().isEmpty()) {
            throw new IllegalArgumentException("The file contains no events.");
        }
        // TODO: build and add events (steps 2-4)
        return List.of();
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

    @Override
    public List<MarketEventDto> getAllMarketEvents() {
        List<MarketEventDto> dtos = new ArrayList<>();
        for (MarketEvent event : eventsMap.values()) {
            dtos.add(createDtoEvent(event));
        }
        return dtos;
    }

    @Override
    public MarketEventDto getMarketEventByName(String eventName) {
        MarketEvent event = getEventByNameInternal(eventName);
        return (event == null) ? null : createDtoEvent(event);
    }

    @Override
    public List<UserDto> getAllUsers() {
        List<UserDto> dtos = new ArrayList<>();
        for (User user : usersMap.values()) {
            dtos.add(createDtoUser(user));
        }
        return dtos;
    }

    @Override
    public UserDto getUserByName(String userName) {
        User user = getUserByNameInternal(userName);
        return (user != null) ? createDtoUser(user) : null;
    }

    @Override
    public void depositFunds(String userName, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        User user = getUserByNameInternal(userName);
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' was not found.");
        }
        user.getAccount().deposit(amount);
    }

    @Override
    public void buySharesLMSR(String userName, String eventName, String outcomeTitle, double sharesToBuy) {
        User user = getUserByNameInternal(userName);
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' was not found.");
        }

        MarketEvent event = getEventByNameInternal(eventName);
        if (event == null) {
            throw new IllegalArgumentException("Event '" + eventName + "' was not found.");
        }
        if (!event.isActive()) {
            throw new IllegalStateException("Event '" + event.getName() + "' is closed.");
        }
        if (event.getTradingMethod() != MarketEvent.TradingMethod.LMSR) {
            throw new IllegalArgumentException("Event '" + event.getName() + "' does not use LMSR trading.");
        }

        Outcome outcome = event.getOutcomeByTitle(outcomeTitle);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome '" + outcomeTitle + "' does not exist in event '" + event.getName() + "'.");
        }

        double rawCost = LmsrCalculator.calculatePurchaseCost(event.getOutcomes(), outcome.getTitle(), sharesToBuy, event.getB());
        double feePaid = 0.0;
        if (event.getFeeType() == MarketEvent.FeeType.AT_PURCHASE) {
            feePaid = LmsrCalculator.calculateFee(rawCost, event.getFeePercentage(), event.getB());
        }

        double totalCost = rawCost + feePaid;

        if (user.getAccount().getBalance() < totalCost) {
            throw new IllegalStateException("Insufficient funds. Required: " + totalCost + ", Available: " + user.getAccount().getBalance());
        }

        user.getAccount().withdraw(totalCost);

        event.getEventAccount().deposit(totalCost);
        if (feePaid > 0) {
            event.addFeeCollected(feePaid);
        }

        outcome.addShares(sharesToBuy);
        user.addShares(event.getName(), outcome.getTitle(), sharesToBuy);

        event.addTransaction(new Transaction(user.getName(), outcome.getTitle(), sharesToBuy, rawCost, feePaid));
    }

    @Override
    public void closeMarket(String eventName, String winningOutcomeTitle) {
        MarketEvent event = getEventByNameInternal(eventName);
        if (event == null) {
            throw new IllegalArgumentException("Event '" + eventName + "' was not found.");
        }
        if (!event.isActive()) {
            throw new IllegalStateException("Event '" + event.getName() + "' is already closed.");
        }

        Outcome winningOutcome = event.getOutcomeByTitle(winningOutcomeTitle);
        if (winningOutcome == null) {
            throw new IllegalArgumentException("Outcome '" + winningOutcomeTitle + "' does not exist in event '" + event.getName() + "'.");
        }

        event.closeEvent(winningOutcome.getTitle());

        for (User user : usersMap.values()) {
            double winningShares = user.getSharesCount(event.getName(), winningOutcome.getTitle());
            if (winningShares > 0) {
                double grossPayout = winningShares * 1.0;
                double fee = 0.0;

                if (event.getFeeType() == MarketEvent.FeeType.AT_RESOLUTION) {
                    fee = grossPayout * (event.getFeePercentage() / 100.0);
                }

                double netPayout = grossPayout - fee;

                event.getEventAccount().withdraw(grossPayout);
                user.getAccount().deposit(netPayout);

                if (fee > 0) {
                    event.addFeeCollected(fee);
                }
            }
        }

        String mmName = event.getMarketMakerName();
        User mmUser = (mmName != null) ? getUserByNameInternal(mmName) : null;
        if (mmUser != null) {
            double remainingBalance = event.getEventAccount().getBalance();
            if (remainingBalance > 0) {
                event.getEventAccount().withdraw(remainingBalance);
                mmUser.getAccount().deposit(remainingBalance);
            }
        }
    }

    @Override
    public void addOrder(String userName, String eventName, String outcomeTitle,
                         String sideStr, double price, double shares) {
        if (price <= 0 || shares <= 0) {
            throw new IllegalArgumentException("Price and shares must be positive.");
        }

        MarketEvent event = getEventByNameInternal(eventName);
        if (event == null) {
            throw new IllegalArgumentException("Event '" + eventName + "' was not found.");
        }
        if (!event.isActive()) {
            throw new IllegalStateException("Event '" + event.getName() + "' is closed.");
        }
        if (event.getTradingMethod() != MarketEvent.TradingMethod.ORDER_BOOK) {
            throw new IllegalArgumentException("Event '" + event.getName() + "' does not use ORDER_BOOK trading.");
        }

        Outcome outcome = event.getOutcomeByTitle(outcomeTitle);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome '" + outcomeTitle + "' does not exist in event '" + event.getName() + "'.");
        }

        if (price > event.getD()) {
            throw new IllegalArgumentException("Price (" + price + ") exceeds maximum allowed price d (" + event.getD() + ") for this event.");
        }

        User user = getUserByNameInternal(userName);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        OrderSide side = OrderSide.valueOf(sideStr.toUpperCase());

        double maxTradeCost = price * shares;
        double potentialFee = calculateTradeFee(event, maxTradeCost);

        if (side == OrderSide.BUY) {
            if (user.getAccount().getBalance() < (maxTradeCost + potentialFee)) {
                throw new IllegalStateException("Insufficient balance to place buy order.");
            }
        } else { // SELL
            double ownedShares = user.getSharesCount(event.getName(), outcome.getTitle());
            if (ownedShares < shares) {
                throw new IllegalStateException("Insufficient shares to place sell order.");
            }
        }

        String orderId = "ORD-" + System.currentTimeMillis();
        Order order = new Order(orderId, user.getName(), event.getName(), outcome.getTitle(), side, price, shares);

        OrderBook orderBook = event.getOrCreateOrderBook(outcome.getTitle());

        List<OrderBook.TradeResult> trades = orderBook.processOrder(order);

        for (OrderBook.TradeResult trade : trades) {
            User buyer = getUserByNameInternal(trade.getBuyerName());
            User seller = getUserByNameInternal(trade.getSellerName());

            double tradeAmount = trade.getPrice() * trade.getShares();
            double fee = calculateTradeFee(event, tradeAmount);

            if (buyer != null) {
                buyer.getAccount().withdraw(tradeAmount + fee);
                buyer.addShares(event.getName(), outcome.getTitle(), trade.getShares());
            }

            if (seller != null) {
                seller.getAccount().deposit(tradeAmount);
                seller.deductShares(event.getName(), outcome.getTitle(), trade.getShares());
            }

            if (fee > 0) {
                event.getEventAccount().deposit(fee);
                event.addFeeCollected(fee);
            }

            Transaction tx = new Transaction(
                    trade.getBuyerName(),
                    outcome.getTitle(),
                    trade.getShares(),
                    tradeAmount,
                    fee
            );
            event.addTransaction(tx);
        }
    }

    @Override
    public OrderBookDto getOrderBook(String eventName, String outcomeTitle) {
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
            return new OrderBookDto(event.getName(), outcome.getTitle(), List.of(), List.of());
        }

        List<OrderDto> buyDtos = orderBook.getBids().stream()
                .map(this::toOrderDto)
                .toList();

        List<OrderDto> sellDtos = orderBook.getAsks().stream()
                .map(this::toOrderDto)
                .toList();

        return new OrderBookDto(event.getName(), outcome.getTitle(), buyDtos, sellDtos);
    }

    private double calculateTradeFee(MarketEvent event, double tradeAmount) {
        if (event.getFeeType() == MarketEvent.FeeType.AT_PURCHASE) {
            return tradeAmount * (event.getFeePercentage() / 100.0);
        }
        return 0.0;
    }

    @Override
    public void saveStateToFile(String filePath) throws IOException {
        String fullPath = ensureExtension(filePath);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fullPath))) {
            out.writeObject(this);
        }
    }

    public static EngineImpl loadStateFromFile(String filePath) throws IOException, ClassNotFoundException {
        String fullPath = ensureExtension(filePath);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fullPath))) {
            return (EngineImpl) in.readObject();
        }
    }

    private static String ensureExtension(String filePath) {
        if (!filePath.endsWith(".dat")) {
            return filePath + ".dat";
        }
        return filePath;
    }

    private User getUserByNameInternal(String name) {
        return (name != null) ? usersMap.get(name.toLowerCase()) : null;
    }

    private MarketEvent getEventByNameInternal(String name) {
        return (name != null) ? eventsMap.get(name.trim().toLowerCase()) : null;
    }

    public void addNewUser(String name, double balance) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name cannot be empty.");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }

        String key = name.toLowerCase();
        if (usersMap.containsKey(key)) {
            throw new IllegalArgumentException("User with name '" + name + "' already exists.");
        }

        User newUser = new User(name, balance);
        usersMap.put(key, newUser);
    }
}
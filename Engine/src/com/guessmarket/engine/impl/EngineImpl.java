package com.guessmarket.engine.impl;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.*;
import com.guessmarket.engine.model.*;
import com.guessmarket.engine.schema.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.util.*;

public class EngineImpl implements EngineApi, Serializable {
    private static final long serialVersionUID = 1L;

    private static final int REQUIRED_OPTIONS_COUNT = 2;
    private static final String COMMISSION_ON_CLOSE = "on-close";
    private static final String COMMISSION_ON_PURCHASE = "on-purchase";

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
        double initialShares = 0.0;
        double d = 0.0;
        boolean allowMint = false;

        if (method.getGMLMSR() != null) {
            tradingMethod = MarketEvent.TradingMethod.LMSR;
            b = method.getGMLMSR().getB();
        } else {
            GMOrderBook orderBook = method.getGMOrderBook();
            tradingMethod = MarketEvent.TradingMethod.ORDER_BOOK;
            initialShares = orderBook.getInitial();
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
                initialShares,
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
    public List<AccountEntryDto> getAccountEntries(String userName, int fromIndex) {
        User user = getUserByNameInternal(userName);
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' was not found.");
        }
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
    public void depositFunds(String userName, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        User user = getUserByNameInternal(userName);
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' was not found.");
        }
        user.getAccount().deposit(amount, "Deposit");
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

        String purchase = formatAmount(sharesToBuy) + " '" + outcome.getTitle() + "' shares";
        user.getAccount().withdraw(totalCost,
                "Bought " + purchase + " in '" + event.getName() + "'" + feeSuffix(feePaid));
        event.getEventAccount().deposit(totalCost,
                "'" + user.getName() + "' bought " + purchase + feeSuffix(feePaid));
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

                String winnings = formatAmount(winningShares) + " winning '" + winningOutcome.getTitle() + "' shares";
                event.getEventAccount().withdraw(grossPayout,
                        "Payout to '" + user.getName() + "' for " + winnings);
                user.getAccount().deposit(netPayout,
                        "Payout for " + winnings + " in '" + event.getName() + "'"
                                + ((fee > 0) ? " (fee " + formatAmount(fee) + " deducted)" : ""));

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
                event.getEventAccount().withdraw(remainingBalance,
                        "Remaining balance returned to market maker '" + mmUser.getName() + "'");
                mmUser.getAccount().deposit(remainingBalance,
                        "Remaining balance of closed event '" + event.getName() + "'");
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
            String traded = formatAmount(trade.getShares()) + " '" + outcome.getTitle() + "' shares in '"
                    + event.getName() + "' at " + formatAmount(trade.getPrice());

            if (buyer != null) {
                buyer.getAccount().withdraw(tradeAmount + fee, "Bought " + traded + feeSuffix(fee));
                buyer.addShares(event.getName(), outcome.getTitle(), trade.getShares());
            }

            if (seller != null) {
                seller.getAccount().deposit(tradeAmount, "Sold " + traded);
                seller.deductShares(event.getName(), outcome.getTitle(), trade.getShares());
            }

            if (fee > 0) {
                event.getEventAccount().deposit(fee, "Fee from '" + trade.getBuyerName() + "' on trade of " + traded);
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
        return (name != null) ? eventsMap.get(toEventKey(name)) : null;
    }

    private static String toEventKey(String eventName) {
        return eventName.trim().toLowerCase();
    }

    private static String formatAmount(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String feeSuffix(double fee) {
        return (fee > 0) ? " (incl. fee " + formatAmount(fee) + ")" : "";
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
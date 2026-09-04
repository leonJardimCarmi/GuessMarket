package com.guessmarket.engine.impl;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.*;
import com.guessmarket.engine.model.*;
import com.guessmarket.engine.schema.*;

import java.io.*;
import java.util.*;
import jakarta.xml.bind.JAXBContext;
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
        User mmUser = (mmName != null) ? usersMap.get(mmName.toLowerCase()) : null;
        double mmBalance = (mmUser != null) ? mmUser.getAccount().getBalance() : 0.0;

        return new MarketEventDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.isActive(),
                event.getWinningOutcome(),
                mmName,
                mmBalance,
                event.getTotalFeesCollected(),
                event.getFeePercentage(),
                event.getFeeType().name(),
                event.getTradingMethod().name(),
                outcomeDtos,
                transactionDtos
        );
    }

    private UserDto createDtoUser(User user) {
        Map<String, Map<String, Double>> holdings = new HashMap<>();
        for (MarketEvent event : eventsMap.values()) {
            Map<String, Double> eventHoldings = new HashMap<>();
            for (Outcome outcome : event.getOutcomes()) {
                double shares = user.getSharesCount(event.getId(), outcome.getTitle());
                if (shares > 0) {
                    eventHoldings.put(outcome.getTitle(), shares);
                }
            }
            if (!eventHoldings.isEmpty()) {
                holdings.put(event.getId(), eventHoldings);
            }
        }
        return new UserDto(user.getName(), user.getAccount().getBalance(), holdings);
    }

    private OrderDto toOrderDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getUserName(),
                order.getEventId(),
                order.getOutcomeTitle(),
                order.getSide().name(),
                order.getActionType().name(),
                order.getPrice(),
                order.getSharesCount(),
                order.getOriginalSharesCount(),
                order.getTimestamp()
        );
    }

    @Override
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

                        // --- קריאת נתוני ה-Order Book מתוך ה-XML ---
                        GMOrderBook ob = xmlEvent.getGMMethod().getGMOrderBook();
                        initialShares = ob.getInitial(); // קריאת ה-100 מניות
                        allowMint = ob.isAllowMint();
                        d = ob.getD();
                    }
                }

                // עדכון הבנאי או ה-setters של MarketEvent לשמירת d, initialShares, allowMint
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

                        // --- תוספת קריטית: הוספת המניות ההתחלתיות ל-Market Maker ב-Order Book ---
                        if (targetEvent.getTradingMethod() == MarketEvent.TradingMethod.ORDER_BOOK) {
                            double initialShares = targetEvent.getInitialShares();
                            for (Outcome outcome : targetEvent.getOutcomes()) {
                                // הענקת כמות ה-initial לכל אופציה באירוע
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

    @Override
    public List<MarketEventDto> getAllMarketEvents() {
        List<MarketEventDto> dtos = new ArrayList<>();
        for (MarketEvent event : eventsMap.values()) {
            dtos.add(createDtoEvent(event));
        }
        return dtos;
    }

    @Override
    public MarketEventDto getMarketEventById(String eventId) {
        MarketEvent event = eventsMap.get(eventId);
        if (event == null) {
            return null;
        }
        return createDtoEvent(event);
    }

    @Override
    public List<UserDto> getAllUsers() {
        List<UserDto> dtos = new ArrayList<>();
        for (User user : usersMap.values()){
            dtos.add(createDtoUser(user));
        }
        return dtos;
    }

    @Override
    public UserDto getUserByName(String userName) {
        User user = usersMap.get(userName.toLowerCase());
        return (user != null) ? createDtoUser(user) : null;
    }

    @Override
    public void depositFunds(String userName, double amount){
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        User user = usersMap.get(userName.toLowerCase());
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' was not found.");
        }
        user.getAccount().deposit(amount);
    }

    @Override
    public void buySharesLMSR(String userName, String eventId, String outcomeTitle, double sharesToBuy) {
        User user = usersMap.get(userName.toLowerCase());
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' was not found.");
        }

        MarketEvent event = eventsMap.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event '" + eventId + "' was not found.");
        }
        if (!event.isActive()) {
            throw new IllegalStateException("Event '" + eventId + "' is closed.");
        }
        if (event.getTradingMethod() != MarketEvent.TradingMethod.LMSR) {
            throw new IllegalArgumentException("Event '" + eventId + "' does not use LMSR trading.");
        }

        Outcome outcome = event.getOutcomeByTitle(outcomeTitle);
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome '" + outcomeTitle + "' does not exist in event '" + eventId + "'.");
        }

        double rawCost = LmsrCalculator.calculatePurchaseCost(event.getOutcomes(), outcomeTitle, sharesToBuy, event.getB());
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
        user.addShares(eventId, outcomeTitle, sharesToBuy);

        event.addTransaction(new Transaction(user.getName(), outcomeTitle, sharesToBuy, rawCost, feePaid));
    }

    @Override
    public void closeMarket(String eventId, String winningOutcomeTitle) {
        MarketEvent event = eventsMap.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Market event with ID '" + eventId + "' was not found.");
        }
        if (!event.isActive()) {
            throw new IllegalStateException("Market event with ID '" + eventId + "' is already closed.");
        }

        Outcome winningOutcome = event.getOutcomeByTitle(winningOutcomeTitle);
        if (winningOutcome == null) {
            throw new IllegalArgumentException("Outcome '" + winningOutcomeTitle + "' does not exist in event '" + eventId + "'.");
        }

        event.closeEvent(winningOutcomeTitle);

        // 1. חלוקת תשלומים לזוכים מתוך חשבון האירוע
        for (User user : usersMap.values()) {
            double winningShares = user.getSharesCount(eventId, winningOutcomeTitle);
            if (winningShares > 0) {
                double grossPayout = winningShares * 1.0;
                double fee = 0.0;

                if (event.getFeeType() == MarketEvent.FeeType.AT_RESOLUTION) {
                    fee = grossPayout * (event.getFeePercentage() / 100.0);
                }

                double netPayout = grossPayout - fee;

                // משיכת מלוא הסכום (הזכייה) מחשבון האירוע כדי למנוע יצירת כסף יש מאין
                event.getEventAccount().withdraw(grossPayout);

                // הפקדת הנטו בחשבון המשתמש הזוכה
                user.getAccount().deposit(netPayout);

                if (fee > 0) {
                    // העמלה נשארת ב-eventAccount ותועבר ל-MM בסוף
                    event.addFeeCollected(fee);
                }
            }
        }

        // 2. העברת היתרה שנותרה בחשבון האירוע אל ה-Market Maker
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
    public void addOrder(String userName, String eventId, String outcomeTitle,
                         String sideStr, String actionTypeStr, double price, double shares) {
        if (price <= 0 || shares <= 0) {
            throw new IllegalArgumentException("Price and shares must be positive.");
        }

        MarketEvent event = eventsMap.get(eventId);
        if (event == null || !event.isActive()) {
            throw new IllegalStateException("Market event is either not found or closed.");
        }
        if (event.getTradingMethod() != MarketEvent.TradingMethod.ORDER_BOOK) {
            throw new IllegalArgumentException("Event '" + eventId + "' does not use ORDER_BOOK trading.");
        }

        User user = getUserByNameInternal(userName);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        OrderSide side = OrderSide.valueOf(sideStr.toUpperCase());
        OrderActionType actionType = OrderActionType.valueOf(actionTypeStr.toUpperCase());

        double totalCost = price * shares;
        double potentialFee = calculateTradeFee(event, totalCost);

        if (side == OrderSide.BUY) {
            if (user.getAccount().getBalance() < (totalCost + potentialFee)) {
                throw new IllegalStateException("Insufficient balance to place buy order.");
            }
        } else { // SELL
            double ownedShares = user.getSharesCount(eventId, outcomeTitle);
            if (ownedShares < shares) {
                throw new IllegalStateException("Insufficient shares to place sell order.");
            }
        }

        String orderId = "ORD-" + System.currentTimeMillis();
        Order order = new Order(orderId, userName, eventId, outcomeTitle, side, actionType, price, shares);

        OrderBook orderBook = event.getOrCreateOrderBook(outcomeTitle);

        if (actionType == OrderActionType.FOK) {
            if (!orderBook.canFullyFill(order)) {
                throw new IllegalStateException("FOK Order could not be fully filled and was cancelled.");
            }
        }

        List<OrderBook.TradeResult> trades = orderBook.processOrder(order);

        for (OrderBook.TradeResult trade : trades) {
            User buyer = getUserByNameInternal(trade.getBuyerName());
            User seller = getUserByNameInternal(trade.getSellerName());

            double tradeAmount = trade.getPrice() * trade.getShares();
            double fee = calculateTradeFee(event, tradeAmount);

            if (buyer != null) {
                buyer.getAccount().withdraw(tradeAmount + fee);
                buyer.addShares(eventId, outcomeTitle, trade.getShares());
            }

            if (seller != null) {
                seller.getAccount().deposit(tradeAmount);
                seller.deductShares(eventId, outcomeTitle, trade.getShares());
            }

            if (fee > 0) {
                event.getEventAccount().deposit(fee);
                event.addFeeCollected(fee);
            }

            Transaction tx = new Transaction(
                    trade.getBuyerName(),
                    outcomeTitle,
                    trade.getShares(),
                    tradeAmount + fee,
                    fee
            );
            event.addTransaction(tx);
        }
    }

    @Override
    public OrderBookDto getOrderBook(String eventId, String outcomeTitle) {
        MarketEvent event = eventsMap.get(eventId);
        if (event == null) {
            return null;
        }

        OrderBook orderBook = event.getOrderBook(outcomeTitle);
        if (orderBook == null) {
            return new OrderBookDto(eventId, outcomeTitle, List.of(), List.of());
        }

        List<OrderDto> buyDtos = orderBook.getBids().stream()
                .map(this::toOrderDto)
                .toList();

        List<OrderDto> sellDtos = orderBook.getAsks().stream()
                .map(this::toOrderDto)
                .toList();

        return new OrderBookDto(eventId, outcomeTitle, buyDtos, sellDtos);
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
}
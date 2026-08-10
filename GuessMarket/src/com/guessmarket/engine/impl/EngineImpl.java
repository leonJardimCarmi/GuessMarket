package com.guessmarket.engine.impl;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OutcomeDto;
import com.guessmarket.engine.dto.TransactionDto;
import com.guessmarket.engine.model.LmsrCalculator;
import com.guessmarket.engine.model.MarketEvent;
import com.guessmarket.engine.model.Outcome;
import com.guessmarket.engine.model.Transaction;
import com.guessmarket.engine.schema.STLCell;
import com.guessmarket.engine.schema.STLLayout;
import com.guessmarket.engine.schema.STLSheet;
import jdk.jfr.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;

public class EngineImpl implements EngineApi {

    private final Map<String , MarketEvent> eventsMap = new HashMap<>();

    public void addEventForTesting(String id, String title, String description, double initialBalance,
                                   double feePercentage, MarketEvent.FeeType feeType){

        MarketEvent event = new MarketEvent(id, title, description, initialBalance, feePercentage, feeType);

        event.addOutcome(new Outcome("YES"));
        event.addOutcome(new Outcome("NO"));

        eventsMap.put(id, event);
    }

    private MarketEventDto createDtoEvent(MarketEvent event){
        List<OutcomeDto> outcomeDtos = new ArrayList<>();
        for(Outcome outcome: event.getOutcomes()){
            double currentPrice = LmsrCalculator.calculatePrice(event.getOutcomes(), outcome.getTitle());
            outcomeDtos.add(new OutcomeDto(outcome.getTitle(),outcome.getSharesBought(), currentPrice));
        }

        List<TransactionDto> transactionDtos = new ArrayList<>();
        for(Transaction transaction : event.getTransactions()){
            transactionDtos.add(new TransactionDto(transaction.getOutcomeTitle(), transaction.getShareAmount(),
                    transaction.getTotalPaid(), transaction.getFeePaid()));
        }

        return new MarketEventDto(event.getId(),event.getTitle(),event.getDescription(),event.isActive(),
                event.getWinningOutcome(),event.getAccount().getBalance(), event.getAccount().getTotalFees(),
                event.getFeePercentage(), event.getFeeType().name(), outcomeDtos, transactionDtos);

    }

    @Override
    public void loadMarketDataFromXml(String filePath) throws Exception {
        File file = new File(filePath);
        eventsMap.clear();

        JAXBContext jaxbContext = JAXBContext.newInstance("com.guessmarket.engine.schema");
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        // 1. טעינת קובץ ה-XML והמרה למחלקת השורש החדשה
        GuessMarket guessMarket = (GuessMarket) jaxbUnmarshaller.unmarshal(file);

        // 2. ריצה על האירועים ב-XML וחילוץ המידע המלא
        for (GMEvent xmlEvent : guessMarket.getGMEvents().getGMEvent()) {
            String id = xmlEvent.getId();
            String title = xmlEvent.getName();
            String description = xmlEvent.getDescription();
            double feePercentage = xmlEvent.getComision().getValue(); // או המתודה שנוצרה ב-Comision

            // יצירת MarketEvent עם המידע האמיתי מה-XML
            MarketEvent event = new MarketEvent(
                    id,
                    title,
                    description,
                    0.0,
                    feePercentage,
                    MarketEvent.FeeType.AT_PURCHASE
            );

            eventsMap.put(event.getId(), event);
        }
    }

    @Override
    public List<MarketEventDto> getAllMarketEvents() {
        List<MarketEventDto> dtos = new ArrayList<>();
        for (MarketEvent event : eventsMap.values()){
            dtos.add(createDtoEvent(event));
        }
        return dtos;
    }

    @Override
    public MarketEventDto getMarketEventById(String eventId) {
        MarketEvent event = eventsMap.get(eventId);
        if( event == null) {
            return null;
        }
        return createDtoEvent(event);
    }

    @Override
    public void buyShares(String eventId, String outcomeTitle, double sharesToBuy) {
        if(sharesToBuy <= 0 ){
            throw new IllegalArgumentException("Amount of shares to buy must be strictly positive.");
        }

        MarketEvent event = eventsMap.get(eventId);
        if (event == null  ){
            throw new IllegalArgumentException("Market event with ID '" + eventId + "' was not found.");
        }

        if (!event.isActive()){
            throw new IllegalStateException("Cannot buy shares: Market event '" + eventId + "' is closed.");
        }

        Outcome outcome = event.getOutcomeByTitle(outcomeTitle);
        if (outcome == null ) {
            throw new IllegalArgumentException("Outcome '" + outcomeTitle + "' does not exist in event '" + eventId + "'.");
        }

        double netCost = LmsrCalculator.calculatePurchaseCost(event.getOutcomes(), outcomeTitle, sharesToBuy);
        double fee = LmsrCalculator.calculateFee(netCost, event.getFeePercentage());

        double totalPaid = netCost;

        if(event.getFeeType() == MarketEvent.FeeType.AT_PURCHASE){
            totalPaid += fee;
        } else {
            fee =0.0;
        }

        outcome.addShares(sharesToBuy);
        event.getAccount().deposit((netCost));
        if( fee > 0){
            event.getAccount().addFee(fee);
        }

        Transaction transaction = new Transaction(outcomeTitle, sharesToBuy , totalPaid, fee);
        event.addTransaction(transaction);
    }

    @Override
    public void closeMarket(String eventId, String winningoutcomeTitle) {
        MarketEvent event = eventsMap.get(eventId);
        if (event == null  ){
            throw new IllegalArgumentException("Market event with ID '" + eventId + "' was not found.");
        }
        if(!event.isActive()){
            throw new IllegalStateException("Market event with ID '" + eventId + "' is already closed.");
        }

        Outcome outcome = event.getOutcomeByTitle(winningoutcomeTitle);
        if(outcome == null ){
            throw new IllegalArgumentException("Outcome '" + winningoutcomeTitle + "' does not exist in event '" + eventId + "'.");
        }

        event.closeEvent(winningoutcomeTitle);
    }
}

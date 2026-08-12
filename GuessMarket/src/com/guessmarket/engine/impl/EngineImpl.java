package com.guessmarket.engine.impl;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OutcomeDto;
import com.guessmarket.engine.dto.TransactionDto;
import com.guessmarket.engine.model.LmsrCalculator;
import com.guessmarket.engine.model.MarketEvent;
import com.guessmarket.engine.model.Outcome;
import com.guessmarket.engine.model.Transaction;

import com.guessmarket.engine.schema.GMEvent;
import com.guessmarket.engine.schema.GMLMSR;
import com.guessmarket.engine.schema.GuessMarket;
import jdk.jfr.Event;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

public class EngineImpl implements EngineApi, Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String , MarketEvent> eventsMap = new HashMap<>();

    private MarketEventDto createDtoEvent(MarketEvent event){
        List<OutcomeDto> outcomeDtos = new ArrayList<>();
        for(Outcome outcome: event.getOutcomes()){
            double currentPrice = LmsrCalculator.calculatePrice(event.getOutcomes(), outcome.getTitle(),event.getB());
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
        if (!filePath.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("File must have a .xml extension.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist at path: " + filePath);
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        GuessMarket guessMarket = (GuessMarket) unmarshaller.unmarshal(file);

        Map<String, MarketEvent> tempEventsMap = new HashMap<>();

        if (guessMarket.getGMEvents() != null && guessMarket.getGMEvents().getGMEvent() != null) {
            for (GMEvent xmlEvent : guessMarket.getGMEvents().getGMEvent()) {

                String id = String.valueOf(xmlEvent.getId());

                if (tempEventsMap.containsKey(id)) {
                    throw new IllegalArgumentException("Duplicate event ID found in XML: " + id);
                }

                String title = "";
                if (xmlEvent.getName() != null) {
                    title = String.join(" ", xmlEvent.getName());
                }
                String description = xmlEvent.getDescription();

                double feePercentage = 0.0;
                MarketEvent.FeeType feeType = MarketEvent.FeeType.AT_PURCHASE;

                if (xmlEvent.getComision() != null) {
                    feePercentage = xmlEvent.getComision().getValue();

                    if ("on-resolution".equalsIgnoreCase(xmlEvent.getComision().getType())) {
                        feeType = MarketEvent.FeeType.AT_RESOLUTION;
                    }
                }

                double b = 0.0;
                if (xmlEvent.getGMMethod() != null && xmlEvent.getGMMethod().getGMLMSR() != null) {
                    b = xmlEvent.getGMMethod().getGMLMSR().getB();
                }

                MarketEvent event = new MarketEvent(
                        id,
                        title,
                        description,
                        0.0, // initialAccountBalance
                        feePercentage,
                        feeType,
                        b
                );

                if (xmlEvent.getGMOptions() != null && xmlEvent.getGMOptions().getGMOption() != null) {
                    for (String optionTitle : xmlEvent.getGMOptions().getGMOption()) {
                        event.addOutcome(new Outcome(optionTitle));
                    }
                }

                if (event.getOutcomes().size() < 2) {
                    throw new IllegalArgumentException("Event ID " + id + " must have at least 2 outcomes.");
                }

                tempEventsMap.put(event.getId(), event);
            }
        }

        eventsMap.clear();
        eventsMap.putAll(tempEventsMap);
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

        double netCost = LmsrCalculator.calculatePurchaseCost(event.getOutcomes(), outcomeTitle, sharesToBuy, event.getB());
        double fee = LmsrCalculator.calculateFee(netCost, event.getFeePercentage(), event.getB());

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

        if(event.getFeeType() == MarketEvent.FeeType.AT_RESOLUTION && event.getFeePercentage() > 0){
            double totalWinningInvestment = 0.0;

            for (Transaction transaction : event.getTransactions()){
                if(transaction.getOutcomeTitle().equalsIgnoreCase(winningoutcomeTitle)){
                    totalWinningInvestment += transaction.getCost();
                }
            }

            double feeAmount = totalWinningInvestment * (event.getFeePercentage() / 100.0);
            if(feeAmount > 0){
                event.getAccount().addFee(feeAmount);
            }
        }

        event.closeEvent(winningoutcomeTitle);
    }

    @Override
    public void saveStateToFile(String filePath) throws IOException {
        String fullPath = ensureExtension(filePath);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fullPath))) {
            out.writeObject(this);
        }
    }

    public static EngineImpl loadStateFromFile(String filePath) throws IOException, ClassNotFoundException{
        String fullPath = ensureExtension(filePath);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fullPath))) {
            return (EngineImpl) in.readObject();
        }
    }

    private static  String ensureExtension(String filePath){
        if(!filePath.endsWith(".dat")){
            return filePath + ".dat";
        }
        return filePath;
    }



}

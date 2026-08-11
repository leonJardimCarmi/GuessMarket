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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;

public class EngineImpl implements EngineApi {

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

        // 1. הגדרת JAXB עם מחלקת השרש GuessMarket
        JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        // 2. המרת הקובץ לאובייקט GuessMarket
        GuessMarket guessMarket = (GuessMarket) unmarshaller.unmarshal(file);

        // מפה זמנית לאיסוף האירועים (כדי לא לאפס את המפה הקיימת במנוע אם הקובץ נכשל באמצע)
        Map<String, MarketEvent> tempEventsMap = new HashMap<>();

        // 3. מעבר על כל האירועים בקובץ ה-XML
        if (guessMarket.getGMEvents() != null && guessMarket.getGMEvents().getGMEvent() != null) {
            for (GMEvent xmlEvent : guessMarket.getGMEvents().getGMEvent()) {

                String id = String.valueOf(xmlEvent.getId());

                // 🛑 בדיקה 1: מזהה כפול בקובץ ה-XML
                if (tempEventsMap.containsKey(id)) {
                    throw new IllegalArgumentException("Duplicate event ID found in XML: " + id);
                }

                String title = "";
                if (xmlEvent.getName() != null) {
                    title = String.join(" ", xmlEvent.getName());
                }
                String description = xmlEvent.getDescription();

                // חילוץ נתוני העמלה מתוך תגית Comision
                double feePercentage = 0.0;
                MarketEvent.FeeType feeType = MarketEvent.FeeType.AT_PURCHASE;

                if (xmlEvent.getComision() != null) {
                    feePercentage = xmlEvent.getComision().getValue();

                    // בדיקת סוג העמלה (on-purchase / on-resolution)
                    if ("on-resolution".equalsIgnoreCase(xmlEvent.getComision().getType())) {
                        feeType = MarketEvent.FeeType.AT_RESOLUTION;
                    }
                }

                double b = 0.0;
                if (xmlEvent.getGMMethod() != null && xmlEvent.getGMMethod().getGMLMSR() != null) {
                    b = xmlEvent.getGMMethod().getGMLMSR().getB();
                }

                // יצירת אובייקט MarketEvent (הבנאי יבדוק ש-b > 0 וש-feePercentage בטווח התקין)
                MarketEvent event = new MarketEvent(
                        id,
                        title,
                        description,
                        0.0, // initialAccountBalance
                        feePercentage,
                        feeType,
                        b
                );

                // חילוץ האפשרויות (Outcomes) מתוך GMOptions
                if (xmlEvent.getGMOptions() != null && xmlEvent.getGMOptions().getGMOption() != null) {
                    for (String optionTitle : xmlEvent.getGMOptions().getGMOption()) {
                        event.addOutcome(new Outcome(optionTitle));
                    }
                }

                // 🛑 בדיקה 2: לפחות 2 אפשרויות לכל אירוע (מתבצעת רק אחרי שהוספנו את ה-Outcomes)
                if (event.getOutcomes().size() < 2) {
                    throw new IllegalArgumentException("Event ID " + id + " must have at least 2 outcomes.");
                }

                // הוספה למפה הזמנית
                tempEventsMap.put(event.getId(), event);
            }
        }

        // אם כל הקובץ תקין - מאפסים את המפה הראשית ומעבירים אליה את הנתונים
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
}

package com.guessmarket.engine.impl;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OutcomeDto;
import com.guessmarket.engine.dto.TransactionDto;
import com.guessmarket.engine.model.LmsrCalculator;
import com.guessmarket.engine.model.MarketEvent;
import com.guessmarket.engine.model.Outcome;
import com.guessmarket.engine.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // בהמשך
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

    }

    @Override
    public void closeMarket(String eventId, String winningoutcomeTitle) {
    }
}

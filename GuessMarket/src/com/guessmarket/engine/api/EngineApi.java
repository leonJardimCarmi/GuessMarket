package com.guessmarket.engine.api;

import com.guessmarket.engine.dto.MarketEventDto;

import java.util.List;

public interface EngineApi {

    void loadMarketDataFromXml(String filePath) throws Exception;

    List<MarketEventDto> getAllMarketEvents();

    MarketEventDto getMarketEventById(String eventId);

    void buyShares(String eventId, String outcomeTitle, double sharesToBuy);

    void closeMarket(String eventId, String winningoutcomeTitle);
}

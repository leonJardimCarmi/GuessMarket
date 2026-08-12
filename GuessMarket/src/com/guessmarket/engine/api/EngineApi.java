package com.guessmarket.engine.api;

import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.impl.EngineImpl;

import java.io.IOException;
import java.util.List;

public interface EngineApi {

    void loadMarketDataFromXml(String filePath) throws Exception;

    List<MarketEventDto> getAllMarketEvents();

    MarketEventDto getMarketEventById(String eventId);

    void buyShares(String eventId, String outcomeTitle, double sharesToBuy);

    void closeMarket(String eventId, String winningoutcomeTitle);

    void saveStateToFile(String filePath) throws IOException;
}

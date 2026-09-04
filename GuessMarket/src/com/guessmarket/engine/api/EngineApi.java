package com.guessmarket.engine.api;

import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OrderBookDto;
import com.guessmarket.engine.dto.UserDto;
import java.io.Serializable;
import java.util.List;

public interface EngineApi extends Serializable {

    void loadMarketDataFromXml(String filePath) throws Exception;

    List<MarketEventDto> getAllMarketEvents();

    MarketEventDto getMarketEventById(String eventId);

    List<UserDto> getAllUsers();

    UserDto getUserByName(String name);

    void buySharesLMSR(String userName, String eventId, String outcomeTitle, double sharesToBuy);

    void addOrder(String userName, String eventId, String outcomeTitle, String sideStr, String actionTypeStr, double price, double shares);

    OrderBookDto getOrderBook(String eventId, String outcomeTitle);

    void closeMarket(String eventId, String winningOutcomeTitle);

    void depositFunds(String userName, double amount);

    void saveStateToFile(String filePath) throws Exception;

    static EngineApi loadStateFromFile(String filePath) throws Exception {
        return null;
    }
}
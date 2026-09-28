package com.guessmarket.engine.api;

import com.guessmarket.engine.dto.AccountEntryDto;
import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OrderBookDto;
import com.guessmarket.engine.dto.UserDto;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

public interface EngineApi extends Serializable {

    List<String> loadEventsFromXml(InputStream xmlContent, String uploaderName);

    List<MarketEventDto> getAllMarketEvents();

    MarketEventDto getMarketEventByName(String eventName);

    List<UserDto> getAllUsers();

    UserDto getUserByName(String name);

    List<AccountEntryDto> getAccountEntries(String userName, int fromIndex);

    void buySharesLMSR(String userName, String eventName, String outcomeTitle, double sharesToBuy);

    void addOrder(String userName, String eventName, String outcomeTitle, String sideStr, double price, double shares);

    OrderBookDto getOrderBook(String eventName, String outcomeTitle);

    void openEvent(String userName, String eventName);

    void closeEvent(String userName, String eventName, String winningOutcomeTitle);

    void depositFunds(String userName, double amount);

    void saveStateToFile(String filePath) throws Exception;

    static EngineApi loadStateFromFile(String filePath) throws Exception {
        return null;
    }
}
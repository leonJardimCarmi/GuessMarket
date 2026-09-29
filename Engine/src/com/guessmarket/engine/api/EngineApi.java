package com.guessmarket.engine.api;

import com.guessmarket.engine.dto.AccountEntryDto;
import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OrderBookDto;
import com.guessmarket.engine.dto.UserDto;
import com.guessmarket.engine.dto.UserEventDetailsDto;
import com.guessmarket.engine.dto.UserSummaryDto;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

public interface EngineApi extends Serializable {

    List<String> loadEventsFromXml(InputStream xmlContent, String uploaderName);

    List<MarketEventDto> getAllMarketEvents();

    MarketEventDto getMarketEventByName(String eventName);

    void registerUser(String userName);

    boolean isUserRegistered(String userName);

    List<UserSummaryDto> getAllUsers();

    UserDto getUserByName(String name);

    UserEventDetailsDto getUserEventDetails(String userName, String eventName);

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
package com.guessmarket.engine.api;

import com.guessmarket.dto.AccountEntryDto;
import com.guessmarket.dto.MarketEventDto;
import com.guessmarket.dto.OrderBookDto;
import com.guessmarket.dto.ParticipantDto;
import com.guessmarket.dto.UserDto;
import com.guessmarket.dto.UserEventDetailsDto;
import com.guessmarket.dto.UserSummaryDto;

import java.io.InputStream;
import java.util.List;

/**
 * The engine's operations. Implementations must be safe to call from many threads at once
 * (the server handles every request on its own thread).
 */
public interface EngineApi {

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

    List<ParticipantDto> getEventParticipants(String eventName);

    void openEvent(String userName, String eventName);

    void closeEvent(String userName, String eventName, String winningOutcomeTitle);

    void depositFunds(String userName, double amount);
}
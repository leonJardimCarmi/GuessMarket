package com.guessmarket.client.http;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.guessmarket.dto.AccountEntryDto;
import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.dto.MarketEventDto;
import com.guessmarket.dto.OrderBookDto;
import com.guessmarket.dto.ParticipantDto;
import com.guessmarket.dto.UserDto;
import com.guessmarket.dto.UserEventDetailsDto;
import com.guessmarket.dto.UserSummaryDto;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * The server's operations as plain Java methods: the screens call these exactly like they called the engine in
 * exercise 2, and never deal with HTTP or JSON. Every method blocks until the answer arrives, so the screens call
 * them through Async (never on the JavaFX thread). A failed request throws ServerException.
 */
public class ServerApi {
    // Java erases the element type of a List at runtime; a TypeToken keeps it, so Gson knows what to build.
    private static final Type EVENT_LIST = new TypeToken<List<MarketEventDto>>() { }.getType();
    private static final Type USER_LIST = new TypeToken<List<UserSummaryDto>>() { }.getType();
    private static final Type ENTRY_LIST = new TypeToken<List<AccountEntryDto>>() { }.getType();
    private static final Type PARTICIPANT_LIST = new TypeToken<List<ParticipantDto>>() { }.getType();
    private static final String UPLOAD_MESSAGE = "message"; // the upload answer is {"message": ..., "events": [...]}

    private final ServerConnection connection = new ServerConnection();

    // --- Session ---

    public JsonObject ping() {
        return connection.get(ApiPaths.PING, Map.of(), JsonObject.class);
    }

    public UserDto login(String userName) {
        return connection.post(ApiPaths.LOGIN, Map.of(ApiParams.USERNAME, userName), UserDto.class);
    }

    public void logout() {
        try {
            connection.post(ApiPaths.LOGOUT, Map.of(), JsonObject.class);
        } finally {
            connection.clearSession(); // whatever the server answered, this client is no longer logged in
        }
    }

    // --- Users ---

    public List<UserSummaryDto> getUsers() {
        return connection.get(ApiPaths.USERS, Map.of(), USER_LIST);
    }

    public UserDto getMe() {
        return connection.get(ApiPaths.ME, Map.of(), UserDto.class);
    }

    public UserEventDetailsDto getMyEvent(String eventName) {
        return connection.get(ApiPaths.MY_EVENT, Map.of(ApiParams.EVENT, eventName), UserEventDetailsDto.class);
    }

    public List<AccountEntryDto> getAccountEntries(int fromIndex) {
        return connection.get(ApiPaths.MY_ACCOUNT, Map.of(ApiParams.FROM, String.valueOf(fromIndex)), ENTRY_LIST);
    }

    public UserDto deposit(double amount) {
        return connection.post(ApiPaths.DEPOSIT, Map.of(ApiParams.AMOUNT, String.valueOf(amount)), UserDto.class);
    }

    // --- Events ---

    public List<MarketEventDto> getEvents() {
        return connection.get(ApiPaths.EVENTS, Map.of(), EVENT_LIST);
    }

    // Returns the server's message, e.g. "File 'x.xml' loaded successfully: 3 event(s) added, ...".
    public String uploadEventsFile(File file) {
        JsonObject answer = connection.upload(ApiPaths.UPLOAD, ApiParams.FILE, file, JsonObject.class);
        return answer.get(UPLOAD_MESSAGE).getAsString();
    }

    public MarketEventDto getEvent(String eventName) {
        return connection.get(ApiPaths.EVENT, Map.of(ApiParams.EVENT, eventName), MarketEventDto.class);
    }

    public List<ParticipantDto> getParticipants(String eventName) {
        return connection.get(ApiPaths.PARTICIPANTS, Map.of(ApiParams.EVENT, eventName), PARTICIPANT_LIST);
    }

    public OrderBookDto getOrderBook(String eventName, String outcome) {
        return connection.get(ApiPaths.ORDER_BOOK,
                Map.of(ApiParams.EVENT, eventName, ApiParams.OUTCOME, outcome), OrderBookDto.class);
    }

    public MarketEventDto openEvent(String eventName) {
        return connection.post(ApiPaths.OPEN_EVENT, Map.of(ApiParams.EVENT, eventName), MarketEventDto.class);
    }

    public MarketEventDto closeEvent(String eventName, String winner) {
        return connection.post(ApiPaths.CLOSE_EVENT,
                Map.of(ApiParams.EVENT, eventName, ApiParams.WINNER, winner), MarketEventDto.class);
    }

    // --- Trading ---

    public UserEventDetailsDto buy(String eventName, String outcome, double shares) {
        return connection.post(ApiPaths.BUY, Map.of(ApiParams.EVENT, eventName, ApiParams.OUTCOME, outcome,
                ApiParams.SHARES, String.valueOf(shares)), UserEventDetailsDto.class);
    }

    public UserEventDetailsDto placeOrder(String eventName, String outcome, String side, double price, double shares) {
        return connection.post(ApiPaths.ORDER, Map.of(ApiParams.EVENT, eventName, ApiParams.OUTCOME, outcome,
                ApiParams.SIDE, side, ApiParams.PRICE, String.valueOf(price), ApiParams.SHARES, String.valueOf(shares)),
                UserEventDetailsDto.class);
    }
}

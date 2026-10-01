package com.guessmarket.client.ui.account;

import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.client.util.Async;
import com.guessmarket.client.util.Format;
import com.guessmarket.client.util.Tables;
import com.guessmarket.client.util.Views;
import com.guessmarket.dto.MarketEventDto;
import com.guessmarket.dto.OrderDto;
import com.guessmarket.dto.TransactionDto;
import com.guessmarket.dto.UserDto;
import com.guessmarket.dto.UserEventDetailsDto;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One event from the logged-in user's side: their role, holdings, money, trades and waiting orders,
 * and only the actions that fit - open / close (market maker), buy (LMSR) or place an order (Order Book) while active.
 */
public class EventTradeController {
    private static final double PRICE_TICK = 0.01; // the highest order price is d - 0.01

    @FXML private ComboBox<String> eventChoice;
    @FXML private Label chooseHint;
    @FXML private VBox detailsBox;
    @FXML private FlowPane factsPane;

    @FXML private VBox actionsBox;
    @FXML private Pane openBox;
    @FXML private Pane buyBox;
    @FXML private ChoiceBox<String> buyOutcomeChoice;
    @FXML private TextField buySharesField;
    @FXML private Pane orderBox;
    @FXML private ChoiceBox<String> sideChoice;
    @FXML private ChoiceBox<String> orderOutcomeChoice;
    @FXML private TextField orderSharesField;
    @FXML private Label priceLabel;
    @FXML private TextField priceField;
    @FXML private Pane closeBox;
    @FXML private ChoiceBox<String> winnerChoice;
    @FXML private Label noActionsLabel;
    @FXML private Label actionMessage;

    @FXML private TableView<HoldingRow> holdingsTable;
    @FXML private TableColumn<HoldingRow, String> holdingOutcomeColumn;
    @FXML private TableColumn<HoldingRow, Double> holdingSharesColumn;
    @FXML private TableColumn<HoldingRow, Double> holdingPaidColumn;
    @FXML private VBox openOrdersBox;
    @FXML private TableView<OrderDto> openOrdersTable;
    @FXML private TableColumn<OrderDto, String> orderSideColumn;
    @FXML private TableColumn<OrderDto, String> orderOutcomeColumn;
    @FXML private TableColumn<OrderDto, Double> orderSharesColumn;
    @FXML private TableColumn<OrderDto, Double> orderPriceColumn;
    @FXML private TableView<TransactionDto> tradesTable;
    @FXML private TableColumn<TransactionDto, String> tradeActionColumn;
    @FXML private TableColumn<TransactionDto, String> tradeOutcomeColumn;
    @FXML private TableColumn<TransactionDto, Double> tradeSharesColumn;
    @FXML private TableColumn<TransactionDto, Double> tradeAmountColumn;
    @FXML private TableColumn<TransactionDto, Double> tradeFeeColumn;

    private final Map<String, MarketEventDto> eventsByName = new HashMap<>();
    private ClientContext context;
    private Runnable onChanged;      // after an action: the whole account and the Events tab show the new state
    private UserDto me;              // for the user's role in the event
    private boolean updatingChoices; // while true, the event chooser's listener ignores changes made by the code
    private volatile String chosenEventName; // the chooser's value; volatile: pull() reads it on another thread

    @FXML
    private void initialize() {
        Tables.text(holdingOutcomeColumn, HoldingRow::outcome);
        Tables.number(holdingSharesColumn, HoldingRow::shares);
        Tables.number(holdingPaidColumn, HoldingRow::paid);

        Tables.text(orderSideColumn, order -> Format.side(order.getSide()));
        Tables.text(orderOutcomeColumn, OrderDto::getOutcomeTitle);
        Tables.number(orderSharesColumn, OrderDto::getRemainingShares);
        Tables.number(orderPriceColumn, OrderDto::getPrice);

        Tables.text(tradeActionColumn, trade -> boughtByMe(trade) ? "Bought" : "Sold");
        Tables.text(tradeOutcomeColumn, TransactionDto::getOutcomeTitle);
        Tables.number(tradeSharesColumn, TransactionDto::getSharesBought);
        Tables.number(tradeAmountColumn, TransactionDto::getAmountPaid);
        Tables.number(tradeFeeColumn, trade -> boughtByMe(trade) ? trade.getFeePaid() : null); // the buyer pays the fee

        // The choice box holds the server's values (BUY / SELL) and shows them as "Buy" / "Sell".
        sideChoice.getItems().setAll(OrderDto.SIDE_BUY, OrderDto.SIDE_SELL);
        sideChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(String side) {
                return side == null ? "" : Format.side(side);
            }

            @Override
            public String fromString(String text) {
                throw new UnsupportedOperationException("the side is only chosen, never typed");
            }
        });
        sideChoice.setValue(OrderDto.SIDE_BUY);

        eventChoice.valueProperty().addListener((observable, oldName, newName) -> {
            chosenEventName = newName;
            if (!updatingChoices) {
                Views.clearMessage(actionMessage);
                loadDetails(); // show the new choice at once, without waiting for the next refresh
            }
        });
        Views.clearMessage(actionMessage);
        showDetails(null);
    }

    public void init(ClientContext context, Runnable onChanged) {
        this.context = context;
        this.onChanged = onChanged;
    }

    // Fetches the chosen event's details (on the Account tab's background thread) and returns the screen update,
    // which also refreshes the events to choose from and the user (for their role).
    public Runnable pull(List<MarketEventDto> events, UserDto me) {
        String eventName = chosenEventName;
        UserEventDetailsDto details = eventName == null ? null : context.api().getMyEvent(eventName);
        return () -> update(events, me, details);
    }

    private void update(List<MarketEventDto> events, UserDto me, UserEventDetailsDto details) {
        this.me = me;
        eventsByName.clear();
        List<String> names = new ArrayList<>();
        for (MarketEventDto event : events) {
            eventsByName.put(event.getName(), event);
            names.add(event.getName());
        }
        if (!eventChoice.getItems().equals(names)) { // replacing the items could clear the chosen value
            String chosen = eventChoice.getValue();
            updatingChoices = true;
            try {
                eventChoice.getItems().setAll(names);
                eventChoice.setValue(chosen);
            } finally {
                updatingChoices = false;
            }
        }
        if (details != null && details.getEventName().equals(eventChoice.getValue())) { // still the chosen event
            showDetails(details);
        }
    }

    // Chooses an event from outside (a click in the "My events" table).
    public void choose(String eventName) {
        eventChoice.setValue(eventName);
    }

    private void loadDetails() {
        String eventName = eventChoice.getValue();
        if (eventName == null) {
            showDetails(null);
            return;
        }
        Async.run(() -> context.api().getMyEvent(eventName),
                details -> {
                    if (eventName.equals(eventChoice.getValue())) { // the user may have chosen another event meanwhile
                        showDetails(details);
                    }
                },
                context::handleError);
    }

    private void showDetails(UserEventDetailsDto details) {
        Views.setShown(chooseHint, details == null);
        Views.setShown(detailsBox, details != null);
        if (details == null) {
            return;
        }
        boolean orderBook = MarketEventDto.METHOD_ORDER_BOOK.equals(details.getTradingMethod());

        factsPane.getChildren().setAll(factsOf(details));
        showActions(details, orderBook);

        List<HoldingRow> holdings = new ArrayList<>();
        details.getHoldings().forEach((outcome, shares) -> holdings.add(
                new HoldingRow(outcome, shares, details.getInvestedByOutcome().getOrDefault(outcome, 0.0))));
        holdingsTable.getItems().setAll(holdings);

        Views.setShown(openOrdersBox, orderBook);
        openOrdersTable.getItems().setAll(details.getOpenOrders());
        tradesTable.getItems().setAll(details.getTrades()); // the server sends them newest first
    }

    private List<Node> factsOf(UserEventDetailsDto details) {
        boolean closed = MarketEventDto.STATUS_CLOSED.equals(details.getStatus());
        List<Node> facts = new ArrayList<>();
        facts.add(Views.fact("Status", Format.status(details.getStatus())));
        if (details.getWinningOutcome() != null) {
            facts.add(Views.fact("Winner", details.getWinningOutcome()));
        }
        facts.add(Views.fact("Method", Format.method(details.getTradingMethod())));
        facts.add(Views.fact("My role", roleIn(details)));
        facts.add(Views.fact("Invested", Format.money(details.getInvested())));
        facts.add(Views.fact("Fees paid", Format.money(details.getFeesPaid())));
        facts.add(Views.fact("Received", Format.money(details.getReceived())));
        if (details.isMarketMaker()) {
            facts.add(Views.fact("Commissions earned", Format.money(details.getCommissionsEarned())));
        }
        facts.add(Views.fact(closed ? "Profit / loss" : "Profit / loss so far", Format.signed(details.getProfitLoss())));
        return facts;
    }

    private String roleIn(UserEventDetailsDto details) {
        if (details.isMarketMaker()) {
            return "Market maker";
        }
        boolean participates = me != null && me.getParticipatedEvents().contains(details.getEventName());
        return participates ? "Participant" : "Not participating yet";
    }

    // Shows only the actions that fit the event's state and the user's role.
    private void showActions(UserEventDetailsDto details, boolean orderBook) {
        String status = details.getStatus();
        boolean active = MarketEventDto.STATUS_ACTIVE.equals(status);
        boolean marketMaker = details.isMarketMaker();

        Views.setShown(openBox, marketMaker && MarketEventDto.STATUS_NOT_STARTED.equals(status));
        Views.setShown(buyBox, active && !orderBook);
        Views.setShown(orderBox, active && orderBook);
        Views.setShown(closeBox, active && marketMaker);

        String noActions = switch (status) {
            case MarketEventDto.STATUS_NOT_STARTED ->
                    marketMaker ? null : "The event has not started yet: trading begins when its market maker opens it.";
            case MarketEventDto.STATUS_CLOSED -> "The event is closed: no more trading.";
            default -> null;
        };
        noActionsLabel.setText(noActions);
        Views.setShown(noActionsLabel, noActions != null);

        List<String> outcomes = new ArrayList<>(details.getHoldings().keySet()); // one entry per outcome, in order
        for (ChoiceBox<String> choice : List.of(buyOutcomeChoice, orderOutcomeChoice, winnerChoice)) {
            setChoices(choice, outcomes);
        }
        MarketEventDto event = eventsByName.get(details.getEventName());
        priceLabel.setText(orderBook && event != null
                ? "Price (up to " + Format.money(event.getDParameter() - PRICE_TICK) + "):"
                : "Price:");
    }

    // The details are reloaded after every action; keep the user's choice when the options did not change.
    private static void setChoices(ChoiceBox<String> choice, List<String> options) {
        if (!choice.getItems().equals(options)) {
            choice.getItems().setAll(options);
            choice.setValue(options.isEmpty() ? null : options.getFirst());
        }
    }

    // --- Actions ---

    @FXML
    private void onOpen() {
        String eventName = eventChoice.getValue();
        runAction(() -> context.api().openEvent(eventName), "Event '" + eventName + "' is open for trading.");
    }

    @FXML
    private void onBuy() {
        String eventName = eventChoice.getValue();
        String outcome = buyOutcomeChoice.getValue();
        double shares;
        try {
            shares = Views.positiveNumber(buySharesField, "Shares");
        } catch (IllegalArgumentException e) {
            Views.showError(actionMessage, e.getMessage());
            return;
        }
        runAction(() -> context.api().buy(eventName, outcome, shares),
                "Bought " + Format.money(shares) + " '" + outcome + "' shares.");
    }

    @FXML
    private void onPlaceOrder() {
        String eventName = eventChoice.getValue();
        String side = sideChoice.getValue();
        String outcome = orderOutcomeChoice.getValue();
        double shares;
        double price;
        try {
            shares = Views.positiveNumber(orderSharesField, "Shares");
            price = Views.positiveNumber(priceField, "Price");
        } catch (IllegalArgumentException e) {
            Views.showError(actionMessage, e.getMessage());
            return;
        }
        runAction(() -> context.api().placeOrder(eventName, outcome, side, price, shares),
                Format.side(side) + " order placed: " + Format.money(shares) + " '" + outcome + "' shares at "
                        + Format.money(price) + ". See below whether it was executed or is waiting.");
    }

    @FXML
    private void onClose() {
        String eventName = eventChoice.getValue();
        String winner = winnerChoice.getValue();
        if (winner == null) {
            Views.showError(actionMessage, "Choose the winning outcome.");
            return;
        }
        if (!confirm("Close '" + eventName + "' with '" + winner + "' as the winner?\n"
                + "The winners are paid now, and this cannot be undone.")) {
            return;
        }
        runAction(() -> context.api().closeEvent(eventName, winner),
                "Event '" + eventName + "' is closed. The winner is '" + winner + "'.");
    }

    // Runs an action on the server. The actions are locked meanwhile (no double clicks),
    // then the result is shown next to them and everything that may have changed is reloaded.
    private void runAction(Supplier<?> action, String successMessage) {
        actionsBox.setDisable(true);
        Views.clearMessage(actionMessage);
        Async.run(action,
                result -> {
                    actionsBox.setDisable(false);
                    Views.showSuccess(actionMessage, successMessage);
                    onChanged.run();
                },
                error -> {
                    actionsBox.setDisable(false);
                    context.handleError(error, message -> Views.showError(actionMessage, message));
                });
    }

    private boolean confirm(String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.initOwner(eventChoice.getScene().getWindow());
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean boughtByMe(TransactionDto trade) {
        return trade.getBuyerName().equalsIgnoreCase(context.userName());
    }

    // One row of "My holdings": an outcome, the shares held and what was paid for them.
    private record HoldingRow(String outcome, double shares, double paid) {
    }
}

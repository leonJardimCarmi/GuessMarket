package com.guessmarket.client.ui.events;

import com.guessmarket.client.http.ServerApi;
import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.client.util.Async;
import com.guessmarket.client.util.Format;
import com.guessmarket.client.util.Tables;
import com.guessmarket.dto.MarketEventDto;
import com.guessmarket.dto.OrderBookDto;
import com.guessmarket.dto.OutcomeDto;
import com.guessmarket.dto.ParticipantDto;
import com.guessmarket.dto.TransactionDto;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The Events tab: every event on the server (of every method and status), filtered by method / status / fee type,
 * and the selected event's details - facts, outcomes, order books (Order Book events), participants and trade history.
 */
public class EventsController {
    private static final String FROM_EVENT = "(event)"; // a trade without a seller: the shares came from the event itself

    // --- Left: filters and the events list ---
    @FXML private Pane methodFilter;
    @FXML private Pane statusFilter;
    @FXML private Pane feeFilter;
    @FXML private Label countLabel;
    @FXML private TableView<MarketEventDto> eventsTable;
    @FXML private TableColumn<MarketEventDto, String> nameColumn;
    @FXML private TableColumn<MarketEventDto, String> statusColumn;
    @FXML private TableColumn<MarketEventDto, String> methodColumn;
    @FXML private TableColumn<MarketEventDto, String> feeColumn;
    @FXML private TableColumn<MarketEventDto, Double> balanceColumn;

    // --- Right: the selected event's details ---
    @FXML private Label selectHint;
    @FXML private VBox detailsBox;
    @FXML private Label eventNameLabel;
    @FXML private Label descriptionLabel;
    @FXML private FlowPane factsPane;
    @FXML private TableView<OutcomeDto> outcomesTable;
    @FXML private TableColumn<OutcomeDto, String> outcomeColumn;
    @FXML private TableColumn<OutcomeDto, Double> outcomePriceColumn;
    @FXML private TableColumn<OutcomeDto, Double> outcomeSharesColumn;
    @FXML private HBox orderBooksBox;
    @FXML private OrderBookController firstBookController;  // <fx:include fx:id="firstBook">
    @FXML private OrderBookController secondBookController; // <fx:include fx:id="secondBook">
    @FXML private TableView<ParticipantDto> participantsTable;
    @FXML private TableView<TransactionDto> historyTable;
    @FXML private TableColumn<TransactionDto, String> buyerColumn;
    @FXML private TableColumn<TransactionDto, String> sellerColumn;
    @FXML private TableColumn<TransactionDto, String> tradeOutcomeColumn;
    @FXML private TableColumn<TransactionDto, Double> tradeSharesColumn;
    @FXML private TableColumn<TransactionDto, Double> tradePaidColumn;
    @FXML private TableColumn<TransactionDto, Double> tradeFeeColumn;

    private final ObservableList<MarketEventDto> allEvents = FXCollections.observableArrayList();
    private final FilteredList<MarketEventDto> shownEvents = new FilteredList<>(allEvents);

    private ClientContext context;
    private String selectedEventName; // kept by name: a refresh brings new objects for the same events
    private String detailsEventName;  // the event the details area was last filled for
    private boolean updatingList;     // while true, the selection listener ignores the table's temporary changes

    // Called by FXMLLoader after the @FXML fields are filled (setup that does not need the server).
    @FXML
    private void initialize() {
        Tables.text(nameColumn, MarketEventDto::getName);
        Tables.text(statusColumn, event -> Format.status(event.getStatus()));
        Tables.text(methodColumn, event -> Format.method(event.getTradingMethod()));
        Tables.text(feeColumn, Format::fee);
        Tables.number(balanceColumn, MarketEventDto::getEventBalance);

        // FilteredList hides what the filters reject; SortedList lets the user sort by clicking a column header.
        SortedList<MarketEventDto> sortedEvents = new SortedList<>(shownEvents);
        sortedEvents.comparatorProperty().bind(eventsTable.comparatorProperty());
        eventsTable.setItems(sortedEvents);
        shownEvents.addListener((ListChangeListener<MarketEventDto>) change -> updateCount());

        for (Pane filter : List.of(methodFilter, statusFilter, feeFilter)) {
            for (ToggleButton toggle : toggles(filter)) {
                toggle.selectedProperty().addListener((observable, wasSelected, isSelected) -> applyFilters());
            }
        }
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldEvent, newEvent) -> onEventSelected(newEvent));

        Tables.text(outcomeColumn, OutcomeDto::getTitle);
        Tables.number(outcomePriceColumn, OutcomeDto::getCurrentPrice);
        Tables.number(outcomeSharesColumn, OutcomeDto::getSharesCount);

        Tables.text(buyerColumn, TransactionDto::getBuyerName);
        Tables.text(sellerColumn, trade -> trade.getSellerName() == null ? FROM_EVENT : trade.getSellerName());
        Tables.text(tradeOutcomeColumn, TransactionDto::getOutcomeTitle);
        Tables.number(tradeSharesColumn, TransactionDto::getSharesBought);
        Tables.number(tradePaidColumn, TransactionDto::getAmountPaid);
        Tables.number(tradeFeeColumn, TransactionDto::getFeePaid);

        updateCount();
        showDetails(null);
    }

    public void init(ClientContext context) {
        this.context = context;
        refresh();
    }

    // Reloads the events from the server. The automatic refresh (stage 5.7) will call it too.
    public void refresh() {
        Async.run(() -> context.api().getEvents(),
                events -> updateList(() -> allEvents.setAll(events)),
                context::handleError);
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    // --- The list ---

    private void applyFilters() {
        updateList(() -> shownEvents.setPredicate(event ->
                allows(methodFilter, event.getTradingMethod())
                        && allows(statusFilter, event.getStatus())
                        && allows(feeFilter, event.getFeeType())));
    }

    // Changes the list, then selects the same event again by its name (or nothing, if it is no longer shown).
    // Replacing a table's items moves its selection around, so the selection listener is muted meanwhile.
    private void updateList(Runnable change) {
        updatingList = true;
        try {
            change.run();
            MarketEventDto selected = findShown(selectedEventName);
            if (selected == null) {
                eventsTable.getSelectionModel().clearSelection();
            } else {
                eventsTable.getSelectionModel().select(selected);
            }
        } finally {
            updatingList = false;
        }
        onEventSelected(eventsTable.getSelectionModel().getSelectedItem());
    }

    private void onEventSelected(MarketEventDto event) {
        if (updatingList) {
            return; // updateList() selects the event again when it finishes
        }
        selectedEventName = event == null ? null : event.getName();
        showDetails(event);
    }

    private MarketEventDto findShown(String eventName) {
        for (MarketEventDto event : shownEvents) {
            if (event.getName().equals(eventName)) {
                return event;
            }
        }
        return null;
    }

    private void updateCount() {
        countLabel.setText(String.format("Showing %d of %d events", shownEvents.size(), allEvents.size()));
    }

    // A filter is a row of toggles, one per value; an event passes if the toggle of its value is on.
    private static boolean allows(Pane filter, String value) {
        return toggles(filter).stream().anyMatch(toggle -> toggle.isSelected() && value.equals(toggle.getUserData()));
    }

    private static List<ToggleButton> toggles(Pane filter) {
        List<ToggleButton> toggles = new ArrayList<>();
        for (Node node : filter.getChildren()) {
            if (node instanceof ToggleButton toggle) {
                toggles.add(toggle);
            }
        }
        return toggles;
    }

    // --- The details ---

    private void showDetails(MarketEventDto event) {
        setShown(selectHint, event == null);
        setShown(detailsBox, event != null);
        if (event == null) {
            return;
        }
        boolean orderBook = MarketEventDto.METHOD_ORDER_BOOK.equals(event.getTradingMethod());

        if (!event.getName().equals(detailsEventName)) {
            // Another event than before: drop the previous one's data before the new data arrives.
            buildParticipantColumns(event.getOutcomes());
            participantsTable.getItems().clear();
            firstBookController.clear();
            secondBookController.clear();
            detailsEventName = event.getName();
        }

        eventNameLabel.setText(event.getName());
        descriptionLabel.setText(event.getDescription());
        factsPane.getChildren().setAll(factsOf(event, orderBook));
        outcomesTable.getItems().setAll(event.getOutcomes());
        historyTable.getItems().setAll(event.getTransactions().reversed()); // newest first
        setShown(orderBooksBox, orderBook);

        loadLiveDetails(event, orderBook);
    }

    private List<Node> factsOf(MarketEventDto event, boolean orderBook) {
        List<Node> facts = new ArrayList<>();
        facts.add(fact("Status", Format.status(event.getStatus())));
        if (event.getWinningOutcome() != null) {
            facts.add(fact("Winner", event.getWinningOutcome()));
        }
        facts.add(fact("Method", Format.method(event.getTradingMethod())));
        facts.add(fact("Market maker", marketMakerText(event)));
        facts.add(fact("Fee", Format.fee(event)));
        facts.add(fact("Event account", Format.money(event.getEventBalance())));
        facts.add(fact("Fees collected", Format.money(event.getTotalFeesCollected())));
        facts.add(orderBook
                ? fact("Winning share pays (d)", Format.money(event.getDParameter()))
                : fact("Liquidity (b)", Format.money(event.getBParameter())));
        return facts;
    }

    private String marketMakerText(MarketEventDto event) {
        String marketMaker = event.getMarketMakerName();
        return context.userName().equalsIgnoreCase(marketMaker) ? marketMaker + " (you)" : marketMaker;
    }

    // A small box: the caption above, the value below.
    private static Node fact(String caption, String value) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("fact-caption");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("fact-value");
        VBox box = new VBox(2, captionLabel, valueLabel);
        box.getStyleClass().add("fact");
        return box;
    }

    // The columns depend on the event's outcomes: under each outcome's name, the shares held and their value.
    private void buildParticipantColumns(List<OutcomeDto> outcomes) {
        List<TableColumn<ParticipantDto, ?>> columns = new ArrayList<>();

        TableColumn<ParticipantDto, String> participant = new TableColumn<>("Participant");
        Tables.text(participant, p -> p.isMarketMaker() ? p.getName() + " (MM)" : p.getName());
        columns.add(participant);

        for (OutcomeDto outcome : outcomes) {
            String title = outcome.getTitle();
            TableColumn<ParticipantDto, Double> shares = new TableColumn<>("Shares");
            Tables.number(shares, p -> p.getHoldings().getOrDefault(title, 0.0));
            TableColumn<ParticipantDto, Double> value = new TableColumn<>("Value");
            Tables.number(value, p -> p.getHoldingValues().getOrDefault(title, 0.0));

            TableColumn<ParticipantDto, ?> group = new TableColumn<>(title); // a header above its two columns
            group.getColumns().add(shares);
            group.getColumns().add(value);
            columns.add(group);
        }

        TableColumn<ParticipantDto, Double> total = new TableColumn<>("Total value");
        Tables.number(total, ParticipantDto::getTotalValue);
        columns.add(total);

        participantsTable.getColumns().setAll(columns);
    }

    // Participants and order books are not part of the event's DTO, so they come in separate requests,
    // done together in one background job. Events are binary, so an Order Book event has exactly two books.
    private void loadLiveDetails(MarketEventDto event, boolean orderBook) {
        ServerApi api = context.api();
        String name = event.getName();
        List<OutcomeDto> outcomes = event.getOutcomes();
        Async.run(() -> new LiveDetails(
                        api.getParticipants(name),
                        orderBook ? api.getOrderBook(name, outcomes.get(0).getTitle()) : null,
                        orderBook ? api.getOrderBook(name, outcomes.get(1).getTitle()) : null),
                details -> {
                    if (name.equals(selectedEventName)) { // the user may have chosen another event meanwhile
                        showLiveDetails(details);
                    }
                },
                context::handleError);
    }

    private void showLiveDetails(LiveDetails details) {
        participantsTable.getItems().setAll(details.participants());
        if (details.firstBook() != null) {
            firstBookController.show(details.firstBook());
            secondBookController.show(details.secondBook());
        }
    }

    // managed=false also frees the node's space, so a hidden part leaves no gap.
    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    // What one background job brings back together. The books are null for an LMSR event.
    private record LiveDetails(List<ParticipantDto> participants, OrderBookDto firstBook, OrderBookDto secondBook) {
    }
}

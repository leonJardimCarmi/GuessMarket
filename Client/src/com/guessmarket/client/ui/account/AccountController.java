package com.guessmarket.client.ui.account;

import com.guessmarket.client.http.ServerApi;
import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.client.util.Async;
import com.guessmarket.client.util.Format;
import com.guessmarket.client.util.Tables;
import com.guessmarket.client.util.Views;
import com.guessmarket.dto.AccountEntryDto;
import com.guessmarket.dto.MarketEventDto;
import com.guessmarket.dto.UserDto;
import com.guessmarket.dto.UserSummaryDto;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The Account tab: uploading event files, the other users, and the user's own account -
 * balance and deposit, the events they take part in, one event's details and trading, and the account history.
 */
public class AccountController {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    // --- Left ---
    @FXML private TextField filePathField;
    @FXML private Button uploadButton;
    @FXML private ProgressIndicator uploadProgress;
    @FXML private Label uploadMessage;
    @FXML private TableView<UserSummaryDto> usersTable;
    @FXML private TableColumn<UserSummaryDto, String> userNameColumn;
    @FXML private TableColumn<UserSummaryDto, Double> userBalanceColumn;
    @FXML private TableColumn<UserSummaryDto, String> userMarketMakerColumn;

    // --- Right ---
    @FXML private FlowPane balancePane;
    @FXML private TextField depositField;
    @FXML private Button depositButton;
    @FXML private Label depositMessage;
    @FXML private TableView<MyEventRow> myEventsTable;
    @FXML private TableColumn<MyEventRow, String> myEventNameColumn;
    @FXML private TableColumn<MyEventRow, String> myEventRoleColumn;
    @FXML private TableColumn<MyEventRow, String> myEventStatusColumn;
    @FXML private TableColumn<MyEventRow, String> myEventMethodColumn;
    @FXML private TableColumn<MyEventRow, String> myEventSharesColumn;
    @FXML private EventTradeController eventTradeController; // <fx:include fx:id="eventTrade">
    @FXML private TableView<LedgerRow> ledgerTable;
    @FXML private TableColumn<LedgerRow, String> ledgerNumberColumn;
    @FXML private TableColumn<LedgerRow, String> ledgerTimeColumn;
    @FXML private TableColumn<LedgerRow, String> ledgerDescriptionColumn;
    @FXML private TableColumn<LedgerRow, Double> ledgerAmountColumn;
    @FXML private TableColumn<LedgerRow, Double> ledgerBalanceColumn;

    private ClientContext context;
    private Consumer<UserDto> onUserChanged; // the header shows the balance too
    private Runnable onEventsChanged;        // the Events tab reloads after an upload or a trade
    private File chosenFile;
    private File lastDirectory;              // the file chooser opens where the previous file was
    // Account entries we already have; a refresh asks only for newer ones. volatile: pull() reads it on another thread.
    private volatile int ledgerSize;

    @FXML
    private void initialize() {
        Tables.text(userNameColumn, user -> isMe(user.getName()) ? user.getName() + " (you)" : user.getName());
        Tables.number(userBalanceColumn, UserSummaryDto::getBalance);
        Tables.text(userMarketMakerColumn, user -> user.isMarketMaker() ? "Yes" : "No");

        Tables.text(myEventNameColumn, MyEventRow::name);
        Tables.text(myEventRoleColumn, MyEventRow::role);
        Tables.text(myEventStatusColumn, MyEventRow::status);
        Tables.text(myEventMethodColumn, MyEventRow::method);
        Tables.text(myEventSharesColumn, MyEventRow::shares);
        // Selecting a row (by mouse or keyboard) shows its event below; a click also works on the already selected row.
        myEventsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldRow, row) -> chooseRow(row));
        myEventsTable.setOnMouseClicked(click -> chooseRow(myEventsTable.getSelectionModel().getSelectedItem()));

        Tables.text(ledgerNumberColumn, row -> String.valueOf(row.number()));
        Tables.text(ledgerTimeColumn, row -> TIME.format(Instant.ofEpochMilli(row.entry().getTimestamp())));
        Tables.text(ledgerDescriptionColumn, row -> row.entry().getDescription());
        Tables.number(ledgerAmountColumn, row -> row.entry().getAmount());
        Tables.number(ledgerBalanceColumn, row -> row.entry().getBalanceAfter());

        Views.clearMessage(uploadMessage);
        Views.clearMessage(depositMessage);
    }

    public void init(ClientContext context, Consumer<UserDto> onUserChanged, Runnable onEventsChanged) {
        this.context = context;
        this.onUserChanged = onUserChanged;
        this.onEventsChanged = onEventsChanged;
        eventTradeController.init(context, this::afterChange);
        refresh();
    }

    // Reloads now, in the background (the Refresh button, after an action, switching to this tab).
    public void refresh() {
        Async.run(this::pull, Runnable::run, context::handleError);
    }

    // Fetches the whole tab: the user, the users, the events, the new account entries and the chosen event's details.
    // Runs on a background thread (Async or the poller); returns the screen update, which must run on the JavaFX thread.
    public Runnable pull() {
        ServerApi api = context.api();
        int ledgerFrom = ledgerSize;
        AccountData data = new AccountData(api.getMe(), api.getUsers(), api.getEvents(),
                ledgerFrom, api.getAccountEntries(ledgerFrom));
        Runnable showChosenEvent = eventTradeController.pull(data.events(), data.me());
        return () -> {
            show(data);
            showChosenEvent.run();
        };
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    // After an upload or an action, the account and the Events tab both change.
    private void afterChange() {
        refresh();
        onEventsChanged.run();
    }

    private void show(AccountData data) {
        UserDto me = data.me();
        onUserChanged.accept(me);
        balancePane.getChildren().setAll(
                Views.fact("Balance", Format.money(me.getBalance())),
                Views.fact("Reserved for buy orders", Format.money(me.getReservedBalance())),
                Views.fact("Available", Format.money(me.getAvailableBalance())));

        usersTable.getItems().setAll(data.users());

        List<MyEventRow> myEvents = myEventRows(me, data.events());
        if (!myEventsTable.getItems().equals(myEvents)) { // records compare by value: replace only on a real change
            myEventsTable.getItems().setAll(myEvents);
        }

        // Two refreshes may run at once; only the one that asked from our current size adds its entries.
        if (data.ledgerFrom() == ledgerSize) {
            List<LedgerRow> newRows = new ArrayList<>();
            for (AccountEntryDto entry : data.newEntries()) {
                newRows.add(new LedgerRow(ledgerSize + newRows.size() + 1, entry)); // numbered from 1
            }
            ledgerTable.getItems().addAll(0, newRows.reversed()); // newest first
            ledgerSize += newRows.size();
        }
    }

    // The events the user is the market maker of, then the others they took part in.
    private List<MyEventRow> myEventRows(UserDto me, List<MarketEventDto> events) {
        Map<String, MarketEventDto> eventsByName = new HashMap<>();
        for (MarketEventDto event : events) {
            eventsByName.put(event.getName(), event);
        }
        Set<String> names = new LinkedHashSet<>(me.getMarketMakerEvents()); // a set: no event appears twice
        names.addAll(me.getParticipatedEvents());

        List<MyEventRow> rows = new ArrayList<>();
        for (String name : names) {
            MarketEventDto event = eventsByName.get(name);
            rows.add(new MyEventRow(name,
                    me.getMarketMakerEvents().contains(name) ? "Market maker" : "Participant",
                    event == null ? "-" : Format.status(event.getStatus()),
                    event == null ? "-" : Format.method(event.getTradingMethod()),
                    sharesText(me.getHoldings().get(name))));
        }
        return rows;
    }

    // e.g. "Yes 10.00, No 2.50"; "-" when no shares are held
    private static String sharesText(Map<String, Double> holdings) {
        if (holdings == null || holdings.isEmpty()) {
            return "-";
        }
        return holdings.entrySet().stream()
                .map(holding -> holding.getKey() + " " + Format.money(holding.getValue()))
                .collect(Collectors.joining(", "));
    }

    private void chooseRow(MyEventRow row) {
        if (row != null) {
            eventTradeController.choose(row.name());
        }
    }

    private boolean isMe(String userName) {
        return userName.equalsIgnoreCase(context.userName());
    }

    // --- Upload ---

    @FXML
    private void onBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an events file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File file = chooser.showOpenDialog(filePathField.getScene().getWindow());
        if (file != null) {
            chosenFile = file;
            lastDirectory = file.getParentFile();
            filePathField.setText(file.getAbsolutePath());
            uploadButton.setDisable(false);
            Views.clearMessage(uploadMessage);
        }
    }

    // The upload runs in the background; the server checks the file and answers with a message either way.
    @FXML
    private void onUpload() {
        File file = chosenFile;
        if (file == null || !file.isFile() || !file.canRead()) {
            Views.showError(uploadMessage, "The chosen file cannot be read. Please choose it again.");
            return;
        }
        setUploading(true);
        Views.clearMessage(uploadMessage);
        Async.run(() -> context.api().uploadEventsFile(file),
                message -> {
                    setUploading(false);
                    Views.showSuccess(uploadMessage, message);
                    afterChange();
                },
                error -> {
                    setUploading(false);
                    context.handleError(error, message -> Views.showError(uploadMessage, message));
                });
    }

    private void setUploading(boolean uploading) {
        uploadButton.setDisable(uploading);
        uploadProgress.setVisible(uploading);
    }

    // --- Deposit ---

    @FXML
    private void onDeposit() {
        double amount;
        try {
            amount = Views.positiveAmount(depositField, "The amount");
        } catch (IllegalArgumentException e) {
            Views.showError(depositMessage, e.getMessage());
            return;
        }
        setDepositing(true);
        Async.run(() -> context.api().deposit(amount),
                user -> {
                    setDepositing(false);
                    depositField.clear();
                    Views.showSuccess(depositMessage, "Deposited " + Format.money(amount) + ".");
                    refresh();
                },
                error -> {
                    setDepositing(false);
                    context.handleError(error, message -> Views.showError(depositMessage, message));
                });
    }

    // The field is locked too, not only the button: Enter in the field would send the same deposit again.
    private void setDepositing(boolean depositing) {
        depositButton.setDisable(depositing);
        depositField.setDisable(depositing);
    }

    // What one refresh brings back together. ledgerFrom: the index the new account entries start from.
    private record AccountData(UserDto me, List<UserSummaryDto> users, List<MarketEventDto> events,
                               int ledgerFrom, List<AccountEntryDto> newEntries) {
    }

    // One row of "My events". A record compares by value, so an unchanged list is not replaced.
    private record MyEventRow(String name, String role, String status, String method, String shares) {
    }

    // An account entry with its number in the account history (1 = the first entry).
    private record LedgerRow(int number, AccountEntryDto entry) {
    }
}

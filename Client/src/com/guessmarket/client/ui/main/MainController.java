package com.guessmarket.client.ui.main;

import com.guessmarket.client.http.ServerException;
import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.client.ui.Theme;
import com.guessmarket.client.ui.account.AccountController;
import com.guessmarket.client.ui.chat.ChatController;
import com.guessmarket.client.ui.events.EventsController;
import com.guessmarket.dto.UserDto;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The main window after login: a header (user, balance, theme, log out), the Events, Account and Chat tabs,
 * and a status bar. Each tab has its own controller; this one connects them to the shared context and runs the
 * automatic refresh.
 */
public class MainController {
    private static final int EVENTS_TAB = 0;
    private static final int ACCOUNT_TAB = 1;
    private static final int CHAT_TAB = 2;
    private static final String CHAT_TITLE = "Chat";
    private static final String STATUS_ERROR = "status-error";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label userLabel;
    @FXML private Label balanceLabel;
    @FXML private ComboBox<Theme> themeComboBox;
    @FXML private TabPane tabPane;
    @FXML private Tab chatTab;
    @FXML private Label statusLabel;

    // Injected from the <fx:include> elements: the name is the include's fx:id + "Controller".
    @FXML private EventsController eventsController;
    @FXML private AccountController accountController;
    @FXML private ChatController chatController;

    private ClientContext context;
    private volatile int shownTab = EVENTS_TAB; // read by the poller's thread

    public void init(ClientContext context, UserDto user) {
        this.context = context;

        themeComboBox.getItems().setAll(Theme.values());
        themeComboBox.setValue(context.theme()); // the theme stays the same after logging out and in again
        themeComboBox.valueProperty().addListener((observable, oldTheme, newTheme) -> context.applyTheme(newTheme));

        eventsController.init(context);
        // The Account tab updates the header's balance, and reloads the Events tab after an upload or a trade.
        accountController.init(context, this::showUser, eventsController::refresh);
        chatController.init(context);

        // A binding keeps the title up to date by itself: "Chat (2)" while messages wait unread, else "Chat".
        chatTab.textProperty().bind(Bindings.when(chatController.unreadProperty().greaterThan(0))
                .then(Bindings.concat(CHAT_TITLE + " (", chatController.unreadProperty(), ")"))
                .otherwise(CHAT_TITLE));

        // Switching tabs shows fresh data at once; from then on the automatic refresh keeps it fresh.
        tabPane.getSelectionModel().selectedIndexProperty().addListener((observable, oldIndex, index) -> {
            shownTab = index.intValue();
            chatController.setShown(shownTab == CHAT_TAB);
            switch (shownTab) {
                case EVENTS_TAB -> eventsController.refresh();
                case ACCOUNT_TAB -> accountController.refresh();
                default -> chatController.refresh();
            }
        });

        showUser(user);
        showStatus("Logged in as " + user.getName() + ".");
        context.startPolling(this::pollRound, this::onPollFailed);
    }

    // Shows the user's balance in the header, visible from every tab.
    // Two labels: in a narrow window the header puts them on separate rows instead of cutting a long one.
    public void showUser(UserDto user) {
        String balance = String.format("Balance: %.2f", user.getBalance());
        if (user.getReservedBalance() > 0) {
            balance += String.format(" (available: %.2f)", user.getAvailableBalance());
        }
        userLabel.setText(user.getName());
        balanceLabel.setText(balance);
    }

    public void showStatus(String message) {
        statusLabel.getStyleClass().remove(STATUS_ERROR);
        statusLabel.setText(message);
    }

    private void showStatusError(String message) {
        showStatus(message);
        statusLabel.getStyleClass().add(STATUS_ERROR);
    }

    // One round of the automatic refresh, on the poller's thread. The Events and Account tabs are fetched only while
    // on screen (each is refreshed when the user switches to it). The chat is fetched in every round, so messages
    // are counted even while its tab is hidden - usually an empty answer, since only new messages are asked for.
    private Runnable pollRound() {
        int tab = shownTab;
        Runnable showTab;
        if (tab == ACCOUNT_TAB) {
            showTab = accountController.pull(); // it updates the header's balance itself
        } else {
            Runnable showEvents = (tab == EVENTS_TAB) ? eventsController.pull() : () -> { };
            UserDto me = context.api().getMe(); // for the balance in the header
            showTab = () -> {
                showEvents.run();
                showUser(me);
            };
        }
        Runnable showChat = chatController.pull();
        return () -> {
            showTab.run();
            showChat.run();
            showUpdated();
        };
    }

    private void showUpdated() {
        showStatus("Up to date - last update " + LocalTime.now().format(TIME));
    }

    // A failed round: an ended session returns to the login screen (which stops the refresh);
    // anything else - e.g. the server is down - is shown in the status bar, and the next rounds keep trying.
    private void onPollFailed(ServerException error) {
        context.handleError(error, this::showStatusError);
    }

    @FXML
    private void onLogout() {
        context.logout();
    }
}

package com.guessmarket.client.ui.main;

import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.client.ui.Theme;
import com.guessmarket.client.ui.account.AccountController;
import com.guessmarket.client.ui.events.EventsController;
import com.guessmarket.dto.UserDto;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

/**
 * The main window after login: a header (user, balance, theme, log out), the Events and Account tabs, and a status bar.
 * Each tab has its own controller; this one only connects them to the shared context.
 */
public class MainController {
    @FXML private Label userLabel;
    @FXML private ComboBox<Theme> themeComboBox;
    @FXML private Label statusLabel;

    // Injected from the <fx:include> elements: the name is the include's fx:id + "Controller".
    @FXML private EventsController eventsController;
    @FXML private AccountController accountController;

    private ClientContext context;

    public void init(ClientContext context, UserDto user) {
        this.context = context;

        themeComboBox.getItems().setAll(Theme.values());
        themeComboBox.setValue(context.theme()); // the theme stays the same after logging out and in again
        themeComboBox.valueProperty().addListener((observable, oldTheme, newTheme) -> context.applyTheme(newTheme));

        eventsController.init(context);
        accountController.init(context);

        showUser(user);
        showStatus("Logged in as " + user.getName() + ".");
    }

    // Shows the user's balance in the header, visible from both tabs. The automatic refresh (stage 5.7) calls it too.
    public void showUser(UserDto user) {
        String balance = String.format("Balance: %.2f", user.getBalance());
        if (user.getReservedBalance() > 0) {
            balance += String.format(" (available: %.2f)", user.getAvailableBalance());
        }
        userLabel.setText(user.getName() + "  |  " + balance);
    }

    public void showStatus(String message) {
        statusLabel.setText(message);
    }

    @FXML
    private void onLogout() {
        context.logout();
    }
}

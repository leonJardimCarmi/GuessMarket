package com.guessmarket.client.ui.login;

import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.client.util.Async;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * The login screen: the user types a name only (no password, by the exercise's definition).
 * It also checks right away whether the server can be reached, so a stopped server is reported before login.
 */
public class LoginController {
    private static final String STATUS_OK = "status-ok";
    private static final String STATUS_ERROR = "status-error";

    @FXML private Label serverStatusLabel;
    @FXML private TextField userNameField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private ClientContext context;

    // Called right after the screen is loaded (FXMLLoader creates controllers without arguments).
    public void init(ClientContext context, String message) {
        this.context = context;
        showError(message);
        checkServer();
    }

    @FXML
    private void onCheckServer() {
        checkServer();
    }

    @FXML
    private void onLogin() {
        String userName = userNameField.getText().trim();
        if (userName.isEmpty()) {
            showError("Please enter your name.");
            return;
        }
        setBusy(true);
        showError(null);
        Async.run(() -> context.api().login(userName),
                user -> context.showMain(user),
                error -> {
                    setBusy(false);
                    showError(error.getMessage());
                    if (error.isConnectionProblem()) {
                        setServerStatus(false, "The server is not reachable.");
                    }
                });
    }

    private void checkServer() {
        serverStatusLabel.getStyleClass().removeAll(STATUS_OK, STATUS_ERROR);
        serverStatusLabel.setText("Checking the server...");
        Async.run(() -> context.api().ping(),
                ping -> setServerStatus(true, "Connected to the server."),
                error -> setServerStatus(false, error.getMessage()));
    }

    private void setServerStatus(boolean reachable, String text) {
        serverStatusLabel.setText(text);
        serverStatusLabel.getStyleClass().removeAll(STATUS_OK, STATUS_ERROR);
        serverStatusLabel.getStyleClass().add(reachable ? STATUS_OK : STATUS_ERROR);
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        userNameField.setDisable(busy);
    }

    // null hides the message
    private void showError(String message) {
        errorLabel.setText(message == null ? "" : message);
        errorLabel.setVisible(message != null);
        errorLabel.setManaged(message != null);
    }
}

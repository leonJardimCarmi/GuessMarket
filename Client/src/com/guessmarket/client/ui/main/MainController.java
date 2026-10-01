package com.guessmarket.client.ui.main;

import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.dto.UserDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * The main window after login. For now (stage 5.3) only shows who is logged in and allows logging out;
 * the Events and Account tabs are added in the next stages.
 */
public class MainController {
    @FXML private Label userLabel;

    private ClientContext context;

    public void init(ClientContext context, UserDto user) {
        this.context = context;
        userLabel.setText(String.format("Logged in as %s  |  Balance: %.2f", user.getName(), user.getBalance()));
    }

    @FXML
    private void onLogout() {
        context.logout();
    }
}

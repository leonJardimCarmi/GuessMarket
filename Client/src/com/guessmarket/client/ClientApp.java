package com.guessmarket.client;

import com.guessmarket.client.ui.ClientContext;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.net.URL;

/**
 * The JavaFX application: opens the window with the login screen, and logs out when the window closes.
 */
public class ClientApp extends Application {
    private static final String TITLE = "Guess Market";
    private static final String STYLESHEET = "/com/guessmarket/client/css/client.css";
    private static final double START_WIDTH = 1000;
    private static final double START_HEIGHT = 650;
    private static final double MIN_WIDTH = 400;
    private static final double MIN_HEIGHT = 300;

    private ClientContext context;

    @Override
    public void start(Stage stage) {
        // One scene for the whole run: screens replace only its root, so size and styles stay.
        Scene scene = new Scene(new Pane(), START_WIDTH, START_HEIGHT);
        scene.getStylesheets().add(resource(STYLESHEET).toExternalForm());

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        context = new ClientContext(stage);
        context.showLogin(null);
        stage.show();
    }

    // Runs when the window closes: frees the user name on the server right away.
    @Override
    public void stop() {
        if (context != null) {
            context.logoutOnExit();
        }
    }

    private static URL resource(String path) {
        URL location = ClientApp.class.getResource(path);
        if (location == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return location;
    }
}

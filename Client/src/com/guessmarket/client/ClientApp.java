package com.guessmarket.client;

import com.guessmarket.client.ui.ClientContext;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The JavaFX application: opens the window with the login screen, and logs out when the window closes.
 */
public class ClientApp extends Application {
    private static final String TITLE = "Guess Market";
    private static final double SCREEN_SHARE = 0.85; // the window opens at 85% of the screen, whatever its size
    private static final double MIN_WIDTH = 400;
    private static final double MIN_HEIGHT = 300;

    private ClientContext context;

    @Override
    public void start(Stage stage) {
        // The "visual bounds" are the screen without the taskbar. A new window opens centered on the screen.
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.max(MIN_WIDTH, screen.getWidth() * SCREEN_SHARE);
        double height = Math.max(MIN_HEIGHT, screen.getHeight() * SCREEN_SHARE);

        // One scene for the whole run: screens replace only its root, so the size and the theme stay.
        stage.setScene(new Scene(new Pane(), width, height));
        stage.setTitle(TITLE);
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
}

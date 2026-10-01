package com.guessmarket.client.ui;

import com.guessmarket.client.http.ServerApi;
import com.guessmarket.client.http.ServerException;
import com.guessmarket.client.ui.login.LoginController;
import com.guessmarket.client.ui.main.MainController;
import com.guessmarket.client.util.Async;
import com.guessmarket.client.util.Poller;
import com.guessmarket.dto.UserDto;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What every screen shares: the connection to the server, the logged-in user, and moving between screens.
 * The window (Stage and Scene) stays the same; switching screens only replaces the scene's content.
 */
public class ClientContext {
    private static final String LOGIN_SCREEN = "/com/guessmarket/client/ui/login/login.fxml";
    private static final String MAIN_SCREEN = "/com/guessmarket/client/ui/main/main.fxml";
    private static final String BASE_STYLESHEET = "/com/guessmarket/client/css/client.css";
    private static final String SESSION_ENDED = "Your session has ended. Please log in again.";

    private final Stage stage;
    private final ServerApi api = new ServerApi();
    private String userName; // null while nobody is logged in
    private Theme theme;
    private Poller poller;   // the automatic refresh; runs only while the main screen is shown

    // The stage must already have its scene.
    public ClientContext(Stage stage) {
        this.stage = stage;
        applyTheme(Theme.DEFAULT);
    }

    public ServerApi api() {
        return api;
    }

    public String userName() {
        return userName;
    }

    public boolean isLoggedIn() {
        return userName != null;
    }

    public Theme theme() {
        return theme;
    }

    // The theme comes first and client.css after it: on equal selectors the later stylesheet wins,
    // so the client's own classes (e.g. the red error text) keep their colors in every theme.
    public void applyTheme(Theme theme) {
        this.theme = theme;
        stage.getScene().getStylesheets().setAll(
                resource(theme.stylesheetPath()).toExternalForm(),
                resource(BASE_STYLESHEET).toExternalForm());
    }

    // message: shown on the login screen (e.g. why the user was sent back to it); null for none.
    public void showLogin(String message) {
        stopPolling();
        userName = null;
        LoginController login = showScreen(LOGIN_SCREEN);
        login.init(this, message);
    }

    public void showMain(UserDto user) {
        userName = user.getName();
        MainController main = showScreen(MAIN_SCREEN);
        main.init(this, user);
    }

    // Called by the main screen. round: fetches on the poller's thread and returns the screen update.
    public void startPolling(Supplier<Runnable> round, Consumer<ServerException> onError) {
        stopPolling();
        poller = new Poller(round, onError);
        poller.start();
    }

    private void stopPolling() {
        if (poller != null) {
            poller.stop();
            poller = null;
        }
    }

    // Logs out on the server; whatever the answer, this client returns to the login screen.
    public void logout() {
        stopPolling(); // no refresh may run during the logout (it would get "not logged in")
        Async.run(api::logout, () -> showLogin(null), error -> showLogin(null));
    }

    // Called when the application closes: frees the user name right away (instead of after the session timeout).
    public void logoutOnExit() {
        stopPolling();
        if (isLoggedIn()) {
            try {
                api.logout();
            } catch (ServerException ignored) {
                // the server is unreachable - nothing more to do, the session will simply expire there
            }
        }
    }

    // The common reaction to a failed request: an ended session returns to the login screen, anything else is shown.
    public void handleError(ServerException error) {
        handleError(error, this::showError);
    }

    // For actions with their own message area (e.g. "not enough money" next to the Buy button):
    // an ended session still returns to the login screen, any other error is shown in that area.
    public void handleError(ServerException error, Consumer<String> showMessage) {
        if (error.isUnauthorized() && isLoggedIn()) {
            showLogin(SESSION_ENDED);
        } else {
            showMessage.accept(error.getMessage());
        }
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(stage);
        alert.show();
    }

    // Loads a screen's FXML, puts it in the window and returns its controller.
    private <C> C showScreen(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(resource(fxmlPath));
        try {
            Parent root = loader.load();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load the screen " + fxmlPath, e);
        }
        return loader.getController();
    }

    private static URL resource(String path) {
        URL location = ClientContext.class.getResource(path);
        if (location == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return location;
    }
}

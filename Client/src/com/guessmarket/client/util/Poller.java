package com.guessmarket.client.util;

import com.guessmarket.client.http.ServerException;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The automatic refresh ("pull"): every half a second, fetches fresh data from the server on its own thread,
 * then applies it to the screen on the JavaFX thread.
 * scheduleWithFixedDelay starts the next round only after the previous one ended (plus the delay),
 * so when the server is slow, rounds are skipped instead of piling up.
 */
public class Poller {
    // The exercise allows up to 2 seconds; half a second feels immediate and is still light for a local server.
    public static final long INTERVAL_MS = 500;

    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "poller");
        thread.setDaemon(true); // never keeps the application alive after its window closes
        return thread;
    });
    private final Supplier<Runnable> round;          // on the poller's thread: fetches, returns the screen update
    private final Consumer<ServerException> onError; // on the JavaFX thread
    private volatile boolean stopped;

    public Poller(Supplier<Runnable> round, Consumer<ServerException> onError) {
        this.round = round;
        this.onError = onError;
    }

    public void start() {
        timer.scheduleWithFixedDelay(this::runRound, INTERVAL_MS, INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // Called on the JavaFX thread. A round that is still running finds 'stopped' set and leaves the screen alone.
    public void stop() {
        stopped = true;
        timer.shutdownNow();
    }

    private void runRound() {
        // Nothing may escape from here: a scheduled task that throws is cancelled for good, and the refresh would stop.
        try {
            Runnable showUpdate = round.get();
            onScreen(showUpdate);
        } catch (ServerException e) {
            onScreen(() -> onError.accept(e));
        } catch (RuntimeException e) {
            e.printStackTrace(); // a bug in the client: keep the details for debugging
            ServerException failure = new ServerException(ServerException.CLIENT_FAILURE, "Unexpected error: " + e);
            onScreen(() -> onError.accept(failure));
        }
    }

    private void onScreen(Runnable action) {
        Platform.runLater(() -> {
            if (!stopped) { // e.g. the user logged out while this round was on its way
                action.run();
            }
        });
    }
}

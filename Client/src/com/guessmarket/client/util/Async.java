package com.guessmarket.client.util;

import com.guessmarket.client.http.ServerException;
import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs server calls off the JavaFX thread and delivers the outcome back on it.
 * A request takes time; running it on the JavaFX thread would freeze the window until the answer arrives.
 * The callbacks run on the JavaFX thread (via Platform.runLater), the only thread allowed to touch the screen.
 */
public abstract class Async {
    private static final int THREADS = 4;

    private static final ExecutorService SERVER_CALLS = Executors.newFixedThreadPool(THREADS, task -> {
        Thread thread = new Thread(task, "server-call");
        thread.setDaemon(true); // background calls never keep the application alive after its window closes
        return thread;
    });

    private Async() {
    }

    // For calls that return an answer: work() runs in the background, then onSuccess / onError on the JavaFX thread.
    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<ServerException> onError) {
        SERVER_CALLS.execute(() -> {
            try {
                T result = work.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (ServerException e) {
                Platform.runLater(() -> onError.accept(e));
            } catch (RuntimeException e) {
                e.printStackTrace(); // a bug in the client: keep the details for debugging
                ServerException failure = new ServerException(ServerException.CLIENT_FAILURE, "Unexpected error: " + e);
                Platform.runLater(() -> onError.accept(failure));
            }
        });
    }

    // For calls that return nothing (e.g. logout).
    public static void run(Runnable work, Runnable onSuccess, Consumer<ServerException> onError) {
        run(() -> {
            work.run();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }
}

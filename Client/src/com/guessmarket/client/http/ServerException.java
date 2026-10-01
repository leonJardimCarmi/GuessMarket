package com.guessmarket.client.http;

/**
 * A request to the server failed. The message is ready to show to the user:
 * for an error answered by the server it is the server's own explanation ({"error": "..."}).
 */
public class ServerException extends RuntimeException {
    public static final int NO_CONNECTION = 0;   // the server could not be reached at all
    public static final int CLIENT_FAILURE = -1; // a failure inside the client itself (a bug)

    private static final int UNAUTHORIZED = 401;

    private final int status;

    public ServerException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    // The session ended (logout elsewhere, or the session-timeout passed): the user must log in again.
    public boolean isUnauthorized() {
        return status == UNAUTHORIZED;
    }

    public boolean isConnectionProblem() {
        return status == NO_CONNECTION;
    }
}

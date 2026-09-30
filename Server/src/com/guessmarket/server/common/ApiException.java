package com.guessmarket.server.common;

/**
 * An error that should reach the client with a specific HTTP status (e.g. 401 when not logged in).
 * Errors coming from the engine do not need it: ApiServlet maps their exception types to statuses.
 */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

package com.guessmarket.server.common;

import com.google.gson.Gson;
import com.guessmarket.engine.api.EngineApi;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Base class of every endpoint. A subclass only implements handleGet and/or handlePost and returns the answer;
 * this class turns it into a JSON response, and turns any exception into a JSON error with a matching status:
 * <ul>
 *   <li>ApiException - its own status (e.g. 401 not logged in)</li>
 *   <li>IllegalArgumentException - 400, the request itself is invalid</li>
 *   <li>IllegalStateException - 409, valid request that conflicts with the current state</li>
 *   <li>anything else - 500, a server bug; the details go to the server log, not to the client</li>
 * </ul>
 * Every error body has the same shape: {"error": "..."}.
 */
public abstract class ApiServlet extends HttpServlet {
    // Gson is thread-safe, so one instance serves every request thread.
    private static final Gson GSON = new Gson();

    @FunctionalInterface
    private interface RequestHandler {
        Object handle(HttpServletRequest request) throws IOException, ServletException;
    }

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, this::handleGet);
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, this::handlePost);
    }

    // Every endpoint requires a logged-in user, except the few that override this (login itself, ping).
    protected boolean requiresLogin() {
        return true;
    }

    // Subclasses override only the methods they support; the others answer 405 (Method Not Allowed).
    protected Object handleGet(HttpServletRequest request) throws IOException, ServletException {
        throw new ApiException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET is not supported for this address.");
    }

    protected Object handlePost(HttpServletRequest request) throws IOException, ServletException {
        throw new ApiException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST is not supported for this address.");
    }

    private void execute(HttpServletRequest request, HttpServletResponse response, RequestHandler handler)
            throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            if (requiresLogin()) {
                currentUserName(request); // answers 401 when there is no logged-in user
            }
            writeJson(response, HttpServletResponse.SC_OK, handler.handle(request));
        } catch (ApiException e) {
            writeError(response, e.getStatus(), e.getMessage());
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            writeError(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (Exception e) {
            // The one place that catches everything: the boundary between the server and the client.
            log("Unexpected error while handling " + request.getMethod() + " " + request.getRequestURI(), e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected server error. Please try again.");
        }
    }

    private static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(GSON.toJson(body));
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, Map.of("error", Objects.requireNonNullElse(message, "The request failed.")));
    }

    // --- Helpers for subclasses ---

    protected EngineApi engine() {
        return ServerContext.engine(getServletContext());
    }

    protected OnlineUsers onlineUsers() {
        return ServerContext.onlineUsers(getServletContext());
    }

    // A simple success answer for actions that have no data to return: {"message": "..."}
    protected static Map<String, String> message(String text) {
        return Map.of("message", text);
    }

    // The logged-in user's name, taken from their session; 401 when there is no logged-in user.
    protected static String currentUserName(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // false: never create a session just to check
        String userName = (session == null) ? null : (String) session.getAttribute(ServerContext.USER_NAME_ATTRIBUTE);
        if (userName == null) {
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, "You are not logged in.");
        }
        return userName;
    }

    protected static String requireParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing parameter '" + name + "'.");
        }
        return value.trim();
    }

    protected static int optionalInt(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a whole number, got '" + value + "'.");
        }
    }

    protected static double requireDouble(HttpServletRequest request, String name) {
        String value = requireParameter(request, name);
        double number;
        try {
            number = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            number = Double.NaN;
        }
        // Double.parseDouble also accepts "NaN" and "Infinity"; a NaN would slip through checks like price <= 0.
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a number, got '" + value + "'.");
        }
        return number;
    }
}

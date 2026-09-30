package com.guessmarket.server.servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check: answers "the server is alive" as JSON.
 * It verifies the whole pipeline (Tomcat -> servlet -> Gson -> JSON response) without touching the engine.
 */
@WebServlet(name = "PingServlet", urlPatterns = "/ping")
public class PingServlet extends HttpServlet {
    // Gson is thread-safe, so one shared instance serves every request thread.
    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "OK");
        body.put("server", "Guess Market");
        body.put("time", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(body));
    }
}

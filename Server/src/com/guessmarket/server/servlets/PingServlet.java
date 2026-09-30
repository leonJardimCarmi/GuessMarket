package com.guessmarket.server.servlets;

import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check: answers "the server is alive" as JSON, together with a short summary of the shared engine.
 */
@WebServlet(name = "PingServlet", urlPatterns = ApiPaths.PING)
public class PingServlet extends ApiServlet {

    @Override
    protected boolean requiresLogin() {
        return false;
    }

    @Override
    protected Object handleGet(HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "OK");
        body.put("server", "Guess Market");
        body.put("time", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("events", engine().getAllMarketEvents().size());
        body.put("onlineUsers", onlineUsers().count());
        return body;
    }
}

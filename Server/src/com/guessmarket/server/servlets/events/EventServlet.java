package com.guessmarket.server.servlets.events;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.dto.MarketEventDto;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /event  (event)  ->  MarketEventDto of one event
 */
@WebServlet(name = "EventServlet", urlPatterns = ApiPaths.EVENT)
public class EventServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        String eventName = requireParameter(request, ApiParams.EVENT);
        MarketEventDto event = engine().getMarketEventByName(eventName);
        if (event == null) {
            throw new IllegalArgumentException("Event '" + eventName + "' was not found.");
        }
        return event;
    }
}

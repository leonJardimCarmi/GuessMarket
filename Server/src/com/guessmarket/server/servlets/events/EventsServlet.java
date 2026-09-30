package com.guessmarket.server.servlets.events;

import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /events  ->  List of MarketEventDto, every event in the system, in upload order
 */
@WebServlet(name = "EventsServlet", urlPatterns = ApiPaths.EVENTS)
public class EventsServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        return engine().getAllMarketEvents();
    }
}

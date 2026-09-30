package com.guessmarket.server.servlets.events;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POST /event/open  (event)  ->  MarketEventDto of the opened event
 * Only the event's market maker may open it; they pay the opening cost (LMSR subsidy / Order Book investment).
 */
@WebServlet(name = "OpenEventServlet", urlPatterns = ApiPaths.OPEN_EVENT)
public class OpenEventServlet extends ApiServlet {

    @Override
    protected Object handlePost(HttpServletRequest request) {
        String eventName = requireParameter(request, ApiParams.EVENT);
        engine().openEvent(currentUserName(request), eventName);
        return engine().getMarketEventByName(eventName);
    }
}

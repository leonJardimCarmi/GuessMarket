package com.guessmarket.server.servlets.events;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POST /event/close  (event, winner)  ->  MarketEventDto of the closed event
 * Only the event's market maker may close it; the winners are paid and open orders are cancelled.
 */
@WebServlet(name = "CloseEventServlet", urlPatterns = ApiPaths.CLOSE_EVENT)
public class CloseEventServlet extends ApiServlet {

    @Override
    protected Object handlePost(HttpServletRequest request) {
        String eventName = requireParameter(request, ApiParams.EVENT);
        String winner = requireParameter(request, ApiParams.WINNER);
        engine().closeEvent(currentUserName(request), eventName, winner);
        return engine().getMarketEventByName(eventName);
    }
}

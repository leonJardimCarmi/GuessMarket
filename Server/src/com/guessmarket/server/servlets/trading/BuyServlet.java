package com.guessmarket.server.servlets.trading;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POST /event/buy  (event, outcome, shares)  ->  UserEventDetailsDto after the purchase
 * An LMSR purchase: the newest trade in the answer shows the amount paid and the fee.
 */
@WebServlet(name = "BuyServlet", urlPatterns = ApiPaths.BUY)
public class BuyServlet extends ApiServlet {

    @Override
    protected Object handlePost(HttpServletRequest request) {
        String userName = currentUserName(request);
        String eventName = requireParameter(request, ApiParams.EVENT);
        String outcome = requireParameter(request, ApiParams.OUTCOME);
        double shares = requireDouble(request, ApiParams.SHARES);

        engine().buySharesLMSR(userName, eventName, outcome, shares);
        return engine().getUserEventDetails(userName, eventName);
    }
}

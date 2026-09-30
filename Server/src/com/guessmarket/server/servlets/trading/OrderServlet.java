package com.guessmarket.server.servlets.trading;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POST /event/order  (event, outcome, side = BUY / SELL, price, shares)  ->  UserEventDetailsDto after the order
 * An Order Book order: it is matched right away when possible, and whatever is left waits in the book
 * (the answer's open orders and trades show what happened).
 */
@WebServlet(name = "OrderServlet", urlPatterns = ApiPaths.ORDER)
public class OrderServlet extends ApiServlet {

    @Override
    protected Object handlePost(HttpServletRequest request) {
        String userName = currentUserName(request);
        String eventName = requireParameter(request, ApiParams.EVENT);
        String outcome = requireParameter(request, ApiParams.OUTCOME);
        String side = requireParameter(request, ApiParams.SIDE);
        double price = requireDouble(request, ApiParams.PRICE);
        double shares = requireDouble(request, ApiParams.SHARES);

        engine().addOrder(userName, eventName, outcome, side, price, shares);
        return engine().getUserEventDetails(userName, eventName);
    }
}

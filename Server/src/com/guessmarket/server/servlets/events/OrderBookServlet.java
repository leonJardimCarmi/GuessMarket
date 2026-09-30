package com.guessmarket.server.servlets.events;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.dto.OrderBookDto;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /event/orderbook  (event, outcome)  ->  OrderBookDto: open orders and market statistics of one outcome
 */
@WebServlet(name = "OrderBookServlet", urlPatterns = ApiPaths.ORDER_BOOK)
public class OrderBookServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        String eventName = requireParameter(request, ApiParams.EVENT);
        String outcome = requireParameter(request, ApiParams.OUTCOME);
        OrderBookDto orderBook = engine().getOrderBook(eventName, outcome);
        if (orderBook == null) {
            throw new IllegalArgumentException("Event '" + eventName + "' with outcome '" + outcome + "' was not found.");
        }
        return orderBook;
    }
}

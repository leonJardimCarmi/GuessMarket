package com.guessmarket.server.servlets.users;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /me/event  (event)  ->  UserEventDetailsDto: the logged-in user's participation in one event
 */
@WebServlet(name = "MyEventServlet", urlPatterns = ApiPaths.MY_EVENT)
public class MyEventServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        return engine().getUserEventDetails(currentUserName(request), requireParameter(request, ApiParams.EVENT));
    }
}

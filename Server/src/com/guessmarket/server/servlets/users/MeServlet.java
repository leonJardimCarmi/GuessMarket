package com.guessmarket.server.servlets.users;

import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /me  ->  UserDto of the logged-in user (balance, holdings, participated events, market maker events)
 */
@WebServlet(name = "MeServlet", urlPatterns = ApiPaths.ME)
public class MeServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        return engine().getUserByName(currentUserName(request));
    }
}

package com.guessmarket.server.servlets.users;

import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /users  ->  List of UserSummaryDto (name, balance, market maker) of every user
 */
@WebServlet(name = "UsersServlet", urlPatterns = ApiPaths.USERS)
public class UsersServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        return engine().getAllUsers();
    }
}

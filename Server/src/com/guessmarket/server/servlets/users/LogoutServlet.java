package com.guessmarket.server.servlets.users;

import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POST /logout  ->  {"message"}
 * Ending the session frees the user name (see AppListener.sessionDestroyed); the user's data stays in the engine.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = ApiPaths.LOGOUT)
public class LogoutServlet extends ApiServlet {

    @Override
    protected Object handlePost(HttpServletRequest request) {
        String userName = currentUserName(request);
        request.getSession().invalidate();
        return message("'" + userName + "' logged out.");
    }
}

package com.guessmarket.server.servlets.users;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.dto.UserDto;
import com.guessmarket.server.common.ApiException;
import com.guessmarket.server.common.ApiServlet;
import com.guessmarket.server.common.ServerContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * POST /login  (username)  ->  UserDto of the logged-in user
 * A name can be used by one connected user at a time. A new name creates a new user; a name that was used before
 * (and is not connected now) continues that same user - with its balance, shares and events.
 */
@WebServlet(name = "LoginServlet", urlPatterns = ApiPaths.LOGIN)
public class LoginServlet extends ApiServlet {

    @Override
    protected boolean requiresLogin() {
        return false;
    }

    @Override
    protected Object handlePost(HttpServletRequest request) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null && existingSession.getAttribute(ServerContext.USER_NAME_ATTRIBUTE) != null) {
            throw new ApiException(HttpServletResponse.SC_CONFLICT,
                    "You are already logged in as '" + existingSession.getAttribute(ServerContext.USER_NAME_ATTRIBUTE) + "'.");
        }

        String userName = requireParameter(request, ApiParams.USERNAME);
        // Claim the name first: it is atomic, so two simultaneous logins with the same name cannot both succeed.
        if (!onlineUsers().tryLogin(userName)) {
            throw new ApiException(HttpServletResponse.SC_CONFLICT,
                    "The name '" + userName + "' is used by a connected user. Please choose another name.");
        }

        try {
            if (!engine().isUserRegistered(userName)) {
                engine().registerUser(userName);
            }
        } catch (RuntimeException e) {
            onlineUsers().logout(userName); // the login failed, so release the claimed name
            throw e;
        }

        UserDto user = engine().getUserByName(userName);
        request.getSession(true).setAttribute(ServerContext.USER_NAME_ATTRIBUTE, user.getName());
        return user;
    }
}

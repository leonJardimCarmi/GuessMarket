package com.guessmarket.server.servlets.users;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /me/account  (from, optional, default 0)  ->  List of AccountEntryDto
 * Delta fetching: a client that already has N entries asks with from=N and receives only the new ones.
 */
@WebServlet(name = "AccountServlet", urlPatterns = ApiPaths.MY_ACCOUNT)
public class AccountServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        int fromIndex = optionalInt(request, ApiParams.FROM, 0);
        return engine().getAccountEntries(currentUserName(request), fromIndex);
    }
}

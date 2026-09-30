package com.guessmarket.server.servlets.users;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POST /me/deposit  (amount)  ->  UserDto with the updated balance
 */
@WebServlet(name = "DepositServlet", urlPatterns = ApiPaths.DEPOSIT)
public class DepositServlet extends ApiServlet {

    @Override
    protected Object handlePost(HttpServletRequest request) {
        String userName = currentUserName(request);
        engine().depositFunds(userName, requireDouble(request, ApiParams.AMOUNT));
        return engine().getUserByName(userName);
    }
}

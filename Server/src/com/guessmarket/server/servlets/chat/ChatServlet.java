package com.guessmarket.server.servlets.chat;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Bonus: the chat of all the logged-in users.
 * GET  /chat  (from, optional)  ->  [ChatMessageDto] from index 'from' on (all of them without 'from')
 * POST /chat  (text)            ->  ChatMessageDto of the message that was sent
 */
@WebServlet(name = "ChatServlet", urlPatterns = ApiPaths.CHAT)
public class ChatServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        return chatRoom().getMessages(optionalInt(request, ApiParams.FROM, 0));
    }

    @Override
    protected Object handlePost(HttpServletRequest request) {
        // request.getParameter, not requireParameter: an empty text gets the chat room's clearer message.
        return chatRoom().send(currentUserName(request), request.getParameter(ApiParams.TEXT));
    }
}

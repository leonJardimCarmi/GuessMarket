package com.guessmarket.server.servlets.events;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /event/participants  (event)  ->  List of ParticipantDto: every participant's shares and their current value
 */
@WebServlet(name = "ParticipantsServlet", urlPatterns = ApiPaths.PARTICIPANTS)
public class ParticipantsServlet extends ApiServlet {

    @Override
    protected Object handleGet(HttpServletRequest request) {
        return engine().getEventParticipants(requireParameter(request, ApiParams.EVENT));
    }
}

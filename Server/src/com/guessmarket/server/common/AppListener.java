package com.guessmarket.server.common;

import com.guessmarket.engine.impl.EngineImpl;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * Tomcat calls this listener at two moments:
 * when the application starts - the single shared engine and the online-users list are created;
 * when a session ends (logout, or no request for the session-timeout period) - the user's name is freed.
 */
@WebListener
public class AppListener implements ServletContextListener, HttpSessionListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.setAttribute(ServerContext.ENGINE_ATTRIBUTE, new EngineImpl());
        context.setAttribute(ServerContext.ONLINE_USERS_ATTRIBUTE, new OnlineUsers());
        context.log("Guess Market engine is ready.");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        String userName = (String) event.getSession().getAttribute(ServerContext.USER_NAME_ATTRIBUTE);
        if (userName != null) {
            ServerContext.onlineUsers(event.getSession().getServletContext()).logout(userName);
        }
    }
}

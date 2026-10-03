package com.guessmarket.server.common;

import com.guessmarket.engine.api.EngineApi;
import jakarta.servlet.ServletContext;

/**
 * The names under which the shared objects are stored, and typed access to them.
 * ServletContext holds one engine and one OnlineUsers for the whole application;
 * each user's HttpSession holds their user name.
 */
public abstract class ServerContext {
    static final String ENGINE_ATTRIBUTE = "engine";
    static final String ONLINE_USERS_ATTRIBUTE = "onlineUsers";
    static final String CHAT_ROOM_ATTRIBUTE = "chatRoom";
    public static final String USER_NAME_ATTRIBUTE = "userName";

    private ServerContext() {
    }

    public static EngineApi engine(ServletContext context) {
        return (EngineApi) context.getAttribute(ENGINE_ATTRIBUTE);
    }

    public static OnlineUsers onlineUsers(ServletContext context) {
        return (OnlineUsers) context.getAttribute(ONLINE_USERS_ATTRIBUTE);
    }

    public static ChatRoom chatRoom(ServletContext context) {
        return (ChatRoom) context.getAttribute(CHAT_ROOM_ATTRIBUTE);
    }
}

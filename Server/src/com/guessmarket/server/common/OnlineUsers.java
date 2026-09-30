package com.guessmarket.server.common;

import java.util.HashSet;
import java.util.Set;

/**
 * The names of the users who are logged in right now. A name can be used by only one connected user at a time;
 * once that user logs out (or their session expires), the name is free again.
 * Every method is synchronized: several login requests may arrive at the same moment.
 */
public class OnlineUsers {
    private final Set<String> userKeys = new HashSet<>();

    // Claims the name for a new login. Returns false when someone with this name is already connected.
    public synchronized boolean tryLogin(String userName) {
        return userKeys.add(toKey(userName));
    }

    public synchronized void logout(String userName) {
        userKeys.remove(toKey(userName));
    }

    public synchronized boolean isOnline(String userName) {
        return userKeys.contains(toKey(userName));
    }

    public synchronized int count() {
        return userKeys.size();
    }

    // Same rule as the engine: names are compared ignoring case and surrounding spaces.
    private static String toKey(String userName) {
        return userName.trim().toLowerCase();
    }
}

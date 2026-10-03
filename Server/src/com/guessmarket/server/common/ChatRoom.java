package com.guessmarket.server.common;

import com.guessmarket.dto.ChatMessageDto;

import java.util.ArrayList;
import java.util.List;

/**
 * The chat shared by all the logged-in users (bonus). The messages live in memory only, like everything on the server.
 * A message's index in the list never changes, so a client asks only for the messages after the ones it has
 * (delta fetching, like the account history). Every method is synchronized: many users write and read at once.
 */
public class ChatRoom {
    // ChatMessageDto is immutable (final fields only), so the same objects can be handed to every reader.
    private final List<ChatMessageDto> messages = new ArrayList<>();

    public synchronized ChatMessageDto send(String userName, String text) {
        String message = (text == null) ? "" : text.strip();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("A chat message cannot be empty.");
        }
        if (message.length() > ChatMessageDto.MAX_LENGTH) {
            throw new IllegalArgumentException("A chat message can have at most " + ChatMessageDto.MAX_LENGTH
                    + " characters (got " + message.length() + ").");
        }
        ChatMessageDto sent = new ChatMessageDto(userName, message, System.currentTimeMillis());
        messages.add(sent);
        return sent;
    }

    // The messages from index 'fromIndex' on (all of them for 0); a copy, so the caller can use it after the lock.
    public synchronized List<ChatMessageDto> getMessages(int fromIndex) {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex cannot be negative.");
        }
        if (fromIndex >= messages.size()) {
            return List.of();
        }
        return List.copyOf(messages.subList(fromIndex, messages.size()));
    }
}

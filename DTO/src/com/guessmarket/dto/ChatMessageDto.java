package com.guessmarket.dto;

/**
 * One chat message, as every logged-in user sees it (bonus: chat between the connected users).
 */
public class ChatMessageDto {
    // The longest message the server accepts; the client also stops typing at this length.
    public static final int MAX_LENGTH = 500;

    private final String userName;
    private final String text;
    private final long timestamp;

    public ChatMessageDto(String userName, String text, long timestamp) {
        this.userName = userName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

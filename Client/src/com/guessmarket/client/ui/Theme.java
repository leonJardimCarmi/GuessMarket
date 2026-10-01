package com.guessmarket.client.ui;

/**
 * The color themes the user can switch between (bonus from exercise 2). DEFAULT is used at startup.
 * Each theme is a stylesheet that changes the background, the buttons and the labels' font.
 */
public enum Theme {
    DEFAULT("Default", "default.css"),
    DARK("Dark Mode", "dark.css"),
    VIBRANT("Vibrant", "vibrant.css");

    private static final String FOLDER = "/com/guessmarket/client/css/themes/";

    private final String displayName;
    private final String fileName;

    Theme(String displayName, String fileName) {
        this.displayName = displayName;
        this.fileName = fileName;
    }

    public String stylesheetPath() {
        return FOLDER + fileName;
    }

    // The ComboBox shows each item's toString()
    @Override
    public String toString() {
        return displayName;
    }
}

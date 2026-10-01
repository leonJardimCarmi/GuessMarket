package com.guessmarket.client;

import javafx.application.Application;

/**
 * The client's entry point (the jar's Main-Class).
 * It is deliberately NOT the JavaFX Application class: when JavaFX is loaded from the class path (not as Java
 * modules), starting an Application subclass directly fails with "JavaFX runtime components are missing".
 * Going through a plain main class avoids that.
 */
public abstract class ClientLauncher {

    private ClientLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}

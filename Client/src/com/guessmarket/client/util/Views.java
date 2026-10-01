package com.guessmarket.client.util;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Small helpers that several screens share: showing / hiding parts, info boxes, result messages and number input.
 */
public abstract class Views {
    private static final String MESSAGE_OK = "status-ok";
    private static final String MESSAGE_ERROR = "status-error";

    private Views() {
    }

    // managed=false also frees the node's space, so a hidden part leaves no gap.
    public static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    // A small box: the caption above, the value below.
    public static Node fact(String caption, String value) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("fact-caption");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("fact-value");
        VBox box = new VBox(2, captionLabel, valueLabel);
        box.getStyleClass().add("fact");
        return box;
    }

    public static void showSuccess(Label label, String message) {
        showMessage(label, message, MESSAGE_OK);
    }

    public static void showError(Label label, String message) {
        showMessage(label, message, MESSAGE_ERROR);
    }

    public static void clearMessage(Label label) {
        label.setText("");
        setShown(label, false);
    }

    private static void showMessage(Label label, String message, String styleClass) {
        label.getStyleClass().removeAll(MESSAGE_OK, MESSAGE_ERROR);
        label.getStyleClass().add(styleClass);
        label.setText(message);
        setShown(label, true);
    }

    // Reads a positive number the user typed. Otherwise throws IllegalArgumentException with a message for the user.
    public static double positiveNumber(TextField field, String fieldName) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            if (value > 0 && Double.isFinite(value)) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // not a number - same message as a non-positive one
        }
        throw new IllegalArgumentException(fieldName + " must be a positive number.");
    }
}

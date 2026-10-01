package com.guessmarket.client.util;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.util.function.Function;

/**
 * Connects table columns to the DTOs' getters with lambdas.
 * Unlike PropertyValueFactory("name"), a lambda is checked by the compiler: a wrong getter name does not compile,
 * instead of silently showing an empty column.
 */
public abstract class Tables {
    private static final String NUMBER_CELL = "number-cell";

    private Tables() {
    }

    public static <S> void text(TableColumn<S, String> column, Function<S, String> value) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    }

    // Shown with 2 decimals, but the cell keeps the number itself, so sorting is numeric (9 before 10).
    public static <S> void number(TableColumn<S, Double> column, Function<S, Double> value) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setCellFactory(ignored -> new TableCell<>() {
            {
                getStyleClass().add(NUMBER_CELL);
            }

            @Override
            protected void updateItem(Double number, boolean empty) {
                super.updateItem(number, empty);
                setText(empty || number == null ? null : Format.money(number));
            }
        });
    }
}

package com.guessmarket.client.ui.events;

import com.guessmarket.client.util.Format;
import com.guessmarket.client.util.Tables;
import com.guessmarket.dto.OrderBookDto;
import com.guessmarket.dto.OrderDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shows one outcome's order book: LAST / BID / ASK / MID / SPREAD and the orders waiting to be executed.
 */
public class OrderBookController {
    private static final String BUY_STYLE = "side-buy";
    private static final String SELL_STYLE = "side-sell";

    @FXML private Label titleLabel;
    @FXML private Label lastLabel;
    @FXML private Label bidLabel;
    @FXML private Label askLabel;
    @FXML private Label midLabel;
    @FXML private Label spreadLabel;
    @FXML private TableView<OrderDto> ordersTable;
    @FXML private TableColumn<OrderDto, String> sideColumn;
    @FXML private TableColumn<OrderDto, String> userColumn;
    @FXML private TableColumn<OrderDto, Double> sharesColumn;
    @FXML private TableColumn<OrderDto, Double> priceColumn;

    // Called by FXMLLoader after the @FXML fields are filled (setup that does not need the server).
    @FXML
    private void initialize() {
        Tables.text(sideColumn, OrderDto::getSide);
        sideColumn.setCellFactory(ignored -> new SideCell());
        Tables.text(userColumn, OrderDto::getUserName);
        Tables.number(sharesColumn, OrderDto::getRemainingShares);
        Tables.number(priceColumn, OrderDto::getPrice);
    }

    public void show(OrderBookDto book) {
        titleLabel.setText("Order book: " + book.getOutcomeTitle());
        lastLabel.setText(Format.optional(book.getLastPrice()));
        bidLabel.setText(Format.optional(book.getBestBid()));
        askLabel.setText(Format.optional(book.getBestAsk()));
        midLabel.setText(Format.optional(book.getMidPrice()));
        spreadLabel.setText(Format.optional(book.getSpread()));

        // Like a trading screen: from the highest price down, so the sells are above the buys
        // and the best ask meets the best bid in the middle. The sort is stable: equal prices keep their time order.
        List<OrderDto> orders = new ArrayList<>(book.getSellOrders());
        orders.addAll(book.getBuyOrders());
        orders.sort(Comparator.comparingDouble(OrderDto::getPrice).reversed());
        ordersTable.getItems().setAll(orders);
    }

    // Empties the book while another event's data is on its way.
    public void clear() {
        titleLabel.setText("Order book");
        for (Label stat : List.of(lastLabel, bidLabel, askLabel, midLabel, spreadLabel)) {
            stat.setText(Format.optional(null));
        }
        ordersTable.getItems().clear();
    }

    // Shows "Buy" in green and "Sell" in red. A cell is reused for different rows, so the old color is removed first.
    private static class SideCell extends TableCell<OrderDto, String> {
        @Override
        protected void updateItem(String side, boolean empty) {
            super.updateItem(side, empty);
            getStyleClass().removeAll(BUY_STYLE, SELL_STYLE);
            if (empty || side == null) {
                setText(null);
                return;
            }
            setText(Format.side(side));
            getStyleClass().add(OrderDto.SIDE_BUY.equals(side) ? BUY_STYLE : SELL_STYLE);
        }
    }
}

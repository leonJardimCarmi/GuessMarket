package com.guessmarket.ui;

import com.guessmarket.engine.dto.*;
import com.guessmarket.engine.impl.EngineImpl;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class MainController {

    // ===== UI Controls =====
    @FXML private Button loadFileButton;
    @FXML private TextField filePathTextField;
    @FXML private TabPane mainTabPane;

    // ===== TableViews =====
    @FXML private TableView<MarketEventDto> eventsTableView;
    @FXML private TableView<UserDto> usersTableView;
    @FXML private TableView<OrderDto> option1TableView;
    @FXML private TableView<OrderDto> option2TableView;
    @FXML private TableView<TransactionDto> participationsTableView;

    // ===== Events Table Columns =====
    @FXML private TableColumn<MarketEventDto, String> eventIdColumn;
    @FXML private TableColumn<MarketEventDto, String> eventTitleColumn;
    @FXML private TableColumn<MarketEventDto, String> eventMethodColumn;
    @FXML private TableColumn<MarketEventDto, Boolean> eventStatusColumn;

    // ===== Users Table Columns =====
    @FXML private TableColumn<UserDto, String> userNameColumn;
    @FXML private TableColumn<UserDto, Double> userBalanceColumn;

    // ===== Option 1 Table Columns (Buy Orders) =====
    @FXML private TableColumn<OrderDto, String> opt1UserColumn;
    @FXML private TableColumn<OrderDto, Double> opt1SharesColumn;
    @FXML private TableColumn<OrderDto, Double> opt1PriceColumn;

    // ===== Option 2 Table Columns (Sell Orders) =====
    @FXML private TableColumn<OrderDto, String> opt2UserColumn;
    @FXML private TableColumn<OrderDto, Double> opt2SharesColumn;
    @FXML private TableColumn<OrderDto, Double> opt2PriceColumn;

    // ===== Participations Table Columns =====
    @FXML private TableColumn<TransactionDto, String> partUserColumn;
    @FXML private TableColumn<TransactionDto, String> partOutcomeColumn;
    @FXML private TableColumn<TransactionDto, Double> partPriceColumn;

    // ===== Filters =====
    @FXML private ComboBox<String> filterMethodComboBox;
    @FXML private ComboBox<String> filterStatusComboBox;

    // ===== Order Book Stats Labels =====
    @FXML private Label lastPriceLabel;
    @FXML private Label bidPriceLabel;
    @FXML private Label askPriceLabel;
    @FXML private Label midPriceLabel;
    @FXML private Label spreadLabel;

    // ===== Container for Order Book Tables =====
    @FXML private HBox orderBookTablesContainer;

    private EngineImpl engine;

    public void setEngine(EngineImpl engine) {
        this.engine = engine;
    }

    @FXML
    public void initialize() {
        // 1. הגדרת מתיחת עמודות אוטומטית למניעת שטחים מתים
        if (eventsTableView != null) eventsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (usersTableView != null) usersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (option1TableView != null) option1TableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (option2TableView != null) option2TableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (participationsTableView != null) participationsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 2. מיפוי עמודות אירועים ומשתמשים
        if (eventIdColumn != null) eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (eventTitleColumn != null) eventTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (eventMethodColumn != null) eventMethodColumn.setCellValueFactory(new PropertyValueFactory<>("tradingMethod"));
        if (eventStatusColumn != null) eventStatusColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        if (userNameColumn != null) userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (userBalanceColumn != null) userBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // 3. מיפוי עמודות Option 1 (Order Book Buy)
        if (opt1UserColumn != null) opt1UserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (opt1SharesColumn != null) opt1SharesColumn.setCellValueFactory(new PropertyValueFactory<>("sharesCount"));
        if (opt1PriceColumn != null) opt1PriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // 4. מיפוי עמודות Option 2 (Order Book Sell)
        if (opt2UserColumn != null) opt2UserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (opt2SharesColumn != null) opt2SharesColumn.setCellValueFactory(new PropertyValueFactory<>("sharesCount"));
        if (opt2PriceColumn != null) opt2PriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // 5. מיפוי עמודות Participations
        if (partUserColumn != null) partUserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (partOutcomeColumn != null) partOutcomeColumn.setCellValueFactory(new PropertyValueFactory<>("outcomeTitle"));
        if (partPriceColumn != null) partPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPaid"));

        // 6. מאזין לבחירת אירוע בטבלה
        if (eventsTableView != null) {
            eventsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedEvent) -> {
                if (selectedEvent != null) {
                    onMarketEventSelected(selectedEvent);
                }
            });
        }
    }

    /**
     * עדכון תצוגת הנתונים בצד ימין לפי האירוע שנבחר
     */
    private void onMarketEventSelected(MarketEventDto selectedEvent) {
        if (selectedEvent == null) return;

        // טעינת היסטוריית עסקאות/השתתפויות
        if (participationsTableView != null && selectedEvent.getTransactions() != null) {
            participationsTableView.getItems().setAll(selectedEvent.getTransactions());
        }

        String method = selectedEvent.getTradingMethod();

        // בדיקה אם האירוע הוא Order Book
        if ("ORDER_BOOK".equalsIgnoreCase(method) || "OB".equalsIgnoreCase(method)) {
            if (orderBookTablesContainer != null) {
                orderBookTablesContainer.setVisible(true);
            }

            if (engine != null && selectedEvent.getOutcomes() != null && !selectedEvent.getOutcomes().isEmpty()) {
                try {
                    // משיכת ה-OrderBook עבור האופציה הראשונה של האירוע
                    String outcomeTitle = selectedEvent.getOutcomes().get(0).getTitle();
                    OrderBookDto orderBook = engine.getOrderBook(selectedEvent.getId(), outcomeTitle);

                    if (orderBook != null) {
                        if (option1TableView != null && orderBook.getBuyOrders() != null) {
                            option1TableView.getItems().setAll(orderBook.getBuyOrders());
                        }
                        if (option2TableView != null && orderBook.getSellOrders() != null) {
                            option2TableView.getItems().setAll(orderBook.getSellOrders());
                        }
                    }
                } catch (Exception e) {
                    clearOrderBookViews();
                }
            }
        } else {
            // באירוע LMSR - מסתירים את טבלאות ה-Order Book ומאפסים מדדים
            if (orderBookTablesContainer != null) {
                orderBookTablesContainer.setVisible(false);
            }
            clearOrderBookViews();
        }
    }

    private void clearOrderBookViews() {
        if (option1TableView != null) option1TableView.getItems().clear();
        if (option2TableView != null) option2TableView.getItems().clear();
        if (lastPriceLabel != null) lastPriceLabel.setText("-");
        if (bidPriceLabel != null) bidPriceLabel.setText("-");
        if (askPriceLabel != null) askPriceLabel.setText("-");
        if (midPriceLabel != null) midPriceLabel.setText("-");
        if (spreadLabel != null) spreadLabel.setText("-");
    }

    @FXML
    void onFilePathTextFieldClicked(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Market Data XML File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );

        File selectedFile = fileChooser.showOpenDialog(filePathTextField.getScene().getWindow());

        if (selectedFile != null) {
            filePathTextField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    void onLoadFileButtonClicked(ActionEvent event) {
        String path = filePathTextField.getText();

        if (path == null || path.trim().isEmpty()) {
            System.err.println("Please select a valid XML file first.");
            return;
        }

        try {
            if (engine != null) {
                engine.loadMarketDataFromXml(path);
                System.out.println("Market data loaded successfully from: " + path);
                refreshTablesData();
            }
        } catch (Exception e) {
            System.err.println("Failed to load XML: " + e.getMessage());
        }
    }

    private void refreshTablesData() {
        if (engine == null) return;

        List<MarketEventDto> events = engine.getAllMarketEvents();
        if (eventsTableView != null && events != null) {
            eventsTableView.getItems().setAll(events);
        }

        List<UserDto> users = engine.getAllUsers();
        if (usersTableView != null && users != null) {
            usersTableView.getItems().setAll(users);
        }
    }
}
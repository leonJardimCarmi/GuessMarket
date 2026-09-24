package com.guessmarket.ui;

import com.guessmarket.engine.dto.*;
import com.guessmarket.engine.impl.EngineImpl;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainController {

    // ===== UI Controls =====
    @FXML private Button loadFileButton;
    @FXML private TextField filePathTextField;
    @FXML private ProgressBar loadProgressBar;
    @FXML private TabPane mainTabPane;

    // ===== TableViews =====
    @FXML private TableView<MarketEventDto> eventsTableView;
    @FXML private TableView<UserDto> usersTableView;
    @FXML private TableView<UserTransactionRow> userHoldingsTableView;
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

    // ===== User Transactions/Holdings Table Columns =====
    @FXML private TableColumn<UserTransactionRow, String> userHoldingsEventIdColumn;
    @FXML private TableColumn<UserTransactionRow, String> userHoldingsOutcomeColumn;
    @FXML private TableColumn<UserTransactionRow, Double> userHoldingsSharesColumn;
    @FXML private TableColumn<UserTransactionRow, Double> userHoldingsPriceColumn;
    @FXML private TableColumn<UserTransactionRow, Double> userHoldingsFeeColumn;
    @FXML private TableColumn<UserTransactionRow, String> userHoldingsStatusColumn;
    @FXML private TableColumn<UserTransactionRow, Double> userHoldingsPnLColumn;

    // ===== Option 1 Table Columns (Buy Orders) =====
    @FXML private TableColumn<OrderDto, String> opt1UserColumn;
    @FXML private TableColumn<OrderDto, String> opt1OutcomeColumn;
    @FXML private TableColumn<OrderDto, Double> opt1SharesColumn;
    @FXML private TableColumn<OrderDto, Double> opt1PriceColumn;

    // ===== Option 2 Table Columns (Sell Orders) =====
    @FXML private TableColumn<OrderDto, String> opt2UserColumn;
    @FXML private TableColumn<OrderDto, String> opt2OutcomeColumn;
    @FXML private TableColumn<OrderDto, Double> opt2SharesColumn;
    @FXML private TableColumn<OrderDto, Double> opt2PriceColumn;

    // ===== Participations Table Columns =====
    @FXML private TableColumn<TransactionDto, String> partUserColumn;
    @FXML private TableColumn<TransactionDto, String> partOutcomeColumn;
    @FXML private TableColumn<TransactionDto, Double> partPriceColumn;

    // ===== Filters =====
    @FXML private ComboBox<String> filterMethodComboBox;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private ComboBox<String> filterMarketMakerComboBox;

    // ===== Event Info & Order Book Stats Labels =====
    @FXML private Label eventNameLabel;
    @FXML private Label lastPriceLabel;
    @FXML private Label bidPriceLabel;
    @FXML private Label askPriceLabel;
    @FXML private Label midPriceLabel;
    @FXML private Label spreadLabel;
    @FXML private Label lmsrInfoLabel;

    // ===== Container for Order Book Tables =====
    @FXML private HBox orderBookTablesContainer;

    @FXML private ScrollPane lmsrInfoScrollPane;
    @FXML private Label selectedUserTitleLabel;
    @FXML private Label statusLabel;

    // ===== Bonus 1: Theme Selector =====
    @FXML private ComboBox<String> themeComboBox;

    private EngineImpl engine;

    // Collections for Event Filtering
    private final ObservableList<MarketEventDto> masterEventsList = FXCollections.observableArrayList();
    private FilteredList<MarketEventDto> filteredEventsList;

    // Store currently selected event
    private MarketEventDto currentlySelectedEvent;

    public void setEngine(EngineImpl engine) {
        this.engine = engine;
    }

    @FXML
    public void initialize() {
        // 1. Set automatic column resize policy
        if (eventsTableView != null) eventsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (usersTableView != null) usersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (userHoldingsTableView != null) userHoldingsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (option1TableView != null) option1TableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (option2TableView != null) option2TableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (participationsTableView != null) participationsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 2. Map events and users table columns
        if (eventIdColumn != null) eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (eventTitleColumn != null) eventTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (eventMethodColumn != null) eventMethodColumn.setCellValueFactory(new PropertyValueFactory<>("tradingMethod"));
        if (eventStatusColumn != null) eventStatusColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        if (userNameColumn != null) userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (userBalanceColumn != null) {
            userBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
            setTwoDecimalCellFactory(userBalanceColumn);
        }

        // 3. Map user transactions/holdings table columns + 2-decimal formatting
        if (userHoldingsEventIdColumn != null) userHoldingsEventIdColumn.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        if (userHoldingsOutcomeColumn != null) userHoldingsOutcomeColumn.setCellValueFactory(new PropertyValueFactory<>("outcomeTitle"));

        if (userHoldingsSharesColumn != null) {
            userHoldingsSharesColumn.setCellValueFactory(new PropertyValueFactory<>("shares"));
            setTwoDecimalCellFactory(userHoldingsSharesColumn);
        }
        if (userHoldingsPriceColumn != null) {
            userHoldingsPriceColumn.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));
            setTwoDecimalCellFactory(userHoldingsPriceColumn);
        }
        if (userHoldingsFeeColumn != null) {
            userHoldingsFeeColumn.setCellValueFactory(new PropertyValueFactory<>("feePaid"));
            setTwoDecimalCellFactory(userHoldingsFeeColumn);
        }
        if (userHoldingsStatusColumn != null) userHoldingsStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (userHoldingsPnLColumn != null) {
            userHoldingsPnLColumn.setCellValueFactory(new PropertyValueFactory<>("profitLoss"));
            setTwoDecimalCellFactory(userHoldingsPnLColumn);
        }

        // 4. Map Option 1 (Order Book Buy) columns + 2-decimal formatting
        if (opt1UserColumn != null) opt1UserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (opt1OutcomeColumn != null) opt1OutcomeColumn.setCellValueFactory(new PropertyValueFactory<>("outcomeTitle"));
        if (opt1SharesColumn != null) {
            opt1SharesColumn.setCellValueFactory(new PropertyValueFactory<>("remainingShares"));
            setTwoDecimalCellFactory(opt1SharesColumn);
        }
        if (opt1PriceColumn != null) {
            opt1PriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
            setTwoDecimalCellFactory(opt1PriceColumn);
        }

        // 5. Map Option 2 (Order Book Sell) columns + 2-decimal formatting
        if (opt2UserColumn != null) opt2UserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (opt2OutcomeColumn != null) opt2OutcomeColumn.setCellValueFactory(new PropertyValueFactory<>("outcomeTitle"));
        if (opt2SharesColumn != null) {
            opt2SharesColumn.setCellValueFactory(new PropertyValueFactory<>("remainingShares"));
            setTwoDecimalCellFactory(opt2SharesColumn);
        }
        if (opt2PriceColumn != null) {
            opt2PriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
            setTwoDecimalCellFactory(opt2PriceColumn);
        }

        // 6. Map Participations columns + 2-decimal formatting
        if (partUserColumn != null) partUserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (partOutcomeColumn != null) partOutcomeColumn.setCellValueFactory(new PropertyValueFactory<>("outcomeTitle"));
        if (partPriceColumn != null) {
            partPriceColumn.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));
            setTwoDecimalCellFactory(partPriceColumn);
        }

        // 7. Hide ProgressBar initially
        if (loadProgressBar != null) {
            loadProgressBar.setVisible(false);
        }

        // 8. Initialize Events Filters & Wrapped List
        initializeEventsFilters();

        // 9. Setup Theme Picker (Bonus 1)
        setupThemePicker();

        // 10. Event selection listener
        if (eventsTableView != null) {
            eventsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedEvent) -> {
                if (selectedEvent != null) {
                    this.currentlySelectedEvent = selectedEvent;
                    onMarketEventSelected(selectedEvent);
                }
            });
        }

        // 11. User selection listener
        if (usersTableView != null) {
            usersTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedUser) -> {
                if (selectedUser != null) {
                    onUserSelected(selectedUser);
                }
            });
        }
    }

    /**
     * Bonus 1: Initializes Theme Selection Dropdown
     */
    private void setupThemePicker() {
        if (themeComboBox != null) {
            themeComboBox.getItems().setAll("Default", "Dark Mode", "Vibrant");
            themeComboBox.getSelectionModel().selectFirst();

            themeComboBox.valueProperty().addListener((obs, oldTheme, newTheme) -> {
                if (newTheme != null) {
                    applyTheme(newTheme);
                }
            });
        }
    }

    /**
     * Bonus 1: Dynamically applies external CSS stylesheets
     */
    private void applyTheme(String themeName) {
        if (mainTabPane == null || mainTabPane.getScene() == null) return;

        var stylesheets = mainTabPane.getScene().getStylesheets();
        stylesheets.clear();

        String cssPath = switch (themeName) {
            case "Dark Mode" -> "/dark.css";
            case "Vibrant" -> "/vibrant.css";
            default -> "/default.css";
        };

        var resource = getClass().getResource(cssPath);
        if (resource != null) {
            stylesheets.add(resource.toExternalForm());
        } else {
            System.err.println("CSS file not found: " + cssPath);
        }
    }

    /**
     * Helper method to set 2-decimal digits formatting for Double TableColumns
     */
    private <T> void setTwoDecimalCellFactory(TableColumn<T, Double> column) {
        column.setCellFactory(col -> new TableCell<T, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
    }

    /**
     * Initializes filter controls for Market Events including Method, Status, and Market Maker
     */
    private void initializeEventsFilters() {
        if (filterMethodComboBox != null) {
            filterMethodComboBox.getItems().setAll("All", "LMSR", "Order Book");
            filterMethodComboBox.getSelectionModel().selectFirst();
            filterMethodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyEventsFilter());
        }

        if (filterStatusComboBox != null) {
            filterStatusComboBox.getItems().setAll("All", "Active", "Closed");
            filterStatusComboBox.getSelectionModel().selectFirst();
            filterStatusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyEventsFilter());
        }

        if (filterMarketMakerComboBox != null) {
            filterMarketMakerComboBox.getItems().setAll("All");
            filterMarketMakerComboBox.getSelectionModel().selectFirst();
            filterMarketMakerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyEventsFilter());
        }

        filteredEventsList = new FilteredList<>(masterEventsList, p -> true);
        if (eventsTableView != null) {
            eventsTableView.setItems(filteredEventsList);
        }
    }

    /**
     * Populates the Market Maker filter ComboBox based on unique market makers in loaded events
     */
    private void populateMarketMakerFilter() {
        if (filterMarketMakerComboBox == null || engine == null) return;

        List<String> mmList = new ArrayList<>();
        mmList.add("All");

        List<MarketEventDto> events = engine.getAllMarketEvents();
        if (events != null) {
            for (MarketEventDto event : events) {
                if (event.getMarketMakerName() != null && !mmList.contains(event.getMarketMakerName())) {
                    mmList.add(event.getMarketMakerName());
                }
            }
        }

        String currentSelection = filterMarketMakerComboBox.getValue();
        filterMarketMakerComboBox.getItems().setAll(mmList);
        if (currentSelection != null && mmList.contains(currentSelection)) {
            filterMarketMakerComboBox.setValue(currentSelection);
        } else {
            filterMarketMakerComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Applies filter predicate on master events list
     */
    private void applyEventsFilter() {
        if (filteredEventsList == null) return;

        String selectedMethod = (filterMethodComboBox != null) ? filterMethodComboBox.getValue() : "All";
        String selectedStatus = (filterStatusComboBox != null) ? filterStatusComboBox.getValue() : "All";
        String selectedMM = (filterMarketMakerComboBox != null) ? filterMarketMakerComboBox.getValue() : "All";

        filteredEventsList.setPredicate(event -> {
            if (event == null) return false;

            // 1. Filter by Trading Method
            boolean matchesMethod = true;
            if (selectedMethod != null && !"All".equalsIgnoreCase(selectedMethod)) {
                String methodStr = event.getTradingMethod() != null ? event.getTradingMethod().toLowerCase() : "";
                if ("lmsr".equalsIgnoreCase(selectedMethod)) {
                    matchesMethod = methodStr.contains("lmsr");
                } else if ("order book".equalsIgnoreCase(selectedMethod)) {
                    matchesMethod = methodStr.contains("order_book") || methodStr.contains("ob") || methodStr.contains("order book");
                }
            }

            // 2. Filter by Active/Closed Status
            boolean matchesStatus = true;
            if (selectedStatus != null && !"All".equalsIgnoreCase(selectedStatus)) {
                if ("active".equalsIgnoreCase(selectedStatus)) {
                    matchesStatus = event.isActive();
                } else if ("closed".equalsIgnoreCase(selectedStatus)) {
                    matchesStatus = !event.isActive();
                }
            }

            // 3. Filter by Market Maker
            boolean matchesMM = true;
            if (selectedMM != null && !"All".equalsIgnoreCase(selectedMM)) {
                matchesMM = selectedMM.equalsIgnoreCase(event.getMarketMakerName());
            }

            return matchesMethod && matchesStatus && matchesMM;
        });
    }

    private void onUserSelected(UserDto selectedUser) {
        if (selectedUser == null || engine == null) return;

        if (selectedUserTitleLabel != null) {
            selectedUserTitleLabel.setText(selectedUser.getName() +
                    " (Balance: $" + String.format("%.2f", selectedUser.getBalance()) + ")");
        }

        if (userHoldingsTableView == null) return;

        List<UserTransactionRow> rows = new ArrayList<>();
        Map<String, Map<String, Double>> holdings = selectedUser.getHoldings();

        if (holdings != null) {
            for (String eventId : holdings.keySet()) {
                MarketEventDto event = engine.getMarketEventById(eventId);
                if (event == null) continue;

                boolean isOrderBook = "ORDER_BOOK".equalsIgnoreCase(event.getTradingMethod())
                        || "OB".equalsIgnoreCase(event.getTradingMethod());

                if (isOrderBook) {
                    Map<String, Double> outcomeHoldings = holdings.get(eventId);
                    if (outcomeHoldings != null) {
                        for (Map.Entry<String, Double> entry : outcomeHoldings.entrySet()) {
                            String outcomeTitle = entry.getKey();
                            Double shares = entry.getValue();

                            if (shares != null && shares > 0) {
                                double amountPaid = calculateUserTotalPaidForOutcome(selectedUser.getName(), event, outcomeTitle);
                                double feePaid = calculateUserTotalFeeForOutcome(selectedUser.getName(), event, outcomeTitle);

                                String status = "ACTIVE";
                                double pnl = 0.0;

                                if (!event.isActive()) {
                                    if (outcomeTitle.equalsIgnoreCase(event.getWinningOutcome())) {
                                        status = "WINNER (" + event.getWinningOutcome() + ")";
                                        double payout = shares * 1.0;
                                        pnl = payout - (amountPaid + feePaid);
                                    } else {
                                        status = "LOST";
                                        pnl = -(amountPaid + feePaid);
                                    }
                                }

                                rows.add(new UserTransactionRow(
                                        eventId,
                                        outcomeTitle,
                                        shares,
                                        amountPaid,
                                        feePaid,
                                        status,
                                        pnl
                                ));
                            }
                        }
                    }
                } else {
                    if (event.getTransactions() != null) {
                        List<TransactionDto> userTxList = new ArrayList<>();
                        for (TransactionDto tx : event.getTransactions()) {
                            if (selectedUser.getName().equalsIgnoreCase(tx.getUserName())) {
                                userTxList.add(tx);
                            }
                        }

                        userTxList.sort((tx1, tx2) -> Integer.compare(
                                event.getTransactions().indexOf(tx2),
                                event.getTransactions().indexOf(tx1)
                        ));

                        for (TransactionDto tx : userTxList) {
                            String status = event.isActive() ? "ACTIVE" :
                                    (tx.getOutcomeTitle().equalsIgnoreCase(event.getWinningOutcome())
                                            ? "WINNER (" + event.getWinningOutcome() + ")"
                                            : "LOST");

                            double pnl = 0.0;
                            if (!event.isActive()) {
                                if (tx.getOutcomeTitle().equalsIgnoreCase(event.getWinningOutcome())) {
                                    pnl = (tx.getSharesBought() * 1.0) - (tx.getAmountPaid() + tx.getFeePaid());
                                } else {
                                    pnl = -(tx.getAmountPaid() + tx.getFeePaid());
                                }
                            }

                            rows.add(new UserTransactionRow(
                                    eventId,
                                    tx.getOutcomeTitle(),
                                    tx.getSharesBought(),
                                    tx.getAmountPaid(),
                                    tx.getFeePaid(),
                                    status,
                                    pnl
                            ));
                        }
                    }
                }
            }
        }

        userHoldingsTableView.getItems().setAll(rows);
        userHoldingsTableView.refresh();
    }

    private double calculateUserTotalPaidForOutcome(String userName, MarketEventDto event, String outcome) {
        if (event.getTransactions() == null) return 0.0;
        return event.getTransactions().stream()
                .filter(tx -> userName.equalsIgnoreCase(tx.getUserName()) && outcome.equalsIgnoreCase(tx.getOutcomeTitle()))
                .mapToDouble(TransactionDto::getAmountPaid)
                .sum();
    }

    private double calculateUserTotalFeeForOutcome(String userName, MarketEventDto event, String outcome) {
        if (event.getTransactions() == null) return 0.0;
        return event.getTransactions().stream()
                .filter(tx -> userName.equalsIgnoreCase(tx.getUserName()) && outcome.equalsIgnoreCase(tx.getOutcomeTitle()))
                .mapToDouble(TransactionDto::getFeePaid)
                .sum();
    }

    /**
     * Updates right panel display based on selected market event and adds detailed LMSR info
     */
    private void onMarketEventSelected(MarketEventDto selectedEvent) {
        if (selectedEvent == null) return;

        this.currentlySelectedEvent = selectedEvent;

        if (eventNameLabel != null) {
            eventNameLabel.setText(selectedEvent.getTitle());
        }

        if (participationsTableView != null && selectedEvent.getTransactions() != null) {
            participationsTableView.getItems().setAll(selectedEvent.getTransactions());
            participationsTableView.refresh();
        }

        String method = selectedEvent.getTradingMethod();

        if ("ORDER_BOOK".equalsIgnoreCase(method) || "OB".equalsIgnoreCase(method)) {
            // באירוע Order Book: מציגים טבלאות מסחר ומסתירים לחלוטין את ה-LMSR ScrollPane
            if (orderBookTablesContainer != null) {
                orderBookTablesContainer.setVisible(true);
                orderBookTablesContainer.setManaged(true);
            }
            if (lmsrInfoScrollPane != null) {
                lmsrInfoScrollPane.setVisible(false);
                lmsrInfoScrollPane.setManaged(false);
            }
            refreshAllOrderBooksForEvent(selectedEvent);
        } else {
            // באירוע LMSR: מסתירים טבלאות Order Book ומציגים את בלוק ה-LMSR המוגדל
            if (orderBookTablesContainer != null) {
                orderBookTablesContainer.setVisible(false);
                orderBookTablesContainer.setManaged(false);
            }
            clearOrderBookViews();

            if (lmsrInfoScrollPane != null) {
                lmsrInfoScrollPane.setVisible(true);
                lmsrInfoScrollPane.setManaged(true);
            }

            if (lmsrInfoLabel != null) {
                double b = selectedEvent.getBParameter();
                int numOutcomes = (selectedEvent.getOutcomes() != null) ? selectedEvent.getOutcomes().size() : 0;
                double initialSubsidy = (numOutcomes > 0) ? (b * Math.log(numOutcomes)) : 0.0;

                StringBuilder sb = new StringBuilder();
                sb.append("LMSR Market Parameters:\n");
                sb.append(String.format("  • Liquidity Parameter (b): %.2f\n", b));
                sb.append(String.format("  • Initial Subsidy (b * ln(N)): %.2f\n", initialSubsidy));
                sb.append("  • Market Maker: ").append(selectedEvent.getMarketMakerName()).append("\n\n");
                sb.append("Outcomes & Probabilities (Current Prices):\n");

                if (selectedEvent.getOutcomes() != null) {
                    for (OutcomeDto outcome : selectedEvent.getOutcomes()) {
                        double price = outcome.getCurrentPrice();
                        double probabilityPercent = price * 100.0;

                        sb.append(String.format("   [%s]\n   ↳ Price: $%.2f | Chance: %.2f%%\n\n",
                                outcome.getTitle(), price, probabilityPercent));
                    }
                }
                lmsrInfoLabel.setText(sb.toString().trim());
            }
        }
    }

    private void refreshAllOrderBooksForEvent(MarketEventDto event) {
        if (engine == null || event == null || event.getOutcomes() == null) return;

        List<OrderDto> allBuyOrders = new ArrayList<>();
        List<OrderDto> allSellOrders = new ArrayList<>();

        for (OutcomeDto outcome : event.getOutcomes()) {
            try {
                OrderBookDto ob = engine.getOrderBook(event.getId(), outcome.getTitle());
                if (ob != null) {
                    if (ob.getBuyOrders() != null) allBuyOrders.addAll(ob.getBuyOrders());
                    if (ob.getSellOrders() != null) allSellOrders.addAll(ob.getSellOrders());
                }
            } catch (Exception ignored) {}
        }

        if (option1TableView != null) {
            option1TableView.getItems().setAll(allBuyOrders);
            option1TableView.refresh();
        }

        if (option2TableView != null) {
            option2TableView.getItems().setAll(allSellOrders);
            option2TableView.refresh();
        }

        updateAggregateOrderBookStats(allBuyOrders, allSellOrders, event);
    }

    private void updateAggregateOrderBookStats(List<OrderDto> buys, List<OrderDto> sells, MarketEventDto event) {
        Double highestBid = buys.stream().map(OrderDto::getPrice).max(Double::compareTo).orElse(null);
        Double lowestAsk = sells.stream().map(OrderDto::getPrice).min(Double::compareTo).orElse(null);

        if (bidPriceLabel != null) {
            bidPriceLabel.setText(highestBid != null ? String.format("%.2f", highestBid) : "-");
        }
        if (askPriceLabel != null) {
            askPriceLabel.setText(lowestAsk != null ? String.format("%.2f", lowestAsk) : "-");
        }

        if (highestBid != null && lowestAsk != null) {
            if (midPriceLabel != null) {
                midPriceLabel.setText(String.format("%.2f", (highestBid + lowestAsk) / 2.0));
            }
            if (spreadLabel != null) {
                spreadLabel.setText(String.format("%.2f", lowestAsk - highestBid));
            }
        } else {
            if (midPriceLabel != null) midPriceLabel.setText("-");
            if (spreadLabel != null) spreadLabel.setText("-");
        }

        if (lastPriceLabel != null && event.getTransactions() != null && !event.getTransactions().isEmpty()) {
            TransactionDto lastTx = event.getTransactions().get(event.getTransactions().size() - 1);
            double lastPrice = (lastTx.getSharesBought() > 0) ? (lastTx.getAmountPaid() / lastTx.getSharesBought()) : 0.0;
            lastPriceLabel.setText(String.format("%.2f", lastPrice));
        } else if (lastPriceLabel != null) {
            lastPriceLabel.setText("-");
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
            showErrorAlert("Error", "Please select a valid XML file first.");
            return;
        }

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0.2, 1.0);

                if (engine != null) {
                    Thread.sleep(200);
                    updateProgress(0.5, 1.0);

                    engine.loadMarketDataFromXml(path);

                    updateProgress(0.9, 1.0);
                    Thread.sleep(100);
                }

                updateProgress(1.0, 1.0);
                return null;
            }
        };

        if (loadProgressBar != null) {
            loadProgressBar.setVisible(true);
            loadProgressBar.progressProperty().bind(loadTask.progressProperty());
        }
        if (loadFileButton != null) {
            loadFileButton.setDisable(true);
        }

        loadTask.setOnSucceeded(e -> {
            unbindAndResetProgress();
            currentlySelectedEvent = null;
            refreshTablesData();
            System.out.println("Market data loaded successfully from: " + path);
        });

        loadTask.setOnFailed(e -> {
            unbindAndResetProgress();
            Throwable exception = loadTask.getException();
            String errorMsg = (exception != null) ? exception.getMessage() : "An unexpected error occurred while loading file.";
            showErrorAlert("XML Load Error", errorMsg);
        });

        new Thread(loadTask).start();
    }

    private void unbindAndResetProgress() {
        if (loadProgressBar != null) {
            loadProgressBar.progressProperty().unbind();
            loadProgressBar.setProgress(0);
            loadProgressBar.setVisible(false);
        }
        if (loadFileButton != null) {
            loadFileButton.setDisable(false);
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void refreshTablesData() {
        if (engine == null) return;

        // 1. Update Master Events list & MM ComboBox
        List<MarketEventDto> events = engine.getAllMarketEvents();
        if (events != null) {
            masterEventsList.setAll(events);
            populateMarketMakerFilter();

            if (currentlySelectedEvent != null) {
                MarketEventDto updatedEvent = engine.getMarketEventById(currentlySelectedEvent.getId());
                if (updatedEvent != null) {
                    currentlySelectedEvent = updatedEvent;
                    onMarketEventSelected(updatedEvent);
                }
            }
        }

        // 2. Update Users Table
        List<UserDto> users = engine.getAllUsers();
        if (usersTableView != null && users != null) {
            UserDto selectedUserBefore = usersTableView.getSelectionModel().getSelectedItem();
            usersTableView.getItems().setAll(users);
            usersTableView.refresh();

            if (selectedUserBefore != null) {
                UserDto updatedUser = engine.getUserByName(selectedUserBefore.getName());
                if (updatedUser != null) {
                    usersTableView.getSelectionModel().select(updatedUser);
                    onUserSelected(updatedUser);
                }
            } else if (!users.isEmpty()) {
                usersTableView.getSelectionModel().selectFirst();
                onUserSelected(users.get(0));
            }
        }
    }

    @FXML
    void onDepositButtonClicked(ActionEvent event) {
        UserDto selectedUser = usersTableView != null ? usersTableView.getSelectionModel().getSelectedItem() : null;
        if (selectedUser == null) {
            showErrorAlert("Error", "Please select a user from the Users table first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Deposit Funds");
        dialog.setHeaderText("Deposit funds for user: " + selectedUser.getName());
        dialog.setContentText("Enter amount to deposit:");

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) throw new NumberFormatException();

                if (engine != null) {
                    engine.depositFunds(selectedUser.getName(), amount);
                    refreshTablesData();
                    showInfoAlert("Success", "Successfully deposited " + String.format("%.2f", amount) + " to user account " + selectedUser.getName());
                }
            } catch (NumberFormatException e) {
                showErrorAlert("Error", "Please enter a valid positive number for amount.");
            } catch (Exception e) {
                showErrorAlert("Deposit Error", e.getMessage());
            }
        });
    }

    @FXML
    void onExecuteTradeButtonClicked(ActionEvent event) {
        MarketEventDto targetEvent = eventsTableView.getSelectionModel().getSelectedItem();
        if (targetEvent == null) {
            targetEvent = currentlySelectedEvent;
        }

        if (targetEvent == null) {
            showErrorAlert("Error", "Please select an event from the table or panel to trade.");
            return;
        }

        if (!targetEvent.isActive()) {
            showErrorAlert("Error", "Selected event is not active for trading.");
            return;
        }

        showTradeDialog(targetEvent);
    }

    private void showTradeDialog(MarketEventDto event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Execute Trade - " + event.getTitle());
        dialog.setHeaderText("Trading Method: " + event.getTradingMethod());

        ButtonType tradeButtonType = new ButtonType("Submit Trade", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(tradeButtonType, ButtonType.CANCEL);

        ComboBox<String> userComboBox = new ComboBox<>();
        if (engine != null) {
            engine.getAllUsers().forEach(u -> userComboBox.getItems().add(u.getName()));
        }

        ComboBox<String> outcomeComboBox = new ComboBox<>();
        if (event.getOutcomes() != null) {
            event.getOutcomes().forEach(o -> outcomeComboBox.getItems().add(o.getTitle()));
        }

        TextField sharesField = new TextField();
        sharesField.setPromptText("Shares count");

        TextField priceField = new TextField();
        priceField.setPromptText("Limit Price");

        ComboBox<String> actionTypeComboBox = new ComboBox<>();
        actionTypeComboBox.getItems().addAll("BUY", "SELL");
        actionTypeComboBox.getSelectionModel().selectFirst();

        boolean isOrderBook = "ORDER_BOOK".equalsIgnoreCase(event.getTradingMethod()) || "OB".equalsIgnoreCase(event.getTradingMethod());

        priceField.setDisable(!isOrderBook);
        actionTypeComboBox.setDisable(!isOrderBook);

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Select User:"), userComboBox,
                new Label("Select Outcome:"), outcomeComboBox,
                new Label("Action Type (Order Book only):"), actionTypeComboBox,
                new Label("Shares Count:"), sharesField,
                new Label("Price per Share (Order Book only):"), priceField
        );

        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(button -> {
            if (button == tradeButtonType) {
                try {
                    String userName = userComboBox.getValue();
                    String outcome = outcomeComboBox.getValue();

                    if (userName == null || userName.isEmpty()) {
                        throw new IllegalArgumentException("Please select a user for the transaction.");
                    }
                    if (outcome == null || outcome.isEmpty()) {
                        throw new IllegalArgumentException("Please select an outcome.");
                    }

                    double shares;
                    try {
                        shares = Double.parseDouble(sharesField.getText());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Shares count must be a valid number.");
                    }

                    if (shares <= 0) {
                        throw new IllegalArgumentException("Shares count must be greater than 0.");
                    }

                    if (isOrderBook) {
                        double price;
                        try {
                            price = Double.parseDouble(priceField.getText());
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Order price must be a valid number.");
                        }

                        if (price <= 0) {
                            throw new IllegalArgumentException("Order price must be positive.");
                        }

                        String side = actionTypeComboBox.getValue();
                        engine.addOrder(userName, event.getId(), outcome, side, price, shares);
                    } else {
                        engine.buySharesLMSR(userName, event.getId(), outcome, shares);
                    }

                    refreshTablesData();

                    MarketEventDto updatedEvent = engine.getMarketEventById(event.getId());
                    if (updatedEvent != null) {
                        currentlySelectedEvent = updatedEvent;
                        onMarketEventSelected(updatedEvent);
                    }

                    showInfoAlert("Success", "Trade submitted and executed successfully!");

                } catch (IllegalArgumentException | IllegalStateException e) {
                    showErrorAlert("Trade Error", e.getMessage());
                } catch (Exception e) {
                    showErrorAlert("Unexpected Error", "An error occurred while executing the trade: " + e.getMessage());
                }
            }
        });
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void onCloseEventButtonClicked(ActionEvent event) {
        MarketEventDto targetEvent = eventsTableView.getSelectionModel().getSelectedItem();
        if (targetEvent == null) {
            targetEvent = currentlySelectedEvent;
        }

        if (targetEvent == null) {
            showErrorAlert("Error", "Please select an event from the table to close.");
            return;
        }

        if (!targetEvent.isActive()) {
            showErrorAlert("Error", "Selected event is already closed or resolved.");
            return;
        }

        showCloseEventDialog(targetEvent);
    }

    private void showCloseEventDialog(MarketEventDto eventDto) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Close & Resolve Event - " + eventDto.getTitle());
        dialog.setHeaderText("Select the winning outcome for event ID: " + eventDto.getId());

        ButtonType confirmButtonType = new ButtonType("Resolve Event", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        ComboBox<String> winningOutcomeComboBox = new ComboBox<>();
        if (eventDto.getOutcomes() != null) {
            eventDto.getOutcomes().forEach(o -> winningOutcomeComboBox.getItems().add(o.getTitle()));
        }

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Winning Outcome:"),
                winningOutcomeComboBox
        );

        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(button -> {
            if (button == confirmButtonType) {
                String winningOutcome = winningOutcomeComboBox.getValue();
                if (winningOutcome == null || winningOutcome.isEmpty()) {
                    showErrorAlert("Error", "You must select a winning outcome to resolve the event.");
                    return;
                }

                try {
                    if (engine != null) {
                        engine.closeMarket(eventDto.getId(), winningOutcome);
                        refreshTablesData();

                        MarketEventDto updatedEvent = engine.getMarketEventById(eventDto.getId());
                        if (updatedEvent != null) {
                            currentlySelectedEvent = updatedEvent;
                            onMarketEventSelected(updatedEvent);
                        }

                        showInfoAlert("Event Closed", "Event '" + eventDto.getTitle() + "' has been successfully resolved with winning outcome: " + winningOutcome);
                    }
                } catch (Exception e) {
                    showErrorAlert("Close Event Error", e.getMessage());
                }
            }
        });
    }

    // =========================================================================
    // Bonus 2: Create Market Event
    // =========================================================================
    @FXML
    void onCreateEventButtonClicked(ActionEvent event) {
        if (engine == null) {
            showErrorAlert("Error", "Engine is not initialized.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Market Event");
        dialog.setHeaderText("Define a new event dynamically");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Event Title");

        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("LMSR", "ORDER_BOOK");
        methodBox.getSelectionModel().selectFirst();

        TextField bParamField = new TextField();
        bParamField.setPromptText("b Parameter (LMSR only)");

        TextField dParamField = new TextField();
        dParamField.setPromptText("d Parameter (Max price for Order Book)");

        TextField initialSharesField = new TextField();
        initialSharesField.setPromptText("Initial Shares Count (Order Book only)");

        TextField outcomesField = new TextField();
        outcomesField.setPromptText("Outcome1, Outcome2, ...");

        ComboBox<String> marketMakerBox = new ComboBox<>();
        if (engine.getAllUsers() != null) {
            engine.getAllUsers().forEach(u -> marketMakerBox.getItems().add(u.getName()));
        }

        // ניהול זמינות השדות לפי שיטת המסחר שנבחרה
        dParamField.setDisable(true);
        initialSharesField.setDisable(true); // LMSR נבחר כברירת מחדל

        methodBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isOrderBook = "ORDER_BOOK".equals(newVal);
            bParamField.setDisable(isOrderBook);
            dParamField.setDisable(!isOrderBook);
            initialSharesField.setDisable(!isOrderBook);
        });

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Event Title:"), titleField,
                new Label("Trading Method:"), methodBox,
                new Label("b Parameter (for LMSR):"), bParamField,
                new Label("d Parameter (Max price for Order Book):"), dParamField,
                new Label("Initial Shares (for Order Book):"), initialSharesField,
                new Label("Outcomes (comma separated):"), outcomesField,
                new Label("Market Maker (Must be an existing user):"), marketMakerBox
        );

        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(button -> {
            if (button == createButtonType) {
                try {
                    String title = titleField.getText().trim();
                    String method = methodBox.getValue();
                    String outcomesStr = outcomesField.getText().trim();
                    String mmName = marketMakerBox.getValue();

                    if (title.isEmpty() || outcomesStr.isEmpty() || mmName == null) {
                        throw new IllegalArgumentException("All required fields must be filled.");
                    }

                    double bParam = 0;
                    double dParam = 0;
                    double initialShares = 0;

                    if ("LMSR".equals(method)) {
                        bParam = Double.parseDouble(bParamField.getText().trim());
                        if (bParam <= 0) throw new IllegalArgumentException("b Parameter must be positive.");
                    } else if ("ORDER_BOOK".equals(method)) {
                        dParam = Double.parseDouble(dParamField.getText().trim());
                        if (dParam <= 0) throw new IllegalArgumentException("d Parameter (max price) must be positive.");

                        initialShares = Double.parseDouble(initialSharesField.getText().trim());
                        if (initialShares <= 0) throw new IllegalArgumentException("Initial Shares must be positive.");
                    }

                    String[] outcomesArray = outcomesStr.split(",");
                    List<String> outcomesList = new ArrayList<>();
                    for (String o : outcomesArray) {
                        if (!o.trim().isEmpty()) outcomesList.add(o.trim());
                    }
                    if (outcomesList.size() < 2) {
                        throw new IllegalArgumentException("Must provide at least 2 outcomes.");
                    }

                    // קריאה למתודה המעודכנת במנוע
                    engine.createMarketEvent(title, method, bParam, dParam, initialShares, outcomesList, mmName);

                    refreshTablesData();
                    updateStatus("New event '" + title + "' created successfully.");
                    showInfoAlert("Success", "Market Event created successfully!");

                } catch (NumberFormatException e) {
                    showErrorAlert("Invalid Input", "Numeric parameters must be valid numbers.");
                } catch (Exception e) {
                    showErrorAlert("Creation Error", "Failed to create event: " + e.getMessage());
                }
            }
        });
    }

    // =========================================================================
    // Bonus 3: Add New User
    // =========================================================================
    @FXML
    void onAddUserButtonClicked(ActionEvent event) {
        if (engine == null) {
            showErrorAlert("Error", "Engine is not initialized.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Register a new user to the system");

        ButtonType addButtonType = new ButtonType("Add User", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Username");

        TextField balanceField = new TextField();
        balanceField.setPromptText("Initial Balance");

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Username:"), nameField,
                new Label("Initial Balance:"), balanceField
        );
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(button -> {
            if (button == addButtonType) {
                try {
                    String name = nameField.getText().trim();
                    if (name.isEmpty()) throw new IllegalArgumentException("Username cannot be empty.");

                    double balance = Double.parseDouble(balanceField.getText().trim());
                    if (balance < 0) throw new IllegalArgumentException("Balance cannot be negative.");

                    engine.addNewUser(name, balance);

                    refreshTablesData();
                    updateStatus("User '" + name + "' added with $" + balance);

                } catch (NumberFormatException e) {
                    showErrorAlert("Invalid Input", "Balance must be a valid number.");
                } catch (Exception e) {
                    showErrorAlert("Error", "Failed to add user: " + e.getMessage());
                }
            }
        });
    }

    // =========================================================================
    // Status Bar Helper
    // =========================================================================
    private void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }

    @FXML
    void onSaveStateButtonClicked(ActionEvent event) {
        if (engine == null) {
            showErrorAlert("Save Error", "Engine is not initialized.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save System State");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DAT Files (*.dat)", "*.dat")
        );

        File selectedFile = fileChooser.showSaveDialog(mainTabPane.getScene().getWindow());
        if (selectedFile != null) {
            try {
                engine.saveStateToFile(selectedFile.getAbsolutePath());
                showInfoAlert("Success", "System state saved successfully to:\n" + selectedFile.getAbsolutePath());
            } catch (Exception e) {
                showErrorAlert("Save Error", "Failed to save state: " + e.getMessage());
            }
        }
    }

    @FXML
    void onLoadStateButtonClicked(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load System State");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DAT Files (*.dat)", "*.dat")
        );

        File selectedFile = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());
        if (selectedFile != null) {
            try {
                this.engine = EngineImpl.loadStateFromFile(selectedFile.getAbsolutePath());

                currentlySelectedEvent = null;
                eventsTableView.getSelectionModel().clearSelection();
                usersTableView.getSelectionModel().clearSelection();
                clearOrderBookViews();

                Platform.runLater(() -> {
                    refreshTablesData();

                    if (eventsTableView != null && !eventsTableView.getItems().isEmpty()) {
                        eventsTableView.getSelectionModel().selectFirst();
                        MarketEventDto firstEvent = eventsTableView.getSelectionModel().getSelectedItem();
                        if (firstEvent != null) {
                            currentlySelectedEvent = firstEvent;
                            onMarketEventSelected(firstEvent);
                        }
                    }
                });

                showInfoAlert("Success", "System state loaded successfully.");
            } catch (Exception e) {
                showErrorAlert("Load Error", "Failed to load state: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // Inner Helper Class for User Transactions & Holdings Table
    // =========================================================================
    public static class UserTransactionRow {
        private final String eventId;
        private final String outcomeTitle;
        private final double shares;
        private final double amountPaid;
        private final double feePaid;
        private final String status;
        private final double profitLoss;

        public UserTransactionRow(String eventId, String outcomeTitle, double shares, double amountPaid, double feePaid, String status, double profitLoss) {
            this.eventId = eventId;
            this.outcomeTitle = outcomeTitle;
            this.shares = shares;
            this.amountPaid = amountPaid;
            this.feePaid = feePaid;
            this.status = status;
            this.profitLoss = profitLoss;
        }

        public String getEventId() { return eventId; }
        public String getOutcomeTitle() { return outcomeTitle; }
        public double getShares() { return shares; }
        public double getAmountPaid() { return amountPaid; }
        public double getFeePaid() { return feePaid; }
        public String getStatus() { return status; }
        public double getProfitLoss() { return profitLoss; }
    }
}
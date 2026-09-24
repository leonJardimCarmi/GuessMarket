package com.guessmarket.ui;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OrderBookDto;
import com.guessmarket.engine.dto.OutcomeDto;
import com.guessmarket.engine.dto.TransactionDto;
import com.guessmarket.engine.dto.UserDto;
import com.guessmarket.engine.impl.EngineImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleUI {

    private EngineApi engine = new EngineImpl();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();
        ui.run();
    }

    public void run() {
        boolean exit = false;

        while (!exit) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        loadXmlFile();
                        break;
                    case "2":
                        showAllEvents();
                        break;
                    case "3":
                        showEventDetails();
                        break;
                    case "4":
                        executeTrade();
                        break;
                    case "5":
                        closeEvent();
                        break;
                    case "6":
                        showAndManageUsers();
                        break;
                    case "7":
                        saveSystemState();
                        break;
                    case "8":
                        loadSystemState();
                        break;
                    case "9":
                        exit = true;
                        System.out.println("See ya!");
                        break;
                    default:
                        System.out.println("Invalid option. Please enter a number between 1 and 9.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private MarketEventDto selectEventFromList(List<MarketEventDto> events_list) {
        if (events_list == null || events_list.isEmpty()) {
            return null;
        }

        System.out.println("\nSelect a market event:");
        for (int i = 0; i < events_list.size(); i++) {
            MarketEventDto event = events_list.get(i);
            System.out.printf("  %d. [ID: %s] %s\n", (i + 1), event.getId(), event.getTitle());
        }

        System.out.print("Enter event number (1-" + events_list.size() + "): ");
        int eventIndex;
        try {
            eventIndex = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid input. Please enter a valid number.");
            return null;
        }

        if (eventIndex < 1 || eventIndex > events_list.size()) {
            System.out.println("Error: Event number out of range.");
            return null;
        }

        return events_list.get(eventIndex - 1);
    }

    private UserDto selectUserFromList(List<UserDto> usersList) {
        if (usersList == null || usersList.isEmpty()) {
            return null;
        }

        System.out.println("\nSelect a user:");
        for (int i = 0; i < usersList.size(); i++) {
            UserDto user = usersList.get(i);
            System.out.printf("  %d. %s (Balance: %.2f)\n", (i + 1), user.getName(), user.getBalance());
        }

        System.out.print("Enter user number (1-" + usersList.size() + "): ");
        int userIndex;
        try {
            userIndex = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid input. Please enter a valid number.");
            return null;
        }

        if (userIndex < 1 || userIndex > usersList.size()) {
            System.out.println("Error: User number out of range.");
            return null;
        }

        return usersList.get(userIndex - 1);
    }

    private String cleanPath(String filePath) {
        if (filePath == null) {
            return "";
        }
        filePath = filePath.trim();

        if (filePath.startsWith("\"") && filePath.endsWith("\"") && filePath.length() >= 2) {
            filePath = filePath.substring(1, filePath.length() - 1).trim();
        }

        return filePath;
    }

    private void loadXmlFile() {
        System.out.print("Enter full path to XML file: ");
        String filePath = scanner.nextLine().trim();
        try {
            filePath = cleanPath(filePath);
            engine.loadMarketDataFromXml(filePath);
            System.out.println("XML file loaded successfully!");
        } catch (Exception e) {
            System.out.println("Error loading XML file: " + e.getMessage());
        }
    }

    private void showAllEvents() {
        List<MarketEventDto> events = engine.getAllMarketEvents();

        if (events == null || events.isEmpty()) {
            System.out.println("No market events available. Please load an XML file first (Option 1).");
            return;
        }

        System.out.println("\n=== All Market Events (" + events.size() + ") ===");

        for (MarketEventDto event : events) {
            System.out.println("--------------------------------------------------");
            System.out.println("Event ID: " + event.getId());
            System.out.println("Title: " + event.getTitle());
            System.out.println("Description: " + event.getDescription());
            System.out.println("Trading Method: " + event.getTradingMethod());
            System.out.println("Market Maker: " + event.getMarketMakerName() + "\nEvent Balance: " + String.format("%.2f", event.getEventBalance()));

            if (event.isActive()) {
                System.out.println("Status: Active");
            } else {
                System.out.println("Status: Closed (Winning Outcome: " + event.getWinningOutcome() + ")");
            }

            System.out.printf("Fee: %.2f%% (%s)\n", event.getFeePercentage(), event.getFeeType());
            System.out.printf("Total Fees Collected: %.2f\n", event.getTotalFeesCollected());

            System.out.println("Outcomes:");
            for (OutcomeDto outcome : event.getOutcomes()) {
                System.out.printf("  • %s | Current Price: %.2f | Shares Bought: %.2f\n",
                        outcome.getTitle(), outcome.getCurrentPrice(), outcome.getSharesCount());
            }
        }
        System.out.println("--------------------------------------------------");
    }

    private void showEventDetails() {
        List<MarketEventDto> events = engine.getAllMarketEvents();

        if (events == null || events.isEmpty()) {
            System.out.println("No market events available. Please load an XML file first (Option 1).");
            return;
        }

        MarketEventDto event = selectEventFromList(events);
        if (event == null) {
            return;
        }

        printSingleEventDetails(event);
    }

    private void printSingleEventDetails(MarketEventDto event) {
        System.out.println("--------------------------------------------------");
        System.out.println("Event ID: " + event.getId());
        System.out.println("Title: " + event.getTitle());
        System.out.println("Description: " + event.getDescription());
        System.out.println("Trading Method: " + event.getTradingMethod());
        System.out.println("Market Maker: " + event.getMarketMakerName() + " (Event Balance: " + String.format("%.2f", event.getEventBalance()) + ")");

        if (event.isActive()) {
            System.out.println("Status: Active");
        } else {
            System.out.println("Status: Closed (Winning Outcome: " + event.getWinningOutcome() + ")");
        }

        System.out.printf("Fee: %.2f%% (%s)\n", event.getFeePercentage(), event.getFeeType());
        System.out.printf("Total Fees Collected: %.2f\n", event.getTotalFeesCollected());

        System.out.println("\nOutcomes:");
        for (OutcomeDto outcome : event.getOutcomes()) {
            boolean isWinner = !event.isActive() && outcome.getTitle().equalsIgnoreCase(event.getWinningOutcome());
            System.out.printf("  • %s %s| Current Price: %.2f | Shares Bought: %.2f\n",
                    outcome.getTitle(),
                    isWinner ? "[WINNING OUTCOME] " : "",
                    outcome.getCurrentPrice(),
                    outcome.getSharesCount());

            if ("ORDER_BOOK".equalsIgnoreCase(event.getTradingMethod())) {
                OrderBookDto ob = engine.getOrderBook(event.getId(), outcome.getTitle());
                if (ob != null) {
                    System.out.println("    [Order Book - Bids (BUY)]: " + ob.getBuyOrders().size() + " orders");
                    for (var bid : ob.getBuyOrders()) {
                        System.out.printf("      - User: %s | Price: %.2f | Shares: %.2f\n", bid.getUserName(), bid.getPrice(), bid.getRemainingShares());
                    }
                    System.out.println("    [Order Book - Asks (SELL)]: " + ob.getSellOrders().size() + " orders");
                    for (var ask : ob.getSellOrders()) {
                        System.out.printf("      - User: %s | Price: %.2f | Shares: %.2f\n", ask.getUserName(), ask.getPrice(), ask.getRemainingShares());
                    }
                }
            }
        }

        List<TransactionDto> transactions = event.getTransactions();
        System.out.println("\nTransaction History (Newest to Oldest):");

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("  No transactions executed yet.");
        } else {
            for (int i = transactions.size() - 1; i >= 0; i--) {
                TransactionDto tx = transactions.get(i);
                System.out.printf("  • User: %s | Outcome: %s | Shares: %.2f | Total Paid: %.2f | Fee Paid: %.2f\n",
                        tx.getUserName(),
                        tx.getOutcomeTitle(),
                        tx.getSharesBought(),
                        tx.getAmountPaid(),
                        tx.getFeePaid());
            }
        }

        System.out.println("--------------------------------------------------");
    }

    private void executeTrade() {
        List<UserDto> users = engine.getAllUsers();
        if (users == null || users.isEmpty()) {
            System.out.println("No users available. Please load an XML file first.");
            return;
        }

        UserDto user = selectUserFromList(users);
        if (user == null) {
            return;
        }

        List<MarketEventDto> events = engine.getAllMarketEvents();
        List<MarketEventDto> activeEvents = new ArrayList<>();
        for (MarketEventDto event : events) {
            if (event.isActive()) {
                activeEvents.add(event);
            }
        }

        if (activeEvents.isEmpty()) {
            System.out.println("There are currently no active market events available for trading.");
            return;
        }

        MarketEventDto event = selectEventFromList(activeEvents);
        if (event == null) {
            return;
        }

        List<OutcomeDto> outcomes = event.getOutcomes();
        System.out.println("\nSelect an outcome:");
        for (int i = 0; i < outcomes.size(); i++) {
            OutcomeDto outcome = outcomes.get(i);
            System.out.printf("  %d. %s | Current Price: %.2f\n",
                    (i + 1),
                    outcome.getTitle(),
                    outcome.getCurrentPrice());
        }

        System.out.print("Enter option number (1-" + outcomes.size() + "): ");
        int optionIndex;
        try {
            optionIndex = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid input. Please enter a valid number.");
            return;
        }

        if (optionIndex < 1 || optionIndex > outcomes.size()) {
            System.out.println("Error: Option number out of range.");
            return;
        }

        String selectedOutcomeTitle = outcomes.get(optionIndex - 1).getTitle();

        if ("LMSR".equalsIgnoreCase(event.getTradingMethod())) {
            executeLmsrTrade(user, event, selectedOutcomeTitle);
        } else if ("ORDER_BOOK".equalsIgnoreCase(event.getTradingMethod())) {
            executeOrderBookTrade(user, event, selectedOutcomeTitle);
        } else {
            System.out.println("Error: Unsupported trading method: " + event.getTradingMethod());
        }
    }

    private void executeLmsrTrade(UserDto user, MarketEventDto event, String outcomeTitle) {
        System.out.print("Enter number of shares to buy: ");
        double sharesToBuy;
        try {
            sharesToBuy = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format for shares.");
            return;
        }

        engine.buySharesLMSR(user.getName(), event.getId(), outcomeTitle, sharesToBuy);

        MarketEventDto updatedEvent = engine.getMarketEventById(event.getId());
        List<TransactionDto> transactions = updatedEvent.getTransactions();

        if (!transactions.isEmpty()) {
            TransactionDto lastTransaction = transactions.get(transactions.size() - 1);

            System.out.println("\n=== Purchase Summary (LMSR) ===");
            System.out.println("Buyer User: " + lastTransaction.getUserName());
            System.out.println("Outcome Bought: " + lastTransaction.getOutcomeTitle());
            System.out.println("Shares Amount: " + lastTransaction.getSharesBought());
            System.out.printf("Total Paid: %.2f\n", lastTransaction.getAmountPaid());
            System.out.printf("  • Shares Cost: %.2f\n", (lastTransaction.getAmountPaid() - lastTransaction.getFeePaid()));
            System.out.printf("  • Fee Paid: %.2f\n", lastTransaction.getFeePaid());
        }

        System.out.println("\n=== Updated Event Status ===");
        printSingleEventDetails(updatedEvent);
    }

    private void executeOrderBookTrade(UserDto user, MarketEventDto event, String outcomeTitle) {
        System.out.print("Select Side (1. BUY / 2. SELL): ");
        String sideChoice = scanner.nextLine().trim();
        String side = "1".equals(sideChoice) ? "BUY" : "2".equals(sideChoice) ? "SELL" : "";
        if (side.isEmpty()) {
            System.out.println("Error: Invalid side selection.");
            return;
        }

        System.out.print("Enter Price per share: ");
        double price;
        try {
            price = Double.parseDouble(scanner.nextLine().trim());
            if (price <= 0) {
                System.out.println("Error: Price must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid price format.");
            return;
        }

        System.out.print("Enter Shares count: ");
        double shares;
        try {
            shares = Double.parseDouble(scanner.nextLine().trim());
            if (shares <= 0) {
                System.out.println("Error: Shares count must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid shares format.");
            return;
        }

        try {
            // ביצוע הפעולה מול המנוע ותפיסת שגיאות במידה והלוגיקה העסקית נכשלה
            engine.addOrder(user.getName(), event.getId(), outcomeTitle, side, price, shares);
            System.out.println("\nOrder placed successfully!");

            // שליפת המצב המעודכן והצגתו
            MarketEventDto updatedEvent = engine.getMarketEventById(event.getId());
            System.out.println("\n=== Updated Event Status ===");
            printSingleEventDetails(updatedEvent);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // תפיסת שגיאות ענפיות (כגון חוסר ב-Cash, FOK שלא יכול להתבצע מלא, חוסר הרשאה ל-Mint וכו')
            System.out.println("\nOrder execution failed: " + e.getMessage());
        }
    }

    private void closeEvent() {
        List<MarketEventDto> events = engine.getAllMarketEvents();

        if (events == null || events.isEmpty()) {
            System.out.println("No market events available. Please load an XML file first (Option 1).");
            return;
        }

        List<MarketEventDto> activeEvents = new ArrayList<>();
        for (MarketEventDto event : events) {
            if (event.isActive()) {
                activeEvents.add(event);
            }
        }

        if (activeEvents.isEmpty()) {
            System.out.println("There are currently no active market events to close.");
            return;
        }

        MarketEventDto event = selectEventFromList(activeEvents);
        if (event == null) {
            return;
        }

        List<OutcomeDto> outcomes = event.getOutcomes();
        System.out.println("\nSelect the winning outcome for event '" + event.getTitle() + "':");
        for (int i = 0; i < outcomes.size(); i++) {
            System.out.printf("  %d. %s\n", (i + 1), outcomes.get(i).getTitle());
        }

        System.out.print("Enter option number (1-" + outcomes.size() + "): ");
        int optionIndex;
        try {
            optionIndex = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid input. Please enter a valid number.");
            return;
        }

        if (optionIndex < 1 || optionIndex > outcomes.size()) {
            System.out.println("Error: Option number out of range.");
            return;
        }

        String winningOutcomeTitle = outcomes.get(optionIndex - 1).getTitle();

        engine.closeMarket(event.getId(), winningOutcomeTitle);

        System.out.println("\nMarket event '" + event.getId() + "' was successfully closed!");

        MarketEventDto closedEvent = engine.getMarketEventById(event.getId());

        System.out.println("\n=== Closed Event Status ===");
        printSingleEventDetails(closedEvent);
    }

    private void showAndManageUsers() {
        List<UserDto> users = engine.getAllUsers();
        if (users == null || users.isEmpty()) {
            System.out.println("No users available. Please load an XML file first.");
            return;
        }

        System.out.println("\n=== System Users (" + users.size() + ") ===");
        for (UserDto user : users) {
            System.out.println("--------------------------------------------------");
            System.out.println("User Name: " + user.getName());
            System.out.printf("Account Balance: %.2f\n", user.getBalance());

            Map<String, Map<String, Double>> holdings = user.getHoldings();
            if (holdings.isEmpty()) {
                System.out.println("Holdings: No shares owned.");
            } else {
                System.out.println("Holdings:");
                for (Map.Entry<String, Map<String, Double>> eventEntry : holdings.entrySet()) {
                    System.out.println("  • Event ID [" + eventEntry.getKey() + "]:");
                    for (Map.Entry<String, Double> outcomeEntry : eventEntry.getValue().entrySet()) {
                        System.out.printf("     - Outcome: %s | Shares: %.2f\n", outcomeEntry.getKey(), outcomeEntry.getValue());
                    }
                }
            }
        }
        System.out.println("--------------------------------------------------");

        System.out.print("\nWould you like to deposit funds for a user? (y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        if ("y".equals(answer)) {
            UserDto selectedUser = selectUserFromList(users);
            if (selectedUser != null) {
                System.out.print("Enter amount to deposit: ");
                try {
                    double amount = Double.parseDouble(scanner.nextLine().trim());
                    engine.depositFunds(selectedUser.getName(), amount);
                    System.out.println("Successfully deposited " + String.format("%.2f", amount) + " to " + selectedUser.getName() + "'s account.");
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid amount format.");
                }
            }
        }
    }

    private void saveSystemState() {
        List<MarketEventDto> events = engine.getAllMarketEvents();

        if (events == null || events.isEmpty()) {
            System.out.println("No market data available to save. Please load an XML file first (Option 1) or load a system state (Option 8).");
            return;
        }

        System.out.print("Enter file path (without extension): ");
        String path = cleanPath(scanner.nextLine());

        if (path.isEmpty()) {
            System.out.println("Error: Path cannot be empty.");
            return;
        }

        try {
            engine.saveStateToFile(path);
            System.out.println("System state saved successfully.");
        } catch (Exception e) {
            System.out.println("Error saving system state: " + e.getMessage());
        }
    }

    private void loadSystemState() {
        System.out.print("Enter file path (without extension): ");
        String path = cleanPath(scanner.nextLine());

        if (path.isEmpty()) {
            System.out.println("Error: Path cannot be empty.");
            return;
        }

        try {
            this.engine = EngineImpl.loadStateFromFile(path);
            System.out.println("System state loaded successfully.");
        } catch (Exception e) {
            System.out.println("Error loading system state: " + e.getMessage());
        }
    }

    private void printMenu() {
        System.out.println("=== GuessMarket System Menu ===");
        System.out.println("1. Load market data from XML file");
        System.out.println("2. Display all market events");
        System.out.println("3. Display specific market event status");
        System.out.println("4. Execute Trade (LMSR / Order Book)");
        System.out.println("5. Close a market event");
        System.out.println("6. Display & Manage Users (Holdings, Balance, Deposit)");
        System.out.println("7. Save system state");
        System.out.println("8. Load system state");
        System.out.println("9. Exit");
        System.out.print("Please select an option (1-9): ");
    }
}
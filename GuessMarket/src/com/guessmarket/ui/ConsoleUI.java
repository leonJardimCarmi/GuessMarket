package com.guessmarket.ui;

import com.guessmarket.engine.api.EngineApi;
import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OutcomeDto;
import com.guessmarket.engine.dto.TransactionDto;
import com.guessmarket.engine.impl.EngineImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private EngineApi engine = new EngineImpl();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args){
        ConsoleUI ui = new ConsoleUI();
        ui.run();
    }

    public void run() {
        boolean exit = false;

        while (!exit){
            printMenu();
            String choice = scanner.nextLine().trim();

            try{
                switch (choice){
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
                        saveSystemState();
                        break;
                    case "7":
                        loadSystemState();
                        break;
                    case "8":
                        exit = true;
                        System.out.println("See ya! ");
                        break;
                    default:
                        System.out.println("Invalid option. Please enter a number between 1 and 6. ");

                }
            }catch (Exception e){
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println();
        }

    }

    private void loadSystemState() {
        System.out.print("Enter file path (without extension): ");
        String path = scanner.nextLine().trim();

        if(path.isEmpty()){
            System.out.println("Path cannot be empty. ");
            return;
        }

        try{
            this.engine = EngineImpl.loadStateFromFile(path);
            System.out.println("System state loaded successfully. ");
        }catch (Exception e){
            System.out.println("Error loading system state: " + e.getMessage());
        }
    }

    private void saveSystemState() {
        System.out.print("Enter file path (without extension): ");
        String path = scanner.nextLine().trim();

        if(path.isEmpty()){
            System.out.println("Path cannot be empty. ");
            return;
        }

        try{
            engine.saveStateToFile(path);
            System.out.println("System state saved successfully. ");
        }catch (Exception e){
            System.out.println("Error saving system state: " + e.getMessage());
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

        System.out.println("\n=== Active Market Events (" + activeEvents.size() + ") ===");
        for (MarketEventDto event : activeEvents) {
            System.out.println("ID: " + event.getId() + " | Title: " + event.getTitle());
        }

        System.out.print("\nEnter event ID to close: ");
        String eventId = scanner.nextLine().trim();

        MarketEventDto event = engine.getMarketEventById(eventId);

        if (event == null || !event.isActive()) {
            System.out.println("Error: No active event exists with ID '" + eventId + "'.");
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

        engine.closeMarket(eventId, winningOutcomeTitle);

        System.out.println("\nMarket event '" + eventId + "' was successfully closed!");

        MarketEventDto closedEvent = engine.getMarketEventById(eventId);
        System.out.println("--------------------------------------------------");
        System.out.println("=== Closed Event Summary ===");
        System.out.println("Event ID: " + closedEvent.getId());
        System.out.println("Title: " + closedEvent.getTitle());
        System.out.println("Status: Closed");
        System.out.println("Winning Outcome: " + closedEvent.getWinningOutcome());
        System.out.printf("Event Account Balance: %.2f\n", closedEvent.getAccountBalance());
        System.out.printf("Total Fees Collected: %.2f\n", closedEvent.getTotalFeesCollected());

        System.out.println("\nFinal Outcomes Breakdown:");
        for (OutcomeDto outcome : closedEvent.getOutcomes()) {
            boolean isWinner = outcome.getTitle().equalsIgnoreCase(closedEvent.getWinningOutcome());
            System.out.printf("  • %s %s| Shares Bought: %.2f\n",
                    outcome.getTitle(),
                    isWinner ? "[WINNING OUTCOME] " : "",
                    outcome.getSharesCount());
        }
        System.out.println("--------------------------------------------------");


    }

    private void executeTrade() {
        List<MarketEventDto> events = engine.getAllMarketEvents();

        if(events == null || events.isEmpty()){
            System.out.println("No market events available. Please load an XML file first (Option 1). ");
            return;
        }

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

        System.out.println("\n=== Active Market Events (" + activeEvents.size() + ") ===");
        for (MarketEventDto event : activeEvents) {
            System.out.println("ID: " + event.getId() + " | Title: " + event.getTitle());
        }

        System.out.print("\nEnter event ID of your choice: ");
        String eventId = scanner.nextLine().trim();

        MarketEventDto event = engine.getMarketEventById(eventId);

        if (event == null || !event.isActive()) {
            System.out.println("Error: No active event exists with ID '" + eventId + "'.");
            return;
        }

        List<OutcomeDto> outcomes = event.getOutcomes();
        System.out.println("\nSelect an outcome to buy shares in:");
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

        System.out.print("Enter number of shares to buy: ");
        double sharesToBuy;
        try {
            sharesToBuy = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format for shares.");
            return;
        }

        engine.buyShares(eventId, selectedOutcomeTitle, sharesToBuy);

        MarketEventDto updatedEvent = engine.getMarketEventById(eventId);
        List<TransactionDto> transactions = updatedEvent.getTransactions();

        TransactionDto lastTransaction = transactions.get(transactions.size() - 1);

        System.out.println("\n=== Purchase Summary ===");
        System.out.println("Outcome Bought: " + lastTransaction.getOutcomeTitle());
        System.out.println("Shares Amount: " + lastTransaction.getSharesBought());
        System.out.printf("Total Paid: %.2f\n", lastTransaction.getAmountPaid());
        System.out.printf("  • Shares Cost: %.2f\n", (lastTransaction.getAmountPaid() - lastTransaction.getFeePaid()));
        System.out.printf("  • Fee Paid: %.2f\n", lastTransaction.getFeePaid());
        System.out.printf("Updated Account Balance: %.2f\n", updatedEvent.getAccountBalance());

    }

    private void showEventDetails() {
        List<MarketEventDto> events = engine.getAllMarketEvents();

        if(events == null || events.isEmpty()){
            System.out.println("No market events available. Please load an XML file first (Option 1). ");
            return;
        }

        System.out.println("\n=== All Market Events (" + events.size() + ") ===");

        for (MarketEventDto event : events) {
            System.out.println("--------------------------------------------------");
            System.out.println("Event ID: " + event.getId() + "| Title: " + event.getTitle());
        }

        System.out.print("\nEnter event id of your choice: ");
        String eventId = scanner.nextLine().trim();

        MarketEventDto event = engine.getMarketEventById(eventId);

        if(event == null){
            System.out.println("Error: No event exists with ID '" + eventId + "'.");
            return;
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Event ID: " + event.getId());
        System.out.println("Title: " + event.getTitle());
        System.out.println("Description: " + event.getDescription());

        if(event.isActive()){
            System.out.println("Status: Active");
        }else {
            System.out.println("Status: closed (Winning Outcome: " + event.getWinningOutcome() + ").");
        }

        System.out.printf("Fee: %.2f%% (%s) \n",event.getFeePercentage(), event.getFeeType());
        System.out.printf("Event Account Balance: %.2f\n", event.getAccountBalance());
        System.out.printf("Total Fees Collected: %.2f\n", event.getTotalFeesCollected());

        System.out.println("\nOutcomes: ");
        for (OutcomeDto outcome : event.getOutcomes()){
            boolean isWinner = !event.isActive() && outcome.getTitle().equalsIgnoreCase(event.getWinningOutcome());
            System.out.printf(" * %s %s| Current Price: %.2f | Shares Bought: %.2f\n",
                    outcome.getTitle(), isWinner? "[WINNING OUTCOME] " : "", outcome.getCurrentPrice(), outcome.getSharesCount());
        }

        List<TransactionDto> transactions = event.getTransactions();
        System.out.println("\nTransaction History (Newest to Oldest):");

        if(transactions == null || transactions.isEmpty()){
            System.out.println("  No transactions executed yet.");
        }else{
            for (int i = transactions.size() -1; i>=0 ; i--){
                TransactionDto tx = transactions.get(i);
                System.out.printf("  • Outcome: %s | Shares: %.2f | Total Paid: %.2f | Fee Paid: %.2f\n",
                        tx.getOutcomeTitle(),
                        tx.getSharesBought(),
                        tx.getAmountPaid(),
                        tx.getFeePaid());
            }
        }


        System.out.println("--------------------------------------------------");
    }

    private void showAllEvents() {
        List<MarketEventDto> events = engine.getAllMarketEvents();

        if(events == null || events.isEmpty()){
            System.out.println("No market events available. Please load an XML file first (Option 1). ");
            return;
        }

        System.out.println("\n=== All Market Events (" + events.size() + ") ===");

        for (MarketEventDto event : events){
            System.out.println("--------------------------------------------------");
            System.out.println("Event ID: " + event.getId());
            System.out.println("Title: " + event.getTitle());
            System.out.println("Description: " + event.getDescription());

            if(event.isActive()){
                System.out.println("Status: Active");
            }else{
                System.out.println("Status: Closed (winning Outcome: " + event.getWinningOutcome() + ")");
            }

            System.out.printf("Fee: %.2f%% (%s)\n", event.getFeePercentage(), event.getFeeType());
            System.out.printf("Event Account Balance: %.2f\n", event.getAccountBalance());

            System.out.println("Outcomes: ");
            for(OutcomeDto outcome : event.getOutcomes()){
                System.out.printf(" * %s | Current Price %.2f | Shares Bought: %.2f\n",
                        outcome.getTitle(), outcome.getCurrentPrice(), outcome.getSharesCount());
            }
        }
        System.out.println("--------------------------------------------------");
    }

    private void loadXmlFile() {
        System.out.print("Enter full path to file: ");
        String filePath = scanner.nextLine().trim();
        try {
            engine.loadMarketDataFromXml(filePath);
            System.out.println("XML file loaded successfully!");
        } catch (Exception e) {
            System.out.println("Error loading XML file: " + e.getMessage());
        }
    }

    private void printMenu() {
        System.out.println("=== GuessMarket System Menu ===");
        System.out.println("1. Load market data from XML file");
        System.out.println("2. Display all market events");
        System.out.println("3. Display specific market event status");
        System.out.println("4. Buy shares in an event");
        System.out.println("5. Close a market event");
        System.out.println("6. Save system state");
        System.out.println("7. Load system state");
        System.out.println("8. Exit");
        System.out.print("Please select an option (1-8): ");
    }


}

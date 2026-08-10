package com.guessmarket.engine;

import com.guessmarket.engine.dto.MarketEventDto;
import com.guessmarket.engine.dto.OutcomeDto;
import com.guessmarket.engine.dto.TransactionDto;
import com.guessmarket.engine.impl.EngineImpl;
import com.guessmarket.engine.model.MarketEvent;

public class main {

    public static void main(String[] args) {
        // 1. יצירת מופע של המנוע
        EngineImpl engine = new EngineImpl();

        System.out.println("=== 1. יצירת אירוע ניסיוני ===");
        engine.addEventForTesting(
                "EVT_1",
                "World Cup Final",
                "Will Team A win the match?",
                1000.0,
                5.0,
                MarketEvent.FeeType.AT_PURCHASE
        );

        // הדפסת מצב התחלתי
        printEventData(engine.getMarketEventById("EVT_1"));

        // 2. ביצוע רכישת מניות תקינה
        System.out.println("\n=== 2. ביצוע רכישה: 10 מניות של YES ===");
        engine.buyShares("EVT_1", "YES", 10.0);

        // הדפסת המצב המעודכן לאחר הרכישה
        printEventData(engine.getMarketEventById("EVT_1"));

        // 3. בדיקת טיפול בשגיאות קלט (כמות שלילית)
        System.out.println("\n=== 3. ניסיון קנייה עם כמות שלילית (בדיקת Exception) ===");
        try {
            engine.buyShares("EVT_1", "YES", -5.0);
        } catch (IllegalArgumentException e) {
            System.out.println("✅ נלכדה שגיאה צפויה בהצלחה: " + e.getMessage());
        }

        // 4. סגירת האירוע
        System.out.println("\n=== 4. סגירת האירוע וקביעת YES כמנצח ===");
        engine.closeMarket("EVT_1", "YES");

        // הדפסת המצב לאחר סגירה
        printEventData(engine.getMarketEventById("EVT_1"));

        // 5. ניסיון קנייה באירוע סגור
        System.out.println("\n=== 5. ניסיון לקנות באירוע סגור (בדיקת Exception) ===");
        try {
            engine.buyShares("EVT_1", "YES", 5.0);
        } catch (IllegalStateException e) {
            System.out.println("✅ נלכדה שגיאה צפויה בהצלחה: " + e.getMessage());
        }
    }

    /**
     * מתודת עזר להדפסת נתוני DTO של אירוע
     */
    private static void printEventData(MarketEventDto event) {
        if (event == null) {
            System.out.println("Event not found!");
            return;
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Event ID: " + event.getId() + " | Title: " + event.getTitle());
        System.out.println("Is Active: " + event.isActive() + " | Winner: " + event.getWinningOutcome());
        System.out.println("Balance: " + event.getAccountBalance() + " | Total Fees: " + event.getTotalFeesCollected());

        System.out.println("Outcomes:");
        for (OutcomeDto outcome : event.getOutcomes()) {
            System.out.printf("  - %s | Shares: %.2f | Price: %.4f\n",
                    outcome.getTitle(), outcome.getSharesCount(), outcome.getCurrentPrice());
        }

        System.out.println("Transactions (" + event.getTransactions().size() + "):");
        for (TransactionDto tx : event.getTransactions()) {
            System.out.printf("  - Outcome: %s | Shares: %.2f | Paid: %.2f | Fee: %.2f\n",
                    tx.getOutcomeTitle(), tx.getSharesBought(), tx.getAmountPaid(), tx.getFeePaid());
        }
        System.out.println("--------------------------------------------------");
    }
}
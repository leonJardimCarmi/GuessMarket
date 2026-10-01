package com.guessmarket.engine.model;

import java.util.List;

/**
 * The LMSR formulas. e^x overflows to Infinity above x ≈ 709 (q / b > 709: e.g. 71,000 shares when b = 100),
 * so every e^(q / b) is computed as e^((q - max) / b), where max is the largest q ("log-sum-exp").
 * Each such power is then between 0 and 1, and the results are the same numbers as the textbook formulas.
 */
public class LmsrCalculator {
    private LmsrCalculator() {}

    //C(q) = b * ln(sum(e^(q_i / b)))
    public static double calculateCostFunction(List<Outcome> outcomes, double B){
        double[] shares = new double[outcomes.size()];
        for (int i = 0; i < shares.length; i++) {
            shares[i] = outcomes.get(i).getSharesBought();
        }
        return costFunction(shares, B);
    }

    public static double calculatePurchaseCost(List<Outcome> outcomes, String targetOutcomeTitle, double amountToBuy, double B){
        if( amountToBuy <= 0 ){
            throw new IllegalArgumentException("Amount to buy be greater than zero. ");
        }

        double[] sharesBefore = new double[outcomes.size()];
        double[] sharesAfter = new double[outcomes.size()];
        boolean foundTarget = false;

        for (int i = 0; i < sharesBefore.length; i++) {
            Outcome outcome = outcomes.get(i);
            sharesBefore[i] = outcome.getSharesBought();
            sharesAfter[i] = outcome.getSharesBought();
            if(outcome.getTitle().equalsIgnoreCase(targetOutcomeTitle)){
                sharesAfter[i] += amountToBuy;
                foundTarget = true;
            }
        }
        if(!foundTarget){
            throw new IllegalArgumentException("Outcome with title " + targetOutcomeTitle + " was not found. ");
        }

        return costFunction(sharesAfter, B) - costFunction(sharesBefore, B);
    }

    //P_i = e^(q_i / b) / sum(e^(q_k / b))
    // Dividing the numerator and the denominator by the same e^(max / b) does not change the result.
    public static double calculatePrice(List<Outcome> outcomes, String targetOutcomeTitle, double B){
        double max = maxShares(outcomes);
        double sumExponentials = 0.0;
        double targetExponential = 0.0;
        boolean foundTarget = false;

        for (Outcome outcome: outcomes){
            double expVal = Math.exp((outcome.getSharesBought() - max) / B);
            sumExponentials += expVal;

            if(outcome.getTitle().equalsIgnoreCase(targetOutcomeTitle)){
                targetExponential = expVal;
                foundTarget = true;
            }
        }

        if(!foundTarget){
            throw new IllegalArgumentException("Outcome with title ' " + targetOutcomeTitle + " ' was not found. " );
        }

        return targetExponential / sumExponentials;
    }

    public static double calculateFee(double amount, double feePercentage, double B){
        return amount * (feePercentage / 100.0);
    }

    // C = b * ln(sum(e^(q_i / b))) = max + b * ln(sum(e^((q_i - max) / b)))
    // (e^(q_i / b) = e^(max / b) * e^((q_i - max) / b), and ln turns the common factor e^(max / b) into max / b.)
    private static double costFunction(double[] shares, double B) {
        double max = Double.NEGATIVE_INFINITY;
        for (double q : shares) {
            max = Math.max(max, q);
        }
        double sumExponentials = 0.0;
        for (double q : shares) {
            sumExponentials += Math.exp((q - max) / B);
        }
        return max + B * Math.log(sumExponentials);
    }

    private static double maxShares(List<Outcome> outcomes) {
        double max = Double.NEGATIVE_INFINITY;
        for (Outcome outcome : outcomes) {
            max = Math.max(max, outcome.getSharesBought());
        }
        return max;
    }
}

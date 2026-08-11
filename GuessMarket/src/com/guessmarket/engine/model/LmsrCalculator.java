package com.guessmarket.engine.model;

import java.util.List;

public class LmsrCalculator {

    private LmsrCalculator() {}

    //C(q) = b * ln(sum(e^(q_i / b)))
    public static double calculateCostFunction(List<Outcome> outcomes, double B){
        double sumExponentials =0.0;
        for (Outcome outcome: outcomes){
            sumExponentials += Math.exp(outcome.getSharesBought() / B);
        }
        return  B * Math.log(sumExponentials);

    }

    public static double calculatePurchaseCost(List<Outcome> outcomes, String targetOutcomeTitle, double amountToBuy, double B){
        if( amountToBuy <= 0 ){
            throw new IllegalArgumentException("Amount to buy be greater than zero. ");
        }

        double currentCost = calculateCostFunction(outcomes, B);

        double sumExponentialsAfter = 0.0;
        boolean foundTarget = false;

        for(Outcome outcome : outcomes){
            double shares = outcome.getSharesBought();
            if(outcome.getTitle().equalsIgnoreCase(targetOutcomeTitle)){
                shares += amountToBuy;
                foundTarget = true;
            }
            sumExponentialsAfter += Math.exp(shares / B);
        }
        if(!foundTarget){
            throw new IllegalArgumentException("Outcome with title " + targetOutcomeTitle + " was not found. ");
        }

        double futureCost = B * Math.log(sumExponentialsAfter);

        return  futureCost - currentCost ;
    }

    //P_i = e^(q_i / b) / sum(e^(q_k / b))
    public static double calculatePrice(List<Outcome> outcomes, String targetOutcomeTitle, double B){
        double sumExponentals = 0.0 ;
        double targetExponential =0.0;
        boolean foundTarget = false;

        for (Outcome outcome: outcomes){
            double expVal = Math.exp(outcome.getSharesBought() / B);
            sumExponentals += expVal;

            if(outcome.getTitle().equalsIgnoreCase(targetOutcomeTitle)){
                targetExponential =expVal;
                foundTarget = true;
            }
        }

        if(!foundTarget){
            throw new IllegalArgumentException("Outcome with title ' " + targetOutcomeTitle + " ' was not found. " );
        }

        return targetExponential / sumExponentals;
    }

    public static double calculateFee(double amount, double feePercentage, double B){
        return amount * (feePercentage / 100.0);
    }


}

package com.yonara.adauction.fairness;

import com.yonara.adauction.model.AuctionResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class FairnessAuditor {

    private static final double DISPARITY_THRESHOLD = 0.15;

    public void auditResults(List<AuctionResult> results) {

        if (results == null || results.isEmpty()) return;

        checkDemographicParity(results);
        checkPredatoryAdTargeting(results);
    }

    private void checkDemographicParity(List<AuctionResult> results) {

        Map<String, Double> winRateByIncome = results.stream()
            .filter(r -> r.getWinningUserProfile() != null)
            .collect(Collectors.groupingBy(
                r -> r.getWinningUserProfile().getIncomeLevel(),
                Collectors.averagingDouble(r -> r.getWinningBid())
            ));

        if (winRateByIncome.isEmpty()) return;

        double max = Collections.max(winRateByIncome.values());
        double min = Collections.min(winRateByIncome.values());
        double disparity = max - min;

        System.out.println("Demographic parity check:");
        winRateByIncome.forEach((group, rate) ->
            System.out.println("  " + group + " avg winning bid: $" + 
                String.format("%.2f", rate)));

        if (disparity > DISPARITY_THRESHOLD) {
            System.out.println("FAIRNESS ALERT: disparity of " +
                String.format("%.2f", disparity) +
                " detected across income groups.");
        } else {
            System.out.println("Demographic parity check passed.");
        }
    }

    private void checkPredatoryAdTargeting(List<AuctionResult> results) {

        Map<String, Long> countByIncome = results.stream()
            .filter(r -> r.getWinningUserProfile() != null)
            .collect(Collectors.groupingBy(
                r -> r.getWinningUserProfile().getIncomeLevel(),
                Collectors.counting()
            ));

        System.out.println("Ad distribution by income level:");
        countByIncome.forEach((group, count) ->
            System.out.println("  " + group + ": " + count + " ads"));

        long lowIncomeCount = countByIncome.getOrDefault("low", 0L);
        long highIncomeCount = countByIncome.getOrDefault("high", 0L);

        if (lowIncomeCount > highIncomeCount * 1.5) {
            System.out.println("FAIRNESS ALERT: low income users receiving " +
                "disproportionately more ads than high income users.");
        }
    }
}
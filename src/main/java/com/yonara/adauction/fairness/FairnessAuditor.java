package com.yonara.adauction.fairness;

import com.yonara.adauction.model.AuctionResult;
import com.yonara.adauction.model.FairnessAlert;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure fairness-checking logic. Takes a window of auction results and
 * returns any fairness violations found, with no side effects (no DB,
 * no Kafka, no logging). That keeps it cheap to unit test and keeps
 * persistence/publishing concerns in FairnessAlertService instead.
 */
@Component
public class FairnessAuditor {

    static final double DEMOGRAPHIC_PARITY_THRESHOLD = 0.15;
    static final double PREDATORY_TARGETING_RATIO = 1.5;

    // Ad categories considered predatory for the purposes of this audit,
    // following Sweeney (2013): payday loans and criminal-record-search
    // ads disproportionately reaching specific demographic groups.
    static final Set<String> PREDATORY_CATEGORIES = Set.of(
        "payday-loan", "criminal-record-search"
    );

    public List<FairnessAlert> evaluate(List<AuctionResult> results) {
        if (results == null || results.isEmpty()) return List.of();

        List<FairnessAlert> alerts = new ArrayList<>();

        Map<String, List<AuctionResult>> byCategory = results.stream()
            .filter(r -> r.getAdCategory() != null)
            .collect(Collectors.groupingBy(AuctionResult::getAdCategory));

        for (Map.Entry<String, List<AuctionResult>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<AuctionResult> categoryResults = entry.getValue();

            checkDemographicParity(category, categoryResults).ifPresent(alerts::add);

            if (PREDATORY_CATEGORIES.contains(category)) {
                checkPredatoryTargeting(category, categoryResults).ifPresent(alerts::add);
            }
        }

        return alerts;
    }

    /**
     * Are average winning bids for this ad category roughly equal across
     * income groups? A large gap means one income group is more likely to
     * be served (or outbid for) this category of ad than another.
     */
    Optional<FairnessAlert> checkDemographicParity(String category, List<AuctionResult> results) {

        Map<String, Double> avgBidByIncome = results.stream()
            .filter(r -> r.getWinningUserProfile() != null)
            .collect(Collectors.groupingBy(
                r -> r.getWinningUserProfile().getIncomeLevel(),
                Collectors.averagingDouble(AuctionResult::getWinningBid)
            ));

        if (avgBidByIncome.size() < 2) return Optional.empty();

        String maxGroup = Collections.max(avgBidByIncome.entrySet(), Map.Entry.comparingByValue()).getKey();
        String minGroup = Collections.min(avgBidByIncome.entrySet(), Map.Entry.comparingByValue()).getKey();
        double disparity = avgBidByIncome.get(maxGroup) - avgBidByIncome.get(minGroup);

        if (disparity > DEMOGRAPHIC_PARITY_THRESHOLD) {
            return Optional.of(new FairnessAlert(
                "demographic_parity", category, disparity, minGroup
            ));
        }
        return Optional.empty();
    }

    /**
     * For predatory ad categories specifically: are low-income users
     * winning (seeing) these ads at a disproportionately higher rate
     * than high-income users, even when bid amounts are comparable?
     * This is the Sweeney finding, reproduced as a live check.
     */
    Optional<FairnessAlert> checkPredatoryTargeting(String category, List<AuctionResult> results) {

        Map<String, Long> countByIncome = results.stream()
            .filter(r -> r.getWinningUserProfile() != null)
            .collect(Collectors.groupingBy(
                r -> r.getWinningUserProfile().getIncomeLevel(),
                Collectors.counting()
            ));

        long lowIncomeCount = countByIncome.getOrDefault("low", 0L);
        long highIncomeCount = countByIncome.getOrDefault("high", 0L);

        if (highIncomeCount == 0 && lowIncomeCount > 0) {
            return Optional.of(new FairnessAlert(
                "predatory_targeting", category, lowIncomeCount, "low"
            ));
        }

        if (highIncomeCount > 0 && lowIncomeCount > highIncomeCount * PREDATORY_TARGETING_RATIO) {
            double ratio = (double) lowIncomeCount / highIncomeCount;
            return Optional.of(new FairnessAlert(
                "predatory_targeting", category, ratio, "low"
            ));
        }

        return Optional.empty();
    }
}
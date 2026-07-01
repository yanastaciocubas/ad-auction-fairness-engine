package com.yonara.adauction.fairness;

import com.yonara.adauction.model.AuctionResult;
import com.yonara.adauction.model.FairnessAlert;
import com.yonara.adauction.model.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FairnessAuditorTest {

    private final FairnessAuditor auditor = new FairnessAuditor();

    private AuctionResult result(String category, double bid, String incomeLevel) {
        UserProfile profile = new UserProfile("user-1", "18-34", "F", incomeLevel);
        return new AuctionResult("homepage-banner", "nike", category, bid,
            System.currentTimeMillis(), profile);
    }

    @Test
    void evaluate_returnsNoAlerts_whenResultsAreEmpty() {
        assertTrue(auditor.evaluate(List.of()).isEmpty());
        assertTrue(auditor.evaluate(null).isEmpty());
    }

    @Test
    void demographicParity_flagsLargeBidGapAcrossIncomeGroups() {
        List<AuctionResult> results = new ArrayList<>();
        results.add(result("retail", 1.00, "low"));
        results.add(result("retail", 1.10, "low"));
        results.add(result("retail", 4.50, "high"));
        results.add(result("retail", 4.40, "high"));

        var alert = auditor.checkDemographicParity("retail", results);

        assertTrue(alert.isPresent());
        assertEquals("demographic_parity", alert.get().getCheckType());
        assertEquals("low", alert.get().getFlaggedGroup());
        assertTrue(alert.get().getDisparityScore() > FairnessAuditor.DEMOGRAPHIC_PARITY_THRESHOLD);
    }

    @Test
    void demographicParity_doesNotFlag_whenBidsAreRoughlyEqual() {
        List<AuctionResult> results = new ArrayList<>();
        results.add(result("retail", 2.00, "low"));
        results.add(result("retail", 2.05, "medium"));
        results.add(result("retail", 2.10, "high"));

        assertTrue(auditor.checkDemographicParity("retail", results).isEmpty());
    }

    @Test
    void demographicParity_doesNotFlag_whenOnlyOneIncomeGroupPresent() {
        List<AuctionResult> results = List.of(
            result("retail", 1.00, "low"),
            result("retail", 1.50, "low")
        );

        assertTrue(auditor.checkDemographicParity("retail", results).isEmpty());
    }

    @Test
    void predatoryTargeting_flagsDisproportionateLowIncomeShare() {
        List<AuctionResult> results = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            results.add(result("payday-loan", 2.00, "low"));
        }
        for (int i = 0; i < 5; i++) {
            results.add(result("payday-loan", 2.00, "high"));
        }

        var alert = auditor.checkPredatoryTargeting("payday-loan", results);

        assertTrue(alert.isPresent());
        assertEquals("predatory_targeting", alert.get().getCheckType());
        assertEquals("low", alert.get().getFlaggedGroup());
        assertEquals("payday-loan", alert.get().getAdCategory());
    }

    @Test
    void predatoryTargeting_doesNotFlag_whenDistributionIsBalanced() {
        List<AuctionResult> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(result("payday-loan", 2.00, "low"));
            results.add(result("payday-loan", 2.00, "high"));
        }

        assertTrue(auditor.checkPredatoryTargeting("payday-loan", results).isEmpty());
    }

    @Test
    void evaluate_onlyRunsPredatoryCheck_forPredatoryCategories() {
        List<AuctionResult> results = new ArrayList<>();
        // Heavily skewed toward low income, but "retail" is not a
        // predatory category, so only a parity alert (if any) should fire,
        // never a predatory_targeting alert.
        for (int i = 0; i < 30; i++) {
            results.add(result("retail", 1.00, "low"));
        }
        for (int i = 0; i < 5; i++) {
            results.add(result("retail", 1.00, "high"));
        }

        List<FairnessAlert> alerts = auditor.evaluate(results);

        assertTrue(alerts.stream().noneMatch(a -> a.getCheckType().equals("predatory_targeting")));
    }

    @Test
    void evaluate_flagsPredatoryCategory_endToEnd() {
        List<AuctionResult> results = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            results.add(result("criminal-record-search", 2.00, "low"));
        }
        for (int i = 0; i < 3; i++) {
            results.add(result("criminal-record-search", 2.00, "high"));
        }

        List<FairnessAlert> alerts = auditor.evaluate(results);

        assertTrue(alerts.stream().anyMatch(a ->
            a.getCheckType().equals("predatory_targeting") &&
            a.getAdCategory().equals("criminal-record-search")));
    }
}
package com.yonara.adauction.api;

import com.yonara.adauction.model.FairnessAlert;
import com.yonara.adauction.repository.FairnessAlertRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final JdbcTemplate jdbcTemplate;
    private final FairnessAlertRepository fairnessAlertRepository;

    public AnalyticsController(JdbcTemplate jdbcTemplate,
                                FairnessAlertRepository fairnessAlertRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.fairnessAlertRepository = fairnessAlertRepository;
    }

    @GetMapping("/analytics/top-advertisers")
    public List<Map<String, Object>> getTopAdvertisers() {
        String sql = "SELECT winner_advertiser, COUNT(*) as wins, " +
            "SUM(winning_bid) as total_spend " +
            "FROM auction_results " +
            "GROUP BY winner_advertiser " +
            "ORDER BY wins DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/analytics/slot/{slotName}/stats")
    public Map<String, Object> getSlotStats(@PathVariable String slotName) {
        String sql = "SELECT ad_slot, COUNT(*) as total_auctions, " +
            "AVG(winning_bid) as avg_winning_bid " +
            "FROM auction_results " +
            "WHERE ad_slot = ? " +
            "GROUP BY ad_slot";
        return jdbcTemplate.queryForMap(sql, slotName);
    }

    @GetMapping("/analytics/revenue/hourly")
    public List<Map<String, Object>> getHourlyRevenue() {
        String sql = "SELECT DATE_TRUNC('hour', auction_time) as hour, " +
            "SUM(winning_bid) as revenue " +
            "FROM auction_results " +
            "GROUP BY hour " +
            "ORDER BY hour DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/fairness/report")
    public List<FairnessAlert> getFairnessReport() {
        return fairnessAlertRepository.findAll();
    }

    @GetMapping("/fairness/demographic-parity/{adCategory}")
    public List<Map<String, Object>> getDemographicParityForCategory(@PathVariable String adCategory) {
        String sql = "SELECT winner_income_level, COUNT(*) as wins, " +
            "AVG(winning_bid) as avg_winning_bid " +
            "FROM auction_results " +
            "WHERE ad_category = ? " +
            "GROUP BY winner_income_level " +
            "ORDER BY avg_winning_bid DESC";
        return jdbcTemplate.queryForList(sql, adCategory);
    }

    @GetMapping("/fairness/predatory-ad-distribution")
    public List<Map<String, Object>> getPredatoryAdDistribution() {
        String sql = "SELECT ad_category, winner_income_level, COUNT(*) as wins " +
            "FROM auction_results " +
            "WHERE ad_category IN ('payday-loan', 'criminal-record-search') " +
            "GROUP BY ad_category, winner_income_level " +
            "ORDER BY ad_category, wins DESC";
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/fairness/flagged-advertisers")
    public List<Map<String, Object>> getFlaggedAdvertisers() {
        return fairnessAlertRepository.findFlaggedAdvertisers();
    }
}
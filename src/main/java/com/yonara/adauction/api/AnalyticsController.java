package com.yonara.adauction.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
    public List<Map<String, Object>> getFairnessReport() {
        String sql = "SELECT * FROM fairness_alerts " +
            "ORDER BY alert_time DESC";
        return jdbcTemplate.queryForList(sql);
    }
}
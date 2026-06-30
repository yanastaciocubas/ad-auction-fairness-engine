package com.yonara.adauction.repository;

import com.yonara.adauction.model.FairnessAlert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class FairnessAlertRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<FairnessAlert> ROW_MAPPER = (rs, rowNum) -> {
        FairnessAlert alert = new FairnessAlert();
        alert.setCheckType(rs.getString("check_type"));
        alert.setAdCategory(rs.getString("ad_category"));
        alert.setDisparityScore(rs.getDouble("disparity_score"));
        alert.setFlaggedGroup(rs.getString("flagged_group"));
        return alert;
    };

    public FairnessAlertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(FairnessAlert alert) {
        String sql = "INSERT INTO fairness_alerts " +
            "(check_type, ad_category, disparity_score, flagged_group) " +
            "VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
            alert.getCheckType(),
            alert.getAdCategory(),
            alert.getDisparityScore(),
            alert.getFlaggedGroup());
    }

    public List<FairnessAlert> findAll() {
        String sql = "SELECT * FROM fairness_alerts ORDER BY alert_time DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public List<Map<String, Object>> findFlaggedAdvertisers() {
        // Advertisers whose ads fall in a category that has at least one
        // predatory_targeting alert on record, ranked by how often they
        // won auctions in that flagged category.
        String sql = "SELECT winner_advertiser, ad_category, COUNT(*) AS wins " +
            "FROM auction_results " +
            "WHERE ad_category IN ( " +
            "  SELECT DISTINCT ad_category FROM fairness_alerts " +
            "  WHERE check_type = 'predatory_targeting' " +
            ") " +
            "GROUP BY winner_advertiser, ad_category " +
            "ORDER BY wins DESC";
        return jdbcTemplate.queryForList(sql);
    }
}
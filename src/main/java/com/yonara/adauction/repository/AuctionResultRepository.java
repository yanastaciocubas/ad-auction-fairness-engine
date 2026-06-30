package com.yonara.adauction.repository;

import com.yonara.adauction.model.AuctionResult;
import com.yonara.adauction.model.UserProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuctionResultRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuctionResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(AuctionResult result) {
        String sql = "INSERT INTO auction_results " +
            "(ad_slot, winner_advertiser, ad_category, winning_bid, " +
            "winner_income_level, winner_age_group, winner_gender) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        UserProfile profile = result.getWinningUserProfile();

        jdbcTemplate.update(sql,
            result.getAdSlot(),
            result.getWinnerAdvertiserId(),
            result.getAdCategory(),
            result.getWinningBid(),
            profile != null ? profile.getIncomeLevel() : null,
            profile != null ? profile.getAgeGroup() : null,
            profile != null ? profile.getGender() : null);
    }
}
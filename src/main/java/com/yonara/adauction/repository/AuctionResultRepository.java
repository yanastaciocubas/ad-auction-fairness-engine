package com.yonara.adauction.repository;

import com.yonara.adauction.model.AuctionResult;
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
            "(ad_slot, winner_advertiser, winning_bid) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql,
            result.getAdSlot(),
            result.getWinnerAdvertiserId(),
            result.getWinningBid());
    }
}
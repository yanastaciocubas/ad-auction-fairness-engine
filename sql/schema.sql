CREATE TABLE auction_results (
    id                  SERIAL PRIMARY KEY,
    ad_slot             VARCHAR(100)   NOT NULL,
    winner_advertiser   VARCHAR(100)   NOT NULL,
    ad_category         VARCHAR(100),
    winning_bid         DECIMAL(10,4)  NOT NULL,
    winner_income_level VARCHAR(50),
    winner_age_group    VARCHAR(50),
    winner_gender       VARCHAR(10),
    auction_time        TIMESTAMP      DEFAULT NOW()
);

CREATE TABLE fairness_alerts (
    id               SERIAL PRIMARY KEY,
    check_type       VARCHAR(100)   NOT NULL,
    ad_category      VARCHAR(100),
    disparity_score  DECIMAL(10,4),
    flagged_group    VARCHAR(100),
    alert_time       TIMESTAMP      DEFAULT NOW()
);

CREATE INDEX idx_slot ON auction_results(ad_slot);
CREATE INDEX idx_advertiser ON auction_results(winner_advertiser);
CREATE INDEX idx_time ON auction_results(auction_time);
CREATE INDEX idx_ad_category ON auction_results(ad_category);
CREATE INDEX idx_alert_category ON fairness_alerts(ad_category);
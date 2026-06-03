USE auction_db;

CREATE TABLE auto_bids (
    auto_bid_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    max_amount DECIMAL(12,2) NOT NULL,
    increment_step DECIMAL(12,2) NOT NULL DEFAULT 10000,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id),
    FOREIGN KEY (bidder_id) REFERENCES users(user_id),
    UNIQUE KEY uk_auction_bidder (auction_id, bidder_id),
    CONSTRAINT chk_auto_max CHECK (max_amount > 0),
    CONSTRAINT chk_auto_step CHECK (increment_step > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_auto_bid_auction_max ON auto_bids(auction_id, max_amount DESC);
CREATE INDEX idx_auto_bid_active ON auto_bids(auction_id, is_active);
CREATE INDEX idx_auto_bid_created ON auto_bids(created_at);
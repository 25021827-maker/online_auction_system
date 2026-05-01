USE auction_db;

CREATE TABLE bids (
    bid_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    bid_amount DECIMAL(12,2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id),
    FOREIGN KEY (bidder_id) REFERENCES users(user_id),
    CONSTRAINT chk_bid_amount CHECK (bid_amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bids_auction_time ON bids(auction_id, bid_time);
CREATE INDEX idx_bids_amount_desc ON bids(auction_id, bid_amount DESC);
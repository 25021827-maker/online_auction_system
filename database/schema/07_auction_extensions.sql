USE auction_db;

CREATE TABLE auction_extensions (
    extension_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    old_end_time TIMESTAMP NOT NULL,
    new_end_time TIMESTAMP NOT NULL,
    extended_by_seconds INT NOT NULL,
    triggered_by_bid_id BIGINT,
    extended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id),
    FOREIGN KEY (triggered_by_bid_id) REFERENCES bids(bid_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_extensions_auction ON auction_extensions(auction_id);
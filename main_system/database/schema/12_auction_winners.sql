USE auction_db;

CREATE TABLE IF NOT EXISTS auction_winners (
    winner_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL UNIQUE,
    item_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    winning_bid_id BIGINT NULL,
    final_price DECIMAL(12,2) NOT NULL,
    status ENUM('UNPAID','PAID','CANCELED') NOT NULL DEFAULT 'PAID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id),
    FOREIGN KEY (item_id) REFERENCES items(item_id),
    FOREIGN KEY (seller_id) REFERENCES users(user_id),
    FOREIGN KEY (bidder_id) REFERENCES users(user_id),
    FOREIGN KEY (winning_bid_id) REFERENCES bids(bid_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_winners_bidder ON auction_winners(bidder_id);
CREATE INDEX idx_winners_seller ON auction_winners(seller_id);

USE auction_db;

CREATE TABLE auctions (
                          auction_id BIGINT PRIMARY KEY AUTO_INCREMENT,

                          item_id BIGINT NOT NULL,

                          start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          end_time TIMESTAMP NOT NULL,
                          original_end_time DATETIME NULL,

                          current_price DECIMAL(12,2) NOT NULL,
                          current_leader_id BIGINT NULL,

                          min_bid_step DECIMAL(12,2) NOT NULL DEFAULT 10,

                          anti_sniping_seconds INT NOT NULL DEFAULT 30,
                          extension_count INT NOT NULL DEFAULT 0,
                          max_extension_seconds INT NOT NULL DEFAULT 600,

                          status ENUM('PENDING','OPEN','RUNNING','FINISHED','PAID','CANCELED') NOT NULL DEFAULT 'OPEN',

                          winning_bid_id BIGINT NULL,
                          final_price DECIMAL(12,2) NULL,
                          last_bid_time TIMESTAMP NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                          FOREIGN KEY (item_id) REFERENCES items(item_id),
                          FOREIGN KEY (current_leader_id) REFERENCES users(user_id),

                          CONSTRAINT chk_end_time CHECK (end_time > start_time),
                          CONSTRAINT chk_min_bid_step CHECK (min_bid_step > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_auction_status_end ON auctions(status, end_time);
CREATE INDEX idx_auction_leader ON auctions(current_leader_id);
CREATE INDEX idx_auction_last_bid ON auctions(last_bid_time);
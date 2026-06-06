USE auction_db;

CREATE TABLE items (
                       item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       seller_id BIGINT NOT NULL,
                       name VARCHAR(200) NOT NULL,
                       description TEXT,
                       starting_price DECIMAL(12,2) NOT NULL CHECK (starting_price >= 0),
                       category ENUM('ELECTRONICS','ART','VEHICLE','OTHER') NOT NULL DEFAULT 'OTHER',
                       condition_status VARCHAR(50) DEFAULT 'New',  -- [THÊM MỚI] Lưu tình trạng sản phẩm (VD: Like New, 99%)
                       default_min_bid_step DECIMAL(12,2) DEFAULT 10 CHECK (default_min_bid_step > 0),
                       status ENUM('ACTIVE','SOLD','HIDDEN') DEFAULT 'ACTIVE',
                       approval_status VARCHAR(20) DEFAULT 'APPROVED',
                       image_url VARCHAR(500),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_items_seller ON items(seller_id);
CREATE INDEX idx_items_category ON items(category);

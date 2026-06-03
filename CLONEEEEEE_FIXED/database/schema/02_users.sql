USE auction_db;

CREATE TABLE users (
                       user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       full_name VARCHAR(100),
                       is_bidder BOOLEAN DEFAULT TRUE,
                       is_seller BOOLEAN DEFAULT FALSE,
                       is_admin BOOLEAN DEFAULT FALSE,
                       is_active BOOLEAN DEFAULT TRUE,
                       role VARCHAR(20) DEFAULT 'BIDDER',      -- [THÊM MỚI] Ánh xạ trực tiếp với Frontend
                       balance DECIMAL(12,2) DEFAULT 0.0,      -- [THÊM MỚI] Lưu số dư tài khoản
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Index cho users
CREATE INDEX idx_users_role ON users(is_bidder, is_seller, is_admin);

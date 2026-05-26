USE auction_db;

CREATE TABLE users (
                       user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(100) NULL,
                       full_name VARCHAR(100),
                       role ENUM('BIDDER','SELLER','ADMIN') NOT NULL DEFAULT 'BIDDER',
                       balance DECIMAL(12,2) NOT NULL DEFAULT 0.0 CHECK (balance >= 0),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_role ON users(role);
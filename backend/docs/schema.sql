-- Tạo Database
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- 1. Bảng lưu thông tin người dùng
CREATE TABLE users (
                       id VARCHAR(50) PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL -- 'ADMIN', 'BIDDER', 'SELLER'
);

-- 2. Bảng lưu thông tin phiên đấu giá (Gộp chung thông tin Item cho đơn giản)
CREATE TABLE auctions (
                          id VARCHAR(50) PRIMARY KEY,
                          item_id VARCHAR(50) NOT NULL,
                          seller_id VARCHAR(50) NOT NULL,
                          start_time DATETIME NOT NULL,
                          end_time DATETIME NOT NULL,
                          status VARCHAR(20) NOT NULL, -- 'OPEN', 'RUNNING', 'FINISHED'
                          winner_id VARCHAR(50),
                          final_price DOUBLE,
                          FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- 3. Bảng lưu lịch sử đặt giá (Bid History)
CREATE TABLE bid_history (
                             id VARCHAR(50) PRIMARY KEY,
                             auction_id VARCHAR(50) NOT NULL,
                             bidder_id VARCHAR(50) NOT NULL,
                             amount DOUBLE NOT NULL,
                             timestamp DATETIME NOT NULL,
                             FOREIGN KEY (auction_id) REFERENCES auctions(id),
                             FOREIGN KEY (bidder_id) REFERENCES users(id)
);

-- Tạo một số dữ liệu mẫu (Mock data) để test đăng nhập
INSERT INTO users (id, username, password, role) VALUES
                                                     ('U1', 'admin', 'admin123', 'ADMIN'),
                                                     ('U2', 'seller_nghia', '123456', 'SELLER'),
                                                     ('U3', 'bidder_tuan', '123456', 'BIDDER');
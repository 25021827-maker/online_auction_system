-- ======================================================
-- FILE: 09_triggers.sql
-- Mô tả: Tất cả triggers cho hệ thống đấu giá
-- ======================================================

USE auction_db;

-- ------------------------------------------------------------------
-- 1. Đảm bảo cột extension_count tồn tại (cho phép gia hạn tối đa 3 lần)
-- ------------------------------------------------------------------
SET @dbname = DATABASE();
SET @tablename = 'auctions';
SET @columnname = 'extension_count';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname
                                                                                            AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) = 0,
                                         'ALTER TABLE auctions ADD COLUMN extension_count INT NOT NULL DEFAULT 0',
                                         'SELECT 1'
                                 ));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------------
-- 2. Xoá các trigger cũ (nếu có)
-- ------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_bid_check;
DROP TRIGGER IF EXISTS trg_first_bid;
DROP TRIGGER IF EXISTS trg_update_auction_after_bid;
DROP TRIGGER IF EXISTS trg_finish_auction;
DROP TRIGGER IF EXISTS trg_notify_outbid;
DROP TRIGGER IF EXISTS trg_anti_sniping;

DELIMITER $$

-- ------------------------------------------------------------------
-- 3. Trigger: Kiểm tra bid hợp lệ trước khi insert
-- ------------------------------------------------------------------
CREATE TRIGGER trg_bid_check
    BEFORE INSERT ON bids
    FOR EACH ROW
BEGIN
    DECLARE cur_price DECIMAL(12,2);
    DECLARE auc_status VARCHAR(20);
    DECLARE end_time_ts TIMESTAMP;
    DECLARE step DECIMAL(12,2);

    SELECT current_price, status, end_time, min_bid_step
    INTO cur_price, auc_status, end_time_ts, step
    FROM auctions WHERE auction_id = NEW.auction_id;

    IF auc_status NOT IN ('OPEN','RUNNING') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Auction is not active';
    END IF;
    IF end_time_ts < NOW() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Auction has ended';
    END IF;
    IF NEW.bid_amount < cur_price + step THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bid amount too low';
    END IF;
END$$

-- ------------------------------------------------------------------
-- 4. Trigger: Chuyển trạng thái từ OPEN → RUNNING khi có bid đầu tiên
-- ------------------------------------------------------------------
CREATE TRIGGER trg_first_bid
    AFTER INSERT ON bids
    FOR EACH ROW
BEGIN
    UPDATE auctions
    SET status = 'RUNNING'
    WHERE auction_id = NEW.auction_id AND status = 'OPEN';
END$$

-- ------------------------------------------------------------------
-- 5. Trigger: Cập nhật giá hiện tại và người dẫn đầu sau mỗi bid
-- ------------------------------------------------------------------
CREATE TRIGGER trg_update_auction_after_bid
    AFTER INSERT ON bids
    FOR EACH ROW
BEGIN
    UPDATE auctions
    SET current_price = NEW.bid_amount,
        current_leader_id = NEW.bidder_id,
        last_bid_time = NEW.bid_time
    WHERE auction_id = NEW.auction_id;
END$$

-- ------------------------------------------------------------------
-- 6. Trigger: Xác định người thắng khi phiên đấu giá kết thúc
-- ------------------------------------------------------------------
CREATE TRIGGER trg_finish_auction
    BEFORE UPDATE ON auctions
    FOR EACH ROW
BEGIN
    DECLARE win_id BIGINT DEFAULT NULL;
    DECLARE final_price_val DECIMAL(12,2) DEFAULT NULL;

    IF NEW.status = 'FINISHED' AND OLD.status != 'FINISHED' THEN
        SELECT bid_id, bid_amount INTO win_id, final_price_val
        FROM bids
        WHERE auction_id = NEW.auction_id
        ORDER BY bid_amount DESC, bid_time DESC
        LIMIT 1;
        SET NEW.winning_bid_id = win_id;
        SET NEW.final_price = final_price_val;
    END IF;
END$$

-- ------------------------------------------------------------------
-- 7. Trigger: Thông báo cho người dùng khi bị outbid (chỉ với bid thủ công)
-- ------------------------------------------------------------------
CREATE TRIGGER trg_notify_outbid
    AFTER INSERT ON bids
    FOR EACH ROW
BEGIN
    DECLARE old_leader BIGINT DEFAULT NULL;

    -- CHỈ gửi thông báo nếu lượt bid mới KHÔNG phải auto-bid
    IF NEW.is_auto_bid = FALSE THEN
        SELECT bidder_id INTO old_leader
        FROM bids
        WHERE auction_id = NEW.auction_id
          AND bid_id != NEW.bid_id
        ORDER BY bid_amount DESC, bid_time DESC
        LIMIT 1;

        IF old_leader IS NOT NULL AND old_leader != NEW.bidder_id THEN
            INSERT INTO notifications (user_id, auction_id, message)
            VALUES (old_leader, NEW.auction_id,
                    CONCAT('You have been outbid in auction ', NEW.auction_id));
        END IF;
    END IF;
END$$

-- ------------------------------------------------------------------
-- 8. Trigger: Chống sniping - Gia hạn phiên khi bid vào những giây cuối
--    Giới hạn tối đa 3 lần gia hạn (dùng cột extension_count)
--    ĐÃ SỬA LỖI: so sánh chính xác khi thời gian còn lại = threshold
--    ĐÃ SỬA: biến extend_ec -> extend_sec
-- ------------------------------------------------------------------
CREATE TRIGGER trg_anti_sniping
    AFTER INSERT ON bids
    FOR EACH ROW
BEGIN
    DECLARE extend_sec INT;
    DECLARE current_end TIMESTAMP;
    DECLARE ext_count INT;

    -- Lấy thông tin cần thiết
    SELECT anti_sniping_seconds, end_time, extension_count
    INTO extend_sec, current_end, ext_count
    FROM auctions
    WHERE auction_id = NEW.auction_id;

    -- Điều kiện gia hạn:
    -- 1. Phiên đấu giá chưa kết thúc (current_end > NOW())
    -- 2. Thời điểm kết thúc hiện tại nằm trong khoảng [NOW(), NOW() + extend_sec]
    --    Nghĩa là thời gian còn lại <= extend_sec (bao gồm cả trường hợp bằng chính xác)
    -- 3. Số lần gia hạn chưa vượt quá 3
    IF current_end > NOW()
        AND current_end <= DATE_ADD(NOW(), INTERVAL extend_sec SECOND)
        AND ext_count < 3 THEN

        -- Gia hạn: cộng thêm extend_sec giây vào end_time
        UPDATE auctions
        SET end_time = DATE_ADD(current_end, INTERVAL extend_sec SECOND),
            extension_count = extension_count + 1
        WHERE auction_id = NEW.auction_id;

        -- Ghi log vào bảng auction_extensions (giả sử bảng đã tồn tại)
        INSERT INTO auction_extensions (auction_id, old_end_time, new_end_time,
                                        extended_by_seconds, triggered_by_bid_id)
        VALUES (NEW.auction_id, current_end,
                DATE_ADD(current_end, INTERVAL extend_sec SECOND),
                extend_sec, NEW.bid_id);
    END IF;
END$$

DELIMITER ;
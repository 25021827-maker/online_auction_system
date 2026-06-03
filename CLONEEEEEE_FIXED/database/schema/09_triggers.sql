USE auction_db;

DELIMITER $$

-- 1. Kiểm tra bid hợp lệ
DROP TRIGGER IF EXISTS trg_bid_check$$
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

-- 2. Tổng hợp tất cả logic chạy sau khi có bid mới (AFTER INSERT ON bids)
DROP TRIGGER IF EXISTS trg_first_bid$$
DROP TRIGGER IF EXISTS trg_update_auction_after_bid$$
DROP TRIGGER IF EXISTS trg_notify_outbid$$
DROP TRIGGER IF EXISTS trg_anti_sniping$$
DROP TRIGGER IF EXISTS trg_after_bid_insert$$

CREATE TRIGGER trg_after_bid_insert
AFTER INSERT ON bids
FOR EACH ROW
BEGIN
    DECLARE time_left INT;
    DECLARE extend_sec INT;
    DECLARE old_end TIMESTAMP;
    DECLARE auc_status VARCHAR(20);
    DECLARE old_leader BIGINT DEFAULT NULL;

    -- Lấy thông tin auction hiện tại
    SELECT TIMESTAMPDIFF(SECOND, NOW(), end_time),
           anti_sniping_seconds,
           end_time,
           status
    INTO time_left, extend_sec, old_end, auc_status
    FROM auctions WHERE auction_id = NEW.auction_id;

    -- Lấy người giữ giá cao nhất trước đó để thông báo outbid
    SELECT bidder_id INTO old_leader
    FROM bids
    WHERE auction_id = NEW.auction_id
      AND bid_id != NEW.bid_id
    ORDER BY bid_amount DESC, bid_time DESC
    LIMIT 1;

    -- 1. Cập nhật bảng auctions (Gộp cập nhật giá, trạng thái, và anti-sniping)
    UPDATE auctions
    SET current_price = NEW.bid_amount,
        current_leader_id = NEW.bidder_id,
        last_bid_time = NEW.bid_time,
        status = CASE WHEN auc_status = 'OPEN' THEN 'RUNNING' ELSE status END,
        end_time = CASE WHEN time_left <= extend_sec THEN DATE_ADD(old_end, INTERVAL extend_sec SECOND) ELSE end_time END
    WHERE auction_id = NEW.auction_id;

    -- 2. Thông báo nếu có người bị vượt giá
    IF old_leader IS NOT NULL AND old_leader != NEW.bidder_id THEN
        INSERT INTO notifications (user_id, auction_id, message)
        VALUES (old_leader, NEW.auction_id,
                CONCAT('You have been outbid in auction ', NEW.auction_id));
    END IF;

    -- 3. Ghi log nếu anti-sniping được kích hoạt
    IF time_left <= extend_sec THEN
        INSERT INTO auction_extensions (auction_id, old_end_time, new_end_time,
                                        extended_by_seconds, triggered_by_bid_id)
        VALUES (NEW.auction_id, old_end, DATE_ADD(old_end, INTERVAL extend_sec SECOND),
                extend_sec, NEW.bid_id);
    END IF;
END$$

DELIMITER ;
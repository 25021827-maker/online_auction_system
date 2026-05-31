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

-- 2. Chuyển OPEN → RUNNING khi có bid đầu tiên
DROP TRIGGER IF EXISTS trg_first_bid$$
CREATE TRIGGER trg_first_bid
AFTER INSERT ON bids
FOR EACH ROW
BEGIN
    UPDATE auctions
    SET status = 'RUNNING'
    WHERE auction_id = NEW.auction_id AND status = 'OPEN';
END$$

-- 3. Cập nhật auction sau bid
DROP TRIGGER IF EXISTS trg_update_auction_after_bid$$
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

-- 4. Xác định người thắng khi phiên kết thúc
DROP TRIGGER IF EXISTS trg_finish_auction$$
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

-- 5. Thông báo outbid
DROP TRIGGER IF EXISTS trg_notify_outbid$$
CREATE TRIGGER trg_notify_outbid
AFTER INSERT ON bids
FOR EACH ROW
BEGIN
    DECLARE old_leader BIGINT DEFAULT NULL;

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
END$$

-- 6. Anti-sniping: gia hạn phiên
DROP TRIGGER IF EXISTS trg_anti_sniping$$
CREATE TRIGGER trg_anti_sniping
AFTER INSERT ON bids
FOR EACH ROW
BEGIN
    DECLARE time_left INT;
    DECLARE extend_sec INT;
    DECLARE old_end TIMESTAMP;

    SELECT TIMESTAMPDIFF(SECOND, NOW(), end_time),
           anti_sniping_seconds,
           end_time
    INTO time_left, extend_sec, old_end
    FROM auctions WHERE auction_id = NEW.auction_id;

    IF time_left <= extend_sec THEN
        UPDATE auctions
        SET end_time = DATE_ADD(end_time, INTERVAL extend_sec SECOND)
        WHERE auction_id = NEW.auction_id;

        INSERT INTO auction_extensions (auction_id, old_end_time, new_end_time,
                                        extended_by_seconds, triggered_by_bid_id)
        VALUES (NEW.auction_id, old_end, DATE_ADD(old_end, INTERVAL extend_sec SECOND),
                extend_sec, NEW.bid_id);
    END IF;
END$$

DELIMITER ;
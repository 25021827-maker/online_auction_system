USE auction_db;

DROP TRIGGER IF EXISTS trg_bid_check;
DROP TRIGGER IF EXISTS trg_after_bid_insert;
DROP TRIGGER IF EXISTS trg_first_bid;
DROP TRIGGER IF EXISTS trg_update_auction_after_bid;
DROP TRIGGER IF EXISTS trg_notify_outbid;
DROP TRIGGER IF EXISTS trg_anti_sniping;

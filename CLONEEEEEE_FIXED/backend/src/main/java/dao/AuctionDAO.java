package dao;

import model.Auction;
import model.BidTransaction;
import model.Item;
import model.ItemFactory;
import util.VietnamTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class AuctionDAO {

    public boolean placeBid(Long auctionId, Long bidderId, double bidAmount) throws SQLException {
        return placeBid(auctionId, bidderId, bidAmount, false);
    }

    public boolean placeBid(Long auctionId, Long bidderId, double bidAmount, boolean isAutoBid) throws SQLException {
        String lockAuctionSql = """
                SELECT a.auction_id,
                       a.current_price,
                       a.current_leader_id,
                       a.min_bid_step,
                       a.status,
                       a.start_time,
                       a.end_time,
                       a.original_end_time,
                       a.anti_sniping_seconds,
                       a.extension_count,
                       a.max_extension_seconds,
                       i.seller_id
                FROM auctions a
                JOIN items i ON i.item_id = a.item_id
                WHERE a.auction_id = ?
                FOR UPDATE
                """;

        String lockUserSql = """
                SELECT balance, available_balance
                FROM users
                WHERE user_id = ?
                FOR UPDATE
                """;

        String refundOldLeaderSql = """
                UPDATE users
                SET available_balance = available_balance + ?
                WHERE user_id = ?
                """;

        String holdNewBidderSql = """
                UPDATE users
                SET available_balance = available_balance - ?
                WHERE user_id = ?
                """;

        String insertBidSql = """
                INSERT INTO bids (auction_id, bidder_id, bid_amount, is_auto_bid)
                VALUES (?, ?, ?, ?)
                """;

        String updateAuctionSql = """
                UPDATE auctions
                SET current_price = ?,
                    current_leader_id = ?,
                    last_bid_time = NOW(),
                    status = CASE
                        WHEN status = 'OPEN' THEN 'RUNNING'
                        ELSE status
                    END,
                    end_time = ?,
                    original_end_time = COALESCE(original_end_time, ?),
                    extension_count = extension_count + ?
                WHERE auction_id = ?
                """;

        String insertExtensionSql = """
                INSERT INTO auction_extensions (auction_id, old_end_time, new_end_time, extended_by_seconds, triggered_by_bid_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            double currentPrice;
            double minBidStep;
            Long oldLeaderId = null;
            String status;
            Timestamp startTime;
            Timestamp endTime;
            Timestamp originalEndTime;
            int antiSnipingSeconds;
            int extensionCount;
            int maxExtensionSeconds;
            Long sellerId;

            try (PreparedStatement stmt = conn.prepareStatement(lockAuctionSql)) {
                stmt.setLong(1, auctionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    currentPrice = rs.getDouble("current_price");
                    minBidStep = rs.getDouble("min_bid_step");
                    status = rs.getString("status");
                    startTime = rs.getTimestamp("start_time");
                    endTime = rs.getTimestamp("end_time");
                    originalEndTime = rs.getTimestamp("original_end_time");
                    antiSnipingSeconds = rs.getInt("anti_sniping_seconds");
                    extensionCount = rs.getInt("extension_count");
                    maxExtensionSeconds = rs.getInt("max_extension_seconds");
                    sellerId = rs.getLong("seller_id");

                    if (originalEndTime == null) {
                        originalEndTime = endTime;
                    }

                    long leader = rs.getLong("current_leader_id");
                    if (!rs.wasNull()) {
                        oldLeaderId = leader;
                    }
                }
            }

            Timestamp now = Timestamp.valueOf(VietnamTime.now());

            if (!"OPEN".equals(status) && !"RUNNING".equals(status)) {
                conn.rollback();
                return false;
            }

            if (startTime != null && now.before(startTime)) {
                conn.rollback();
                return false;
            }

            if (endTime != null && !now.before(endTime)) {
                conn.rollback();
                return false;
            }

            if (sellerId != null && sellerId.equals(bidderId)) {
                conn.rollback();
                return false;
            }

            long secondsLeft = Long.MAX_VALUE;
            if (endTime != null) {
                secondsLeft = java.time.Duration.between(
                        VietnamTime.now(),
                        endTime.toLocalDateTime()
                ).getSeconds();
            }

            boolean isLateBid = antiSnipingSeconds > 0
                    && secondsLeft >= 0
                    && secondsLeft <= antiSnipingSeconds;

            double requiredMinStep = minBidStep;

            /*
             * Chống spam kéo dài phiên:
             * Trong 30 giây cuối, bước nhảy tối thiểu = max(minBidStep, 1% currentPrice).
             * Ví dụ currentPrice = 10,000 thì bid tối thiểu phải tăng ít nhất 100.
             */
            if (isLateBid) {
                double lateMinStep = currentPrice * 0.01;
                requiredMinStep = Math.max(minBidStep, lateMinStep);
            }

            double minimumValidBid = currentPrice + requiredMinStep;

            if (bidAmount < minimumValidBid) {
                conn.rollback();
                throw new SQLException("Gia dat phai toi thieu " + minimumValidBid + ".");
            }

            double availableBalance;
            try (PreparedStatement stmt = conn.prepareStatement(lockUserSql)) {
                stmt.setLong(1, bidderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    availableBalance = rs.getDouble("available_balance");
                }
            }

            double amountNeedToHold = oldLeaderId != null && oldLeaderId.equals(bidderId)
                    ? bidAmount - currentPrice
                    : bidAmount;

            if (availableBalance < amountNeedToHold) {
                conn.rollback();
                throw new SQLException("Khong du so du kha dung de dat gia.");
            }

            if (oldLeaderId != null && !oldLeaderId.equals(bidderId)) {
                try (PreparedStatement stmt = conn.prepareStatement(refundOldLeaderSql)) {
                    stmt.setDouble(1, currentPrice);
                    stmt.setLong(2, oldLeaderId);
                    stmt.executeUpdate();
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(holdNewBidderSql)) {
                stmt.setDouble(1, amountNeedToHold);
                stmt.setLong(2, bidderId);
                stmt.executeUpdate();
            }

            long bidId;
            try (PreparedStatement stmt = conn.prepareStatement(insertBidSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, auctionId);
                stmt.setLong(2, bidderId);
                stmt.setDouble(3, bidAmount);
                stmt.setBoolean(4, isAutoBid);
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return false;
                    }
                    bidId = keys.getLong(1);
                }
            }

            Timestamp newEndTime = endTime;
            int extendedBySeconds = 0;

            /*
             * Chỉ kéo dài nếu:
             * 1. Bid nằm trong cửa sổ anti-sniping.
             * 2. Bid làm đổi người dẫn đầu.
             * 3. Tổng thời gian kéo dài chưa vượt maxExtensionSeconds.
             */
            boolean leaderChanged = oldLeaderId == null || !oldLeaderId.equals(bidderId);
            int usedExtensionSeconds = Math.max(0, extensionCount) * antiSnipingSeconds;
            boolean canExtendMore = maxExtensionSeconds > 0 && usedExtensionSeconds < maxExtensionSeconds;

            if (isLateBid && leaderChanged && canExtendMore) {
                int remainingExtensionQuota = maxExtensionSeconds - usedExtensionSeconds;
                extendedBySeconds = Math.min(antiSnipingSeconds, remainingExtensionQuota);

                newEndTime = Timestamp.valueOf(
                        endTime.toLocalDateTime().plusSeconds(extendedBySeconds)
                );
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateAuctionSql)) {
                stmt.setDouble(1, bidAmount);
                stmt.setLong(2, bidderId);
                stmt.setTimestamp(3, newEndTime);
                stmt.setTimestamp(4, originalEndTime);
                stmt.setInt(5, extendedBySeconds > 0 ? 1 : 0);
                stmt.setLong(6, auctionId);
                stmt.executeUpdate();
            }

            if (extendedBySeconds > 0) {
                try (PreparedStatement stmt = conn.prepareStatement(insertExtensionSql)) {
                    stmt.setLong(1, auctionId);
                    stmt.setTimestamp(2, endTime);
                    stmt.setTimestamp(3, newEndTime);
                    stmt.setInt(4, extendedBySeconds);
                    stmt.setLong(5, bidId);
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }


    public int finishExpiredAuctions() {
        String findExpiredSql = """
                SELECT a.auction_id, a.item_id, i.seller_id, a.current_leader_id, a.current_price,
                       (
                           SELECT b.bid_id
                           FROM bids b
                           WHERE b.auction_id = a.auction_id
                             AND b.bidder_id = a.current_leader_id
                           ORDER BY b.bid_amount DESC, b.bid_time DESC, b.bid_id DESC
                           LIMIT 1
                       ) AS winning_bid_id
                FROM auctions a
                JOIN items i ON i.item_id = a.item_id
                WHERE a.status IN ('OPEN','RUNNING')
                  AND a.end_time <= NOW()
                FOR UPDATE
                """;

        Connection conn = null;
        int count = 0;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            List<SettlementAuction> expiredAuctions = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(findExpiredSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expiredAuctions.add(mapSettlementAuction(rs));
                }
            }

            for (SettlementAuction auction : expiredAuctions) {
                settleAuction(conn, auction);
                count++;
            }

            conn.commit();
            return count;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            System.err.println("Loi khi tu dong dong phien het han: " + e.getMessage());
            return 0;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    public boolean settleAuction(Long auctionId) {
        String findAuctionSql = """
                SELECT a.auction_id, a.item_id, i.seller_id, a.current_leader_id, a.current_price,
                       (
                           SELECT b.bid_id
                           FROM bids b
                           WHERE b.auction_id = a.auction_id
                             AND b.bidder_id = a.current_leader_id
                           ORDER BY b.bid_amount DESC, b.bid_time DESC, b.bid_id DESC
                           LIMIT 1
                       ) AS winning_bid_id
                FROM auctions a
                JOIN items i ON i.item_id = a.item_id
                WHERE a.auction_id = ?
                  AND a.status IN ('OPEN','RUNNING')
                FOR UPDATE
                """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            SettlementAuction auction = null;
            try (PreparedStatement stmt = conn.prepareStatement(findAuctionSql)) {
                stmt.setLong(1, auctionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        auction = mapSettlementAuction(rs);
                    }
                }
            }

            if (auction == null) {
                conn.rollback();
                return false;
            }

            settleAuction(conn, auction);
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            System.err.println("Loi settle auction: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    private SettlementAuction mapSettlementAuction(ResultSet rs) throws SQLException {
        SettlementAuction auction = new SettlementAuction();
        auction.auctionId = rs.getLong("auction_id");
        auction.itemId = rs.getLong("item_id");
        auction.sellerId = rs.getLong("seller_id");
        auction.finalPrice = rs.getDouble("current_price");

        long leader = rs.getLong("current_leader_id");
        if (!rs.wasNull()) {
            auction.leaderId = leader;
        }

        long bidId = rs.getLong("winning_bid_id");
        if (!rs.wasNull()) {
            auction.winningBidId = bidId;
        }
        return auction;
    }

    private void settleAuction(Connection conn, SettlementAuction auction) throws SQLException {
        String insertWinnerSql = """
            INSERT INTO auction_winners (auction_id, item_id, seller_id, bidder_id, winning_bid_id, final_price, status)
            VALUES (?, ?, ?, ?, ?, ?, 'PAID')
            ON DUPLICATE KEY UPDATE
                item_id = VALUES(item_id),
                seller_id = VALUES(seller_id),
                bidder_id = VALUES(bidder_id),
                winning_bid_id = VALUES(winning_bid_id),
                final_price = VALUES(final_price),
                status = VALUES(status)
            """;

        String deductWinnerSql = """
            UPDATE users
            SET balance = balance - ?
            WHERE user_id = ? AND balance >= ?
            """;

        String syncWinnerAvailableSql = """
            UPDATE users
            SET available_balance = balance
            WHERE user_id = ?
            """;

        String creditSellerSql = """
            UPDATE users
            SET balance = balance + ?,
                available_balance = available_balance + ?
            WHERE user_id = ?
            """;

        String finishAuctionSql = """
            UPDATE auctions
            SET status = 'FINISHED',
                final_price = ?,
                winning_bid_id = ?
            WHERE auction_id = ?
            """;

        String finishNoWinnerSql = """
            UPDATE auctions
            SET status = 'FINISHED',
                final_price = NULL,
                winning_bid_id = NULL
            WHERE auction_id = ?
            """;

        if (auction.leaderId == null) {
            try (PreparedStatement stmt = conn.prepareStatement(finishNoWinnerSql)) {
                stmt.setLong(1, auction.auctionId);
                stmt.executeUpdate();
            }
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(insertWinnerSql)) {
            stmt.setLong(1, auction.auctionId);
            stmt.setLong(2, auction.itemId);
            stmt.setLong(3, auction.sellerId);
            stmt.setLong(4, auction.leaderId);
            stmt.setObject(5, auction.winningBidId);
            stmt.setDouble(6, auction.finalPrice);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = conn.prepareStatement(deductWinnerSql)) {
            stmt.setDouble(1, auction.finalPrice);
            stmt.setLong(2, auction.leaderId);
            stmt.setDouble(3, auction.finalPrice);

            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Winner balance is insufficient during settlement.");
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(syncWinnerAvailableSql)) {
            stmt.setLong(1, auction.leaderId);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = conn.prepareStatement(creditSellerSql)) {
            stmt.setDouble(1, auction.finalPrice);
            stmt.setDouble(2, auction.finalPrice);
            stmt.setLong(3, auction.sellerId);

            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Seller not found during settlement.");
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(finishAuctionSql)) {
            stmt.setDouble(1, auction.finalPrice);
            stmt.setObject(2, auction.winningBidId);
            stmt.setLong(3, auction.auctionId);
            stmt.executeUpdate();
        }
    }

    private static class SettlementAuction {
        Long auctionId;
        Long itemId;
        Long sellerId;
        Long leaderId;
        Long winningBidId;
        double finalPrice;
    }

    public List<Auction> getActiveAuctions() {
        finishExpiredAuctions();
        List<Auction> activeAuctions = new ArrayList<>();
        // JOIN báº£ng i Ä‘á»ƒ láº¥y seller_id
        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.starting_price, i.category, i.condition_status, i.image_url, " +
                "i.status AS item_status, i.approval_status " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.item_id " +
                "WHERE a.status IN ('OPEN', 'RUNNING') " +
                "AND i.status = 'ACTIVE' " +
                "AND i.approval_status = 'APPROVED' " +
                "ORDER BY a.created_at DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Item item = ItemFactory.createItem(
                        rs.getString("category"),
                        rs.getLong("item_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("starting_price"),
                        null
                );

                hydrateItemMetadata(item, rs);

                Auction auction = new Auction(
                        rs.getLong("auction_id"),
                        item,
                        rs.getLong("seller_id"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime()
                );

                auction.setStatus(parseAuctionStatus(rs.getString("status")));
                auction.setCurrentPrice(rs.getDouble("current_price"));

                long leaderId = rs.getLong("current_leader_id");
                if (!rs.wasNull()) {
                    auction.setHighestBidderId(leaderId);
                }

                activeAuctions.add(auction);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách Auction: " + e.getMessage());
        }

        return activeAuctions;
    }
    // Láº¥y Ä‘á»“ cá»§a chÃ­nh mÃ¬nh Ä‘Äƒng
    public List<Auction> getAuctionsBySeller(Long sellerId) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.starting_price, i.category, i.condition_status, i.image_url, " +
                "i.status AS item_status, i.approval_status " +
                "FROM auctions a JOIN items i ON a.item_id = i.item_id " +
                "WHERE i.seller_id = ? " +
                "ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Logic láº·p y há»‡t hÃ m getActiveAuctions() bÃªn trÃªn
                    Item item = ItemFactory.createItem(rs.getString("category"), rs.getLong("item_id"), rs.getString("name"), rs.getString("description"), rs.getDouble("starting_price"), null);
                    hydrateItemMetadata(item, rs);
                    Auction auction = new Auction(rs.getLong("auction_id"), item, rs.getLong("seller_id"), rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime());
                    auction.setStatus(parseAuctionStatus(rs.getString("status")));
                    auction.setCurrentPrice(rs.getDouble("current_price"));
                    long leaderId = rs.getLong("current_leader_id");
                    if (!rs.wasNull()) auction.setHighestBidderId(leaderId);
                    list.add(auction);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // XÃ³a sáº£n pháº©m (XÃ³a cáº£ auction vÃ  item)
    public boolean deleteAuction(Long auctionId) {
        String sql = "DELETE a, i FROM auctions a JOIN items i ON a.item_id = i.item_id WHERE a.auction_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteAuction(Long auctionId, Long sellerId) {
        String sql = "DELETE a, i FROM auctions a JOIN items i ON a.item_id = i.item_id WHERE a.auction_id = ? AND i.seller_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, sellerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }


    public boolean addToWatchlist(Long userId, Long auctionId) {
        String sql = "INSERT IGNORE INTO watchlist (user_id, auction_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, auctionId);

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Loi them watchlist: " + e.getMessage());
            return false;
        }
    }

    public boolean removeFromWatchlist(Long userId, Long auctionId) {
        String sql = "DELETE FROM watchlist WHERE user_id = ? AND auction_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, auctionId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Loi xoa watchlist: " + e.getMessage());
            return false;
        }
    }

    public List<Long> getWatchlistIds(Long userId) {
        List<Long> ids = new ArrayList<>();

        String sql = "SELECT auction_id FROM watchlist WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("auction_id"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Loi lay watchlist ids: " + e.getMessage());
        }

        return ids;
    }

    private void hydrateItemMetadata(Item item, ResultSet rs) throws SQLException {
        item.setCategory(rs.getString("category"));
        item.setCondition(rs.getString("condition_status"));
        item.setImagePath(rs.getString("image_url"));
        item.setItemStatus(rs.getString("item_status"));
        item.setApprovalStatus(rs.getString("approval_status"));
    }

    private Auction.Status parseAuctionStatus(String status) {
        if (status == null || status.isBlank()) {
            return Auction.Status.OPEN;
        }
        return switch (status.toUpperCase()) {
            case "SOLD" -> Auction.Status.FINISHED;
            case "SCHEDULED" -> Auction.Status.OPEN;
            case "CANCELLED" -> Auction.Status.CANCELED;
            default -> Auction.Status.valueOf(status.toUpperCase());
        };
    }

    public boolean updateAuction(Long auctionId, String itemName, String description, double startingPrice, String category, String condition, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        // Cáº­p nháº­t cáº£ báº£ng items vÃ  auctions
        String updateItemSql = "UPDATE items i JOIN auctions a ON i.item_id = a.item_id SET i.name=?, i.description=?, i.category=?, i.condition_status=?, i.starting_price=?, a.current_price=? WHERE a.auction_id=?";
        String updateAuctionSql = "UPDATE auctions SET start_time=?, end_time=?, original_end_time=?, extension_count=0 WHERE auction_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(updateItemSql);
                 PreparedStatement pstmt2 = conn.prepareStatement(updateAuctionSql)) {

                pstmt1.setString(1, itemName);
                pstmt1.setString(2, description);
                pstmt1.setString(3, category);
                pstmt1.setString(4, condition);
                pstmt1.setDouble(5, startingPrice);
                pstmt1.setDouble(6, startingPrice); // current_price reset vá» starting_price
                pstmt1.setLong(7, auctionId);
                pstmt1.executeUpdate();

                pstmt2.setTimestamp(1, java.sql.Timestamp.valueOf(startTime));
                pstmt2.setTimestamp(2, java.sql.Timestamp.valueOf(endTime));
                pstmt2.setTimestamp(3, java.sql.Timestamp.valueOf(endTime));
                pstmt2.setLong(4, auctionId);
                pstmt2.executeUpdate();

                conn.commit();
                return true;
            } catch (Exception e) { conn.rollback(); return false; }
        } catch (Exception e) { return false; }
    }

    public boolean updateAuction(Long auctionId, Long sellerId, String itemName, String description, double startingPrice, String category, String condition, String imagePath, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        String updateItemSql = "UPDATE items i JOIN auctions a ON i.item_id = a.item_id SET i.name=?, i.description=?, i.category=?, i.condition_status=?, i.image_url=?, i.starting_price=?, a.current_price=? WHERE a.auction_id=? AND i.seller_id=?";
        String updateAuctionSql = "UPDATE auctions a JOIN items i ON a.item_id = i.item_id SET a.start_time=?, a.end_time=?, a.original_end_time=?, a.extension_count=0 WHERE a.auction_id=? AND i.seller_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(updateItemSql);
                 PreparedStatement pstmt2 = conn.prepareStatement(updateAuctionSql)) {

                pstmt1.setString(1, itemName);
                pstmt1.setString(2, description);
                pstmt1.setString(3, category);
                pstmt1.setString(4, condition);
                pstmt1.setString(5, imagePath != null ? imagePath : "");
                pstmt1.setDouble(6, startingPrice);
                pstmt1.setDouble(7, startingPrice);
                pstmt1.setLong(8, auctionId);
                pstmt1.setLong(9, sellerId);
                int updatedItem = pstmt1.executeUpdate();

                pstmt2.setTimestamp(1, java.sql.Timestamp.valueOf(startTime));
                pstmt2.setTimestamp(2, java.sql.Timestamp.valueOf(endTime));
                pstmt2.setTimestamp(3, java.sql.Timestamp.valueOf(endTime));
                pstmt2.setLong(4, auctionId);
                pstmt2.setLong(5, sellerId);
                int updatedAuction = pstmt2.executeUpdate();

                if (updatedItem == 0 || updatedAuction == 0) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                return false;
            }
        } catch (Exception e) { return false; }
    }
    // THÃŠM Láº I HÃ€M NÃ€Y NHÆ¯ Má»˜T Cáº¦U Ná»I Äá»‚ AUCTION_SERVICE KHÃ”NG Bá»Š Lá»–I
    public boolean saveBidTransaction(Long auctionId, model.BidTransaction bid) throws SQLException {
        // Truyá»n ID vÃ  Sá»‘ tiá»n sang hÃ m placeBid
        return placeBid(auctionId, bid.getBidderId(), bid.getAmount(), bid.isAutoBid());
    }

    public boolean createAuction(Long sellerId, String itemName, String description, double startingPrice, String category, String condition, String imagePath, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        // Cáº§n insert vÃ o items trÆ°á»›c, láº¥y ID, rá»“i insert vÃ o auctions
        // 1. CHÃš Ã SQL NÃ€Y: Pháº£i cÃ³ image_url
        String insertItemSql = "INSERT INTO items (seller_id, name, description, starting_price, category, condition_status, image_url, status, approval_status) VALUES (?, ?, ?, ?, ?, ?, ?, 'HIDDEN', 'PENDING')";
        String insertAuctionSql = "INSERT INTO auctions (item_id, start_time, end_time, original_end_time, current_price, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            long generatedItemId = -1;

            // Insert Item
            try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtItem.setLong(1, sellerId);
                pstmtItem.setString(2, itemName);
                pstmtItem.setString(3, description);
                pstmtItem.setDouble(4, startingPrice);
                pstmtItem.setString(5, category);
                pstmtItem.setString(6, condition);
                pstmtItem.setString(7, imagePath != null ? imagePath : ""); // GHI áº¢NH XUá»NG DB

                pstmtItem.executeUpdate();
                try (ResultSet rs = pstmtItem.getGeneratedKeys()) {
                    if (rs.next()) generatedItemId = rs.getLong(1);
                }
            }

            if (generatedItemId != -1) {
                // Insert Auction
                try (PreparedStatement pstmtAuction = conn.prepareStatement(insertAuctionSql)) {
                    pstmtAuction.setLong(1, generatedItemId);
                    pstmtAuction.setTimestamp(2, java.sql.Timestamp.valueOf(startTime));
                    pstmtAuction.setTimestamp(3, java.sql.Timestamp.valueOf(endTime));
                    pstmtAuction.setTimestamp(4, java.sql.Timestamp.valueOf(endTime));
                    pstmtAuction.setDouble(5, startingPrice);
                    pstmtAuction.executeUpdate();
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (Exception ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception ex) {}
        }
    }

    public boolean upsertAutoBid(Long auctionId, Long bidderId, double maxAmount, double incrementStep) {
        String sql = """
                INSERT INTO auto_bids (auction_id, bidder_id, max_amount, increment_step, is_active)
                VALUES (?, ?, ?, ?, TRUE)
                ON DUPLICATE KEY UPDATE
                    max_amount = VALUES(max_amount),
                    increment_step = VALUES(increment_step),
                    is_active = TRUE,
                    created_at = CURRENT_TIMESTAMP
                """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, bidderId);
            pstmt.setDouble(3, maxAmount);
            pstmt.setDouble(4, incrementStep);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi khi luu auto bid: " + e.getMessage());
            return false;
        }
    }

    public void deactivateAutoBid(Long auctionId, Long bidderId) {
        String sql = "UPDATE auto_bids SET is_active = FALSE WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, bidderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Loi khi tat auto bid: " + e.getMessage());
        }
    }

    public AuctionBidState getAuctionBidState(Long auctionId) {
        String sql = """
                SELECT current_price, current_leader_id, min_bid_step, status, start_time, end_time
                FROM auctions
                WHERE auction_id = ?
                """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Long leaderId = rs.getLong("current_leader_id");
                    if (rs.wasNull()) leaderId = null;

                    return new AuctionBidState(
                            rs.getDouble("current_price"),
                            leaderId,
                            rs.getDouble("min_bid_step"),
                            rs.getString("status"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi khi doc trang thai auction: " + e.getMessage());
        }
        return null;
    }

    public List<AutoBidConfig> getActiveAutoBids(Long auctionId) {
        List<AutoBidConfig> configs = new ArrayList<>();

        String sql = """
            SELECT auto_bid_id, auction_id, bidder_id, max_amount, increment_step
            FROM auto_bids
            WHERE auction_id = ? AND is_active = TRUE
            ORDER BY max_amount DESC, created_at ASC, auto_bid_id ASC
            """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql))  {

            stmt.setLong(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    configs.add(new AutoBidConfig(
                            rs.getLong("auto_bid_id"),
                            rs.getLong("auction_id"),
                            rs.getLong("bidder_id"),
                            rs.getDouble("max_amount"),
                            rs.getDouble("increment_step")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return configs;
    }

    public List<BidTransaction> getBidHistory(Long auctionId) {
        List<BidTransaction> history = new ArrayList<>();
        String sql = "SELECT bid_id, bidder_id, bid_amount, is_auto_bid FROM bids WHERE auction_id = ? ORDER BY bid_time ASC, bid_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new BidTransaction(
                            rs.getLong("bid_id"),
                            rs.getLong("bidder_id"),
                            rs.getDouble("bid_amount"),
                            rs.getBoolean("is_auto_bid")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lá»—i khi láº¥y lá»‹ch sá»­ bid: " + e.getMessage());
        }
        return history;
    }

    public static class AuctionBidState {
        public final double currentPrice;
        public final Long currentLeaderId;
        public final double minBidStep;
        public final String status;
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;

        public AuctionBidState(double currentPrice, Long currentLeaderId, double minBidStep, String status, LocalDateTime startTime, LocalDateTime endTime) {
            this.currentPrice = currentPrice;
            this.currentLeaderId = currentLeaderId;
            this.minBidStep = minBidStep;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public boolean canAcceptBids() {
            LocalDateTime now = VietnamTime.now();
            return ("OPEN".equals(status) || "RUNNING".equals(status))
                    && !now.isBefore(startTime)
                    && now.isBefore(endTime);
        }
    }

    public static class AutoBidConfig {
        public final Long id;
        public final Long auctionId;
        public final Long bidderId;
        public final double maxAmount;
        public final double incrementStep;

        public AutoBidConfig(Long id, Long auctionId, Long bidderId, double maxAmount, double incrementStep) {
            this.id = id;
            this.auctionId = auctionId;
            this.bidderId = bidderId;
            this.maxAmount = maxAmount;
            this.incrementStep = incrementStep;
        }
    }
    // Lay danh sach Watchlist cua user
    public List<Auction> getWatchlist(Long userId) {
        List<Auction> list = new ArrayList<>();

        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.starting_price, " +
                "i.category, i.condition_status, i.image_url, " +
                "i.status AS item_status, i.approval_status " +
                "FROM watchlist w " +
                "JOIN auctions a ON a.auction_id = w.auction_id " +
                "JOIN items i ON a.item_id = i.item_id " +
                "WHERE w.user_id = ? " +
                "AND i.approval_status = 'APPROVED' " +
                "ORDER BY w.created_at DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = ItemFactory.createItem(
                            rs.getString("category"),
                            rs.getLong("item_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("starting_price"),
                            null
                    );

                    hydrateItemMetadata(item, rs);

                    Auction auction = new Auction(
                            rs.getLong("auction_id"),
                            item,
                            rs.getLong("seller_id"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime()
                    );

                    auction.setStatus(parseAuctionStatus(rs.getString("status")));
                    auction.setCurrentPrice(rs.getDouble("current_price"));

                    long leaderId = rs.getLong("current_leader_id");
                    if (!rs.wasNull()) {
                        auction.setHighestBidderId(leaderId);
                    }

                    list.add(auction);
                }
            }

        } catch (SQLException e) {
            System.err.println("Loi khi lay watchlist: " + e.getMessage());
        }

        return list;
    }


}



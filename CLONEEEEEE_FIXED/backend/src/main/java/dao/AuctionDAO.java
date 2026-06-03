package dao;

import model.Auction;
import model.BidTransaction;
import model.Item;
import model.ItemFactory;

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

    public boolean createNewAuction(Long sellerId, String itemName, String description, double startingPrice, String category, String condition, String imagePath, LocalDateTime startTime, LocalDateTime endTime) {
        // Lưu thông tin Sản phẩm (Có seller_id)
        // Sửa câu lệnh để nhét imagePath (dấu ? số 7)
        String insertItemSql = "INSERT INTO items (seller_id, name, description, starting_price, category, condition_status, image_url, status, approval_status) VALUES (?, ?, ?, ?, ?, ?, ?, 'HIDDEN', 'PENDING')";

        // LƯU Ý: Bảng auctions KHÔNG có seller_id. Đã xóa khỏi câu lệnh này để tránh Crash Database!
        String insertAuctionSql = "INSERT INTO auctions (item_id, start_time, end_time, current_price, status) VALUES (?, ?, ?, ?, 'OPEN')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            Long newItemId = null;

            try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtItem.setLong(1, sellerId);
                pstmtItem.setString(2, itemName);
                pstmtItem.setString(3, description);
                pstmtItem.setDouble(4, startingPrice);
                pstmtItem.setString(5, category);
                // Trong khối gán giá trị của createNewAuction:
                pstmtItem.setString(6, condition);
                // THÊM DÒNG NÀY (Đừng quên truyền biến imagePath vào tham số hàm)
                pstmtItem.setString(7, imagePath != null ? imagePath : "");
                pstmtItem.executeUpdate();

                try (ResultSet rs = pstmtItem.getGeneratedKeys()) {
                    if (rs.next()) newItemId = rs.getLong(1);
                }
            }

            if (newItemId == null) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement pstmtAuction = conn.prepareStatement(insertAuctionSql)) {
                pstmtAuction.setLong(1, newItemId);
                pstmtAuction.setTimestamp(2, java.sql.Timestamp.valueOf(startTime));
                pstmtAuction.setTimestamp(3, java.sql.Timestamp.valueOf(endTime));
                pstmtAuction.setDouble(4, startingPrice);
                pstmtAuction.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) {}
            }
        }
    } // ĐÃ XÓA DẤU NGOẶC NHỌN THỪA Ở ĐÂY

    public boolean placeBid(Long auctionId, Long bidderId, double bidAmount) throws SQLException {
        return placeBid(auctionId, bidderId, bidAmount, false);
    }

    public boolean placeBid(Long auctionId, Long bidderId, double bidAmount, boolean isAutoBid) throws SQLException {
        String checkSql = "SELECT current_price, status, end_time FROM auctions WHERE auction_id = ? FOR UPDATE";
        String insertBidSql = "INSERT INTO bids (auction_id, bidder_id, bid_amount, is_auto_bid) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Bắt buộc dùng Transaction

            // BƯỚC 1: Lấy giá và chặn lỗi
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setLong(1, auctionId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        double currentPrice = rs.getDouble("current_price");
                        Timestamp endTime = rs.getTimestamp("end_time");

                        if (!"OPEN".equals(status) && !"RUNNING".equals(status)) {
                            conn.rollback();
                            return false;
                        }
                        if (endTime != null && endTime.before(new Timestamp(System.currentTimeMillis()))) {
                            conn.rollback();
                            return false;
                        }
                        if (bidAmount <= currentPrice) {
                            conn.rollback();
                            return false;
                        }
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // BƯỚC 2: Lưu lịch sử đặt giá
            try (PreparedStatement insertStmt = conn.prepareStatement(insertBidSql)) {
                insertStmt.setLong(1, auctionId);
                insertStmt.setLong(2, bidderId);
                insertStmt.setDouble(3, bidAmount);
                insertStmt.setBoolean(4, isAutoBid);
                insertStmt.executeUpdate();
            }

            // BƯỚC 3: (ĐÃ XÓA) - Trigger trg_after_bid_insert trong DB sẽ tự động cập nhật giá, trạng thái, anti-sniping lên bảng auctions

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) {}
            }
            throw e; // 👉 ĐÃ SỬA: Ném thẳng lỗi lên cho AuctionService bắt!
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ex) {}
            }
        }
    }


    public int finishExpiredAuctions() {
        String sql = "UPDATE auctions SET status = 'FINISHED' WHERE status IN ('OPEN','RUNNING') AND end_time <= NOW()";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động đóng phiên hết hạn: " + e.getMessage());
            return 0;
        }
    }

    public List<Auction> getActiveAuctions() {
        finishExpiredAuctions();
        List<Auction> activeAuctions = new ArrayList<>();
        // JOIN bảng i để lấy seller_id
        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.starting_price, i.category, i.condition_status, i.image_url " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.item_id " +
                "WHERE a.status IN ('OPEN', 'RUNNING') " +
                "AND i.status = 'ACTIVE' " +
                "AND i.approval_status = 'APPROVED'";

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

                item.setCondition(rs.getString("condition_status"));
                item.setImagePath(rs.getString("image_url"));

                Auction auction = new Auction(
                        rs.getLong("auction_id"),
                        item,
                        rs.getLong("seller_id"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime()
                );

                auction.setStatus(Auction.Status.valueOf(rs.getString("status")));
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
    // Lấy đồ của chính mình đăng
    public List<Auction> getAuctionsBySeller(Long sellerId) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.starting_price, i.category, i.condition_status, i.image_url " +
                "FROM auctions a JOIN items i ON a.item_id = i.item_id " +
                "WHERE i.seller_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Logic lặp y hệt hàm getActiveAuctions() bên trên
                    Item item = ItemFactory.createItem(rs.getString("category"), rs.getLong("item_id"), rs.getString("name"), rs.getString("description"), rs.getDouble("starting_price"), null);
                    item.setCondition(rs.getString("condition_status"));
                    item.setImagePath(rs.getString("image_url"));
                    Auction auction = new Auction(rs.getLong("auction_id"), item, rs.getLong("seller_id"), rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime());
                    auction.setStatus(Auction.Status.valueOf(rs.getString("status")));
                    auction.setCurrentPrice(rs.getDouble("current_price"));
                    list.add(auction);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // Xóa sản phẩm (Xóa cả auction và item)
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

    // Lấy danh sách Watchlist
    public List<Auction> getWatchlist(Long userId) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, i.seller_id, i.name, i.description, i.starting_price, i.category, i.condition_status, i.image_url " +
                "FROM auctions a JOIN items i ON a.item_id = i.item_id " +
                "JOIN watchlist w ON a.auction_id = w.auction_id " +
                "WHERE w.user_id = ? " +
                "AND i.status = 'ACTIVE' " +
                "AND i.approval_status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = ItemFactory.createItem(rs.getString("category"), rs.getLong("item_id"),
                            rs.getString("name"), rs.getString("description"), rs.getDouble("starting_price"), null);
                    item.setCondition(rs.getString("condition_status"));
                    item.setImagePath(rs.getString("image_url"));
                    Auction auction = new Auction(rs.getLong("auction_id"), item, rs.getLong("seller_id"),
                            rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime());
                    auction.setStatus(Auction.Status.valueOf(rs.getString("status")));
                    auction.setCurrentPrice(rs.getDouble("current_price"));
                    long leaderId = rs.getLong("current_leader_id");
                    if (!rs.wasNull()) auction.setHighestBidderId(leaderId);
                    list.add(auction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy watchlist: " + e.getMessage());
        }
        return list;
    }
    public boolean updateAuction(Long auctionId, String itemName, String description, double startingPrice, String category, String condition, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        // Cập nhật cả bảng items và auctions
        String updateItemSql = "UPDATE items i JOIN auctions a ON i.item_id = a.item_id SET i.name=?, i.description=?, i.category=?, i.condition_status=?, i.starting_price=?, a.current_price=? WHERE a.auction_id=?";
        String updateAuctionSql = "UPDATE auctions SET start_time=?, end_time=? WHERE auction_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(updateItemSql);
                 PreparedStatement pstmt2 = conn.prepareStatement(updateAuctionSql)) {

                pstmt1.setString(1, itemName);
                pstmt1.setString(2, description);
                pstmt1.setString(3, category);
                pstmt1.setString(4, condition);
                pstmt1.setDouble(5, startingPrice);
                pstmt1.setDouble(6, startingPrice); // current_price reset về starting_price
                pstmt1.setLong(7, auctionId);
                pstmt1.executeUpdate();

                pstmt2.setTimestamp(1, java.sql.Timestamp.valueOf(startTime));
                pstmt2.setTimestamp(2, java.sql.Timestamp.valueOf(endTime));
                pstmt2.setLong(3, auctionId);
                pstmt2.executeUpdate();

                conn.commit();
                return true;
            } catch (Exception e) { conn.rollback(); return false; }
        } catch (Exception e) { return false; }
    }

    public boolean updateAuction(Long auctionId, Long sellerId, String itemName, String description, double startingPrice, String category, String condition, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        String updateItemSql = "UPDATE items i JOIN auctions a ON i.item_id = a.item_id SET i.name=?, i.description=?, i.category=?, i.condition_status=?, i.starting_price=?, a.current_price=? WHERE a.auction_id=? AND i.seller_id=?";
        String updateAuctionSql = "UPDATE auctions a JOIN items i ON a.item_id = i.item_id SET a.start_time=?, a.end_time=? WHERE a.auction_id=? AND i.seller_id=?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(updateItemSql);
                 PreparedStatement pstmt2 = conn.prepareStatement(updateAuctionSql)) {

                pstmt1.setString(1, itemName);
                pstmt1.setString(2, description);
                pstmt1.setString(3, category);
                pstmt1.setString(4, condition);
                pstmt1.setDouble(5, startingPrice);
                pstmt1.setDouble(6, startingPrice);
                pstmt1.setLong(7, auctionId);
                pstmt1.setLong(8, sellerId);
                int updatedItem = pstmt1.executeUpdate();

                pstmt2.setTimestamp(1, java.sql.Timestamp.valueOf(startTime));
                pstmt2.setTimestamp(2, java.sql.Timestamp.valueOf(endTime));
                pstmt2.setLong(3, auctionId);
                pstmt2.setLong(4, sellerId);
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
    // THÊM LẠI HÀM NÀY NHƯ MỘT CẦU NỐI ĐỂ AUCTION_SERVICE KHÔNG BỊ LỖI
    public boolean saveBidTransaction(Long auctionId, model.BidTransaction bid) throws SQLException {
        // Truyền ID và Số tiền sang hàm placeBid
        return placeBid(auctionId, bid.getBidderId(), bid.getAmount(), bid.isAutoBid());
    }

    public boolean createAuction(Long sellerId, String itemName, String description, double startingPrice, String category, String condition, String imagePath, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        // Cần insert vào items trước, lấy ID, rồi insert vào auctions
        // 1. CHÚ Ý SQL NÀY: Phải có image_url
        String insertItemSql = "INSERT INTO items (seller_id, name, description, starting_price, category, condition_status, image_url, status, approval_status) VALUES (?, ?, ?, ?, ?, ?, ?, 'HIDDEN', 'PENDING')";
        String insertAuctionSql = "INSERT INTO auctions (item_id, start_time, end_time, current_price, status) VALUES (?, ?, ?, ?, 'OPEN')";

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
                pstmtItem.setString(7, imagePath != null ? imagePath : ""); // GHI ẢNH XUỐNG DB

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
                    pstmtAuction.setDouble(4, startingPrice);
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
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
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
            System.err.println("Loi khi lay danh sach auto bid: " + e.getMessage());
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
            System.err.println("Lỗi khi lấy lịch sử bid: " + e.getMessage());
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
            LocalDateTime now = LocalDateTime.now();
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

}

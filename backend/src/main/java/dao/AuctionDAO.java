package dao;

import entity.Auction;
import entity.Bid;
import exception.InvalidBidException;
import config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDAO {

    private static final Logger LOGGER = Logger.getLogger(AuctionDAO.class.getName());

    // ==================== Các phương thức cơ bản (tự tạo connection) ====================

    public Auction findById(Long auctionId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    public List<Auction> findActiveAuctions() throws SQLException {
        String sql = "SELECT * FROM auctions WHERE status IN ('OPEN','RUNNING') AND end_time > NOW() ORDER BY end_time ASC";
        List<Auction> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean updateStatus(Long auctionId, String newStatus) throws SQLException {
        String sql = "UPDATE auctions SET status = ? WHERE auction_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setLong(2, auctionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public void insertBid(Long auctionId, Long bidderId, BigDecimal amount, boolean isAuto) throws SQLException {
        String sql = "INSERT INTO bids (auction_id, bidder_id, bid_amount, is_auto_bid) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, bidderId);
            pstmt.setBigDecimal(3, amount);
            pstmt.setBoolean(4, isAuto);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new InvalidBidException(e.getMessage());
            }
            throw e;
        }
    }

    public Bid getCurrentHighestBid(Long auctionId) throws SQLException {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_amount DESC, bid_time DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapBid(rs);
            }
            return null;
        }
    }

    /**
     * @deprecated Dùng closeExpiredAuctionsAndReturnIds() thay thế để tránh mất danh sách ID
     */
    @Deprecated
    public int closeExpiredAuctions() throws SQLException {
        String sql = "UPDATE auctions SET status = 'FINISHED' WHERE end_time <= NOW() AND status IN ('OPEN','RUNNING')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate();
        }
    }

    public Long createAuction(Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (item_id, start_time, end_time, current_price, current_leader_id, " +
                "min_bid_step, status, anti_sniping_seconds, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, auction.getItemId());
            pstmt.setTimestamp(2, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setBigDecimal(4, auction.getCurrentPrice());
            pstmt.setObject(5, auction.getCurrentLeaderId(), Types.BIGINT);
            pstmt.setBigDecimal(6, auction.getMinBidStep());
            pstmt.setString(7, auction.getStatus());
            pstmt.setInt(8, auction.getAntiSnipingSeconds());
            pstmt.setTimestamp(9, Timestamp.valueOf(auction.getCreatedAt()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Không thể lấy ID của auction mới");
        }
    }

    public List<Auction> findBySellerId(Long sellerId) throws SQLException {
        String sql = "SELECT a.* FROM auctions a JOIN items i ON a.item_id = i.item_id WHERE i.seller_id = ?";
        List<Auction> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sellerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Auction> filter(String category, Double minPrice, Double maxPrice, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT a.* FROM auctions a JOIN items i ON a.item_id = i.item_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (category != null && !category.isEmpty()) {
            sql.append(" AND i.category = ?");
            params.add(category);
        }
        if (minPrice != null) {
            sql.append(" AND a.current_price >= ?");
            params.add(BigDecimal.valueOf(minPrice));
        }
        if (maxPrice != null) {
            sql.append(" AND a.current_price <= ?");
            params.add(BigDecimal.valueOf(maxPrice));
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }
        List<Auction> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ==================== Các phương thức hỗ trợ transaction (nhận Connection) ====================

    public Auction findByIdForUpdate(Long auctionId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE auction_id = ? FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    public void insertBid(Long auctionId, Long bidderId, BigDecimal amount, boolean isAuto, Connection conn) throws SQLException {
        String sql = "INSERT INTO bids (auction_id, bidder_id, bid_amount, is_auto_bid) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            pstmt.setLong(2, bidderId);
            pstmt.setBigDecimal(3, amount);
            pstmt.setBoolean(4, isAuto);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                throw new InvalidBidException(e.getMessage());
            }
            throw e;
        }
    }

    public Bid getCurrentHighestBid(Long auctionId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_amount DESC, bid_time DESC LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapBid(rs);
            }
            return null;
        }
    }

    public Bid getBidById(Long bidId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM bids WHERE bid_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, bidId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapBid(rs);
            }
            return null;
        }
    }

    /**
     * Đóng các phiên đấu giá hết hạn (status OPEN hoặc RUNNING và end_time <= NOW()).
     * Cách tiếp cận:
     * 1. Lấy danh sách ID cần đóng (không khóa)
     * 2. Duyệt từng ID, khóa riêng lẻ (SELECT FOR UPDATE) và cập nhật.
     * Tránh khóa diện rộng, giảm xung đột với các transaction đặt giá.
     * @return danh sách auction_id đã được chuyển sang FINISHED
     */
    public List<Long> closeExpiredAuctionsAndReturnIds() throws SQLException {
        List<Long> expiredIds = new ArrayList<>();
        // Bước 1: lấy danh sách ID không khóa (chỉ đọc)
        String selectIds = "SELECT auction_id FROM auctions WHERE end_time <= NOW() AND status IN ('OPEN','RUNNING')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectIds);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                expiredIds.add(rs.getLong(1));
            }
        }

        if (expiredIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> finishedIds = new ArrayList<>();
        // Bước 2: cập nhật từng cái một với khóa hàng riêng lẻ
        for (Long id : expiredIds) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);

                // Khóa hàng cụ thể
                String lockSql = "SELECT status FROM auctions WHERE auction_id = ? FOR UPDATE";
                try (PreparedStatement pstmt = conn.prepareStatement(lockSql)) {
                    pstmt.setLong(1, id);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.commit();
                            continue;
                        }
                        String status = rs.getString("status");
                        if (!"OPEN".equals(status) && !"RUNNING".equals(status)) {
                            conn.commit();
                            continue;
                        }
                    }
                }

                // Cập nhật trạng thái
                String updateSql = "UPDATE auctions SET status = 'FINISHED' WHERE auction_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setLong(1, id);
                    int updated = pstmt.executeUpdate();
                    if (updated > 0) {
                        finishedIds.add(id);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                // Log lỗi nhưng không ảnh hưởng các phiên khác
                LOGGER.log(Level.SEVERE, "Lỗi khi đóng auction " + id, e);
            }
        }
        return finishedIds;
    }

    // ==================== Helper methods ====================

    private Auction mapRow(ResultSet rs) throws SQLException {
        Auction a = new Auction();
        a.setAuctionId(rs.getLong("auction_id"));
        a.setItemId(rs.getLong("item_id"));
        a.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        a.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        a.setCurrentPrice(rs.getBigDecimal("current_price"));

        long leaderId = rs.getLong("current_leader_id");
        if (!rs.wasNull()) a.setCurrentLeaderId(leaderId);

        a.setMinBidStep(rs.getBigDecimal("min_bid_step"));
        a.setStatus(rs.getString("status"));
        a.setAntiSnipingSeconds(rs.getInt("anti_sniping_seconds"));

        long winningBidId = rs.getLong("winning_bid_id");
        if (!rs.wasNull()) a.setWinningBidId(winningBidId);

        BigDecimal finalPrice = rs.getBigDecimal("final_price");
        if (finalPrice != null) a.setFinalPrice(finalPrice);

        Timestamp lastBidTime = rs.getTimestamp("last_bid_time");
        if (lastBidTime != null) a.setLastBidTime(lastBidTime.toLocalDateTime());

        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return a;
    }

    private Bid mapBid(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setBidId(rs.getLong("bid_id"));
        bid.setAuctionId(rs.getLong("auction_id"));
        bid.setBidderId(rs.getLong("bidder_id"));
        bid.setBidAmount(rs.getBigDecimal("bid_amount"));
        bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
        bid.setAutoBid(rs.getBoolean("is_auto_bid"));
        return bid;
    }
}
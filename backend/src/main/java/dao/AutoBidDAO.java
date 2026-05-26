package dao;

import config.DatabaseConfig;
import entity.AutoBid;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {

    // Xóa auto_bid theo auction – dùng connection truyền vào
    public void deleteByAuction(Long auctionId, Connection conn) throws SQLException {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            pstmt.executeUpdate();
        }
    }

    // Overload: tự mở connection
    public void deleteByAuction(Long auctionId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            deleteByAuction(auctionId, conn);
        }
    }

    // ==================== CÁC PHƯƠNG THỨC BỔ SUNG ====================

    // Tìm tất cả auto_bid đang active của một phiên đấu giá (không dùng connection)
    public List<AutoBid> findActiveByAuction(Long auctionId) throws SQLException {
        String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND is_active = TRUE ORDER BY max_amount DESC";
        List<AutoBid> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // Tìm tất cả auto_bid đang active của một phiên đấu giá (dùng connection truyền vào)
    public List<AutoBid> findActiveByAuction(Long auctionId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND is_active = TRUE ORDER BY max_amount DESC";
        List<AutoBid> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // Vô hiệu hóa một auto_bid (không dùng connection)
    public void deactivate(Long autoBidId) throws SQLException {
        String sql = "UPDATE auto_bids SET is_active = FALSE WHERE auto_bid_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, autoBidId);
            pstmt.executeUpdate();
        }
    }

    // Vô hiệu hóa một auto_bid (dùng connection truyền vào)
    public void deactivate(Long autoBidId, Connection conn) throws SQLException {
        String sql = "UPDATE auto_bids SET is_active = FALSE WHERE auto_bid_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, autoBidId);
            pstmt.executeUpdate();
        }
    }

    // ==================== PHƯƠNG THỨC TIỆN ÍCH ====================

    // Thêm mới một auto_bid
    public void insert(AutoBid autoBid) throws SQLException {
        String sql = "INSERT INTO auto_bids (auction_id, bidder_id, max_amount, increment_step, is_active) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, autoBid.getAuctionId());
            pstmt.setLong(2, autoBid.getBidderId());
            pstmt.setBigDecimal(3, autoBid.getMaxAmount());
            pstmt.setBigDecimal(4, autoBid.getIncrementStep());
            pstmt.setBoolean(5, autoBid.isActive());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    autoBid.setAutoBidId(generatedKeys.getLong(1));
                }
            }
        }
    }

    // Chuyển đổi ResultSet thành đối tượng AutoBid
    private AutoBid mapRow(ResultSet rs) throws SQLException {
        AutoBid ab = new AutoBid();
        ab.setAutoBidId(rs.getLong("auto_bid_id"));
        ab.setAuctionId(rs.getLong("auction_id"));
        ab.setBidderId(rs.getLong("bidder_id"));
        ab.setMaxAmount(rs.getBigDecimal("max_amount"));
        ab.setIncrementStep(rs.getBigDecimal("increment_step"));
        ab.setActive(rs.getBoolean("is_active"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            ab.setCreatedAt(createdAt.toLocalDateTime());
        }
        return ab;
    }
}
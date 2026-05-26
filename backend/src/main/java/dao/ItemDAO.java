package dao;

import config.DatabaseConfig;
import entity.Item;
import java.sql.*;

public class ItemDAO {

    // Tạo item (phiên bản tự mở connection)
    public Long createItem(Item item) throws SQLException {
        String sql = "INSERT INTO items (seller_id, name, description, starting_price, category, default_min_bid_step, status, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, item.getSellerId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setBigDecimal(4, item.getStartingPrice());
            pstmt.setString(5, item.getCategory());
            pstmt.setBigDecimal(6, item.getDefaultMinBidStep());
            pstmt.setString(7, item.getStatus());
            pstmt.setString(8, item.getImageUrl());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
            throw new SQLException("Tạo item thất bại");
        }
    }

    // Lấy sellerId – dùng connection truyền vào
    public Long getSellerIdByItemId(Long itemId, Connection conn) throws SQLException {
        String sql = "SELECT seller_id FROM items WHERE item_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getLong("seller_id");
            return null;
        }
    }

    // Cập nhật status – dùng connection truyền vào
    public void updateStatus(Long itemId, String status, Connection conn) throws SQLException {
        String sql = "UPDATE items SET status = ? WHERE item_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setLong(2, itemId);
            pstmt.executeUpdate();
        }
    }

    // Lấy tên item – dùng connection truyền vào (cho transaction)
    public String getItemNameById(Long itemId, Connection conn) throws SQLException {
        String sql = "SELECT name FROM items WHERE item_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("name");
            return null;
        }
    }

    // === Overload: phiên bản tự mở connection ===
    public Long getSellerIdByItemId(Long itemId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return getSellerIdByItemId(itemId, conn);
        }
    }

    public void updateStatus(Long itemId, String status) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            updateStatus(itemId, status, conn);
        }
    }

    public String getItemNameById(Long itemId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return getItemNameById(itemId, conn);
        }
    }
}
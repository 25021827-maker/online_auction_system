package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NotificationDAO {

    // Tạo thông báo – dùng connection truyền vào
    public void createNotification(Long userId, Long auctionId, String message, Connection conn) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, auction_id, message) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, auctionId);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        }
    }

    // Overload: tự mở connection
    public void createNotification(Long userId, Long auctionId, String message) throws SQLException {
        try (Connection conn = config.DatabaseConfig.getConnection()) {
            createNotification(userId, auctionId, message, conn);
        }
    }
}
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public void createNotification(
            Connection conn,
            Long userId,
            Long auctionId,
            String type,
            String title,
            String message
    ) throws SQLException {
        String sql = """
                INSERT INTO notifications (user_id, auction_id, type, title, message)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            if (auctionId == null) {
                stmt.setNull(2, Types.BIGINT);
            } else {
                stmt.setLong(2, auctionId);
            }

            stmt.setString(3, type);
            stmt.setString(4, title);
            stmt.setString(5, message);
            stmt.executeUpdate();
        }
    }

    public List<NotificationRecord> getNotifications(Long userId) {
        List<NotificationRecord> notifications = new ArrayList<>();
        String sql = """
                SELECT notification_id, user_id, auction_id, type, title, message, is_read, created_at
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC, notification_id DESC
                """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi lay notifications: " + e.getMessage());
        }

        return notifications;
    }

    public List<NotificationRecord> getUnreadNotifications(Long userId, int limit) {
        List<NotificationRecord> notifications = new ArrayList<>();
        String sql = """
                SELECT notification_id, user_id, auction_id, type, title, message, is_read, created_at
                FROM notifications
                WHERE user_id = ? AND is_read = FALSE
                ORDER BY created_at DESC, notification_id DESC
                LIMIT ?
                """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, Math.max(1, limit));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi lay unread notifications: " + e.getMessage());
        }

        return notifications;
    }

    public int getUnreadCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Loi dem unread notifications: " + e.getMessage());
            return 0;
        }
    }

    public boolean markAsRead(Long userId, Long notificationId) {
        String sql = """
                UPDATE notifications
                SET is_read = TRUE
                WHERE notification_id = ? AND user_id = ?
                """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi mark notification read: " + e.getMessage());
            return false;
        }
    }

    public String resolveAuctionNotificationType(Long userId, Long auctionId, String fallbackType) {
        if (userId == null || auctionId == null) {
            return fallbackType;
        }

        String sql = """
                SELECT i.seller_id,
                       aw.bidder_id AS winner_id,
                       EXISTS (
                           SELECT 1
                           FROM bids b
                           WHERE b.auction_id = a.auction_id
                             AND b.bidder_id = ?
                       ) AS has_bid
                FROM auctions a
                JOIN items i ON i.item_id = a.item_id
                LEFT JOIN auction_winners aw ON aw.auction_id = a.auction_id
                WHERE a.auction_id = ?
                """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return fallbackType;
                }

                long sellerId = rs.getLong("seller_id");
                Long winnerId = rs.getLong("winner_id");
                if (rs.wasNull()) {
                    winnerId = null;
                }
                boolean hasBid = rs.getBoolean("has_bid");

                if (winnerId != null && winnerId.equals(userId)) {
                    return "AUCTION_WON";
                }

                if (sellerId == userId) {
                    return winnerId == null ? "AUCTION_NO_WINNER" : "AUCTION_SOLD";
                }

                if (hasBid) {
                    return "AUCTION_LOST";
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi resolve notification type: " + e.getMessage());
        }

        return fallbackType;
    }

    private NotificationRecord mapNotification(ResultSet rs) throws SQLException {
        Long auctionId = rs.getLong("auction_id");
        if (rs.wasNull()) {
            auctionId = null;
        }

        return new NotificationRecord(
                rs.getLong("notification_id"),
                rs.getLong("user_id"),
                auctionId,
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getBoolean("is_read"),
                rs.getTimestamp("created_at") == null
                        ? null
                        : rs.getTimestamp("created_at").toLocalDateTime().toString()
        );
    }

    public static class NotificationRecord {
        public final Long notificationId;
        public final Long userId;
        public final Long auctionId;
        public final String type;
        public final String title;
        public final String message;
        public final boolean read;
        public final String createdAt;

        public NotificationRecord(
                Long notificationId,
                Long userId,
                Long auctionId,
                String type,
                String title,
                String message,
                boolean read,
                String createdAt
        ) {
            this.notificationId = notificationId;
            this.userId = userId;
            this.auctionId = auctionId;
            this.type = type;
            this.title = title;
            this.message = message;
            this.read = read;
            this.createdAt = createdAt;
        }
    }
}

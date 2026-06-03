package dao;

import dto.AdminAuctionDTO;
import dto.AdminDashboardDTO;
import dto.AdminDepositDTO;
import dto.AdminProductDTO;
import dto.AdminUserDTO;
import dto.DepositRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    public boolean submitDepositRequest(DepositRequest request) {
        String sql = "INSERT INTO deposit_requests (user_id, amount, proof_image_path, status) VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, request.userId);
            pstmt.setDouble(2, request.amount);
            pstmt.setString(3, request.proofImagePath == null ? "" : request.proofImagePath);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi tao yeu cau nap tien: " + e.getMessage());
            return false;
        }
    }

    public AdminDashboardDTO getDashboard() {
        AdminDashboardDTO dto = new AdminDashboardDTO();
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            dto.totalUsers = count(conn, "SELECT COUNT(*) FROM users");
            dto.totalAuctions = count(conn, "SELECT COUNT(*) FROM auctions");
            dto.activeAuctions = count(conn, "SELECT COUNT(*) FROM auctions WHERE status IN ('OPEN','RUNNING')");
            dto.pendingDeposits = count(conn, "SELECT COUNT(*) FROM deposit_requests WHERE status = 'PENDING'");
            dto.pendingProducts = count(conn, "SELECT COUNT(*) FROM items WHERE approval_status = 'PENDING'");
            dto.totalUserBalance = sum(conn, "SELECT COALESCE(SUM(balance), 0) FROM users");
        } catch (SQLException e) {
            System.err.println("Loi dashboard admin: " + e.getMessage());
        }
        return dto;
    }

    public List<AdminUserDTO> getUsers() {
        List<AdminUserDTO> users = new ArrayList<>();
        String sql = """
                SELECT user_id, username, email, role, balance, is_active, is_admin, is_seller, is_bidder, created_at
                FROM users
                ORDER BY created_at DESC, user_id DESC
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                AdminUserDTO dto = new AdminUserDTO();
                dto.userId = rs.getLong("user_id");
                dto.username = rs.getString("username");
                dto.email = rs.getString("email");
                dto.role = rs.getString("role");
                dto.balance = rs.getDouble("balance");
                dto.active = rs.getBoolean("is_active");
                dto.admin = rs.getBoolean("is_admin");
                dto.seller = rs.getBoolean("is_seller");
                dto.bidder = rs.getBoolean("is_bidder");
                dto.createdAt = timestampToString(rs.getTimestamp("created_at"));
                users.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("Loi lay users admin: " + e.getMessage());
        }
        return users;
    }

    public boolean setUserActive(Long userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ? AND username <> 'admin'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, active);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi cap nhat user active: " + e.getMessage());
            return false;
        }
    }

    public List<AdminDepositDTO> getDepositRequests() {
        List<AdminDepositDTO> deposits = new ArrayList<>();
        String sql = """
                SELECT d.request_id, d.user_id, u.username, d.amount, d.proof_image_path,
                       d.status, d.created_at, d.reviewed_at
                FROM deposit_requests d
                JOIN users u ON u.user_id = d.user_id
                ORDER BY FIELD(d.status, 'PENDING', 'APPROVED', 'REJECTED'), d.created_at DESC
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                AdminDepositDTO dto = new AdminDepositDTO();
                dto.requestId = rs.getLong("request_id");
                dto.userId = rs.getLong("user_id");
                dto.username = rs.getString("username");
                dto.amount = rs.getDouble("amount");
                dto.proofImagePath = rs.getString("proof_image_path");
                dto.status = rs.getString("status");
                dto.createdAt = timestampToString(rs.getTimestamp("created_at"));
                dto.reviewedAt = timestampToString(rs.getTimestamp("reviewed_at"));
                deposits.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("Loi lay yeu cau nap tien: " + e.getMessage());
        }
        return deposits;
    }

    public boolean approveDeposit(Long requestId, Long adminId) {
        return reviewDeposit(requestId, adminId, true);
    }

    public boolean rejectDeposit(Long requestId, Long adminId) {
        return reviewDeposit(requestId, adminId, false);
    }

    private boolean reviewDeposit(Long requestId, Long adminId, boolean approve) {
        String selectSql = "SELECT user_id, amount FROM deposit_requests WHERE request_id = ? AND status = 'PENDING' FOR UPDATE";
        String updateUserSql = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
        String updateRequestSql = "UPDATE deposit_requests SET status = ?, reviewed_at = CURRENT_TIMESTAMP, reviewed_by = ? WHERE request_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            Long userId;
            double amount;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setLong(1, requestId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    userId = rs.getLong("user_id");
                    amount = rs.getDouble("amount");
                }
            }

            if (approve) {
                try (PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql)) {
                    updateUserStmt.setDouble(1, amount);
                    updateUserStmt.setLong(2, userId);
                    updateUserStmt.executeUpdate();
                }
            }

            try (PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql)) {
                updateRequestStmt.setString(1, approve ? "APPROVED" : "REJECTED");
                if (adminId == null) {
                    updateRequestStmt.setNull(2, java.sql.Types.BIGINT);
                } else {
                    updateRequestStmt.setLong(2, adminId);
                }
                updateRequestStmt.setLong(3, requestId);
                updateRequestStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("Loi duyet nap tien: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<AdminProductDTO> getProductsForReview() {
        List<AdminProductDTO> products = new ArrayList<>();
        String sql = """
                SELECT a.auction_id, i.item_id, i.seller_id, u.username AS seller_username,
                       i.name, i.category, i.condition_status, i.starting_price,
                       i.status AS item_status, i.approval_status, a.status AS auction_status,
                       a.start_time, a.end_time
                FROM items i
                JOIN users u ON u.user_id = i.seller_id
                JOIN auctions a ON a.item_id = i.item_id
                ORDER BY FIELD(i.approval_status, 'PENDING', 'APPROVED', 'REJECTED'), a.created_at DESC
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                AdminProductDTO dto = new AdminProductDTO();
                dto.auctionId = rs.getLong("auction_id");
                dto.itemId = rs.getLong("item_id");
                dto.sellerId = rs.getLong("seller_id");
                dto.sellerUsername = rs.getString("seller_username");
                dto.itemName = rs.getString("name");
                dto.category = rs.getString("category");
                dto.condition = rs.getString("condition_status");
                dto.startingPrice = rs.getDouble("starting_price");
                dto.itemStatus = rs.getString("item_status");
                dto.approvalStatus = rs.getString("approval_status");
                dto.auctionStatus = rs.getString("auction_status");
                dto.startTime = timestampToString(rs.getTimestamp("start_time"));
                dto.endTime = timestampToString(rs.getTimestamp("end_time"));
                products.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("Loi lay san pham can duyet: " + e.getMessage());
        }
        return products;
    }

    public boolean approveProduct(Long auctionId) {
        String sql = """
                UPDATE items i
                JOIN auctions a ON a.item_id = i.item_id
                SET i.status = 'ACTIVE', i.approval_status = 'APPROVED',
                    a.status = CASE WHEN a.status = 'CANCELED' THEN 'OPEN' ELSE a.status END
                WHERE a.auction_id = ?
                """;
        return executeSingleIdUpdate(sql, auctionId);
    }

    public boolean rejectProduct(Long auctionId) {
        String sql = """
                UPDATE items i
                JOIN auctions a ON a.item_id = i.item_id
                SET i.status = 'HIDDEN', i.approval_status = 'REJECTED', a.status = 'CANCELED'
                WHERE a.auction_id = ?
                """;
        return executeSingleIdUpdate(sql, auctionId);
    }

    public List<AdminAuctionDTO> getAuctions() {
        List<AdminAuctionDTO> auctions = new ArrayList<>();
        String sql = """
                SELECT a.auction_id, i.name, u.username AS seller_username,
                       a.current_price, a.status, a.current_leader_id,
                       COUNT(b.bid_id) AS bid_count, i.approval_status,
                       a.start_time, a.end_time
                FROM auctions a
                JOIN items i ON i.item_id = a.item_id
                JOIN users u ON u.user_id = i.seller_id
                LEFT JOIN bids b ON b.auction_id = a.auction_id
                GROUP BY a.auction_id, i.name, u.username, a.current_price, a.status,
                         a.current_leader_id, i.approval_status, a.start_time, a.end_time
                ORDER BY a.created_at DESC
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                AdminAuctionDTO dto = new AdminAuctionDTO();
                dto.auctionId = rs.getLong("auction_id");
                dto.itemName = rs.getString("name");
                dto.sellerUsername = rs.getString("seller_username");
                dto.currentPrice = rs.getDouble("current_price");
                dto.status = rs.getString("status");
                long leaderId = rs.getLong("current_leader_id");
                dto.leaderId = rs.wasNull() ? null : leaderId;
                dto.bidCount = rs.getInt("bid_count");
                dto.approvalStatus = rs.getString("approval_status");
                dto.startTime = timestampToString(rs.getTimestamp("start_time"));
                dto.endTime = timestampToString(rs.getTimestamp("end_time"));
                auctions.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("Loi lay auctions admin: " + e.getMessage());
        }
        return auctions;
    }

    public boolean updateAuctionStatus(Long auctionId, String status) {
        if (!List.of("OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED").contains(status)) {
            return false;
        }
        return executeSingleIdUpdate("UPDATE auctions SET status = ? WHERE auction_id = ?", status, auctionId);
    }

    private boolean executeSingleIdUpdate(String sql, Long id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi update admin: " + e.getMessage());
            return false;
        }
    }

    private boolean executeSingleIdUpdate(String sql, String value, Long id) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, value);
            pstmt.setLong(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi update admin: " + e.getMessage());
            return false;
        }
    }

    private long count(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private double sum(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private String timestampToString(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString();
    }
}

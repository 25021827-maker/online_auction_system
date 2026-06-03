package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {

    public model.User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("user_id");
                    String userStr = rs.getString("username");
                    String passStr = rs.getString("password");
                    double balance = rs.getDouble("balance");

                    model.User user;
                    if (rs.getBoolean("is_admin")) {
                        user = new model.Admin(id, userStr, passStr);
                    } else if (rs.getBoolean("is_seller")) {
                        user = new model.Seller(id, userStr, passStr);
                    } else {
                        user = new model.Bidder(id, userStr, passStr, balance);
                    }

                    // Nạp đầy đủ thông tin từ DB lên Object
                    user.setBalance(balance);
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));

                    // 👉 ĐÃ THÊM: Bắt buộc phải nạp Role thì Frontend mới biết ai là người bán/người mua
                    user.setRole(rs.getString("role"));

                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi xác thực User: " + e.getMessage());
        }
        return null;
    }

    public Long registerUser(String username, String password, String email, String role) {
        String sql = "INSERT INTO users (username, password, email, is_bidder, is_seller, is_admin, role, balance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0)"; // Thêm balance = 0 làm mặc định

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);

            boolean isBidder = "BIDDER".equalsIgnoreCase(role);
            boolean isSeller = "SELLER".equalsIgnoreCase(role);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

            pstmt.setBoolean(4, isBidder);
            pstmt.setBoolean(5, isSeller);
            pstmt.setBoolean(6, isAdmin);
            pstmt.setString(7, role);

            if (pstmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo User mới: " + e.getMessage());
        }
        return null;
    }

    // Cộng tiền và trả về số dư mới nhất
    public double addBalance(Long userId, double amountToAdd) {
        String updateSql = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
        String selectSql = "SELECT balance FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // 1. Cộng tiền
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amountToAdd);
                updateStmt.setLong(2, userId);
                updateStmt.executeUpdate();
            }
            // 2. Lấy số dư mới
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setLong(1, userId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) return rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Báo lỗi
    }
}

package dao;

import config.DatabaseConfig;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserDAO {

    // ==================== Các phương thức cơ bản (tự tạo connection) ====================

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return BCrypt.checkpw(password, rs.getString("password"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Đăng ký người dùng mới (có email).
     * @return true nếu thành công, false nếu username/email đã tồn tại hoặc lỗi DB
     */
    public boolean registerUser(String username, String password, String email, String role) {
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password, email, role, balance) VALUES (?, ?, ?, ?, 0)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashed);
            pstmt.setString(3, email);
            pstmt.setString(4, role);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Unique constraint violation hoặc lỗi khác
        }
    }

    public UserInfo getUserInfo(String username) {
        String sql = "SELECT user_id, username, role, balance FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UserInfo(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getDouble("balance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===== Thêm mới: Lấy thông tin user theo userId =====
    public UserInfo getUserInfoById(Long userId) throws SQLException {
        String sql = "SELECT user_id, username, role, balance FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UserInfo(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getDouble("balance")
                );
            }
            return null;
        }
    }

    // ===== Thêm mới: Lấy role của user theo userId =====
    public String getUserRole(Long userId) throws SQLException {
        String sql = "SELECT role FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
            return null;
        }
    }

    public boolean updateBalance(Long userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addBalance(Long userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getBalance(Long userId) throws SQLException {
        String sql = "SELECT balance FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
            return 0;
        }
    }

    // ==================== Các phương thức hỗ trợ transaction (nhận Connection) ====================

    /**
     * Lấy số dư trong transaction (dùng chung connection).
     */
    public double getBalance(Long userId, Connection conn) throws SQLException {
        String sql = "SELECT balance FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
            return 0;
        }
    }

    /**
     * Cập nhật số dư trong transaction.
     */
    public void updateBalance(Long userId, double newBalance, Connection conn) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Cộng tiền vào tài khoản trong transaction.
     */
    public void addBalance(Long userId, double amount, Connection conn) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Lấy thông tin user theo username (có thể dùng trong transaction nếu cần).
     */
    public UserInfo getUserInfo(String username, Connection conn) throws SQLException {
        String sql = "SELECT user_id, username, role, balance FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UserInfo(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getDouble("balance")
                );
            }
            return null;
        }
    }

    // ===== Thêm mới: Lấy role trong transaction =====
    public String getUserRole(Long userId, Connection conn) throws SQLException {
        String sql = "SELECT role FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
            return null;
        }
    }

    // ==================== Inner class ====================

    public static class UserInfo {
        public final long id;
        public final String username;
        public final String role;
        public final double balance;

        // Sửa constructor để nhận long id thay vì String
        public UserInfo(long id, String username, String role, double balance) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.balance = balance;
        }
    }
}
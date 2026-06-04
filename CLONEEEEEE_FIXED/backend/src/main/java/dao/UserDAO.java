package dao;

import model.Admin;
import model.Bidder;
import model.Seller;
import model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {

    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Long id = rs.getLong("user_id");
                String storedPassword = rs.getString("password");
                if (!passwordMatches(password, storedPassword)) {
                    return null;
                }
                if (!isBcryptHash(storedPassword)) {
                    upgradePasswordHash(conn, id, password);
                }

                String userStr = rs.getString("username");
                double balance = rs.getDouble("balance");

                User user;
                if (rs.getBoolean("is_admin")) {
                    user = new Admin(id, userStr, "");
                } else if (rs.getBoolean("is_seller")) {
                    user = new Seller(id, userStr, "");
                } else {
                    user = new Bidder(id, userStr, "", balance);
                }

                user.setBalance(balance);
                user.setEmail(rs.getString("email"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(rs.getString("role"));

                return user;
            }
        } catch (SQLException e) {
            System.err.println("Loi khi xac thuc User: " + e.getMessage());
        }
        return null;
    }

    public Long registerUser(String username, String password, String email, String role) {
        String sql = "INSERT INTO users (username, password, email, is_bidder, is_seller, is_admin, role, balance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt(12)));
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
            System.err.println("Loi khi tao User moi: " + e.getMessage());
        }
        return null;
    }

    public double addBalance(Long userId, double amountToAdd) {
        String updateSql = "UPDATE users SET balance = COALESCE(balance, 0) + ? WHERE user_id = ?";
        String selectSql = "SELECT balance FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amountToAdd);
                updateStmt.setLong(2, userId);
                updateStmt.executeUpdate();
            }
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setLong(1, userId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi cong tien user: " + e.getMessage());
        }
        return -1;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            try {
                return BCrypt.checkpw(rawPassword, storedPassword);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return rawPassword.equals(storedPassword);
    }

    private boolean isBcryptHash(String password) {
        return password != null
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private void upgradePasswordHash(Connection conn, Long userId, String rawPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, BCrypt.hashpw(rawPassword, BCrypt.gensalt(12)));
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        }
    }
}
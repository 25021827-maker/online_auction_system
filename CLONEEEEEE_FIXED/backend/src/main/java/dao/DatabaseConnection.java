package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(System.getenv().getOrDefault(
                "AUCTION_DB_URL",
                "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
        ));

        config.setUsername(System.getenv().getOrDefault("AUCTION_DB_USER", "root"));
        config.setPassword(System.getenv().getOrDefault("AUCTION_DB_PASSWORD", "1234"));

        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        this.dataSource = new HikariDataSource(config);

        ensureCompatibleSchema();

        System.out.println("Đã khởi tạo Database Connection Pool thành công!");
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void ensureCompatibleSchema() {
        try (Connection conn = dataSource.getConnection()) {
            addColumnIfMissing(conn, "users", "role", "VARCHAR(20) DEFAULT 'BIDDER'");
            addColumnIfMissing(conn, "users", "balance", "DECIMAL(12,2) DEFAULT 0.0");
            addColumnIfMissing(conn, "users", "is_active", "BOOLEAN DEFAULT TRUE");
            addColumnIfMissing(conn, "items", "condition_status", "VARCHAR(50) DEFAULT 'New'");
            addColumnIfMissing(conn, "items", "image_url", "VARCHAR(500)");
            addColumnIfMissing(conn, "items", "approval_status", "VARCHAR(20) DEFAULT 'APPROVED'");
            addColumnIfMissing(conn, "bids", "is_auto_bid", "BOOLEAN DEFAULT FALSE");
            ensureAutoBidTable(conn);
            ensureDepositRequestTable(conn);
            ensureDefaultAdmin(conn);

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                        UPDATE users
                        SET role = CASE
                            WHEN is_admin = TRUE THEN 'ADMIN'
                            WHEN is_seller = TRUE THEN 'SELLER'
                            ELSE 'BIDDER'
                        END
                        WHERE role IS NULL OR role = ''
                        """);

                stmt.executeUpdate("""
                        UPDATE users
                        SET balance = 0
                        WHERE balance IS NULL
                        """);

                stmt.executeUpdate("""
                        UPDATE items
                        SET condition_status = 'New'
                        WHERE condition_status IS NULL OR condition_status = ''
                        """);

                stmt.executeUpdate("""
                        ALTER TABLE items ALTER COLUMN default_min_bid_step SET DEFAULT 10
                        """);
                        
                stmt.executeUpdate("""
                        ALTER TABLE auctions ALTER COLUMN min_bid_step SET DEFAULT 10
                        """);

                stmt.executeUpdate("""
                        UPDATE items SET default_min_bid_step = 10
                        WHERE default_min_bid_step = 10000
                        """);
                        
                stmt.executeUpdate("""
                        UPDATE auctions SET min_bid_step = 10
                        WHERE min_bid_step = 10000
                        """);
            }

        } catch (SQLException e) {
            System.err.println("Không thể tự cập nhật schema database.");
            System.err.println("Lỗi: " + e.getMessage());
            System.err.println("Hãy chạy SQL migration thủ công trong MySQL Workbench.");
        }
    }

    private void ensureDefaultAdmin(Connection conn) throws SQLException {
        String updateSql = """
                UPDATE users
                SET password = 'admin123',
                    is_bidder = FALSE,
                    is_seller = FALSE,
                    is_admin = TRUE,
                    is_active = TRUE,
                    role = 'ADMIN',
                    balance = COALESCE(balance, 0)
                WHERE username = 'admin'
                """;

        try (Statement stmt = conn.createStatement()) {
            int updated = stmt.executeUpdate(updateSql);
            if (updated > 0) {
                return;
            }
        }

        String insertSql = """
                INSERT INTO users (username, password, email, is_bidder, is_seller, is_admin, is_active, role, balance)
                VALUES (?, ?, ?, FALSE, FALSE, TRUE, TRUE, 'ADMIN', 0)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, "admin");
            pstmt.setString(2, "admin123");
            pstmt.setString(3, "admin@bvbid.local");
            pstmt.executeUpdate();
        }
    }

    private void ensureAutoBidTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS auto_bids (
                        auto_bid_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        auction_id BIGINT NOT NULL,
                        bidder_id BIGINT NOT NULL,
                        max_amount DECIMAL(12,2) NOT NULL,
                        increment_step DECIMAL(12,2) NOT NULL DEFAULT 10,
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (auction_id) REFERENCES auctions(auction_id),
                        FOREIGN KEY (bidder_id) REFERENCES users(user_id),
                        UNIQUE KEY uk_auction_bidder (auction_id, bidder_id),
                        CONSTRAINT chk_auto_max CHECK (max_amount > 0),
                        CONSTRAINT chk_auto_step CHECK (increment_step > 0)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
    }

    private void ensureDepositRequestTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS deposit_requests (
                        request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        amount DECIMAL(12,2) NOT NULL,
                        proof_image_path VARCHAR(500),
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        reviewed_at TIMESTAMP NULL,
                        reviewed_by BIGINT NULL,
                        FOREIGN KEY (user_id) REFERENCES users(user_id),
                        FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
                        CONSTRAINT chk_deposit_amount CHECK (amount > 0)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
    }

    private void addColumnIfMissing(
            Connection conn,
            String tableName,
            String columnName,
            String columnDefinition
    ) throws SQLException {
        String checkSql = """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, tableName);
            checkStmt.setString(2, columnName);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String alterSql = "ALTER TABLE " + tableName
                            + " ADD COLUMN " + columnName + " " + columnDefinition;

                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(alterSql);
                        System.out.println("Đã bổ sung cột thiếu: " + tableName + "." + columnName);
                    }
                }
            }
        }
    }
}

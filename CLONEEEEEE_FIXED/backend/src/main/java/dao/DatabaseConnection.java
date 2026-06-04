package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;
import util.VietnamTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private static final String DEFAULT_JDBC_URL =
            "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(resolveJdbcUrl());
        config.setUsername(System.getenv().getOrDefault("AUCTION_DB_USER", "root"));
        config.setPassword(System.getenv().getOrDefault("AUCTION_DB_PASSWORD", "1234"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setConnectionInitSql("SET time_zone = '" + VietnamTime.MYSQL_OFFSET + "'");

        this.dataSource = new HikariDataSource(config);
        ensureCompatibleSchema();

        System.out.println("Database connection pool initialized.");
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

    private String resolveJdbcUrl() {
        String url = System.getenv().getOrDefault("AUCTION_DB_URL", DEFAULT_JDBC_URL);
        if (url.contains("serverTimezone=UTC")) {
            return url.replace("serverTimezone=UTC", "serverTimezone=Asia/Ho_Chi_Minh");
        }
        if (!url.contains("serverTimezone=")) {
            return url + (url.contains("?") ? "&" : "?") + "serverTimezone=Asia/Ho_Chi_Minh";
        }
        return url;
    }

    private void ensureCompatibleSchema() {
        try (Connection conn = dataSource.getConnection()) {
            addColumnIfMissing(conn, "users", "role", "VARCHAR(20) DEFAULT 'BIDDER'");
            addColumnIfMissing(conn, "users", "balance", "DECIMAL(12,2) NOT NULL DEFAULT 0.0");
            boolean addedAvailableBalance = addColumnIfMissing(
                    conn,
                    "users",
                    "available_balance",
                    "DECIMAL(12,2) NOT NULL DEFAULT 0.0 AFTER balance"
            );
            addColumnIfMissing(conn, "users", "is_active", "BOOLEAN DEFAULT TRUE");
            addColumnIfMissing(conn, "items", "condition_status", "VARCHAR(50) DEFAULT 'New'");
            addColumnIfMissing(conn, "items", "image_url", "VARCHAR(500)");
            addColumnIfMissing(conn, "items", "approval_status", "VARCHAR(20) DEFAULT 'APPROVED'");
            addColumnIfMissing(conn, "bids", "is_auto_bid", "BOOLEAN DEFAULT FALSE");
            addColumnIfMissing(conn, "auctions", "original_end_time", "DATETIME NULL");
            addColumnIfMissing(conn, "auctions", "anti_sniping_seconds", "INT NOT NULL DEFAULT 30");
            addColumnIfMissing(conn, "auctions", "extension_count", "INT NOT NULL DEFAULT 0");
            addColumnIfMissing(conn, "auctions", "max_extension_seconds", "INT NOT NULL DEFAULT 600");

            dropLegacyBidTriggers(conn);
            ensureAuctionStatusEnum(conn);
            ensureAutoBidTable(conn);
            ensureAuctionExtensionTable(conn);
            ensureDepositRequestTable(conn);
            ensureAuctionWinnerTable(conn);
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
                    UPDATE auctions
                    SET original_end_time = end_time
                    WHERE original_end_time IS NULL
                    """);

                stmt.executeUpdate("""
                        UPDATE users
                        SET balance = 0
                        WHERE balance IS NULL
                        """);

                if (addedAvailableBalance) {
                    stmt.executeUpdate("""
                            UPDATE users
                            SET available_balance = COALESCE(balance, 0)
                            """);
                }

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
            System.err.println("Cannot auto-update database schema.");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Run the SQL migrations manually in MySQL Workbench.");
        }
    }

    private void ensureAuctionStatusEnum(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    UPDATE auctions
                    SET status = 'CANCELED'
                    WHERE status = 'CANCELLED'
                    """);

            stmt.executeUpdate("""
                    ALTER TABLE auctions
                    MODIFY status ENUM('PENDING','OPEN','RUNNING','FINISHED','PAID','CANCELED') NOT NULL DEFAULT 'OPEN'
                    """);
        }
    }

    private void dropLegacyBidTriggers(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TRIGGER IF EXISTS trg_bid_check");
            stmt.executeUpdate("DROP TRIGGER IF EXISTS trg_after_bid_insert");
            stmt.executeUpdate("DROP TRIGGER IF EXISTS trg_first_bid");
            stmt.executeUpdate("DROP TRIGGER IF EXISTS trg_update_auction_after_bid");
            stmt.executeUpdate("DROP TRIGGER IF EXISTS trg_notify_outbid");
            stmt.executeUpdate("DROP TRIGGER IF EXISTS trg_anti_sniping");
        }
    }

    private void ensureDefaultAdmin(Connection conn) throws SQLException {
        String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt(12));
        String updateSql = """
                UPDATE users
                SET password = ?,
                    is_bidder = FALSE,
                    is_seller = FALSE,
                    is_admin = TRUE,
                    is_active = TRUE,
                    role = 'ADMIN',
                    balance = COALESCE(balance, 0),
                    available_balance = COALESCE(available_balance, balance, 0)
                WHERE username = 'admin'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, hashedPassword);
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                return;
            }
        }

        String insertSql = """
                INSERT INTO users (username, password, email, is_bidder, is_seller, is_admin, is_active, role, balance, available_balance)
                VALUES (?, ?, ?, FALSE, FALSE, TRUE, TRUE, 'ADMIN', 0, 0)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, "admin");
            pstmt.setString(2, hashedPassword);
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

    private void ensureAuctionExtensionTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auction_extensions (
                    extension_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    auction_id BIGINT NOT NULL,
                    old_end_time DATETIME NOT NULL,
                    new_end_time DATETIME NOT NULL,
                    extended_by_seconds INT NOT NULL,
                    triggered_by_bid_id BIGINT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        }
    }

    private void ensureAuctionWinnerTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS auction_winners (
                        winner_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        auction_id BIGINT NOT NULL UNIQUE,
                        item_id BIGINT NOT NULL,
                        seller_id BIGINT NOT NULL,
                        bidder_id BIGINT NOT NULL,
                        winning_bid_id BIGINT NULL,
                        final_price DECIMAL(12,2) NOT NULL,
                        status ENUM('UNPAID','PAID','CANCELED') NOT NULL DEFAULT 'PAID',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (auction_id) REFERENCES auctions(auction_id),
                        FOREIGN KEY (item_id) REFERENCES items(item_id),
                        FOREIGN KEY (seller_id) REFERENCES users(user_id),
                        FOREIGN KEY (bidder_id) REFERENCES users(user_id),
                        FOREIGN KEY (winning_bid_id) REFERENCES bids(bid_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
    }

    private boolean addColumnIfMissing(
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
                        System.out.println("Added missing column: " + tableName + "." + columnName);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}

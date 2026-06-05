package dao;

import model.WalletTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class WalletTransactionDAO {

    public void insertTransaction(
            Connection conn,
            Long userId,
            Long auctionId,
            String type,
            double amount,
            double balanceBefore,
            double balanceAfter,
            double availableBefore,
            double availableAfter,
            String note
    ) throws SQLException {
        String sql = """
                INSERT INTO wallet_transactions
                (user_id, auction_id, type, amount,
                 balance_before, balance_after,
                 available_before, available_after, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);

            if (auctionId == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, auctionId);
            }

            ps.setString(3, type);
            ps.setDouble(4, amount);
            ps.setDouble(5, balanceBefore);
            ps.setDouble(6, balanceAfter);
            ps.setDouble(7, availableBefore);
            ps.setDouble(8, availableAfter);
            ps.setString(9, note);
            ps.executeUpdate();
            System.out.println("[WalletTransactionDAO] Inserted " + type
                    + " userId=" + userId
                    + " auctionId=" + auctionId
                    + " amount=" + amount
                    + " note=" + note);
        }
    }

    public List<WalletTransaction> getTransactionsByUser(Long userId) throws SQLException {
        List<WalletTransaction> list = new ArrayList<>();
        String sql = """
                SELECT wt.transaction_id, wt.user_id, wt.auction_id, wt.type, wt.amount,
                       wt.balance_before, wt.balance_after,
                       wt.available_before, wt.available_after, wt.note, wt.created_at
                FROM wallet_transactions wt
                WHERE wt.user_id = ?
                  AND wt.type <> 'DEPOSIT_REQUEST'
                  AND (wt.note IS NULL OR wt.note NOT LIKE 'Yeu cau nap tien%')
                ORDER BY wt.created_at DESC, wt.transaction_id DESC
                """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        }

        return list;
    }

    private WalletTransaction mapTransaction(ResultSet rs) throws SQLException {
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionId(rs.getLong("transaction_id"));
        tx.setUserId(rs.getLong("user_id"));

        Long auctionId = rs.getLong("auction_id");
        if (rs.wasNull()) {
            auctionId = null;
        }
        tx.setAuctionId(auctionId);

        tx.setType(rs.getString("type"));
        tx.setAmount(rs.getDouble("amount"));
        tx.setBalanceBefore(rs.getDouble("balance_before"));
        tx.setBalanceAfter(rs.getDouble("balance_after"));
        tx.setAvailableBefore(rs.getDouble("available_before"));
        tx.setAvailableAfter(rs.getDouble("available_after"));
        tx.setNote(rs.getString("note"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            tx.setCreatedAt(createdAt.toLocalDateTime());
        }

        return tx;
    }
}

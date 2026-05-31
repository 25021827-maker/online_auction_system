package dao;

import model.Auction;
import model.BidTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuctionDAO {
    private final Connection conn;

    public AuctionDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }


    public boolean saveAuction(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, seller_id, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setString(3, auction.getSellerId());
            pstmt.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setString(6, auction.getStatus().name());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu Auction: " + e.getMessage());
            return false;
        }
    }


    public boolean saveBidTransaction(String auctionId, BidTransaction bid) {
        String sql = "INSERT INTO bid_history (id, auction_id, bidder_id, amount, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bid.getId());
            pstmt.setString(2, auctionId);
            pstmt.setString(3, bid.getBidderId());
            pstmt.setDouble(4, bid.getAmount());
            pstmt.setTimestamp(5, Timestamp.valueOf(bid.getTimestamp()));

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu BidTransaction: " + e.getMessage());
            return false;
        }
    }


    public boolean updateAuctionWinner(String auctionId, String winnerId, double finalPrice) {
        String sql = "UPDATE auctions SET status = 'FINISHED', winner_id = ?, final_price = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, winnerId);
            pstmt.setDouble(2, finalPrice);
            pstmt.setString(3, auctionId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật kết quả Auction: " + e.getMessage());
            return false;
        }
    }
}
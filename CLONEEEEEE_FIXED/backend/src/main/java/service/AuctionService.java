package service;

import dao.AuctionDAO;
import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.Auction;
import model.BidTransaction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
    private final Auction auction;
    private final AuctionDAO auctionDAO;
    private final ReentrantLock lock = new ReentrantLock(true);

    public AuctionService(Auction auction) {
        this(auction, new AuctionDAO());
    }

    // Constructor phục vụ unit test: truyền null để chạy thuần in-memory, không cần MySQL.
    public AuctionService(Auction auction, AuctionDAO auctionDAO) {
        this.auction = auction;
        this.auctionDAO = auctionDAO;
    }

    public boolean placeBid(BidTransaction newBid) {
        lock.lock();
        try {
            validateBid(newBid);

            if (auctionDAO != null) {
                boolean isSaved = auctionDAO.saveBidTransaction(auction.getId(), newBid);
                if (!isSaved) {
                    throw new InvalidBidException("Giá đấu không hợp lệ hoặc phiên đấu giá đã đóng.");
                }
            }

            // Cập nhật bản sao trong RAM để test và các luồng đang giữ Auction thấy trạng thái mới.
            auction.addBid(newBid);
            if (auction.getStatus() == Auction.Status.OPEN) {
                auction.setStatus(Auction.Status.RUNNING);
            }
            return true;
        } catch (SQLException e) {
            throw new InvalidBidException(cleanSqlMessage(e));
        } finally {
            lock.unlock();
        }
    }

    private void validateBid(BidTransaction bid) {
        if (bid == null || bid.getBidderId() == null) {
            throw new InvalidBidException("Thiếu thông tin người đặt giá.");
        }
        if (bid.getAmount() <= 0) {
            throw new InvalidBidException("Giá đặt phải lớn hơn 0.");
        }
        if (auction.getStatus() == Auction.Status.FINISHED
                || auction.getStatus() == Auction.Status.PAID
                || auction.getStatus() == Auction.Status.CANCELED
                || LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new AuctionClosedException("Phiên đấu giá đã đóng.");
        }
        if (LocalDateTime.now().isBefore(auction.getStartTime())) {
            throw new AuctionClosedException("Phiên đấu giá chưa bắt đầu.");
        }
        if (bid.getAmount() <= auction.getCurrentPrice()) {
            throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại.");
        }
    }

    private String cleanSqlMessage(SQLException e) {
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? "Lỗi CSDL khi đặt giá." : message;
    }
}

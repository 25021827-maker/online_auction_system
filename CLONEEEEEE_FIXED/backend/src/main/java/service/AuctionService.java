package service;

import dao.AuctionDAO;
import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.Auction;
import model.BidTransaction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class AuctionService {
    private final Auction auction;
    private final AuctionDAO auctionDAO;
    private final ReentrantLock lock = new ReentrantLock(true);

    public AuctionService(Auction auction) {
        this(auction, new AuctionDAO());
    }

    public AuctionService(Auction auction, AuctionDAO auctionDAO) {
        this.auction = auction;
        this.auctionDAO = auctionDAO;
    }

    public boolean placeBid(BidTransaction newBid) {
        lock.lock();
        try {
            return placeBidInternal(newBid);
        } finally {
            lock.unlock();
        }
    }

    public <T> T withBidLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    boolean placeBidInternal(BidTransaction newBid) {
        validateBid(newBid);

        try {
            if (auctionDAO != null) {
                boolean isSaved = auctionDAO.saveBidTransaction(auction.getId(), newBid);
                if (!isSaved) {
                    throw new InvalidBidException("Gia dat khong hop le hoac phien dau gia da dong.");
                }
            }
        } catch (SQLException e) {
            throw new InvalidBidException(cleanSqlMessage(e));
        }

        auction.addBid(newBid);
        if (auction.getStatus() == Auction.Status.OPEN) {
            auction.setStatus(Auction.Status.RUNNING);
        }
        return true;
    }

    private void validateBid(BidTransaction bid) {
        if (bid == null || bid.getBidderId() == null) {
            throw new InvalidBidException("Thieu thong tin nguoi dat gia.");
        }
        if (bid.getAmount() <= 0) {
            throw new InvalidBidException("Gia dat phai lon hon 0.");
        }
        if (bid.getBidderId().equals(auction.getSellerId())) {
            throw new InvalidBidException("Nguoi ban khong duoc tu dat gia cho san pham cua minh.");
        }
        if (auction.getStatus() == Auction.Status.FINISHED
                || auction.getStatus() == Auction.Status.PAID
                || auction.getStatus() == Auction.Status.CANCELED
                || LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new AuctionClosedException("Phien dau gia da dong.");
        }
        if (LocalDateTime.now().isBefore(auction.getStartTime())) {
            throw new AuctionClosedException("Phien dau gia chua bat dau.");
        }
        if (bid.getAmount() <= auction.getCurrentPrice()) {
            throw new InvalidBidException("Gia dat phai cao hon gia hien tai.");
        }
    }

    private String cleanSqlMessage(SQLException e) {
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? "Loi CSDL khi dat gia." : message;
    }
}

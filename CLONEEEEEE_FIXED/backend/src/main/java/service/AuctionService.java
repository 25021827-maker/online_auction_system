package service;

import dao.AuctionDAO;
import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.Auction;
import model.BidTransaction;

import java.sql.SQLException;
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

        /*
         * AuctionDAO.saveBidTransaction(...) moi la noi cap nhat current_price,
         * current_leader_id, end_time va anti-sniping trong DB.
         * Object auction trong RAM chi dung de hien thi tam thoi.
         */
        auction.addBid(newBid);
        auction.setCurrentPrice(newBid.getAmount());

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

        /*
         * Chi chan cac trang thai da dong chac chan trong RAM.
         * Khong check now > auction.getEndTime() o day nua.
         *
         * Ly do:
         * - endTime trong RAM co the cu sau khi DB da duoc anti-sniping keo dai.
         * - AuctionDAO.placeBid(...) se lock DB bang SELECT ... FOR UPDATE
         *   va check start_time/end_time that su moi nhat.
         */
        if (auction.getStatus() == Auction.Status.FINISHED
                || auction.getStatus() == Auction.Status.PAID
                || auction.getStatus() == Auction.Status.CANCELED) {
            throw new AuctionClosedException("Phien dau gia da dong.");
        }

        /*
         * Khong check now.isBefore(auction.getStartTime()) o day nua.
         * DB se check start_time moi nhat trong AuctionDAO.placeBid(...).
         */

        /*
         * Check nhanh gia phai cao hon currentPrice trong RAM de chan input qua thap.
         * DB van la nguon chinh xac cuoi cung va se check lai min_bid_step/current_price.
         */
        if (bid.getAmount() <= auction.getCurrentPrice()) {
            throw new InvalidBidException("Gia dat phai cao hon gia hien tai.");
        }
    }

    private String cleanSqlMessage(SQLException e) {
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? "Loi CSDL khi dat gia." : message;
    }
}
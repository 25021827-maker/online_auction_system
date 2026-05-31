package service;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.Auction;
import model.BidTransaction;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
    private final Auction auction;


    private final ReentrantLock lock = new ReentrantLock();

    public AuctionService(Auction auction) {
        this.auction = auction;
    }


    public boolean placeBid(BidTransaction newBid) {
        lock.lock();
        try {

            if (!auction.isRunning()) {
                throw new AuctionClosedException("Thất bại: Phiên đấu giá này đã kết thúc hoặc chưa mở!");
            }


            BidTransaction currentHighest = auction.getCurrentHighestBid();
            if (currentHighest != null && newBid.getAmount() <= currentHighest.getAmount()) {
                throw new InvalidBidException("Thất bại: Giá đặt ($" + newBid.getAmount() +
                        ") phải cao hơn giá cao nhất hiện tại ($" + currentHighest.getAmount() + ")");
            }


            auction.addBid(newBid);
            System.out.println("Thành công: " + newBid.toString());


            applyAntiSniping();

            return true;

        } finally {
            lock.unlock();
        }
    }


    private void applyAntiSniping() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();


        if (now.plusMinutes(5).isAfter(endTime) || now.plusMinutes(5).isEqual(endTime)) {
            auction.setEndTime(endTime.plusMinutes(10));
            System.out.println("[Anti-sniping] Có người đặt giá phút chót! Phiên đấu giá được gia hạn thêm 10 phút.");
        }
    }
}
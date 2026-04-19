package service;

import model.Auction;
import model.Bid;

public class AuctionService {

    public synchronized boolean placeBid(Auction auction, Bid bid) {

        if (!auction.isOpen() || System.currentTimeMillis() > auction.getEndTime()) {
            auction.close();
            return false;
        }

        if (bid.getAmount() <= auction.getCurrentPrice()) {
            return false;
        }

        auction.setCurrentPrice(bid.getAmount());
        auction.setHighestBidderId(bid.getUserId());
        auction.getBidHistory().add(bid);

        return true;
    }

    public void closeAuction(Auction auction) {
        if (System.currentTimeMillis() >= auction.getEndTime()) {
            auction.close();

            System.out.println("=== KẾT QUẢ ===");
            System.out.println("Winner: " + auction.getHighestBidderId());
            System.out.println("Price: " + auction.getCurrentPrice());
        }
    }
}
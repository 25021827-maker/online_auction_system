package service;

import model.BidTransaction;
import java.util.PriorityQueue;

public class AutoBidService {
    private final AuctionService auctionService;

    private final PriorityQueue<AutoBidConfig> autoBidders;

    public AutoBidService(AuctionService auctionService) {
        this.auctionService = auctionService;

        this.autoBidders = new PriorityQueue<>((a, b) -> Double.compare(b.maxLimit, a.maxLimit));
    }


    public void registerAutoBid(String bidderId, double maxLimit, double increment) {
        autoBidders.add(new AutoBidConfig(bidderId, maxLimit, increment));
        System.out.println("Đã đăng ký Auto-bid cho " + bidderId + " (Max: $" + maxLimit + ")");
    }


    public void triggerAutoBidding(double currentHighestPrice) {
        if (autoBidders.isEmpty()) return;


        AutoBidConfig topBidder = autoBidders.peek();


        double nextBidAmount = currentHighestPrice + topBidder.increment;

        if (nextBidAmount <= topBidder.maxLimit) {

            BidTransaction autoBid = new BidTransaction("AUTO-" + System.currentTimeMillis(), topBidder.bidderId, nextBidAmount);

            System.out.println("[Auto-Bidding] Hệ thống đang tự động đặt giá cho " + topBidder.bidderId);
            auctionService.placeBid(autoBid);
        } else {
            System.out.println("[Auto-Bidding] " + topBidder.bidderId + " đã chạm ngưỡng tối đa. Dừng tự động đấu giá.");
            autoBidders.poll();
        }
    }


    private static class AutoBidConfig {
        String bidderId;
        double maxLimit;
        double increment;

        public AutoBidConfig(String bidderId, double maxLimit, double increment) {
            this.bidderId = bidderId;
            this.maxLimit = maxLimit;
            this.increment = increment;
        }
    }
}
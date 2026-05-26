package service;

import network.AuctionSubject;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionScheduler {
    private final AuctionSubject subject;
    private final AuctionService auctionService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger LOGGER = Logger.getLogger(AuctionScheduler.class.getName());

    public AuctionScheduler(AuctionSubject subject, AuctionService auctionService) {
        this.subject = subject;
        this.auctionService = auctionService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Lấy danh sách phiên vừa chuyển sang FINISHED
                List<Long> finishedIds = auctionService.closeExpiredAuctionsAndGetIds();
                for (Long auctionId : finishedIds) {
                    auctionService.processFinishedAuction(auctionId);
                    // Broadcast cho client
                    subject.notifyAllClients("{\"event\":\"AUCTION_ENDED\",\"auctionId\":" + auctionId + "}");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Lỗi khi đóng phiên đấu giá", e);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
package service;

import dao.AuctionDAO;
import dto.AutoBidRequest;
import dto.BidRequest;
import model.Auction;
import model.BidTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private volatile ConcurrentHashMap<Long, AuctionService> activeServices;
    private final AuctionDAO auctionDAO;
    private final AutoBidService autoBidService;

    public AuctionManager() {
        this.auctionDAO = new AuctionDAO();
        this.autoBidService = new AutoBidService(auctionDAO);
        this.activeServices = loadActiveAuctionsFromDB();
    }

    private ConcurrentHashMap<Long, AuctionService> loadActiveAuctionsFromDB() {
        ConcurrentHashMap<Long, AuctionService> services = new ConcurrentHashMap<>();
        List<Auction> activeAuctions = auctionDAO.getActiveAuctions();
        for (Auction auction : activeAuctions) {
            services.put(auction.getId(), new AuctionService(auction));
        }
        System.out.println("Da tai " + activeAuctions.size() + " phien dau gia tu Database len he thong.");
        return services;
    }

    public void reloadActiveAuctions() {
        activeServices = loadActiveAuctionsFromDB();
    }

    public boolean processBid(Long auctionId, BidTransaction newBid) {
        AuctionService targetService = getServiceOrThrow(auctionId);
        return targetService.placeBid(newBid);
    }

    public List<BidRequest> processBidWithAutoBids(Long auctionId, BidTransaction newBid) {
        AuctionService targetService = getServiceOrThrow(auctionId);
        return targetService.withBidLock(() -> {
            boolean ok = targetService.placeBidInternal(newBid);
            return ok ? processAutoBidsLocked(auctionId, targetService) : List.of();
        });
    }

    public void configureAutoBid(AutoBidRequest request) {
        autoBidService.configureAutoBid(
                request.auctionId,
                request.bidderId,
                request.maxAmount,
                request.incrementStep
        );
    }

    public List<BidRequest> processAutoBids(Long auctionId) {
        AuctionService targetService = activeServices.get(auctionId);
        if (targetService == null) {
            return List.of();
        }
        return targetService.withBidLock(() -> {
            return processAutoBidsLocked(auctionId, targetService);
        });
    }

    private List<BidRequest> processAutoBidsLocked(Long auctionId, AuctionService targetService) {
        List<BidRequest> generatedBids = new ArrayList<>();

        /*
         * Mỗi lần có người bid hoặc bật auto bid chỉ cho hệ thống sinh 1 auto bid.
         */
        AutoBidService.AutoBidDecision decision = autoBidService.findNextBid(auctionId);

        if (decision == null) {
            return generatedBids;
        }

        try {
            BidTransaction autoBid =
                    new BidTransaction(null, decision.bidderId, decision.amount, util.VietnamTime.now(), true);

            targetService.placeBidInternal(autoBid);

            BidRequest event = new BidRequest();
            event.auctionId = auctionId;
            event.bidderId = decision.bidderId;
            event.amount = decision.amount;
            event.autoBid = true;
            event.bidTime = autoBid.getTimestamp() == null ? null : autoBid.getTimestamp().toString();

            generatedBids.add(event);

        } catch (exception.InvalidBidException | exception.AuctionClosedException e) {
            autoBidService.deactivateAutoBid(auctionId, decision.bidderId);
        }

        return generatedBids;
    }

    private AuctionService getServiceOrThrow(Long auctionId) {
        AuctionService targetService = activeServices.get(auctionId);
        if (targetService == null) {
            throw new exception.AuctionClosedException("Khong tim thay phien dau gia nay hoac phien da ket thuc.");
        }
        return targetService;
    }

    public List<Auction> getActiveAuctionsList() {
        return auctionDAO.getActiveAuctions();
    }

    public boolean createAuction(Long sellerId, String itemName, String description, double startingPrice, String category, String condition, String imagePath, LocalDateTime startTime, LocalDateTime endTime) {
        boolean isSuccess = auctionDAO.createAuction(sellerId, itemName, description, startingPrice, category, condition, imagePath, startTime, endTime);
        if (isSuccess) {
            reloadActiveAuctions();
        }
        return isSuccess;
    }
}

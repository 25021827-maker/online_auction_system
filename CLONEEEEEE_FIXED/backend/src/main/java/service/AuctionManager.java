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
    private final ConcurrentHashMap<Long, AuctionService> activeServices;
    private final AuctionDAO auctionDAO;
    private final AutoBidService autoBidService;

    public AuctionManager() {
        this.activeServices = new ConcurrentHashMap<>();
        this.auctionDAO = new AuctionDAO();
        this.autoBidService = new AutoBidService(auctionDAO);
        loadActiveAuctionsFromDB();
    }

    private void loadActiveAuctionsFromDB() {
        List<Auction> activeAuctions = auctionDAO.getActiveAuctions();
        for (Auction auction : activeAuctions) {
            activeServices.put(auction.getId(), new AuctionService(auction));
        }
        System.out.println("Da tai " + activeAuctions.size() + " phien dau gia tu Database len he thong.");
    }

    public void reloadActiveAuctions() {
        activeServices.clear();
        loadActiveAuctionsFromDB();
    }

    public boolean processBid(Long auctionId, BidTransaction newBid) {
        AuctionService targetService = getServiceOrThrow(auctionId);
        synchronized (targetService) {
            return targetService.placeBid(newBid);
        }
    }

    public List<BidRequest> processBidWithAutoBids(Long auctionId, BidTransaction newBid) {
        AuctionService targetService = getServiceOrThrow(auctionId);
        synchronized (targetService) {
            boolean ok = targetService.placeBid(newBid);
            return ok ? processAutoBidsLocked(auctionId, targetService) : List.of();
        }
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
        synchronized (targetService) {
            return processAutoBidsLocked(auctionId, targetService);
        }
    }

    private List<BidRequest> processAutoBidsLocked(Long auctionId, AuctionService targetService) {
        List<BidRequest> generatedBids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            AutoBidService.AutoBidDecision decision = autoBidService.findNextBid(auctionId);
            if (decision == null) {
                break;
            }

            try {
                BidTransaction autoBid = new BidTransaction(null, decision.bidderId, decision.amount, true);
                targetService.placeBid(autoBid);

                BidRequest event = new BidRequest();
                event.auctionId = auctionId;
                event.bidderId = decision.bidderId;
                event.amount = decision.amount;
                event.autoBid = true;
                generatedBids.add(event);
            } catch (exception.InvalidBidException | exception.AuctionClosedException e) {
                autoBidService.deactivateAutoBid(auctionId, decision.bidderId);
            }
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

    public boolean createNewAuction(Long sellerId, String itemName, String description, double startingPrice, String category, String condition, String imagePath, LocalDateTime startTime, LocalDateTime endTime) {
        boolean isSuccess = auctionDAO.createNewAuction(sellerId, itemName, description, startingPrice, category, condition, imagePath, startTime, endTime);
        if (isSuccess) {
            reloadActiveAuctions();
        }
        return isSuccess;
    }
}

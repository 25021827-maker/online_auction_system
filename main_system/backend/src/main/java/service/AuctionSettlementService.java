package service;

import dao.AuctionDAO;

import java.util.List;

public class AuctionSettlementService {
    private final AuctionDAO auctionDAO;

    public AuctionSettlementService() {
        this(new AuctionDAO());
    }

    public AuctionSettlementService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    public boolean settleAuction(long auctionId) {
        return auctionDAO.settleAuction(auctionId);
    }

    public AuctionDAO.SettlementResult settleAuctionAndGetResult(long auctionId) {
        return auctionDAO.settleAuctionAndGetResult(auctionId);
    }

    public List<AuctionDAO.SettlementResult> finishExpiredAuctionsAndGetResults() {
        return auctionDAO.finishExpiredAuctionsAndGetResults();
    }
}

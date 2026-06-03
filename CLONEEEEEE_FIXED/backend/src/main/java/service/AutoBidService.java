package service;

import dao.AuctionDAO;
import exception.InvalidBidException;

public class AutoBidService {
    private final AuctionDAO auctionDAO;

    public AutoBidService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    public void configureAutoBid(Long auctionId, Long bidderId, double maxAmount, double incrementStep) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("Thieu thong tin auction hoac nguoi dat auto bid.");
        }
        if (maxAmount <= 0 || incrementStep <= 0) {
            throw new InvalidBidException("Max bid va buoc nhay phai lon hon 0.");
        }

        AuctionDAO.AuctionBidState state = auctionDAO.getAuctionBidState(auctionId);
        if (state == null || !state.canAcceptBids()) {
            throw new InvalidBidException("Phien dau gia khong con nhan bid.");
        }
        if (incrementStep < state.minBidStep) {
            throw new InvalidBidException("Buoc nhay phai toi thieu " + state.minBidStep + ".");
        }
        if (maxAmount < state.currentPrice + incrementStep) {
            throw new InvalidBidException("Max bid phai du lon de dat gia tiep theo.");
        }

        if (!auctionDAO.upsertAutoBid(auctionId, bidderId, maxAmount, incrementStep)) {
            throw new InvalidBidException("Khong the luu cau hinh auto bid.");
        }
    }

    public AutoBidDecision findNextBid(Long auctionId) {
        AuctionDAO.AuctionBidState state = auctionDAO.getAuctionBidState(auctionId);
        if (state == null || !state.canAcceptBids() || state.currentLeaderId == null) {
            return null;
        }

        for (AuctionDAO.AutoBidConfig config : auctionDAO.getActiveAutoBids(auctionId)) {
            if (config.bidderId.equals(state.currentLeaderId)) {
                continue;
            }

            double nextAmount = state.currentPrice + config.incrementStep;
            if (nextAmount <= config.maxAmount) {
                return new AutoBidDecision(config.bidderId, nextAmount);
            }

            auctionDAO.deactivateAutoBid(config.auctionId, config.bidderId);
        }

        return null;
    }

    public void deactivateAutoBid(Long auctionId, Long bidderId) {
        auctionDAO.deactivateAutoBid(auctionId, bidderId);
    }

    public static class AutoBidDecision {
        public final Long bidderId;
        public final double amount;

        public AutoBidDecision(Long bidderId, double amount) {
            this.bidderId = bidderId;
            this.amount = amount;
        }
    }
}

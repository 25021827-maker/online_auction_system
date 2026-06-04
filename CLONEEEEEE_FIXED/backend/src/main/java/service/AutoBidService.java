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

        if (maxAmount <= 0) {
            throw new InvalidBidException("Max bid phai lon hon 0.");
        }

        if (incrementStep <= 0) {
            throw new InvalidBidException("Buoc nhay phai lon hon 0.");
        }

        AuctionDAO.AuctionBidState state = auctionDAO.getAuctionBidState(auctionId);

        if (state == null || !state.canAcceptBids()) {
            throw new InvalidBidException("Phien dau gia khong con nhan bid.");
        }

        if (incrementStep < state.minBidStep) {
            throw new InvalidBidException("Buoc nhay phai toi thieu " + state.minBidStep + ".");
        }

        double minimumRequiredMax = state.currentPrice + state.minBidStep;

        /*
         * Nếu chính người đang dẫn đầu set/chỉnh auto bid,
         * cho phép max bằng giá hiện tại.
         */
        if (bidderId.equals(state.currentLeaderId)) {
            minimumRequiredMax = state.currentPrice;
        }

        if (maxAmount < minimumRequiredMax) {
            throw new InvalidBidException("Max bid phai lon hon hoac bang " + minimumRequiredMax + ".");
        }

        if (!auctionDAO.upsertAutoBid(auctionId, bidderId, maxAmount, incrementStep)) {
            throw new InvalidBidException("Khong the luu cau hinh auto bid.");
        }
    }

    public AutoBidDecision findNextBid(Long auctionId) {
        AuctionDAO.AuctionBidState state = auctionDAO.getAuctionBidState(auctionId);

        if (state == null || !state.canAcceptBids()) {
            return null;
        }

        java.util.List<AuctionDAO.AutoBidConfig> configs =
                auctionDAO.getActiveAutoBids(auctionId);

        if (configs == null || configs.isEmpty()) {
            return null;
        }

        /*
         * 1. Max cao nhất đứng đầu.
         * 2. Nếu cùng max, người đặt auto bid trước đứng đầu.
         */
        AuctionDAO.AutoBidConfig best = configs.get(0);

        AuctionDAO.AutoBidConfig strongestOpponent = null;

        for (AuctionDAO.AutoBidConfig config : configs) {
            if (!config.bidderId.equals(best.bidderId)) {
                strongestOpponent = config;
                break;
            }
        }

        double minimumValidBid = state.currentPrice + state.minBidStep;
        double targetAmount;

        /*
         * CASE 1:
         * Best auto bidder đang là người dẫn đầu.
         *
         * Ví dụ:
         * A max 9000 đang dẫn ở 1010.
         * B vừa set auto max 7000.
         *
         * Kết quả đúng:
         * A tự nâng lên 7000 để thắng B.
         */
        if (best.bidderId.equals(state.currentLeaderId)) {
            if (strongestOpponent == null) {
                return null;
            }

            targetAmount = Math.min(best.maxAmount, strongestOpponent.maxAmount);

            if (targetAmount < minimumValidBid) {
                targetAmount = minimumValidBid;
            }

            targetAmount = normalizeMoney(targetAmount);

            if (targetAmount <= state.currentPrice) {
                return null;
            }

            if (targetAmount > best.maxAmount) {
                return null;
            }

            return new AutoBidDecision(best.bidderId, targetAmount);
        }

        /*
         * CASE 2:
         * Best auto bidder chưa phải người dẫn đầu.
         *
         * Nếu chỉ có 1 auto bid:
         * - Nó chỉ cần vượt giá hiện tại tối thiểu.
         *
         * Nếu có 2 auto bid:
         * - Giá nhảy thẳng lên max nhỏ hơn.
         *
         * Ví dụ:
         * A max 9000
         * B max 7000
         * => A bid 7000.
         *
         * Nếu:
         * A max 9000 đặt trước
         * B max 9000 đặt sau
         * => A bid 9000.
         */
        if (strongestOpponent == null) {
            targetAmount = minimumValidBid;
        } else {
            targetAmount = Math.min(best.maxAmount, strongestOpponent.maxAmount);
        }

        if (targetAmount < minimumValidBid) {
            targetAmount = minimumValidBid;
        }

        targetAmount = normalizeMoney(targetAmount);

        if (targetAmount <= state.currentPrice) {
            return null;
        }

        if (targetAmount > best.maxAmount) {
            auctionDAO.deactivateAutoBid(best.auctionId, best.bidderId);
            return null;
        }

        return new AutoBidDecision(best.bidderId, targetAmount);
    }

    private double normalizeMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
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
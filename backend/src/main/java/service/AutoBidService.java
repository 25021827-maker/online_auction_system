package service;

import dao.AutoBidDAO;
import dto.BidResult;
import entity.AutoBid;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoBidService {
    private static final Logger LOGGER = Logger.getLogger(AutoBidService.class.getName());
    private final AuctionService auctionService;
    private final AutoBidDAO autoBidDAO = new AutoBidDAO();

    public AutoBidService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void triggerAutoBid(Long auctionId, BigDecimal currentPrice) {
        try {
            List<AutoBid> autoBids = autoBidDAO.findActiveByAuction(auctionId);
            if (autoBids.isEmpty()) return;

            // Lấy người có max_amount cao nhất
            AutoBid top = autoBids.get(0);
            BigDecimal nextBid = currentPrice.add(top.getIncrementStep());
            if (nextBid.compareTo(top.getMaxAmount()) <= 0) {
                // Đặt giá tự động
                BidResult result = auctionService.placeBid(auctionId, top.getBidderId(), nextBid, true);
                if (!result.isSuccess()) {
                    // Nếu đặt giá thất bại (ví dụ số dư không đủ), vô hiệu hóa auto-bid này
                    LOGGER.log(Level.WARNING, "Auto-bid {0} failed: {1}",
                            new Object[]{top.getAutoBidId(), result.getMessage()});
                    autoBidDAO.deactivate(top.getAutoBidId());
                }
            } else {
                // Hết khả năng (giá vượt quá max_amount), vô hiệu auto-bid
                autoBidDAO.deactivate(top.getAutoBidId());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error in triggerAutoBid for auction " + auctionId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in triggerAutoBid", e);
        }
    }
}
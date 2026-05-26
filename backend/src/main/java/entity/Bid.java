package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid {
    private Long bidId;
    private Long auctionId;
    private Long bidderId;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;
    private boolean isAutoBid;
    // getters/setters
}
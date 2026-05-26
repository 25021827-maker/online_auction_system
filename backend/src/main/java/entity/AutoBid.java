package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBid {
    private Long autoBidId;
    private Long auctionId;
    private Long bidderId;
    private BigDecimal maxAmount;
    private BigDecimal incrementStep;
    private boolean isActive;
    private LocalDateTime createdAt;
    // getters/setters
}
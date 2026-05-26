package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Auction {
    private Long auctionId;
    private Long itemId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal currentPrice;
    private Long currentLeaderId;
    private BigDecimal minBidStep;
    private String status; // OPEN, RUNNING, FINISHED, PAID, CANCELED
    private Integer antiSnipingSeconds;
    private Long winningBidId;
    private BigDecimal finalPrice;
    private LocalDateTime lastBidTime;
    private LocalDateTime createdAt;

    // getters/setters
}
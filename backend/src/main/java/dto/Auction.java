package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Auction {
    private Long auctionId;
    private Long itemId;
    private BigDecimal currentPrice;
    private Long currentLeaderId;
    private String status;
    private LocalDateTime endTime;

    // Getters and setters
    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public Long getCurrentLeaderId() { return currentLeaderId; }
    public void setCurrentLeaderId(Long currentLeaderId) { this.currentLeaderId = currentLeaderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionDTO {
    private Long auctionId;
    private Long itemId;
    private String itemName;          // thêm tên sản phẩm để hiển thị
    private BigDecimal currentPrice;
    private Long currentLeaderId;
    private String status;
    private LocalDateTime endTime;
    private BigDecimal minBidStep;

    public AuctionDTO() {}

    // Getter & Setter
    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public Long getCurrentLeaderId() { return currentLeaderId; }
    public void setCurrentLeaderId(Long currentLeaderId) { this.currentLeaderId = currentLeaderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public BigDecimal getMinBidStep() { return minBidStep; }
    public void setMinBidStep(BigDecimal minBidStep) { this.minBidStep = minBidStep; }
}
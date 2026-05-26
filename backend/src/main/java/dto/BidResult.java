package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResult {
    private boolean success;
    private String message;
    private Long auctionId;          // ← thêm
    private BigDecimal newPrice;
    private Long leaderId;
    private LocalDateTime endTime;
    private BigDecimal newBalance;

    public static BidResult success(Long auctionId, BigDecimal newPrice, Long leaderId, LocalDateTime endTime, BigDecimal newBalance) {
        BidResult r = new BidResult();
        r.success = true;
        r.auctionId = auctionId;
        r.newPrice = newPrice;
        r.leaderId = leaderId;
        r.endTime = endTime;
        r.newBalance = newBalance;
        return r;
    }

    public static BidResult failure(String message) {
        BidResult r = new BidResult();
        r.success = false;
        r.message = message;
        return r;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Long getAuctionId() { return auctionId; }
    public BigDecimal getNewPrice() { return newPrice; }
    public Long getLeaderId() { return leaderId; }
    public LocalDateTime getEndTime() { return endTime; }
    public BigDecimal getNewBalance() { return newBalance; }

    // Setters (nếu cần cho Gson)
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
}
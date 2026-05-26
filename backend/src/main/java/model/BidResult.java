package model; // hoặc service tuỳ cấu trúc

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResult {
    private boolean success;
    private String message;
    private BigDecimal currentPrice;
    private Long leaderId;
    private LocalDateTime endTime;

    public BidResult() {}

    public static BidResult success(BigDecimal currentPrice, Long leaderId, LocalDateTime endTime) {
        BidResult r = new BidResult();
        r.success = true;
        r.currentPrice = currentPrice;
        r.leaderId = leaderId;
        r.endTime = endTime;
        return r;
    }

    public static BidResult failure(String message) {
        BidResult r = new BidResult();
        r.success = false;
        r.message = message;
        return r;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
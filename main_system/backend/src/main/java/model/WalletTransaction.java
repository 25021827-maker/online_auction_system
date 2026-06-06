package model;

import java.time.LocalDateTime;

public class WalletTransaction {
    private Long transactionId;
    private Long userId;
    private Long auctionId;
    private String type;
    private double amount;
    private double balanceBefore;
    private double balanceAfter;
    private double availableBefore;
    private double availableAfter;
    private String note;
    private LocalDateTime createdAt;

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(double balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public double getAvailableBefore() {
        return availableBefore;
    }

    public void setAvailableBefore(double availableBefore) {
        this.availableBefore = availableBefore;
    }

    public double getAvailableAfter() {
        return availableAfter;
    }

    public void setAvailableAfter(double availableAfter) {
        this.availableAfter = availableAfter;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

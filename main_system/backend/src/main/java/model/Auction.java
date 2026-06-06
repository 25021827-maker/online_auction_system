package model;

import util.VietnamTime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Entity {
    private Long id;
    private Item item;
    private Long sellerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public enum Status { PENDING, OPEN, RUNNING, FINISHED, PAID, CANCELED }
    private Status status;

    private double currentPrice;       // BỔ SUNG: Giá hiện tại
    private Long highestBidderId;      // BỔ SUNG: ID người giữ giá cao nhất

    private BidTransaction currentHighestBid;
    private List<BidTransaction> bidHistory;

    public Auction(Long id, Item item, Long sellerId, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.sellerId = sellerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Status.OPEN;
        this.bidHistory = new ArrayList<>();
        this.currentHighestBid = null;
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public Long getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(Long highestBidderId) { this.highestBidderId = highestBidderId; }

    public BidTransaction getCurrentHighestBid() { return currentHighestBid; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }

    public void addBid(BidTransaction newBid) {
        this.currentHighestBid = newBid;
        this.bidHistory.add(newBid);
        this.currentPrice = newBid.getAmount();
        this.highestBidderId = newBid.getBidderId();
    }

    public boolean isRunning() {
        LocalDateTime now = VietnamTime.now();
        return (this.status == Status.RUNNING || this.status == Status.OPEN)
                && now.isAfter(startTime) && now.isBefore(endTime);
    }
}

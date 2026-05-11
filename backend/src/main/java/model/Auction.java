package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Entity {
    private String id;
    private Item item;
    private String sellerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public enum Status { OPEN, RUNNING, FINISHED, PAID, CANCELED }
    private Status status;

    private BidTransaction currentHighestBid;
    private List<BidTransaction> bidHistory;

    public Auction(String id, Item item, String sellerId, LocalDateTime startTime, LocalDateTime endTime) {
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
    public String getId() { return id; }
    public Item getItem() { return item; }
    public String getSellerId() { return sellerId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Status getStatus() { return status; }
    public BidTransaction getCurrentHighestBid() { return currentHighestBid; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }

    public void setStatus(Status status) { this.status = status; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }


    public void addBid(BidTransaction newBid) {
        this.currentHighestBid = newBid;
        this.bidHistory.add(newBid);
    }


    public boolean isRunning() {
        LocalDateTime now = LocalDateTime.now();
        return this.status == Status.RUNNING && now.isAfter(startTime) && now.isBefore(endTime);
    }
}
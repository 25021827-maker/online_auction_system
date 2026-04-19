package model;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    private String itemId;
    private double currentPrice;
    private String highestBidderId;
    private long endTime;
    private boolean isOpen;

    private List<Bid> bidHistory = new ArrayList<>();

    public Auction(String itemId, double startPrice, long durationMillis) {
        this.itemId = itemId;
        this.currentPrice = startPrice;
        this.endTime = System.currentTimeMillis() + durationMillis;
        this.isOpen = true;
    }

    public String getItemId() {
        return itemId;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String id) {
        this.highestBidderId = id;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        this.isOpen = false;
    }

    public List<Bid> getBidHistory() {
        return bidHistory;
    }
}
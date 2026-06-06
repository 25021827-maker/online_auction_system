package Model;

import util.VietnamTime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private String title;
    private double currentPrice;
    private String imagePath;
    private String imageBase64;
    private static int nextId = 1;
    private int id;
    private String seller;
    private String highestBidder;
    private List<Bid> bidHistory = new ArrayList<>();
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private String category;
    private String condition;
    private String description;
    private String backendStatus = "";

    /*
     * Do lech giua gio server va gio client.
     * Neu client cham/nhanh hon server, countdown van tinh theo gio server.
     */
    private long serverTimeOffsetSeconds = 0;

    public Product(String title, double currentPrice, String imagePath, String seller,
                   LocalDateTime startTime, LocalDateTime endTime, String description) {
        this.title = title;
        this.currentPrice = currentPrice;
        this.imagePath = imagePath;
        this.imageBase64 = "";
        this.seller = seller;
        this.highestBidder = "";
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.category = "OTHER";
        this.condition = "Used";
        this.id = nextId++;
        this.createdAt = VietnamTime.now();
    }

    public Product(String title, double currentPrice) {
        this.title = title;
        this.currentPrice = currentPrice;
        this.imagePath = "";
        this.imageBase64 = "";
        this.seller = "Unknown";
        this.highestBidder = "";
        this.startTime = VietnamTime.now();
        this.endTime = VietnamTime.now().plusHours(1);
        this.category = "OTHER";
        this.condition = "Used";
        this.description = "";
        this.id = nextId++;
        this.createdAt = VietnamTime.now();
    }

    /*
     * Dong bo gio client theo gio server gui ve trong AuctionDTO.serverTime.
     */
    public void syncServerTime(LocalDateTime serverNow) {
        if (serverNow == null) {
            this.serverTimeOffsetSeconds = 0;
            return;
        }

        LocalDateTime clientNow = VietnamTime.now();
        this.serverTimeOffsetSeconds = ChronoUnit.SECONDS.between(clientNow, serverNow);
    }

    private LocalDateTime getSyncedNow() {
        return VietnamTime.now().plusSeconds(serverTimeOffsetSeconds);
    }

    public boolean canModify() {
        LocalDateTime now = getSyncedNow();

        if (!now.isBefore(startTime)) {
            return false;
        }

        Duration remaining = Duration.between(now, startTime);
        return remaining.toMinutes() > 20;
    }

    public String getTimeRemaining() {
        LocalDateTime now = getSyncedNow();

        if (now.isBefore(startTime)) {
            Duration untilStart = Duration.between(now, startTime);
            return "Starts in: " + formatDuration(untilStart);
        }

        if (!now.isBefore(endTime)) {
            return "Ended";
        }

        return formatDuration(Duration.between(now, endTime));
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public void addBid(Bid bid) {
        bidHistory.add(bid);
    }

    public List<Bid> getBidHistory() {
        return bidHistory;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(String highestBidder) {
        this.highestBidder = highestBidder;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.backendStatus = status;
    }

    public String getStatus() {
        LocalDateTime now = getSyncedNow();

        if (now.isBefore(startTime)) {
            return "SCHEDULED";
        }

        if (!now.isBefore(endTime)) {
            return "FINISHED";
        }

        if (backendStatus != null && !backendStatus.isEmpty()) {
            return backendStatus;
        }

        return "OPEN";
    }
}
package Model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Product {

    private String title;

    private double currentPrice;

    private String imagePath;

    private static int nextId = 1;

    private int id;

    // =========================
    // AUCTION INFO
    // =========================

    private String seller;

    private String highestBidder;

    // =========================
// BID HISTORY
// =========================
    private List<Bid> bidHistory =
            new ArrayList<>();



    // =========================
    // TIME
    // =========================

    private LocalDateTime startTime;
    private String auctionId;

    // Getter/Setter
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    private LocalDateTime endTime;

    private String category;
    private String condition;
    private String description;


    // =========================
    // FULL CONSTRUCTOR
    // =========================
    public Product(
            String title,
            double currentPrice,
            String imagePath,
            String seller,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String description
    ) {

        this.title = title;

        this.currentPrice = currentPrice;

        this.imagePath = imagePath;

        this.seller = seller;

        this.highestBidder = "";

        this.startTime = startTime;

        this.endTime = endTime;

        this.description = description;

        this.category = "Other";

        this.condition = "Used";

        this.id = nextId++;

    }


    // =========================
    // SIMPLE CONSTRUCTOR
    // =========================
    public Product(
            String title,
            double currentPrice
    ) {

        this.title = title;

        this.currentPrice = currentPrice;

        this.imagePath = "";

        this.seller = "Unknown";

        this.highestBidder = "";

        this.startTime = LocalDateTime.now();

        this.endTime = LocalDateTime.now().plusHours(1);
        this.category = "Other";

        this.condition = "Used";
        this.description = "";
    }

    // =========================
    // STATUS LOGIC
    // =========================
    public String getStatus() {

        LocalDateTime now = LocalDateTime.now();

        // CHƯA BẮT ĐẦU
        if (now.isBefore(startTime)) {

            return "SCHEDULED";
        }

        // ĐÃ KẾT THÚC
        if (now.isAfter(endTime)) {

            return "SOLD";
        }

        // ĐANG ĐẤU
        return "OPEN";
    }
    public boolean canModify() {

        LocalDateTime now = LocalDateTime.now();

        // auction already started
        if (!now.isBefore(startTime)) {

            return false;
        }

        Duration remaining = Duration.between(
                now,
                startTime
        );

        // lock before 20 minutes
        return remaining.toMinutes() > 20;
    }

    // =========================
    // TIMER LOGIC
    // =========================
    public String getTimeRemaining() {

        LocalDateTime now = LocalDateTime.now();

        // CHƯA BẮT ĐẦU
        if (now.isBefore(startTime)) {

            Duration untilStart = Duration.between(
                    now,
                    startTime
            );

            long seconds = untilStart.getSeconds();

            long hours =
                    seconds / 3600;

            long minutes =
                    (seconds % 3600) / 60;

            long secs =
                    seconds % 60;

            return "Bắt đầu sau: "
                    + String.format(
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    secs
            );
        }

        // ĐÃ KẾT THÚC
        if (now.isAfter(endTime)) {

            return "Đã kết thúc";
        }

        // ĐANG ĐẤU
        Duration duration = Duration.between(
                now,
                endTime
        );

        long seconds = duration.getSeconds();

        long hours =
                seconds / 3600;

        long minutes =
                (seconds % 3600) / 60;

        long secs =
                seconds % 60;

        return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                secs
        );
    }
    // =========================
// BID HISTORY
// =========================
    public void addBid(Bid bid) {

        bidHistory.add(bid);
    }

    public List<Bid> getBidHistory() {

        return bidHistory;
    }


    // =========================
    // SETTERS
    // =========================

    public void setCurrentPrice(double price) {

        this.currentPrice = price;
    }

    public void setSeller(String seller) {

        this.seller = seller;
    }

    public void setHighestBidder(String highestBidder) {

        this.highestBidder = highestBidder;
    }

    public void setStartTime(LocalDateTime startTime) {

        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {

        this.endTime = endTime;
    }

    // =========================
    // GETTERS
    // =========================

    public String getTitle() {

        return title;
    }

    public double getCurrentPrice() {

        return currentPrice;
    }

    public String getImagePath() {

        return imagePath;
    }

    public String getSeller() {

        return seller;
    }

    public String getHighestBidder() {

        return highestBidder;
    }

    public LocalDateTime getStartTime() {

        return startTime;
    }

    public LocalDateTime getEndTime() {

        return endTime;
    }
    public int getId() {

        return id;
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
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}

}
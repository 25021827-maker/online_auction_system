package Model;

import java.time.Duration;
import java.time.LocalDateTime;

public class Product {

    private String title;

    private double currentPrice;

    private String imagePath;

    // =========================
    // AUCTION INFO
    // =========================

    private String seller;

    private String highestBidder;

    // =========================
    // TIME
    // =========================

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    // =========================
    // FULL CONSTRUCTOR
    // =========================
    public Product(
            String title,
            double currentPrice,
            String imagePath,
            String seller,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        this.title = title;

        this.currentPrice = currentPrice;

        this.imagePath = imagePath;

        this.seller = seller;

        this.highestBidder = "";

        this.startTime = startTime;

        this.endTime = endTime;
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
}
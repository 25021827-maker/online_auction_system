package Model;

public class Product {

    private String title;

    private double currentPrice;

    private String timeRemaining;

    private String imagePath;

    // =========================
    // AUCTION INFO
    // =========================

    private String status;

    private String seller;

    private String highestBidder;

    // =========================
    // FULL CONSTRUCTOR
    // =========================
    public Product(
            String title,
            double currentPrice,
            String timeRemaining,
            String imagePath,
            String status,
            String seller
    ) {

        this.title = title;

        this.currentPrice = currentPrice;

        this.timeRemaining = timeRemaining;

        this.imagePath = imagePath;

        this.status = status;

        this.seller = seller;

        this.highestBidder = "";
    }

    // =========================
    // OLD CONSTRUCTOR
    // =========================
    public Product(
            String title,
            double currentPrice,
            String timeRemaining,
            String imagePath
    ) {

        this.title = title;

        this.currentPrice = currentPrice;

        this.timeRemaining = timeRemaining;

        this.imagePath = imagePath;

        this.status = "OPEN";

        this.seller = "Unknown";

        this.highestBidder = "";
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

        this.timeRemaining = "Chưa xác định";

        this.imagePath = "";

        this.status = "OPEN";

        this.seller = "Unknown";

        this.highestBidder = "";
    }

    // =========================
    // SETTERS
    // =========================

    public void setCurrentPrice(double price) {

        this.currentPrice = price;

    }

    public void setStatus(String status) {

        this.status = status;

    }

    public void setSeller(String seller) {

        this.seller = seller;

    }

    public void setHighestBidder(String highestBidder) {

        this.highestBidder = highestBidder;

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

    public String getTimeRemaining() {

        return timeRemaining;

    }

    public String getImagePath() {

        return imagePath;

    }

    public String getStatus() {

        return status;

    }

    public String getSeller() {

        return seller;

    }

    public String getHighestBidder() {

        return highestBidder;

    }
}
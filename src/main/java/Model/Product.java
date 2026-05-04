package Model;

public class Product {
    private String title;
    private double currentPrice;
    private String timeRemaining;
    private String imagePath;

    // 👉 thêm mới (quan trọng)
    private String status;   // OPEN, SOLD
    private String seller;   // người đăng

    // constructor đầy đủ (nâng cấp)
    public Product(String title, double currentPrice, String timeRemaining, String imagePath, String status, String seller) {
        this.title = title;
        this.currentPrice = currentPrice;
        this.timeRemaining = timeRemaining;
        this.imagePath = imagePath;
        this.status = status;
        this.seller = seller;
    }

    // ✅ constructor cũ (giữ để không vỡ code)
    public Product(String title, double currentPrice, String timeRemaining, String imagePath) {
        this.title = title;
        this.currentPrice = currentPrice;
        this.timeRemaining = timeRemaining;
        this.imagePath = imagePath;

        // 👉 mặc định
        this.status = "OPEN";
        this.seller = "Unknown";
    }

    // ✅ constructor đơn giản (form)
    public Product(String title, double currentPrice) {
        this.title = title;
        this.currentPrice = currentPrice;
        this.timeRemaining = "Chưa xác định";
        this.imagePath = "";

        // 👉 mặc định
        this.status = "OPEN";
        this.seller = "Unknown";
    }

    // ========================
    // SETTERS
    // ========================

    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    // ========================
    // GETTERS
    // ========================

    public String getTitle() { return title; }

    public double getCurrentPrice() { return currentPrice; }

    public String getTimeRemaining() { return timeRemaining; }

    public String getImagePath() { return imagePath; }

    public String getStatus() { return status; }

    public String getSeller() { return seller; }
}
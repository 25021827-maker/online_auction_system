package Model;

public class Product {
    private String title;
    private double currentPrice;
    private String timeRemaining;
    private String imagePath;

    public Product(String title, double currentPrice, String timeRemaining, String imagePath) {
        this.title = title;
        this.currentPrice = currentPrice;
        this.timeRemaining = timeRemaining;
        this.imagePath = imagePath;
    }
    // Getters
    public String getTitle() { return title; }
    public double getCurrentPrice() { return currentPrice; }
    public String getTimeRemaining() { return timeRemaining; }
    public String getImagePath() { return imagePath; }
}
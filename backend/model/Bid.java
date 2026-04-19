package model;

public class Bid {
    private String userId;
    private double amount;
    private long timestamp;

    public Bid(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
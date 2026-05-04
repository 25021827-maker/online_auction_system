package model;

import java.time.LocalDateTime;

public class BidTransaction implements Entity {
    private String id;
    private String bidderId;
    private double amount;
    private LocalDateTime timestamp;

    public BidTransaction(String id, String bidderId, double amount) {
        this.id = id;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getId() { return id; }

    public String getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "Bidder " + bidderId + " đặt $" + amount + " lúc " + timestamp;
    }
}
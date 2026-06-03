package model;

import java.time.LocalDateTime;

public class BidTransaction implements Entity {
    private Long id;
    private Long bidderId;
    private double amount;
    private LocalDateTime timestamp;
    private boolean autoBid;

    public BidTransaction(Long id, Long bidderId, double amount) {
        this(id, bidderId, amount, false);
    }

    public BidTransaction(Long id, Long bidderId, double amount, boolean autoBid) {
        this.id = id;
        this.bidderId = bidderId;
        this.amount = amount;
        this.autoBid = autoBid;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }

    public Long getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAutoBid() { return autoBid; }

    @Override
    public String toString() {
        return "Bidder " + bidderId + " đặt $" + amount + " lúc " + timestamp;
    }
}

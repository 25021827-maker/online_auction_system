package Model;

import java.time.LocalDateTime;

public class Bid {

    private String bidder;

    private double amount;

    private LocalDateTime bidTime;

    // =========================
    // CONSTRUCTOR
    // =========================
    public Bid(
            String bidder,
            double amount
    ) {

        this.bidder = bidder;

        this.amount = amount;

        this.bidTime = LocalDateTime.now();
    }

    // =========================
    // GETTERS
    // =========================
    public String getBidder() {

        return bidder;
    }

    public double getAmount() {

        return amount;
    }

    public LocalDateTime getBidTime() {

        return bidTime;
    }

    // =========================
    // DISPLAY
    // =========================
    @Override
    public String toString() {

        return bidder
                + " bid $"
                + amount
                + " | "
                + bidTime.toLocalTime()
                .withNano(0);
    }
}



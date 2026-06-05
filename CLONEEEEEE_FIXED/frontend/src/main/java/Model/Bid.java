package Model;

import util.VietnamTime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        this(bidder, amount, VietnamTime.now());
    }

    public Bid(
            String bidder,
            double amount,
            LocalDateTime bidTime
    ) {
        this.bidder = bidder;

        this.amount = amount;

        this.bidTime = bidTime != null ? bidTime : VietnamTime.now();
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
                + String.format("%.2f", amount)
                + " | "
                + bidTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}



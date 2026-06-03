package dto;

public class BidRequest {
    public Long auctionId; // Bắt buộc dùng Long để khớp Backend
    public Long bidderId;  // Bắt buộc dùng Long
    public double amount;
    public boolean autoBid;
}

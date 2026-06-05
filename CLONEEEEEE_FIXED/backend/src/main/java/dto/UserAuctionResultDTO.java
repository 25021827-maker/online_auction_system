package dto;

public class UserAuctionResultDTO {
    public Long winnerId;
    public Long auctionId;
    public Long itemId;

    public Long sellerId;
    public String sellerUsername;

    public Long bidderId;
    public String winnerUsername;

    public String itemName;
    public String category;
    public String condition;
    public String imagePath;
    public String imageBase64;

    public double finalPrice;
    public String status;
    public String createdAt;

    public String roleView;
}

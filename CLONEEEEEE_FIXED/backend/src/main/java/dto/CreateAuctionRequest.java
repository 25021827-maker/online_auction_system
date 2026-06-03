package dto;

public class CreateAuctionRequest {
    public Long auctionId;
    public Long sellerId;
    public String itemName;
    public String description;
    public double startingPrice;
    public String category;
    public String condition;
    public String startTime;
    public String endTime;
    public String imagePath; // ĐÃ THÊM DÒNG NÀY
}
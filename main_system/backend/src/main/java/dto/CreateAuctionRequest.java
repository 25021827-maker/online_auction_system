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

    // Đường dẫn ảnh đã lưu trên server, ví dụ: uploads/products/xxx.jpg
    public String imagePath;

    // Ảnh client gửi lên server
    public String imageBase64;
    public String imageFileName;
}
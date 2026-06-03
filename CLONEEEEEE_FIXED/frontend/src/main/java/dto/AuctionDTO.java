package dto;

public class AuctionDTO {
    public Long id;
    public Long sellerId;
    public String startTime;
    public String endTime;
    public String status;
    public double currentPrice;
    public Long highestBidderId;

    public ItemDTO item;

    public static class ItemDTO {
        public Long id;
        public String name;
        public String description;
        public double startingPrice;
        public String category;
        public String condition;
        public String imagePath;
    }
}
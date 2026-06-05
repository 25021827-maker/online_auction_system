package dto;

public class AuctionDTO {
    public Long id;
    public Long sellerId;
    public String startTime;
    public String endTime;
    public String status;
    public double currentPrice;
    public Long highestBidderId;

    /*
     * Gio hien tai cua server.
     * Frontend dung de dong bo countdown voi server.
     */
    public String serverTime;

    public ItemDTO item;

    public static class ItemDTO {
        public Long id;
        public String name;
        public String description;
        public double startingPrice;
        public String category;
        public String condition;
        public String imagePath;
        public String imageBase64;
        public String itemStatus;
        public String approvalStatus;
    }
}
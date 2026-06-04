package dto;

import model.Auction;
import model.Item;

public class AuctionDTO {
    public Long id;
    public Long sellerId;
    public String startTime;
    public String endTime;
    public String status;
    public double currentPrice;
    public Long highestBidderId;
    public ItemDTO item;

    public static AuctionDTO from(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.id = auction.getId();
        dto.sellerId = auction.getSellerId();
        dto.startTime = auction.getStartTime() == null ? null : auction.getStartTime().toString();
        dto.endTime = auction.getEndTime() == null ? null : auction.getEndTime().toString();
        dto.status = auction.getStatus() == null ? "" : auction.getStatus().name();
        dto.currentPrice = auction.getCurrentPrice();
        dto.highestBidderId = auction.getHighestBidderId();
        dto.item = ItemDTO.from(auction.getItem());
        return dto;
    }

    public static class ItemDTO {
        public Long id;
        public String name;
        public String description;
        public double startingPrice;
        public String category;
        public String condition;
        public String imagePath;
        public String itemStatus;
        public String approvalStatus;

        public static ItemDTO from(Item item) {
            if (item == null) {
                return null;
            }

            ItemDTO dto = new ItemDTO();
            dto.id = item.getId();
            dto.name = item.getName();
            dto.description = item.getDescription();
            dto.startingPrice = item.getStartingPrice();
            dto.category = item.getCategory();
            dto.condition = item.getCondition();
            dto.imagePath = item.getImagePath();
            dto.itemStatus = item.getItemStatus();
            dto.approvalStatus = item.getApprovalStatus();
            return dto;
        }
    }
}

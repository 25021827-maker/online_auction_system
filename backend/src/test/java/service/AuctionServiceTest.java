package service;

import exception.InvalidBidException;
import exception.AuctionClosedException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    private Auction auction;
    private AuctionService auctionService;


    @BeforeEach
    public void setUp() {
        // Tạo một sản phẩm bằng Factory Pattern
        Item item = ItemFactory.createItem("ELECTRONICS", "ITEM1", "Laptop Dell", "Core i7", 1000.0, 12);

        // Tạo một phiên đấu giá đang chạy (từ 10 phút trước đến 10 phút sau)
        auction = new Auction("AUC1", item, "SELLER1", LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        auction.setStatus(Auction.Status.RUNNING);

        // Truyền vào Service
        auctionService = new AuctionService(auction);
    }


    @Test
    public void testPlaceValidBid_Success() {

        BidTransaction validBid = new BidTransaction("BID1", "BIDDER1", 1200.0);

        boolean result = auctionService.placeBid(validBid);


        assertTrue(result, "Hàm placeBid phải trả về true khi giá hợp lệ");


        assertEquals(1200.0, auction.getCurrentHighestBid().getAmount());
    }


    @Test
    public void testPlaceInvalidBid_ThrowsException() {

        auctionService.placeBid(new BidTransaction("BID1", "BIDDER1", 1500.0));


        BidTransaction invalidBid = new BidTransaction("BID2", "BIDDER2", 1400.0);


        assertThrows(InvalidBidException.class, () -> {
            auctionService.placeBid(invalidBid);
        }, "Hệ thống phải ném ra lỗi InvalidBidException khi giá đặt thấp hơn giá cao nhất");
    }


    @Test
    public void testPlaceBidOnClosedAuction_ThrowsException() {

        auction.setStatus(Auction.Status.FINISHED);

        BidTransaction lateBid = new BidTransaction("BID1", "BIDDER1", 2000.0);


        assertThrows(AuctionClosedException.class, () -> {
            auctionService.placeBid(lateBid);
        }, "Hệ thống phải ném ra lỗi AuctionClosedException khi phiên đã đóng");
    }
}
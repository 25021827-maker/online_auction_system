package service;

import exception.AuctionClosedException;
import exception.InvalidBidException;
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
        Item item = ItemFactory.createItem("ELECTRONICS", 1L, "Laptop Dell", "Core i7", 1000.0, 12);
        auction = new Auction(1L, item, 2L, LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        auction.setStatus(Auction.Status.RUNNING);
        auction.setCurrentPrice(1000.0);

        // Không dùng DB trong unit test để CI có thể chạy ổn định.
        auctionService = new AuctionService(auction, null);
    }

    @Test
    public void testPlaceValidBid_Success() {
        BidTransaction validBid = new BidTransaction(null, 3L, 1200.0);
        assertTrue(auctionService.placeBid(validBid));
        assertEquals(1200.0, auction.getCurrentHighestBid().getAmount());
        assertEquals(3L, auction.getHighestBidderId());
    }

    @Test
    public void testPlaceInvalidBid_ThrowsException() {
        auctionService.placeBid(new BidTransaction(null, 3L, 1500.0));
        BidTransaction invalidBid = new BidTransaction(null, 4L, 1400.0);
        assertThrows(InvalidBidException.class, () -> auctionService.placeBid(invalidBid));
    }

    @Test
    public void testPlaceBidOnClosedAuction_ThrowsException() {
        auction.setStatus(Auction.Status.FINISHED);
        BidTransaction lateBid = new BidTransaction(null, 3L, 2000.0);
        assertThrows(AuctionClosedException.class, () -> auctionService.placeBid(lateBid));
    }

    @Test
    public void testSellerCannotBidOwnAuction_ThrowsException() {
        BidTransaction sellerBid = new BidTransaction(null, 2L, 1200.0);
        assertThrows(InvalidBidException.class, () -> auctionService.placeBid(sellerBid));
    }
}

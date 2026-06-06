package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import dao.AuctionDAO;
import exception.InvalidBidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutoBidServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    @InjectMocks
    private AutoBidService autoBidService;

    private AuctionDAO.AuctionBidState mockState;

    // GIẢI PHÁP: Dạy Gson bỏ qua các class thời gian của hệ thống để tránh lỗi bảo mật của Java 21+
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Timestamp.class, (JsonDeserializer<Timestamp>) (json, typeOfT, context) -> null)
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> null)
            .create();

    @BeforeEach
    public void setUp() {
        // 1. Tạo JSON cơ bản
        String stateJson = "{ 'auctionId': 1, 'currentPrice': 1000.0, 'minBidStep': 10.0, 'currentLeaderId': 2, 'status': 'RUNNING' }";
        mockState = gson.fromJson(stateJson, AuctionDAO.AuctionBidState.class);

        // 2. Không cần gán startTime và endTime nữa, cứ để nó null!

        // 3. Spy và mock (Dùng doReturn để KHÔNG chạy code thật, bỏ qua vụ check null thời gian)
        mockState = spy(mockState);
        lenient().doReturn(true).when(mockState).canAcceptBids();
    }

    @Test
    public void testConfigureAutoBid_Success() {
        when(auctionDAO.getAuctionBidState(1L)).thenReturn(mockState);
        when(auctionDAO.upsertAutoBid(1L, 3L, 5000.0, 50.0)).thenReturn(true);

        assertDoesNotThrow(() -> autoBidService.configureAutoBid(1L, 3L, 5000.0, 50.0));
        verify(auctionDAO, times(1)).upsertAutoBid(1L, 3L, 5000.0, 50.0);
    }

    @Test
    public void testConfigureAutoBid_MaxAmountTooLow_ThrowsException() {
        when(auctionDAO.getAuctionBidState(1L)).thenReturn(mockState);
        assertThrows(InvalidBidException.class,
                () -> autoBidService.configureAutoBid(1L, 3L, 1005.0, 10.0));
    }

    @Test
    public void testFindNextBid_NoAutoBids_ReturnsNull() {
        when(auctionDAO.getAuctionBidState(1L)).thenReturn(mockState);
        when(auctionDAO.getActiveAutoBids(1L)).thenReturn(Collections.emptyList());

        AutoBidService.AutoBidDecision decision = autoBidService.findNextBid(1L);
        assertNull(decision);
    }

    @Test
    public void testFindNextBid_SingleAutoBid_JumpsToMinimumRequired() {
        when(auctionDAO.getAuctionBidState(1L)).thenReturn(mockState);

        String p3Json = "{ 'auctionId': 1, 'bidderId': 3, 'maxAmount': 5000.0, 'incrementStep': 20.0 }";
        AuctionDAO.AutoBidConfig config = gson.fromJson(p3Json, AuctionDAO.AutoBidConfig.class);

        when(auctionDAO.getActiveAutoBids(1L)).thenReturn(Arrays.asList(config));

        AutoBidService.AutoBidDecision decision = autoBidService.findNextBid(1L);

        assertNotNull(decision);
        assertEquals(3L, decision.bidderId);
        assertEquals(1020.0, decision.amount);
    }

    @Test
    public void testFindNextBid_TwoAutoBids_FightToTheMax() {
        when(auctionDAO.getAuctionBidState(1L)).thenReturn(mockState);

        String p3Json = "{ 'auctionId': 1, 'bidderId': 3, 'maxAmount': 9000.0, 'incrementStep': 50.0 }";
        AuctionDAO.AutoBidConfig p3 = gson.fromJson(p3Json, AuctionDAO.AutoBidConfig.class);

        String p4Json = "{ 'auctionId': 1, 'bidderId': 4, 'maxAmount': 7000.0, 'incrementStep': 50.0 }";
        AuctionDAO.AutoBidConfig p4 = gson.fromJson(p4Json, AuctionDAO.AutoBidConfig.class);

        when(auctionDAO.getActiveAutoBids(1L)).thenReturn(Arrays.asList(p3, p4));

        AutoBidService.AutoBidDecision decision = autoBidService.findNextBid(1L);

        assertNotNull(decision);
        assertEquals(3L, decision.bidderId);
        assertEquals(7000.0, decision.amount);
    }
}
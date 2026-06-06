package service;

import dao.AuctionDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionSettlementServiceTest {

    @Mock
    private AuctionDAO auctionDAO; // Làm giả DAO

    @InjectMocks
    private AuctionSettlementService settlementService; // Bơm DAO giả vào Service thật

    @Test
    public void testSettleAuction_Success() {
        // Kịch bản: Khi gọi DAO thanh toán auction 1L, giả lập trả về true
        when(auctionDAO.settleAuction(1L)).thenReturn(true);

        boolean result = settlementService.settleAuction(1L);

        assertTrue(result, "Phải trả về true khi thanh toán thành công");
        verify(auctionDAO, times(1)).settleAuction(1L);
    }

    @Test
    public void testFinishExpiredAuctions_ReturnsList() {
        // Chuẩn bị 1 kết quả giả
        AuctionDAO.SettlementResult mockResult = new AuctionDAO.SettlementResult();
        mockResult.auctionId = 100L;
        mockResult.hasWinner = true;
        mockResult.finalPrice = 5000.0;

        when(auctionDAO.finishExpiredAuctionsAndGetResults()).thenReturn(List.of(mockResult));

        // Chạy hàm service
        List<AuctionDAO.SettlementResult> results = settlementService.finishExpiredAuctionsAndGetResults();

        // Kiểm tra
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).auctionId);
        assertEquals(5000.0, results.get(0).finalPrice);
    }
}
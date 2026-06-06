package service;

import dao.AuctionDAO;
import model.Auction;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionManagerTest {

    @Test
    public void testGetActiveAuctionsList_Success() {
        try (MockedConstruction<AuctionDAO> mockedDao = mockConstruction(AuctionDAO.class, (mock, context) -> {
            Auction dummyAuction = mock(Auction.class);
            when(dummyAuction.getId()).thenReturn(10L);
            when(dummyAuction.getStatus()).thenReturn(Auction.Status.RUNNING);

            when(mock.getActiveAuctions()).thenReturn(List.of(dummyAuction));
        })) {
            AuctionManager auctionManager = new AuctionManager();
            List<Auction> activeAuctions = auctionManager.getActiveAuctionsList();

            assertNotNull(activeAuctions);
            assertEquals(1, activeAuctions.size());
            assertEquals(10L, activeAuctions.get(0).getId());
            assertEquals(Auction.Status.RUNNING, activeAuctions.get(0).getStatus());
        }
    }

    @Test
    public void testCreateAuction_Success() {
        try (MockedConstruction<AuctionDAO> mockedDao = mockConstruction(AuctionDAO.class, (mock, context) -> {
            // FIX LỖI: Dùng lệnh any() thay cho anyString() để bao trọn mọi trường hợp, kể cả null
            when(mock.createAuction(any(), any(), any(), anyDouble(),
                    any(), any(), any(), any(), any()))
                    .thenReturn(true);
        })) {
            AuctionManager auctionManager = new AuctionManager();

            LocalDateTime start = LocalDateTime.now().plusMinutes(5);
            LocalDateTime end = LocalDateTime.now().plusHours(1);

            // FIX LỖI: Truyền "image.png" thay vì truyền null để khớp hoàn toàn với dữ liệu giả định
            boolean isSuccess = auctionManager.createAuction(
                    1L, "Laptop", "Mô tả", 1000.0, "Điện tử", "Mới", "image.png", start, end
            );

            assertTrue(isSuccess, "Hệ thống phải trả về true khi tạo phiên đấu giá thành công");
        }
    }
}
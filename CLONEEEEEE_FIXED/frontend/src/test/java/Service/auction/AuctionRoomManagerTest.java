package Service.auction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuctionRoomManagerTest {

    @Test
    void testJoinAndLeaveRoomFlow() {
        int productId = 123;

        // 1. Trước khi join, số lượng người xem phải bằng 0
        assertEquals(0, AuctionRoomManager.getViewerCount(productId));

        // 2. Thực hiện join phòng
        try {
            AuctionRoomManager.joinRoom(productId);
        } catch (Exception e) {
            // Bỏ qua lỗi kết nối Socket thực tế nếu có
            System.out.println("Gặp lỗi mạng thực tế khi gửi request: " + e.getMessage());
        }

        // 3. Kiểm tra xem biến viewers nội bộ đã tăng lên 1 chưa
        assertEquals(1, AuctionRoomManager.getViewerCount(productId));

        // 4. Thực hiện rời phòng (leave)
        try {
            AuctionRoomManager.leaveRoom(productId);
        } catch (Exception e) {
            System.out.println("Gặp lỗi mạng thực tế khi gửi request: " + e.getMessage());
        }

        // 5. Sau khi leave, số lượng người xem phải giảm về 0
        assertEquals(0, AuctionRoomManager.getViewerCount(productId));
    }
}

package Model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void testProductSimpleFlow() {
        // 1. Khởi tạo đối tượng Product đơn giản
        Product product = new Product("Laptop Gaming", 1500.0);

        // 2. Kiểm tra Get/Set cơ bản
        assertEquals("Laptop Gaming", product.getTitle());
        assertEquals(1500.0, product.getCurrentPrice(), 0.001);

        // 3. Kiểm tra tính năng thêm Lịch sử đấu giá (đã sửa lỗi cú pháp .get(0))
        Bid mockBid = new Bid("UserA", 1600.0);
        product.addBid(mockBid);

        assertEquals(1, product.getBidHistory().size());
        assertEquals("UserA", product.getBidHistory().get(0).getBidder());

        // 4. Chạy thử hàm đồng bộ thời gian an toàn
        assertDoesNotThrow(() -> {
            product.syncServerTime(LocalDateTime.now());
            assertNotNull(product.getTimeRemaining());
        });
    }
}
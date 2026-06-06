package Model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BidTest {

    @Test
    void testBidCreationAndGetters() {
        // 1. Khai báo các biến dữ liệu test mẫu
        String expectedBidder = "NguyenVanA";
        double expectedAmount = 250.5;

        // 2. Khởi tạo đối tượng Bid
        Bid bid = new Bid(expectedBidder, expectedAmount);

        // 3. Kiểm tra xem các hàm Getter trả về đúng giá trị không
        assertEquals(expectedBidder, bid.getBidder());
        assertEquals(expectedAmount, bid.getAmount(), 0.001);

        // 4. Kiểm tra thời gian tạo bidTime hợp lệ (đã sửa hàm getGetBidTime thành getBidTime)
        assertNotNull(bid.getBidTime());
        assertTrue(bid.getBidTime().isBefore(LocalDateTime.now().plusSeconds(5)));
    }

    @Test
    void testToStringMethod() {
        // 5. Kiểm tra định dạng chuỗi hiển thị của hàm toString()
        Bid bid = new Bid("NgườiĐấuGiá", 100.0);
        String result = bid.toString();

        assertNotNull(result);
        assertTrue(result.contains("NgườiĐấuGiá"));
        assertTrue(result.contains("100.0"));
    }
}
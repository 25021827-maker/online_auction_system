package Model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserSimpleFlow() {
        // 1. Khởi tạo đối tượng User
        User user = new User();
        assertNotNull(user);

        // 2. Kiểm tra các hàm Get/Set cơ bản
        user.setId(10L);
        user.setUsername("quangnv");
        user.setBalance(500.0);

        assertEquals(10L, user.getId());
        assertEquals("quangnv", user.getUsername());
        assertEquals(500.0, user.getBalance(), 0.001);

        // 3. Kiểm tra hàm trừ tiền tiện ích (deductMoney)
        user.deductMoney(100.0);
        assertEquals(400.0, user.getBalance(), 0.001);

        // 4. Kiểm tra toàn bộ luồng logic của danh sách theo dõi (Watchlist)
        int productId = 99;

        // Ban đầu chưa theo dõi
        assertFalse(user.isWatching(productId));

        // Thêm vào danh sách theo dõi
        user.addToWatchlist(productId);
        assertTrue(user.isWatching(productId));

        // Lấy danh sách ID kiểm tra số lượng phần tử
        List<Integer> list = user.getWatchlistProductIds();
        assertEquals(1, list.size());

        // Xóa khỏi danh sách theo dõi
        user.removeFromWatchlist(productId);
        assertFalse(user.isWatching(productId));
    }
}
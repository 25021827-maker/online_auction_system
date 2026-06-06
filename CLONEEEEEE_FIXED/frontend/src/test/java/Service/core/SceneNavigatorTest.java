package Service.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneNavigatorTest {

    @Test
    void testLoadFromNode_WithNullNode_ShouldReturnFalseSafely() {
        // Kiểm tra xem khi truyền vào một Node bị null thì hàm có tự bắt lỗi
        // và trả về false an toàn thay vì làm sập ứng dụng hay không
        boolean result = SceneNavigator.loadFromNode(null, "/View/Login.fxml", "Đăng nhập");
        assertFalse(result, "Khi node bị null, hàm phải trả về false an toàn");
    }

    @Test
    void testLoadInitial_WithInvalidPath_ShouldReturnFalseSafely() {
        // Kiểm tra tính năng an toàn (Null-Safety / Exception Handling)
        // Khi truyền đường dẫn FXML bậy bạ hoặc Stage null, hàm phải nhảy vào khối catch và trả về false
        boolean result = SceneNavigator.loadInitial(null, "duong_dan_bay_ba.fxml", "Test Title");
        assertFalse(result, "Khi đường dẫn lỗi hoặc Stage null, hàm phải trả về false thay vì crash");
    }
}
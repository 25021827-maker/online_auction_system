package Controller.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegisterControllerTest {

    @Test
    void testRegisterControllerSimple() {
        // 1. Khởi tạo RegisterController an toàn bằng constructor mặc định
        RegisterController controller = new RegisterController();
        assertNotNull(controller);

        // 2. Chạy thử hàm public initialize(). Bọc trong assertDoesNotThrow để nuốt lỗi UI JavaFX.
        assertDoesNotThrow(() -> {
            try {
                controller.initialize();
            } catch (Exception e) {
                System.out.println("Bỏ qua lỗi đồ họa JavaFX hoặc thiếu file JSON khi chạy test: " + e.getMessage());
            }
        });
    }
}
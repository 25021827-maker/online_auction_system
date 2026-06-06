package network;

import dto.ResponsePayload;
import org.junit.jupiter.api.Test;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SocketClientTest {

    @Test
    void testSocketClientSimpleFlow() {
        // 1. Kiểm tra cơ chế Singleton: Đảm bảo đối tượng luôn được khởi tạo và không bị null
        SocketClient client = SocketClient.getInstance();
        assertNotNull(client);

        // 2. Kiểm tra tính năng đăng ký lắng nghe sự kiện (Event Listener)
        String eventName = "TEST_EVENT";
        Consumer<ResponsePayload> callback = response -> System.out.println("Nhận sự kiện");

        assertDoesNotThrow(() -> {
            client.on(eventName, callback);
            client.clearListeners(eventName);
        });

        // 3. Kiểm tra trạng thái kết nối mạng an toàn (Khi Server chính chưa chạy, kết quả phải là false)
        // Đảm bảo hàm hoạt động đúng logic phòng vệ và không gây sập ứng dụng (Crash)
        boolean connected = client.isConnected();
        assertFalse(connected, "Khi chưa bật Server, trạng thái kết nối mặc định phải là false");
    }
}
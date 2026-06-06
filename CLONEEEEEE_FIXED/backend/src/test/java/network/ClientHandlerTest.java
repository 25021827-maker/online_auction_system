package network;

import org.junit.jupiter.api.Test;
import service.AuctionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    @Test
    public void testHandleRequest_InvalidAction_ReturnsError() throws Exception {
        // 1. Chuẩn bị "Đồ giả"
        Socket mockSocket = mock(Socket.class);
        AuctionManager mockManager = mock(AuctionManager.class);

        // 2. Giả lập Client gửi một chuỗi JSON lên Server với Action bịa đặt
        String fakeClientRequest = "{\"action\":\"ACTION_KHONG_TON_TAI\",\"data\":\"{}\"}\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fakeClientRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 3. Gắn ống nước giả vào Socket
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);

        // 4. Khởi tạo và chạy Handler (chạy trực tiếp hàm run() thay vì start Thread để test đồng bộ)
        ClientHandler handler = new ClientHandler(mockSocket, mockManager);
        handler.run();

        // 5. Bắt lấy chuỗi trả về từ Server
        String serverResponse = outputStream.toString();
        System.out.println("DEBUG - Invalid Action Response: [" + serverResponse + "]");

        // 6. Kiểm tra kết quả (Dựa theo code thực tế của Server trả về FAIL)
        assertTrue(serverResponse.contains("\"status\":\"FAIL\""), "Phải trả về FAIL status");

        // Lưu ý: Nếu Server của bạn báo lỗi khác với "Hanh dong khong hop le", hãy xem dòng DEBUG ở trên và sửa lại chuỗi này cho khớp 100% nhé.
        assertTrue(serverResponse.contains("Hanh dong khong hop le"), "Phải chứa câu cảnh báo lỗi hành động");
    }

    @Test
    public void testRequireLoggedIn_BlocksUnauthorizedRequest() throws Exception {
        Socket mockSocket = mock(Socket.class);
        AuctionManager mockManager = mock(AuctionManager.class);

        // Gửi yêu cầu lấy số dư (GET_BALANCE) nhưng chưa hề có bước LOGIN trước đó
        String fakeClientRequest = "{\"action\":\"GET_BALANCE\",\"data\":\"{}\"}\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fakeClientRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);

        ClientHandler handler = new ClientHandler(mockSocket, mockManager);
        handler.run();

        String serverResponse = outputStream.toString();
        System.out.println("DEBUG - Unauthorized Request Response: [" + serverResponse + "]");

        // Kiểm tra xem hệ thống có chặn lại, báo FAIL và yêu cầu đăng nhập không
        assertTrue(serverResponse.contains("\"status\":\"FAIL\""), "Phải trả về FAIL status");
        assertTrue(serverResponse.contains("Vui long dang nhap truoc"), "Phải chứa câu yêu cầu đăng nhập");
    }
}
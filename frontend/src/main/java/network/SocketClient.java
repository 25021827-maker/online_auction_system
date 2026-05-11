package network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    private static final String SERVER_IP = "127.0.0.1"; // Đổi IP nếu Backend chạy ở máy khác
    private static final int SERVER_PORT = 8080; // Đảm bảo Backend của bạn đang chạy port này

    public static String sendRequest(String jsonPayload) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(jsonPayload);
            return in.readLine();

        } catch (Exception e) {
            return null; // Trả về null nếu không tìm thấy Server
        }
    }

    // Hàm main này chỉ để TEST xem đường truyền có thông không
    public static void main(String[] args) {
        System.out.println("Đang gửi thử yêu cầu Đăng nhập lên Backend...");

        // Gói tin mô phỏng đăng nhập
        String testJson = "{\"action\": \"LOGIN\", \"data\": \"{\\\"username\\\": \\\"admin\\\", \\\"password\\\": \\\"123\\\"}\"}";

        String response = sendRequest(testJson);

        if (response != null) {
            System.out.println("✅ MẠNG ĐÃ THÔNG! Server trả lời: " + response);
        } else {
            System.out.println("❌ THẤT BẠI! Hãy kiểm tra xem Backend đã bật chưa?");
        }
    }
}
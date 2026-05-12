package Service;

import network.SocketClient;

public class UserService {

    public boolean login(String u, String p) {
        try {
            // Tạo gói tin JSON gửi lên Backend
            String request = String.format("{\"action\": \"LOGIN\", \"data\": \"{\\\"username\\\": \\\"%s\\\", \\\"password\\\": \\\"%s\\\"}\"}", u, p);

            // Gọi qua đường ống mạng
            String response = SocketClient.sendRequest(request);

            // Nếu Server trả về SUCCESS thì cho phép đăng nhập
            if (response != null && response.contains("\"SUCCESS\"")) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String register(String u, String p) {
        try {
            // Tạo gói tin JSON đăng ký gửi lên Backend
            // ID tự tạo bằng thời gian hiện tại cho khỏi trùng
            long fakeId = System.currentTimeMillis();
            String request = String.format("{\"action\": \"REGISTER\", \"data\": \"{\\\"id\\\": \\\"%d\\\", \\\"username\\\": \\\"%s\\\", \\\"password\\\": \\\"%s\\\", \\\"role\\\": \\\"BIDDER\\\"}\"}", fakeId, u, p);

            String response = SocketClient.sendRequest(request);

            if (response != null && response.contains("\"SUCCESS\"")) {
                return "OK";
            }
            return "Username đã tồn tại hoặc lỗi Server";

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi kết nối mạng";
        }
    }
}
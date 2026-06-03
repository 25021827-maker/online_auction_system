package Service;

import com.google.gson.Gson;
import dto.LoginRequest;
import dto.RegisterRequest;
import dto.RequestPayload;
import network.SocketClient;

public class UserService {
    private final Gson gson = new Gson();

    public void login(String username, String password) {
        // 1. Tạo DTO chuẩn bị dữ liệu
        LoginRequest req = new LoginRequest();
        req.username = username;
        req.password = password;

        // 2. Bọc vào vỏ RequestPayload kèm Action "LOGIN"
        RequestPayload payload = new RequestPayload("LOGIN", gson.toJson(req));

        // 3. Gửi qua ống Socket
        SocketClient.getInstance().sendRequest(payload);
    }

    public void register(String username, String password, String email, String role) {
        // 1. Tạo DTO chuẩn bị dữ liệu
        RegisterRequest req = new RegisterRequest();
        req.username = username;
        req.password = password;
        req.email = email;
        req.role = role;

        // 2. Bọc vào vỏ RequestPayload kèm Action "REGISTER"
        RequestPayload payload = new RequestPayload("REGISTER", gson.toJson(req));

        // 3. Gửi qua ống Socket
        SocketClient.getInstance().sendRequest(payload);
    }
}
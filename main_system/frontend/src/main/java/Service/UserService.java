package Service;

import com.google.gson.Gson;
import dto.LoginRequest;
import dto.RegisterRequest;
import dto.RequestPayload;
import network.SocketClient;

public class UserService {
    private final Gson gson = new Gson();

    public boolean login(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.username = username;
        req.password = password;

        RequestPayload payload = new RequestPayload("LOGIN", gson.toJson(req));
        return SocketClient.getInstance().sendRequest(payload);
    }

    public boolean register(String username, String password, String email, String role) {
        RegisterRequest req = new RegisterRequest();
        req.username = username;
        req.password = password;
        req.email = email;
        req.role = role;

        RequestPayload payload = new RequestPayload("REGISTER", gson.toJson(req));
        return SocketClient.getInstance().sendRequest(payload);
    }
}
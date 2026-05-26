package Service;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import Session.UserSession;
import Model.User;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UserService {
    private Gson gson = new Gson();

    public CompletableFuture<Boolean> login(String username, String password) {
        Map<String, String> creds = Map.of("username", username, "password", password);
        return AuctionClient.getInstance()
                .sendRequest("LOGIN", creds)
                .thenApply(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        JsonObject userJson = gson.toJsonTree(response.getData()).getAsJsonObject();
                        String id = userJson.get("id").getAsString();
                        String uname = userJson.get("username").getAsString();
                        String role = userJson.get("role").getAsString();
                        double balance = userJson.get("balance").getAsDouble();
                        User user = new User(id, uname, "", role, balance);
                        UserSession.getInstance().setCurrentUser(user);
                        return true;
                    }
                    return false;
                });
    }

    public CompletableFuture<String> register(String username, String password, String email, String role) {
        Map<String, String> data = Map.of(
                "username", username,
                "password", password,
                "email", email == null ? "" : email,
                "role", role
        );
        return AuctionClient.getInstance()
                .sendRequest("REGISTER", data)
                .thenApply(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        return "OK";
                    } else {
                        return response.getData().toString();
                    }
                });
    }
}
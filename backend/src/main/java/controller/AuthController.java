package controller.auth;

import client.AuctionClient;
import com.google.gson.Gson;
import dto.ResponsePayload;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.UserService;
import util.EventBus;
import util.UserSession;

import java.util.Map;

public class LoginController {
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Label lblMessage;
    @FXML private Button btnLogin;
    @FXML private Label lblBalance; // hiển thị số dư trên thanh menu (nếu có)

    private UserService userService = new UserService();
    private Gson gson = new Gson();
    private AuctionClient client;

    @FXML
    private void initialize() {
        try {
            client = AuctionClient.getInstance();
            client.connect("localhost", 8080);
            client.setOnBroadcast(this::handleBroadcast);
        } catch (Exception e) {
            showMessage("Không thể kết nối đến server: " + e.getMessage());
            if (btnLogin != null) btnLogin.setDisable(true);
            lblMessage.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleLogin() {
        if (!client.isConnected()) {
            showMessage("Mất kết nối server. Vui lòng khởi động lại ứng dụng.");
            return;
        }

        String username = txtUser.getText();
        String password = txtPass.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        userService.login(username, password)
                .thenAccept(userInfo -> {
                    if (userInfo != null) {
                        UserSession session = UserSession.getInstance();
                        session.setUserId(userInfo.getId());
                        session.setUsername(userInfo.getUsername());
                        session.setBalance(userInfo.getBalance());
                        session.setRole(userInfo.getRole());

                        Platform.runLater(() -> {
                            if (lblBalance != null) {
                                lblBalance.setText(String.format("%.2f VND", userInfo.getBalance()));
                            }
                            openAuctionScreen();
                        });
                    } else {
                        Platform.runLater(() -> showMessage("Sai tài khoản hoặc mật khẩu"));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showMessage("Lỗi kết nối: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleBroadcast(ResponsePayload broadcast) {
        if (!"SUCCESS".equals(broadcast.getStatus())) return;
        Object data = broadcast.getData();
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            if ("BID_PLACED".equals(map.get("event"))) {
                Object newBalanceObj = map.get("newBalance");
                if (newBalanceObj instanceof Number) {
                    double newBalance = ((Number) newBalanceObj).doubleValue();
                    UserSession.getInstance().setBalance(newBalance);
                    Platform.runLater(() -> {
                        if (lblBalance != null) {
                            lblBalance.setText(String.format("%.2f VND", newBalance));
                        }
                    });
                    EventBus.getInstance().fireBalanceChanged(newBalance);
                }
            }
        }
    }

    private void openAuctionScreen() {
        try {
            Stage stage = (Stage) txtUser.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/product/AuctionMain.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Sàn đấu giá");
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Không thể mở màn hình đấu giá");
        }
    }

    @FXML
    private void handleRegister() throws Exception {
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/auth/Register.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Đăng ký");
    }

    private void showMessage(String msg) {
        if (lblMessage != null) lblMessage.setText(msg);
        else System.out.println(msg);
    }
}
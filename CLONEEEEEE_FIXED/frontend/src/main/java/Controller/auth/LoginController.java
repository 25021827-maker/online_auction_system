package Controller.auth;

import Service.UserService;
import Session.Session;
import Model.User;
import Service.core.SceneNavigator;
import network.SocketClient;
import dto.ResponsePayload;
import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

public class LoginController {

    @FXML
    private TextField txtUser;

    @FXML
    private PasswordField txtPass;

    private UserService service = new UserService();
    private Gson gson = new Gson();

    @FXML
    public void initialize() {
        // Lắng nghe tín hiệu "LOGIN_RESPONSE" từ Server
        SocketClient.getInstance().on("LOGIN_RESPONSE", this::handleLoginResponse);
    }

    @FXML
    private void handleLogin() {
        String user = txtUser.getText();
        String pass = txtPass.getText();

        if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // Gọi API Đăng nhập
        service.login(user, pass);
    }

    // Hàm callback tự động chạy khi Backend xử lý xong Đăng nhập
    private void handleLoginResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    // "Phiên dịch" JSON trả về thành Model User của giao diện
                    String userJson = gson.toJson(response.getData());

                    // Khai báo biến loggedUser (Biến này sẽ bao trùm toàn bộ các lệnh bên dưới)
                    User loggedUser = gson.fromJson(userJson, User.class);

                    // Lưu User vào phiên hoạt động
                    Session.setCurrentUser(loggedUser);
                    SocketClient.getInstance().startBalancePolling();

                    // Điều hướng sang trang chính dựa trên Role
                    if (loggedUser.getRole() != null && loggedUser.getRole().equalsIgnoreCase("ADMIN")) {
                        SceneNavigator.loadFromNode(txtUser, "/ui/user/AdminView.fxml", "Admin Dashboard");
                    } else {
                        SceneNavigator.loadFromNode(txtUser, "/ui/product/AuctionMain.fxml", "Sàn đấu giá");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể đọc dữ liệu từ Server.");
                    e.printStackTrace();
                }
            });
        } else {
            // Hiển thị thông báo thất bại (Sai pass/User không tồn tại)
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", response.getMessage());
            });
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        SceneNavigator.load(event, "/ui/auth/Register.fxml", "Đăng ký tài khoản");
    }

    // Hàm tiện ích hiển thị hộp thoại thông báo
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}

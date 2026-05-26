package Controller.auth;

<<<<<<< HEAD
import client.AuctionClient;
=======
import Service.UserService;
import Session.Session;
import Model.User;
import Service.core.SceneNavigator;

>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
import javafx.fxml.FXML;
import javafx.scene.control.*;
<<<<<<< HEAD
import javafx.stage.Stage;
import service.UserService;
=======
import javafx.event.ActionEvent;
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e

public class LoginController {
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Label lblMessage;
    @FXML private Button btnLogin;          // ← Thêm reference đến nút Login (có trong FXML)
    private UserService userService = new UserService();

    @FXML
<<<<<<< HEAD
    private void initialize() {
        try {
            AuctionClient.getInstance().connect("localhost", 8080);
            AuctionClient.getInstance().setOnBroadcast(this::handleBroadcast);
            // Kết nối thành công, đảm bảo nút được bật (mặc định đã bật)
        } catch (Exception e) {
            // Kết nối thất bại: disable nút Login, hiện thông báo đỏ
            showMessage("Không thể kết nối đến server: " + e.getMessage());
            if (btnLogin != null) {
                btnLogin.setDisable(true);
                btnLogin.setStyle("-fx-opacity: 0.6;");
            }
            lblMessage.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleLogin() {
        // Kiểm tra kết nối lần nữa (phòng trường hợp người dùng bật lại UI)
        if (!AuctionClient.getInstance().isConnected()) {
            showMessage("Mất kết nối server. Vui lòng thoát và khởi động lại ứng dụng.");
            return;
        }

        String username = txtUser.getText();
        String password = txtPass.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        userService.login(username, password)
                .thenAccept(success -> {
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            openAuctionScreen();
                        } else {
                            showMessage("Sai tài khoản hoặc mật khẩu");
                        }
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> showMessage("Lỗi kết nối: " + ex.getMessage()));
                    return null;
                });
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

    private void handleBroadcast(ResponsePayload payload) {
        System.out.println("Broadcast: " + payload);
        // Có thể cập nhật UI realtime tại đây, nhưng trong LoginController không cần thiết
=======
    private TextField txtUser;

    @FXML
    private PasswordField txtPass;

    private UserService service = new UserService();

    @FXML
    private void handleLogin() throws Exception {
        String user = txtUser.getText();
        String pass = txtPass.getText();

        User loggedUser = service.login(user, pass);

        if (loggedUser != null) {
            Session.currentUser = loggedUser;
            // Đăng nhập thành công -> Vào màn chính kích thước lớn 1280x750
            SceneNavigator.loadFromNode(txtUser, "/ui/product/AuctionMain.fxml", "Sàn đấu giá");
        } else {
            System.out.println("Sai tài khoản hoặc mật khẩu");
        }
    }

    // Thêm hàm này để fix lỗi sập Namespace tại dòng 65 trong file Login.fxml của bạn
    @FXML
    private void handleRegister(ActionEvent event) {
        SceneNavigator.load(event, "/ui/auth/Register.fxml", "Đăng ký tài khoản");
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
    }
}
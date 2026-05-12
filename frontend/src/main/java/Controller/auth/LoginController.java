package Controller.auth;

import Service.UserService;
import Session.Session;
import Model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField txtUser;

    @FXML
    private PasswordField txtPass;

    private UserService service = new UserService();

    @FXML
    private void handleLogin() throws Exception {
        String user = txtUser.getText();
        String pass = txtPass.getText();

        // 1. Gọi service kiểm tra, nó sẽ trả về true (đúng) hoặc false (sai)
        boolean isSuccess = service.login(user, pass);

        // 2. Kiểm tra kết quả
        if (isSuccess) {
            // Nếu thành công, tự tạo đối tượng User và lưu vào Session
            User loggedUser = new User(user, pass);
            Session.currentUser = loggedUser;

            // Chuyển sang màn hình Sàn đấu giá
            switchScene("/ui/product/AuctionMain.fxml", "Sàn đấu giá");
        } else {
            // Nếu thất bại (false) thì báo lỗi
            System.out.println("Sai tài khoản hoặc mật khẩu");
        }
    }

    private void switchScene(
            String fxmlPath,
            String title
    ) throws Exception {

        Stage stage = (Stage) txtUser
                .getScene()
                .getWindow();

        Parent root = FXMLLoader.load(
                getClass().getResource(fxmlPath)
        );

        stage.setScene(new Scene(root));
        stage.setTitle(title);
    }

    @FXML
    private void handleRegister() throws Exception {

        switchScene(
                "/ui/auth/Register.fxml",
                "Đăng ký"
        );
    }
}
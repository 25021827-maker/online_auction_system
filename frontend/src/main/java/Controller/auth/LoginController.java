package Controller.auth;

import Service.UserService;
import Session.Session;
import Model.User;
import Service.core.SceneNavigator;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

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
    }
}
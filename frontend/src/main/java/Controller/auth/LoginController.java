package Controller.auth;

import Service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    private UserService service = new UserService();

    @FXML
    private void handleLogin() throws Exception {
        String user = txtUser.getText();
        String pass = txtPass.getText();

        if (service.login(user, pass)) {
            switchScene("/ui/product/AuctionMain.fxml", "Sàn đấu giá");
        } else {
            System.out.println("Sai tài khoản hoặc mật khẩu");
        }
    }

    private void switchScene(String fxmlPath, String title) throws Exception {
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setScene(new Scene(root));
        stage.setTitle(title);
    }

    @FXML private void handleRegister() throws Exception
    {
        switchScene("/ui/auth/Register.fxml", "Đăng ký");
    }
}

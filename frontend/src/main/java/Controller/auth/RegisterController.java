package Controller.auth;

import Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

public class RegisterController {

    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private PasswordField txtConfirm;
    @FXML private Label lblMessage;
    private UserService service = new UserService();

    @FXML
    private void handleRegister() {
        String user = txtUser.getText();
        String pass = txtPass.getText();
        String confirm = txtConfirm.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            lblMessage.setText("Không được để trống");
            return;
        }

        if (!pass.equals(confirm)) {
            lblMessage.setText("Mật khẩu không khớp");
            return;
        }

        String result = service.register(user, pass);

        if (result.equals("OK")) {
            lblMessage.setText("Đăng ký thành công!");
        } else {
            lblMessage.setText(result);
        }
    }

    @FXML
    private void goToLogin() throws Exception {
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/auth/Login.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Đăng nhập");
    }
}

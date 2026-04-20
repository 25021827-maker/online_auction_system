package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;

    @FXML
    private void handleLogin() throws Exception {
        String user = txtUser.getText();
        String pass = txtPass.getText();

        if (user.equals("admin") && pass.equals("123")) {
            switchScene("/ui/AdminView.fxml", "Hệ thống Quản trị");
        } else if (!user.isEmpty()) {
            switchScene("/ui/AuctionMain.fxml", "Sàn Đấu Giá");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Sai tài khoản hoặc mật khẩu!");
            alert.show();
        }
    }

    private void switchScene(String fxmlPath, String title) throws Exception {
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setScene(new Scene(root));
        stage.setTitle(title);
    }

    @FXML private void handleRegister() { System.out.println("Chuyển sang màn hình đăng ký..."); }
}

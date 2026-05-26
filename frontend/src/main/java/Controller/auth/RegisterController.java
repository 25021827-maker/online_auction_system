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
    @FXML private TextField txtPassVisible;
    @FXML private PasswordField txtConfirm;
    @FXML private TextField txtConfirmVisible;
    @FXML private TextField txtEmail;
    @FXML private Label lblMessage;

    private boolean passVisible = false;
    private boolean confirmVisible = false;
    private UserService service = new UserService();

    @FXML
    private void handleRegister() {
        String user = txtUser.getText();
        String pass = passVisible ? txtPassVisible.getText() : txtPass.getText();
        String confirm = confirmVisible ? txtConfirmVisible.getText() : txtConfirm.getText();
        String email = txtEmail.getText();

        if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
            lblMessage.setText("Vui lòng nhập đầy đủ username, password và email");
            return;
        }
        if (!pass.equals(confirm)) {
            lblMessage.setText("Mật khẩu xác nhận không khớp");
            return;
        }

        service.register(user, pass, email, "BIDDER")
                .thenAccept(result -> {
                    javafx.application.Platform.runLater(() -> {
                        if ("OK".equals(result)) {
                            lblMessage.setText("Đăng ký thành công!");
                            new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.5), e -> {
                                try { goToLogin(); } catch (Exception ex) { ex.printStackTrace(); }
                            })).play();
                        } else {
                            lblMessage.setText(result);
                        }
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> lblMessage.setText("Lỗi: " + ex.getMessage()));
                    return null;
                });
    }

<<<<<<< HEAD
    @FXML private void goToLogin() throws Exception {
=======
    @FXML
    private void goToLogin() throws Exception {
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/auth/Login.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Đăng nhập");

        // Kích thước nhỏ gọn cho màn Auth
        stage.setWidth(450);
        stage.setHeight(600);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

<<<<<<< HEAD
    // Toggle password visibility (giữ nguyên code cũ nếu có)
    @FXML private void togglePassword() { /* giữ nguyên */ }
    @FXML private void toggleConfirm() { /* giữ nguyên */ }
}
=======

}
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e

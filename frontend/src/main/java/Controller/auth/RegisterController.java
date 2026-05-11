package Controller.auth;

import Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.List;
import FakeDB.FakeDB;

public class RegisterController {

    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private TextField txtPassVisible;
    @FXML private PasswordField txtConfirm;
    @FXML private TextField txtConfirmVisible;

    // ===== FIELD MỚI =====
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private ComboBox<String> cbNation;
    @FXML private ComboBox<String> cbAddress;

    @FXML private Label lblMessage;
    private boolean passVisible = false;
    private boolean confirmVisible = false;
    private Map<String, List<String>> countryData;
    private UserService service = new UserService();
    @FXML
    public void initialize() {
        loadCountryData();

        cbNation.getItems().addAll(countryData.keySet());

        cbNation.setOnAction(e -> {
            String nation = cbNation.getValue();

            if (nation != null) {
                cbAddress.getItems().setAll(countryData.get(nation));
            }
        });
    }
    private void loadCountryData() {
        try {
            Gson gson = new Gson();

            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();

            InputStreamReader reader = new InputStreamReader(
                    getClass().getResourceAsStream("/data/countries.json")
            );

            countryData = gson.fromJson(reader, type);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void togglePassword() {
        if (passVisible) {
            txtPass.setText(txtPassVisible.getText());
            txtPass.setVisible(true);
            txtPassVisible.setVisible(false);
        } else {
            txtPassVisible.setText(txtPass.getText());
            txtPass.setVisible(false);
            txtPassVisible.setVisible(true);
        }
        passVisible = !passVisible;
    }

    @FXML
    private void toggleConfirm() {
        if (confirmVisible) {
            txtConfirm.setText(txtConfirmVisible.getText());
            txtConfirm.setVisible(true);
            txtConfirmVisible.setVisible(false);
        } else {
            txtConfirmVisible.setText(txtConfirm.getText());
            txtConfirm.setVisible(false);
            txtConfirmVisible.setVisible(true);
        }
        confirmVisible = !confirmVisible;
    }

    @FXML
    private void handleRegister() {

        String user = txtUser.getText();
        String pass = passVisible ? txtPassVisible.getText() : txtPass.getText();
        String confirm = confirmVisible ? txtConfirmVisible.getText() : txtConfirm.getText();

        String email = txtEmail.getText();
        String phone = txtPhone.getText();
        String nation = cbNation.getValue();
        String address = cbAddress.getValue();

        // VALIDATE
        if (user.isEmpty() || pass.isEmpty()) {
            lblMessage.setText("Không được để trống");
            return;
        }

        if (!pass.equals(confirm)) {
            lblMessage.setText("Mật khẩu không khớp");
            return;
        }

        if (!email.contains("@")) {
            lblMessage.setText("Email không hợp lệ");
            return;
        }

        if (nation == null || address == null) {
            lblMessage.setText("Chọn quốc tịch và địa chỉ");
            return;
        }

        // 👉 TẠM THỜI (chưa sửa UserService)
        lblMessage.setText("Đăng ký thành công!");
        FakeDB.addUser(user, pass);
    }


    @FXML
    private void goToLogin() throws Exception {
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/auth/Login.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Đăng nhập");
    }
}

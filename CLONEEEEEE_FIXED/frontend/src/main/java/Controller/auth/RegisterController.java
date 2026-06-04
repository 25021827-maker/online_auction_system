package Controller.auth;

import Service.UserService;
import Service.core.SceneNavigator;
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
import network.SocketClient;
import dto.ResponsePayload;

public class RegisterController {

    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private TextField txtPassVisible;
    @FXML private PasswordField txtConfirm;
    @FXML private TextField txtConfirmVisible;

    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private ComboBox<String> cbNation;
    @FXML private ComboBox<String> cbAddress;

    @FXML private Label lblMessage;

    @FXML private ComboBox<String> cbRole;

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
        if (cbRole != null) {
            cbRole.getItems().addAll("Người mua (BIDDER)", "Người bán (SELLER)");
            cbRole.getSelectionModel().selectFirst();
        }
        SocketClient socketClient = SocketClient.getInstance();
        socketClient.clearListeners("REGISTER_RESPONSE");
        socketClient.on("REGISTER_RESPONSE", this::handleRegisterResponse);
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

        // VALIDATE ĐẦU VÀO
        if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
            lblMessage.setText("Không được để trống tài khoản và mật khẩu");
            return;
        }

        if (!pass.equals(confirm)) {
            lblMessage.setText("Mật khẩu không khớp");
            return;
        }

        if (email == null || !email.contains("@")) {
            lblMessage.setText("Email không hợp lệ");
            return;
        }

        if (nation == null || address == null) {
            lblMessage.setText("Vui lòng chọn quốc tịch và địa chỉ");
            return;
        }

        lblMessage.setText("Đang xử lý hệ thống...");
        lblMessage.setStyle("-fx-text-fill: gray;");

        // --- ĐOẠN MỚI THÊM: Lấy Role từ ComboBox ---
        String roleSelection = cbRole.getValue();
        String roleCode = "BIDDER"; // Đặt mặc định là người mua cho an toàn

        // Nếu người dùng chọn dòng có chữ SELLER, ta sẽ gán roleCode = "SELLER"
        if (roleSelection != null && roleSelection.contains("SELLER")) {
            roleCode = "SELLER";
        }

        // Gửi lệnh đăng ký qua mạng với Role đã chọn
        service.register(user, pass, email, roleCode);
    }

    // Hàm callback tự động chạy khi Backend xử lý xong Đăng ký
    private void handleRegisterResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            lblMessage.setText("Đăng ký thành công! Vui lòng quay lại Đăng nhập.");
            lblMessage.setStyle("-fx-text-fill: green;");
        } else {
            lblMessage.setText(response.getMessage());
            lblMessage.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void goToLogin() throws Exception {
        if (txtUser != null) {
            SceneNavigator.loadFromNode(txtUser, "/ui/auth/Login.fxml", "Dang nhap");
            return;
        }
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/auth/Login.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Đăng nhập");

        stage.setWidth(450);
        stage.setHeight(600);
        stage.setResizable(false);
        stage.centerOnScreen();
    }
}

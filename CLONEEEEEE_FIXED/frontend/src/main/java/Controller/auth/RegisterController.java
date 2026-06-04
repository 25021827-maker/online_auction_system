package Controller.auth;

import Service.UserService;
import Service.core.SceneNavigator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.ResponsePayload;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.SocketClient;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, List<String>> countryData = new HashMap<>();
    private final UserService service = new UserService();

    @FXML
    public void initialize() {
        loadCountryData();
        cbNation.getItems().addAll(countryData.keySet());
        cbNation.setOnAction(e -> {
            String nation = cbNation.getValue();
            if (nation != null && countryData.containsKey(nation)) {
                cbAddress.getItems().setAll(countryData.get(nation));
            }
        });

        if (cbRole != null) {
            cbRole.getItems().addAll("Buyer (BIDDER)", "Seller (SELLER)");
            cbRole.getSelectionModel().selectFirst();
        }

        clearMessage();
        SocketClient socketClient = SocketClient.getInstance();
        socketClient.clearListeners("REGISTER_RESPONSE");
        socketClient.on("REGISTER_RESPONSE", this::handleRegisterResponse);
    }

    private void loadCountryData() {
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/data/countries.json"))) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> data = gson.fromJson(reader, type);
            if (data != null) {
                countryData = data;
            }
        } catch (Exception e) {
            countryData = new HashMap<>();
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
        String user = value(txtUser.getText());
        String pass = value(passVisible ? txtPassVisible.getText() : txtPass.getText());
        String confirm = value(confirmVisible ? txtConfirmVisible.getText() : txtConfirm.getText());
        String email = value(txtEmail.getText());
        String nation = cbNation.getValue();
        String address = cbAddress.getValue();

        if (user.isEmpty() || pass.isEmpty()) {
            showMessage("Username and password are required.", false);
            return;
        }

        if (!pass.equals(confirm)) {
            showMessage("Password confirmation does not match.", false);
            return;
        }

        if (email.isEmpty() || !email.contains("@")) {
            showMessage("Email is not valid.", false);
            return;
        }

        if (nation == null || address == null) {
            showMessage("Please choose nationality and address.", false);
            return;
        }

        showMessage("Creating account...", true);

        String roleSelection = cbRole.getValue();
        String roleCode = roleSelection != null && roleSelection.contains("SELLER") ? "SELLER" : "BIDDER";
        service.register(user, pass, email, roleCode);
    }

    private void handleRegisterResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            showMessage("Account created. Please go back to login.", true);
        } else {
            showMessage("Cannot create account. Please check the information and try again.", false);
        }
    }

    @FXML
    private void goToLogin() throws Exception {
        if (txtUser != null) {
            SceneNavigator.loadFromNode(txtUser, "/ui/auth/Login.fxml", "Login");
            return;
        }
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/ui/auth/Login.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Login");
        stage.setWidth(450);
        stage.setHeight(600);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    private String value(String text) {
        return text == null ? "" : text.trim();
    }

    private void clearMessage() {
        if (lblMessage != null) {
            lblMessage.setText("");
        }
    }

    private void showMessage(String message, boolean success) {
        if (lblMessage == null) {
            return;
        }
        lblMessage.setText(message);
        lblMessage.setStyle(success ? "-fx-text-fill: #2ecc71;" : "-fx-text-fill: #ff6262;");
    }
}

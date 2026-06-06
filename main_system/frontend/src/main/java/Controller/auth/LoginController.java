package Controller.auth;

import Service.UserService;
import Session.Session;
import Model.User;
import Service.core.SceneNavigator;
import network.SocketClient;
import dto.ResponsePayload;
import com.google.gson.Gson;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.util.Duration;

public class LoginController {

    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Label lblMessage;
    @FXML private Button btnLogin;

    private final UserService service = new UserService();
    private final Gson gson = new Gson();
    private PauseTransition loginTimeout;
    private int loginAttemptId = 0;

    @FXML
    public void initialize() {
        SocketClient socketClient = SocketClient.getInstance();
        socketClient.clearListeners("LOGIN_RESPONSE");
        socketClient.on("LOGIN_RESPONSE", this::handleLoginResponse);

        hideMessage();
        txtUser.textProperty().addListener((obs, oldValue, newValue) -> hideMessage());
        txtPass.textProperty().addListener((obs, oldValue, newValue) -> hideMessage());
    }

    @FXML
    private void handleLogin() {
        String user = txtUser.getText() == null ? "" : txtUser.getText().trim();
        String pass = txtPass.getText() == null ? "" : txtPass.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showMessage("Please enter both username and password.", false);
            return;
        }

        setLoginBusy(true);
        showMessage("Checking credentials...", true);
        int attemptId = ++loginAttemptId;
        if (!service.login(user, pass)) {
            setLoginBusy(false);
            showMessage("Cannot connect to backend. Please start the server and try again.", false);
            return;
        }
        startLoginTimeout(attemptId);
    }

    private void handleLoginResponse(ResponsePayload response) {
        Platform.runLater(() -> {
            cancelLoginTimeout();
            if ("SUCCESS".equals(response.getStatus())) {
                try {
                    String userJson = gson.toJson(response.getData());
                    User loggedUser = gson.fromJson(userJson, User.class);
                    Session.setCurrentUser(loggedUser);

                    boolean navigated;
                    if (loggedUser.getRole() != null && loggedUser.getRole().equalsIgnoreCase("ADMIN")) {
                        navigated = SceneNavigator.loadFromNode(txtUser, "/ui/user/AdminView.fxml", "Admin Dashboard");
                    } else {
                        navigated = SceneNavigator.loadFromNode(txtUser, "/ui/product/AuctionMain.fxml", "Auction Home");
                    }

                    if (!navigated) {
                        setLoginBusy(false);
                        showMessage("Login succeeded, but the next screen could not be opened. Check the console error.", false);
                    }
                } catch (Exception e) {
                    setLoginBusy(false);
                    showMessage("Cannot read login data from server.", false);
                    e.printStackTrace();
                }
                return;
            }

            setLoginBusy(false);
            showMessage("Invalid username or password.", false);
        });
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        SceneNavigator.load(event, "/ui/auth/Register.fxml", "Register");
    }

    private void setLoginBusy(boolean busy) {
        if (btnLogin != null) {
            btnLogin.setDisable(busy);
            btnLogin.setText(busy ? "LOGGING IN..." : "LOG IN");
        }
    }

    private void startLoginTimeout(int attemptId) {
        cancelLoginTimeout();
        loginTimeout = new PauseTransition(Duration.seconds(8));
        loginTimeout.setOnFinished(event -> {
            if (attemptId == loginAttemptId && btnLogin != null && btnLogin.isDisabled()) {
                setLoginBusy(false);
                showMessage("Backend is not responding. Check that the server and database are running.", false);
            }
        });
        loginTimeout.play();
    }

    private void cancelLoginTimeout() {
        if (loginTimeout != null) {
            loginTimeout.stop();
            loginTimeout = null;
        }
    }

    private void showMessage(String message, boolean info) {
        if (lblMessage == null) {
            return;
        }
        lblMessage.setText(message);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
        lblMessage.getStyleClass().removeAll("login-message-error", "login-message-info");
        lblMessage.getStyleClass().add(info ? "login-message-info" : "login-message-error");
    }

    private void hideMessage() {
        if (lblMessage == null || (btnLogin != null && btnLogin.isDisabled())) {
            return;
        }
        lblMessage.setText("");
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}

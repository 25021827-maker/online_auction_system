package Controller.user;

import Service.core.SceneNavigator;
import Session.Session;
import com.google.gson.Gson;
import dto.DepositRequest;
import dto.RequestPayload;
import dto.ResponsePayload;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import network.SocketClient;

import java.io.InputStream;
import java.util.Optional;

public class ProfileController {

    @FXML private Label lblUsername;
    @FXML private Label lblBalance;
    @FXML private Label proofLabel;
    @FXML private ImageView qrImageView;
    @FXML private ImageView imgProfileAvatar;

    private final Gson gson = new Gson();
    private String proofImagePath = "";
    private double pendingAmount = 0;
    private Timeline balanceRefreshTimeline;

    @FXML
    public void initialize() {
        SocketClient.getInstance().on("SUBMIT_DEPOSIT_RESPONSE", this::handleDepositSubmitResponse);
        SocketClient.getInstance().on("BALANCE_UPDATE", this::handleBalanceUpdate);
        SocketClient.getInstance().on("GET_BALANCE_RESPONSE", this::handleBalanceUpdate);

        if (Session.getCurrentUser() != null) {
            lblUsername.setText(Session.getCurrentUser().getUsername());
            updateBalanceUI();
            setupProfileAvatar();
        }
        startBalanceRefresh();

        try (InputStream qrStream = getClass().getResourceAsStream("/images/myqr.png")) {
            if (qrStream != null) {
                qrImageView.setImage(new Image(qrStream));
            }
        } catch (Exception e) {
            System.out.println("Cannot load QR image: " + e.getMessage());
        }
    }

    private void updateBalanceUI() {
        if (Session.getCurrentUser() == null) {
            return;
        }
        lblBalance.setText("$" + String.format("%.2f", Session.getCurrentUser().getBalance()));
    }

    private void startBalanceRefresh() {
        requestBalanceRefresh();
        balanceRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> requestBalanceRefresh()));
        balanceRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        balanceRefreshTimeline.play();
    }

    private void requestBalanceRefresh() {
        if (Session.getCurrentUser() != null) {
            SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));
        }
    }

    private void setupProfileAvatar() {
        if (Session.getCurrentUser() == null || imgProfileAvatar == null) return;

        try {
            String userAvatarPath = Session.getCurrentUser().getAvatarPath();
            if (userAvatarPath != null && !userAvatarPath.isEmpty()) {
                imgProfileAvatar.setImage(new Image(userAvatarPath));
            } else {
                InputStream is = getClass().getResourceAsStream("/images/defaultavatar.png");
                if (is != null) imgProfileAvatar.setImage(new Image(is));
            }
        } catch (Exception e) {
            System.out.println("Cannot load avatar: " + e.getMessage());
        }

        double width = imgProfileAvatar.getFitWidth() > 0 ? imgProfileAvatar.getFitWidth() : 100;
        double height = imgProfileAvatar.getFitHeight() > 0 ? imgProfileAvatar.getFitHeight() : 100;
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imgProfileAvatar.setClip(clip);
    }

    @FXML
    private void handleAddMoney() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Deposit");
        dialog.setHeaderText("Enter the amount you transferred");
        dialog.setContentText("Amount ($):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double amount = Double.parseDouble(result.get());
                if (amount > 0) {
                    pendingAmount = amount;
                    proofLabel.setText("Pending proof upload for $" + String.format("%.2f", amount));
                }
            } catch (Exception e) {
                proofLabel.setText("Invalid amount.");
            }
        }
    }

    @FXML
    private void chooseProofImage() {
        proofImagePath = "uploaded_proof_bill.png";
        proofLabel.setText("Proof uploaded. Submit for admin approval.");
    }

    @FXML
    private void handleFakePayment() {
        if (pendingAmount <= 0 || proofImagePath.isEmpty()) {
            proofLabel.setText("Enter amount and upload proof first.");
            return;
        }

        DepositRequest deposit = new DepositRequest();
        deposit.userId = Session.getCurrentUser().getId();
        deposit.amount = pendingAmount;
        deposit.proofImagePath = proofImagePath;

        SocketClient.getInstance().sendRequest(new RequestPayload("SUBMIT_DEPOSIT", gson.toJson(deposit)));
    }

    private void handleDepositSubmitResponse(ResponsePayload response) {
        Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
                new Alert(Alert.AlertType.INFORMATION, "Deposit request submitted. Please wait for admin approval.").show();
                proofLabel.setText("Deposit request is pending admin approval.");
                pendingAmount = 0;
                proofImagePath = "";
            } else {
                proofLabel.setText("System error: " + response.getMessage());
            }
        });
    }

    private void handleBalanceUpdate(ResponsePayload response) {
        Platform.runLater(this::updateBalanceUI);
    }

    @FXML
    private void goHome(ActionEvent event) {
        if (balanceRefreshTimeline != null) {
            balanceRefreshTimeline.stop();
        }
        if (Session.getCurrentUser() != null
                && "ADMIN".equalsIgnoreCase(Session.getCurrentUser().getRole())) {
            SceneNavigator.loadFromNode(lblUsername, "/ui/user/AdminView.fxml", "Admin Dashboard");
        } else {
            SceneNavigator.loadFromNode(lblUsername, "/ui/product/AuctionMain.fxml", "San dau gia");
        }
    }

    @FXML
    private void handleUploadAvatar(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose avatar");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            String newAvatarPath = selectedFile.toURI().toString();
            Session.getCurrentUser().setAvatarPath(newAvatarPath);
            imgProfileAvatar.setImage(new Image(newAvatarPath));
        }
    }
}

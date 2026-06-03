package Controller.user;

import Service.core.SceneNavigator;
import Session.Session;
import com.google.gson.Gson;
import dto.DepositRequest;
import dto.RequestPayload;
import dto.ResponsePayload;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
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

    @FXML
    public void initialize() {
        SocketClient.getInstance().on("SUBMIT_DEPOSIT_RESPONSE", this::handleDepositSubmitResponse);

        if (Session.currentUser != null) {
            lblUsername.setText(Session.currentUser.getUsername());
            updateBalanceUI();
            setupProfileAvatar();
        }

        try (InputStream qrStream = getClass().getResourceAsStream("/images/myqr.png")) {
            if (qrStream != null) {
                qrImageView.setImage(new Image(qrStream));
            }
        } catch (Exception e) {
            System.out.println("Cannot load QR image: " + e.getMessage());
        }
    }

    private void updateBalanceUI() {
        lblBalance.setText("$" + String.format("%.2f", Session.currentUser.getBalance()));
    }

    private void setupProfileAvatar() {
        if (Session.currentUser == null || imgProfileAvatar == null) return;

        try {
            String userAvatarPath = Session.currentUser.getAvatarPath();
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
        deposit.userId = Session.currentUser.getId();
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

    @FXML
    private void goHome(ActionEvent event) {
        if (Session.currentUser != null
                && "ADMIN".equalsIgnoreCase(Session.currentUser.getRole())) {
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
            Session.currentUser.setAvatarPath(newAvatarPath);
            imgProfileAvatar.setImage(new Image(newAvatarPath));
        }
    }
}

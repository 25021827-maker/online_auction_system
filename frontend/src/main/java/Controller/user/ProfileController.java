package Controller.user;

import FakeDB.FakeDB;

import Model.Product;

import Session.Session;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;

import javafx.event.ActionEvent;

import javafx.stage.Stage;

import javafx.stage.FileChooser;

import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import javafx.scene.layout.VBox;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

import java.util.Optional;

public class ProfileController {

    @FXML
    private Label lblUsername;

    @FXML
    private Label lblBalance;

    @FXML
    private Label proofLabel;

    @FXML
    private VBox myProductsBox;

    @FXML
    private ImageView qrImageView;

    // ảnh xác minh
    private String proofImagePath = "";

    // số tiền pending
    private double pendingAmount = 0;

    @FXML
    public void initialize() {

        // USERNAME
        lblUsername.setText(
                Session.currentUser.getUsername()
        );

        // BALANCE
        updateBalance();

        // PRODUCTS
        loadMyProducts();

        // LOAD QR IMAGE
        try {

            qrImageView.setImage(

                    new Image(

                            getClass().getResourceAsStream(
                                    "/images/myqr.png"
                            )
                    )
            );

        } catch (Exception e) {

            System.out.println("Không tìm thấy QR");
        }
    }

    // =========================
    // UPDATE BALANCE
    // =========================
    private void updateBalance() {

        lblBalance.setText(

                "$"
                        + String.format(
                        "%.2f",
                        Session.currentUser.getBalance()
                )
        );
    }

    // =========================
    // INPUT MONEY
    // =========================
    @FXML
    private void handleAddMoney() {

        TextInputDialog dialog =
                new TextInputDialog();

        dialog.setTitle("Nạp tiền");

        dialog.setHeaderText(
                "Nhập số tiền muốn nạp"
        );

        dialog.setContentText(
                "Amount:"
        );

        Optional<String> result =
                dialog.showAndWait();

        if (result.isPresent()) {

            try {

                double amount =
                        Double.parseDouble(
                                result.get()
                        );

                if (amount > 0) {

                    pendingAmount = amount;

                    proofLabel.setText(

                            "Đang chờ xác minh cho "
                                    + amount
                                    + "$"

                    );
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    // =========================
    // CHOOSE PROOF IMAGE
    // =========================
    @FXML
    private void chooseProofImage() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Chọn ảnh xác minh"
        );

        fileChooser.getExtensionFilters().add(

                new FileChooser.ExtensionFilter(

                        "Image Files",

                        "*.png",
                        "*.jpg",
                        "*.jpeg"

                )
        );

        File file =
                fileChooser.showOpenDialog(null);

        if (file != null) {

            proofImagePath =
                    file.toURI().toString();

            proofLabel.setText(

                    "Đã tải ảnh xác minh"

            );
        }
    }

    // =========================
    // FAKE PAYMENT
    // =========================
    @FXML
    private void handleFakePayment() {

        // chưa nhập tiền
        if (pendingAmount <= 0) {

            proofLabel.setText(
                    "Hãy nhập số tiền trước"
            );

            return;
        }

        // chưa upload ảnh
        if (proofImagePath.isEmpty()) {

            proofLabel.setText(
                    "Phải tải ảnh xác minh"
            );

            return;
        }

        // cộng tiền
        Session.currentUser.addMoney(
                pendingAmount
        );

        updateBalance();

        proofLabel.setText(

                "Nạp "
                        + pendingAmount
                        + "$ thành công"

        );

        // reset
        pendingAmount = 0;

        proofImagePath = "";
    }

    // =========================
    // LOAD PRODUCTS
    // =========================
    private void loadMyProducts() {

        myProductsBox.getChildren().clear();

        for (Product p : FakeDB.getProducts()) {

            if (p.getSeller().equals(
                    Session.currentUser.getUsername()
            )) {

                Label label = new Label(

                        p.getTitle()
                                + " - $"
                                + p.getCurrentPrice()

                );

                myProductsBox.getChildren()
                        .add(label);
            }
        }
    }

    // =========================
    // BACK HOME
    // =========================
    @FXML
    private void goHome(ActionEvent event) {

        try {

            Stage stage = (Stage)

                    ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            Parent root = FXMLLoader.load(

                    getClass().getResource(
                            "/ui/product/AuctionMain.fxml"
                    )
            );

            stage.setScene(
                    new Scene(root)
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}

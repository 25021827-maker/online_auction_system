package Controller.product;

import FakeDB.FakeDB;

import Model.Product;
import Model.User;

import Session.Session;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.stage.Stage;

public class ProductController {

    @FXML
    private Label nameLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label sellerLabel;

    @FXML
    private Label highestBidderLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label lblMessage;

    @FXML
    private TextField txtBid;

    @FXML
    private ImageView imageView;

    private Product currentProduct;

    // =========================
    // SET DATA
    // =========================
    public void setData(Product p) {

        this.currentProduct = p;

        // PRODUCT NAME
        nameLabel.setText(
                p.getTitle()
        );

        // CURRENT PRICE
        priceLabel.setText(
                "Giá hiện tại: $"
                        + p.getCurrentPrice()
        );

        // SELLER
        sellerLabel.setText(
                "Người bán: "
                        + p.getSeller()
        );

        // HIGHEST BIDDER
        updateHighestBidderLabel();

        // USER BALANCE
        updateBalanceLabel();

        // IMAGE
        if (p.getImagePath() != null
                && !p.getImagePath().isEmpty()) {

            imageView.setImage(
                    new Image(p.getImagePath())
            );
        }
    }

    // =========================
    // UPDATE BALANCE LABEL
    // =========================
    private void updateBalanceLabel() {

        balanceLabel.setText(
                "Số dư của bạn: $"
                        + String.format(
                        "%.2f",
                        Session.currentUser.getBalance()
                )
        );
    }

    // =========================
    // UPDATE HIGHEST BIDDER
    // =========================
    private void updateHighestBidderLabel() {

        if (currentProduct.getHighestBidder() == null
                || currentProduct.getHighestBidder().isEmpty()) {

            highestBidderLabel.setText(
                    "Người giữ giá cao nhất: Chưa có"
            );

        } else {

            highestBidderLabel.setText(
                    "Người giữ giá cao nhất: "
                            + currentProduct.getHighestBidder()
            );
        }
    }

    // =========================
    // HANDLE BID
    // =========================
    @FXML
    private void handleBid() {

        try {

            double bidAmount = Double.parseDouble(
                    txtBid.getText()
            );

            // =========================
            // CHECK BID > CURRENT
            // =========================
            if (bidAmount <= currentProduct.getCurrentPrice()) {

                lblMessage.setText(
                        "Bid phải lớn hơn giá hiện tại"
                );

                return;
            }

            // =========================
            // CHECK BALANCE
            // =========================
            if (Session.currentUser.getBalance()
                    < bidAmount) {

                lblMessage.setText(
                        "Không đủ số dư"
                );

                return;
            }

            // =========================
            // CANNOT BID OWN PRODUCT
            // =========================
            if (currentProduct.getSeller().equals(
                    Session.currentUser.getUsername()
            )) {

                lblMessage.setText(
                        "Không thể bid sản phẩm của mình"
                );

                return;
            }

            // =========================
            // REFUND OLD BIDDER
            // =========================
            String oldBidder =
                    currentProduct.getHighestBidder();

            if (oldBidder != null
                    && !oldBidder.isEmpty()) {

                User oldUser =
                        FakeDB.getUserByUsername(
                                oldBidder
                        );

                if (oldUser != null) {

                    oldUser.addMoney(
                            currentProduct.getCurrentPrice()
                    );
                }
            }

            // =========================
            // DEDUCT MONEY
            // =========================
            Session.currentUser.deductMoney(
                    bidAmount
            );

            // =========================
            // UPDATE PRODUCT
            // =========================
            currentProduct.setCurrentPrice(
                    bidAmount
            );

            currentProduct.setHighestBidder(
                    Session.currentUser.getUsername()
            );

            // =========================
            // UPDATE UI
            // =========================
            priceLabel.setText(
                    "Giá hiện tại: $"
                            + bidAmount
            );

            updateHighestBidderLabel();

            updateBalanceLabel();

            lblMessage.setText(
                    "Bid thành công!"
            );

            // CLEAR INPUT
            txtBid.clear();

        } catch (Exception e) {

            lblMessage.setText(
                    "Bid không hợp lệ"
            );
        }
    }

    // =========================
    // GO BACK
    // =========================
    @FXML
    private void goBack() throws Exception {

        Stage stage = (Stage)
                nameLabel.getScene().getWindow();

        Parent root = FXMLLoader.load(

                getClass().getResource(
                        "/ui/product/AuctionMain.fxml"
                )
        );

        stage.setScene(
                new Scene(root)
        );
    }
}

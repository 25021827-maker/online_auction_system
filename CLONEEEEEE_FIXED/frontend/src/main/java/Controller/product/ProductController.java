package Controller.product;

import Model.Product;
import Service.core.SceneNavigator;
import Session.Session;
import dto.BidRequest;
import dto.RequestPayload;
import dto.ResponsePayload;
import network.SocketClient;
import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class ProductController {

    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label balanceLabel;
    @FXML private Label lblMessage;
    @FXML private TextField txtBid;
    @FXML private Button bidButton;
    @FXML private ImageView imageView;

    private Product currentProduct;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        // Đăng ký nhận kết quả khi chính mình đặt giá
        SocketClient.getInstance().on("PLACE_BID_RESPONSE", this::handleBidResponse);

        // Đăng ký nhận thông báo Real-time khi người khác đặt giá
        SocketClient.getInstance().on("NEW_BID_EVENT", this::handleRealtimeBid);
        SocketClient.getInstance().on("BALANCE_UPDATE", response -> updateBalanceLabel());
        SocketClient.getInstance().on("GET_BALANCE_RESPONSE", response -> updateBalanceLabel());
    }

    public void setData(Product p) {
        this.currentProduct = p;

        nameLabel.setText(p.getTitle());
        priceLabel.setText("Giá hiện tại: $" + p.getCurrentPrice());
        sellerLabel.setText("Người bán: " + p.getSeller());
        updateHighestBidderLabel();
        updateBalanceLabel();

        String status = p.getStatus();
        if (!(status.equals("OPEN") || status.equals("RUNNING"))) {
            txtBid.setDisable(true);
            bidButton.setDisable(true);
            if (status.equals("SCHEDULED")) {
                lblMessage.setText("Phiên đấu giá chưa bắt đầu");
            } else {
                lblMessage.setText("Phiên đấu giá đã kết thúc");
            }
        }

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try { imageView.setImage(new Image(p.getImagePath())); } catch (Exception e) {}
        }
    }

    private void updateBalanceLabel() {
        if (Session.getCurrentUser() != null) {
            balanceLabel.setText("Số dư của bạn: $" + String.format("%.2f", Session.getCurrentUser().getBalance()));
        }
    }

    private void updateHighestBidderLabel() {
        if (currentProduct.getHighestBidder() == null || currentProduct.getHighestBidder().isEmpty()) {
            highestBidderLabel.setText("Người giữ giá cao nhất: Chưa có");
        } else {
            highestBidderLabel.setText("Người giữ giá cao nhất: " + currentProduct.getHighestBidder());
        }
    }

    @FXML
    private void handleBid() {
        try {
            if (!(currentProduct.getStatus().equals("OPEN") || currentProduct.getStatus().equals("RUNNING"))) {
                lblMessage.setText("Phiên đấu giá chưa mở hoặc đã kết thúc");
                return;
            }

            double bidAmount = Double.parseDouble(txtBid.getText());
            lblMessage.setText("Đang gửi yêu cầu đặt giá...");

            // Đóng gói DTO gửi lên Server
            BidRequest req = new BidRequest();
            req.auctionId = (long) currentProduct.getId();
            req.bidderId = (long) Session.getCurrentUser().getId(); // Sử dụng ID vừa thêm vào User.java
            req.amount = bidAmount;

            RequestPayload payload = new RequestPayload("PLACE_BID", gson.toJson(req));
            SocketClient.getInstance().sendRequest(payload);

        } catch (Exception e) {
            lblMessage.setText("Bid không hợp lệ");
        }
    }

    // Hàm callback khi nhận kết quả đặt giá từ Server
    private void handleBidResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            lblMessage.setText("Bid thành công!");

            // Trừ tiền tạm trên giao diện (Đồng bộ chuẩn sẽ dựa vào API lấy Profile sau)
            try {
                double bidAmount = Double.parseDouble(txtBid.getText());
                Session.getCurrentUser().deductMoney(bidAmount);
                updateBalanceLabel();
                txtBid.clear();
            } catch (Exception e) {}
        } else {
            lblMessage.setText(response.getMessage());
        }
    }

    // Hàm callback cập nhật giá Real-time khi ai đó đặt giá
    private void handleRealtimeBid(ResponsePayload response) {
        try {
            BidRequest newBid = gson.fromJson(gson.toJson(response.getData()), BidRequest.class);
            if (newBid != null && newBid.auctionId == currentProduct.getId()) {
                currentProduct.setCurrentPrice(newBid.amount);
                currentProduct.setHighestBidder("User#" + newBid.bidderId);

                // Bắt buộc đẩy vào Platform.runLater để vẽ lại giao diện
                Platform.runLater(() -> {
                    priceLabel.setText("Giá hiện tại: $" + newBid.amount);
                    updateHighestBidderLabel();
                });
            }
        } catch (Exception e) {}
    }

    @FXML
    private void goBack() throws Exception {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/product/AuctionMain.fxml"));
        SceneNavigator.showFixedFullScreen(stage, root, "San dau gia");
    }
}

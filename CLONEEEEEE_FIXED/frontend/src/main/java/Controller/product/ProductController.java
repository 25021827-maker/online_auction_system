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

        SocketClient socketClient = SocketClient.getInstance();
        socketClient.clearListeners("PLACE_BID_RESPONSE");
        socketClient.clearListeners("NEW_BID_EVENT");
        socketClient.clearListeners("AUCTION_PRICE_CHANGED");
        socketClient.clearListeners("BALANCE_UPDATE");
        socketClient.clearListeners("GET_BALANCE_RESPONSE");
        socketClient.on("PLACE_BID_RESPONSE", this::handleBidResponse);

        socketClient.on("NEW_BID_EVENT", this::handleRealtimeBid);
        socketClient.on("AUCTION_PRICE_CHANGED", this::handleRealtimeBid);
        socketClient.on("BALANCE_UPDATE", response -> updateBalanceLabel());
        socketClient.on("GET_BALANCE_RESPONSE", response -> updateBalanceLabel());
    }

    public void setData(Product p) {
        this.currentProduct = p;

        nameLabel.setText(p.getTitle());
        priceLabel.setText("GiÃ¡ hiá»‡n táº¡i: $" + p.getCurrentPrice());
        sellerLabel.setText("NgÆ°á»i bÃ¡n: " + p.getSeller());
        updateHighestBidderLabel();
        updateBalanceLabel();
        SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));

        String status = p.getStatus();
        if (!(status.equals("OPEN") || status.equals("RUNNING"))) {
            txtBid.setDisable(true);
            bidButton.setDisable(true);
            if (status.equals("SCHEDULED")) {
                lblMessage.setText("PhiÃªn Ä‘áº¥u giÃ¡ chÆ°a báº¯t Ä‘áº§u");
            } else {
                lblMessage.setText("PhiÃªn Ä‘áº¥u giÃ¡ Ä‘Ã£ káº¿t thÃºc");
            }
        }

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try { imageView.setImage(new Image(p.getImagePath())); } catch (Exception e) {}
        }
    }

    private void updateBalanceLabel() {
        if (Session.getCurrentUser() != null) {
            double held = Math.max(0, Session.getCurrentUser().getBalance() - Session.getCurrentUser().getAvailableBalance());
            balanceLabel.setText("Balance: $"
                    + String.format("%.2f", Session.getCurrentUser().getBalance())
                    + " | Available: $"
                    + String.format("%.2f", Session.getCurrentUser().getAvailableBalance())
                    + " | Held: $"
                    + String.format("%.2f", held));
        }
    }

    private void updateHighestBidderLabel() {
        if (currentProduct.getHighestBidder() == null || currentProduct.getHighestBidder().isEmpty()) {
            highestBidderLabel.setText("NgÆ°á»i giá»¯ giÃ¡ cao nháº¥t: ChÆ°a cÃ³");
        } else {
            highestBidderLabel.setText("NgÆ°á»i giá»¯ giÃ¡ cao nháº¥t: " + currentProduct.getHighestBidder());
        }
    }

    @FXML
    private void handleBid() {
        try {
            if (!(currentProduct.getStatus().equals("OPEN") || currentProduct.getStatus().equals("RUNNING"))) {
                lblMessage.setText("Phien dau gia chua mo hoac da ket thuc");
                return;
            }
            if (Session.getCurrentUser() == null) {
                lblMessage.setText("Ban can dang nhap de dat gia.");
                return;
            }

            double bidAmount = Double.parseDouble(txtBid.getText());
            if (bidAmount > Session.getCurrentUser().getAvailableBalance()) {
                lblMessage.setText("Khong du so du kha dung de dat gia.");
                return;
            }
            lblMessage.setText("Dang gui yeu cau dat gia...");

            BidRequest req = new BidRequest();
            req.auctionId = (long) currentProduct.getId();
            req.bidderId = Session.getCurrentUser().getId();
            req.amount = bidAmount;

            RequestPayload payload = new RequestPayload("PLACE_BID", gson.toJson(req));
            SocketClient.getInstance().sendRequest(payload);
        } catch (Exception e) {
            lblMessage.setText("Bid khong hop le");
        }
    }

    // HÃ m callback khi nháº­n káº¿t quáº£ Ä‘áº·t giÃ¡ tá»« Server
    private void handleBidResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            lblMessage.setText("Bid thanh cong!");
            txtBid.clear();
            SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));
        } else {
            lblMessage.setText(response.getMessage());
        }
    }

    // HÃ m callback cáº­p nháº­t giÃ¡ Real-time khi ai Ä‘Ã³ Ä‘áº·t giÃ¡
    private void handleRealtimeBid(ResponsePayload response) {
        try {
            BidRequest newBid = gson.fromJson(gson.toJson(response.getData()), BidRequest.class);
            if (newBid != null && newBid.auctionId == currentProduct.getId()) {
                currentProduct.setCurrentPrice(newBid.amount);
                currentProduct.setHighestBidder("User#" + newBid.bidderId);

                // Báº¯t buá»™c Ä‘áº©y vÃ o Platform.runLater Ä‘á»ƒ váº½ láº¡i giao diá»‡n
                Platform.runLater(() -> {
                    priceLabel.setText("GiÃ¡ hiá»‡n táº¡i: $" + newBid.amount);
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


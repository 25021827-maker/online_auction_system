package Controller.product;

import Model.Bid;
import Model.Product;
import Service.auction.AuctionRoomManager;
import Service.core.SceneNavigator;
import Session.Session;
import network.SocketClient;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.BidRequest;
import dto.AutoBidRequest;
import com.google.gson.Gson;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AuctionRoomController {

    @FXML private Label countdownLabel, statusLabel, viewerCountLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label nameLabel, priceLabel, sellerLabel, highestBidderLabel, balanceLabel, lblMessage, winnerLabel, viewerLabel, idLabel;
    @FXML private TextField txtBid;
    @FXML private TextField txtAutoMax, txtAutoStep;
    @FXML private Button bidButton;
    @FXML private ListView<String> bidHistoryList;
    @FXML private ImageView imageView;
    @FXML private LineChart<String, Number> priceChart;

    private Product currentProduct;
    private Timeline refreshTimeline;
    private XYChart.Series<String, Number> bidSeries;
    private int lastBidCount = 0;
    private boolean roomJoined = false;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        SocketClient.getInstance().on("PLACE_BID_RESPONSE", this::handleBidResponse);
        SocketClient.getInstance().on("BID_UPDATE", this::handleRealtimeBid);
        SocketClient.getInstance().on("SET_AUTO_BID_RESPONSE", this::handleAutoBidResponse);

        // ĐÃ THÊM: Đăng ký nghe dữ liệu lịch sử đặt giá từ Server
        SocketClient.getInstance().on("GET_BID_HISTORY_RESPONSE", this::handleLoadBidHistory);
    }
    public void setData(Product p) {
        currentProduct = p;
        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try { imageView.setImage(new Image(p.getImagePath())); } catch (Exception e) {}
        }

        setupChart();
        loadChartHistory();
        updateAllUI();

        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(1000), e -> updateAllUI()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        Platform.runLater(() -> {
            Stage stage = (Stage) nameLabel.getScene().getWindow();
            AuctionRoomManager.joinRoom(currentProduct.getId());
            roomJoined = true;
            stage.setOnCloseRequest(e -> cleanupRoom());
        });
        // ĐÃ THÊM: Vừa vào phòng là bắn ngay lệnh xin lịch sử giá của phiên này
        RequestPayload req = new RequestPayload("GET_BID_HISTORY", "{\"auctionId\":" + currentProduct.getId() + "}");
        SocketClient.getInstance().sendRequest(req);
    }

    private void updateAllUI() {
        nameLabel.setText(currentProduct.getTitle());
        priceLabel.setText("$ " + currentProduct.getCurrentPrice());
        sellerLabel.setText("SELLER: " + currentProduct.getSeller());
        descriptionArea.setText(currentProduct.getDescription());
        if (idLabel != null) idLabel.setText("PRODUCT ID: #" + currentProduct.getId());
        statusLabel.setText(currentProduct.getStatus());
        countdownLabel.setText(currentProduct.getTimeRemaining());
        updateHighestBidder();
        updateBalance();
        updateBidHistory();
        updateAuctionStatus();
        updateViewerCount();
    }

    private void updateViewerCount() {
        viewerLabel.setText("LIVE VIEWERS");
        viewerCountLabel.setText(AuctionRoomManager.getViewerCount(currentProduct.getId()) + " watching");
    }

    private void updateBalance() {
        balanceLabel.setText("YOUR BALANCE: $" + String.format("%.2f", Session.currentUser.getBalance()));
    }

    private void updateHighestBidder() {
        String bidder = currentProduct.getHighestBidder();
        highestBidderLabel.setText((bidder == null || bidder.isEmpty()) ? "HIGHEST BIDDER: None" : "HIGHEST BIDDER: " + bidder);
    }

    private void updateBidHistory() {
        bidHistoryList.getItems().clear();
        for (Bid bid : currentProduct.getBidHistory()) {
            bidHistoryList.getItems().add(bid.toString());
        }
        if (!bidHistoryList.getItems().isEmpty()) {
            bidHistoryList.scrollTo(bidHistoryList.getItems().size() - 1);
        }
    }

    private void updateAuctionStatus() {
        String status = currentProduct.getStatus();
        boolean open = status.equals("OPEN") || status.equals("RUNNING");
        txtBid.setDisable(!open);
        bidButton.setDisable(!open);
        if (txtAutoMax != null) txtAutoMax.setDisable(!open);
        if (txtAutoStep != null) txtAutoStep.setDisable(!open);

        if (status.equals("SCHEDULED")) {
            lblMessage.setText("Auction has not started yet");
        } else if (status.equals("FINISHED") || status.equals("SOLD")) {
            lblMessage.setText("Auction has ended");
            String winner = currentProduct.getHighestBidder();
            winnerLabel.setText((winner == null || winner.isEmpty()) ? "No winner" : "Winner: " + winner);
        } else {
            // Không xóa lblMessage để giữ nguyên thông báo thành công/lỗi
            winnerLabel.setText("");
        }
    }

    // --- ĐẨY LOGIC LÊN SERVER ---
    @FXML
    private void handleBid() {
        try {
            double bidAmount = Double.parseDouble(txtBid.getText());
            lblMessage.setText("Đang gửi yêu cầu...");

            // Đóng gói DTO gửi lên Server (Mọi logic trừ tiền, kiểm tra giá, refund đều do MySQL lo)
            BidRequest req = new BidRequest();
            req.auctionId = (long) currentProduct.getId();

            // Ép kiểu an toàn cho ID (bạn cần có ID trong class User, tạm giả định lấy qua Username hoặc thuộc tính)
            req.bidderId = (long) Session.currentUser.getId();// LƯU Ý: Chỗ này bạn cần lấy ID thực tế của User từ Session.currentUser.getId()
            req.amount = bidAmount;

            RequestPayload payload = new RequestPayload("PLACE_BID", gson.toJson(req));
            SocketClient.getInstance().sendRequest(payload);

        } catch (Exception e) {
            lblMessage.setText("Invalid bid amount");
        }
    }

    // Xử lý khi Backend trả kết quả cho cú click Đặt giá của chính mình
    @FXML
    private void handleAutoBid() {
        try {
            if (currentProduct == null || Session.currentUser == null) {
                lblMessage.setText("Cannot set auto bid right now");
                return;
            }

            double maxAmount = Double.parseDouble(txtAutoMax.getText().trim());
            double step = Double.parseDouble(txtAutoStep.getText().trim());
            if (maxAmount <= currentProduct.getCurrentPrice() || step <= 0) {
                lblMessage.setText("Auto bid values are not valid");
                return;
            }

            AutoBidRequest req = new AutoBidRequest();
            req.auctionId = (long) currentProduct.getId();
            req.bidderId = Session.currentUser.getId();
            req.maxAmount = maxAmount;
            req.incrementStep = step;

            RequestPayload payload = new RequestPayload("SET_AUTO_BID", gson.toJson(req));
            SocketClient.getInstance().sendRequest(payload);
            lblMessage.setText("Setting auto bid...");
        } catch (Exception e) {
            lblMessage.setText("Invalid auto bid values");
        }
    }

    private void handleBidResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            lblMessage.setText("Bid successful!");
            txtBid.clear();
            updateBalance();
        } else {
            lblMessage.setText(response.getMessage()); // Hiển thị lỗi từ Server
        }
    }

    // Xử lý khi có ai đó (hoặc chính mình) vừa đặt giá thành công
    private void handleAutoBidResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            lblMessage.setText("Auto bid enabled");
            txtAutoMax.clear();
            txtAutoStep.clear();
        } else {
            lblMessage.setText(response.getMessage());
        }
    }

    private void handleRealtimeBid(ResponsePayload response) {
        try {
            BidRequest newBid = gson.fromJson(gson.toJson(response.getData()), BidRequest.class);
            // Kiểm tra xem luồng bid này có thuộc về căn phòng hiện tại không
            if (newBid != null && newBid.auctionId == currentProduct.getId()) {
                currentProduct.setCurrentPrice(newBid.amount);
                String bidderName = "User#" + newBid.bidderId + (newBid.autoBid ? " (AUTO)" : "");
                currentProduct.setHighestBidder(bidderName);
                currentProduct.addBid(new Bid(bidderName, newBid.amount));
                updateChartIfNeeded();
            }
        } catch (Exception e) {}
    }

    private void setupChart() {
        bidSeries = new XYChart.Series<>();
        priceChart.getData().clear();
        priceChart.getData().add(bidSeries);
        priceChart.setAnimated(false);
        CategoryAxis xAxis = (CategoryAxis) priceChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
        yAxis.setForceZeroInRange(false);
        yAxis.setAutoRanging(true);
    }

    private void loadChartHistory() {
        bidSeries.getData().clear();
        int count = 1;
        for (Bid bid : currentProduct.getBidHistory()) {
            bidSeries.getData().add(new XYChart.Data<>("Bid #" + (count++), bid.getAmount()));
        }
        lastBidCount = currentProduct.getBidHistory().size();
    }

    private void updateChartIfNeeded() {
        int currentSize = currentProduct.getBidHistory().size();
        if (currentSize > lastBidCount) {
            Bid latest = currentProduct.getBidHistory().get(currentSize - 1);
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            Platform.runLater(() -> {
                bidSeries.getData().add(new XYChart.Data<>(timestamp, latest.getAmount()));
                if (bidSeries.getData().size() > 10) bidSeries.getData().remove(0);
            });
            lastBidCount = currentSize;
        }
    }

    @FXML
    private void goBack() throws Exception {
        cleanupRoom();
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/product/AuctionMain.fxml"));
        SceneNavigator.showFixedFullScreen(stage, root, "San dau gia");
    }

    private void cleanupRoom() {
        if (currentProduct != null && roomJoined) {
            AuctionRoomManager.leaveRoom(currentProduct.getId());
            roomJoined = false;
        }
        if (refreshTimeline != null) refreshTimeline.stop();
    }

    public void handleQuickBid(ActionEvent event) {
        try {
            Button clickedButton = (Button) event.getSource();
            double incrementValue = Double.parseDouble(clickedButton.getText().replace("+", "").trim());
            double currentBidAmount = (txtBid.getText() != null && !txtBid.getText().trim().isEmpty())
                    ? Double.parseDouble(txtBid.getText().trim()) : currentProduct.getCurrentPrice();
            txtBid.setText(String.format("%.0f", currentBidAmount + incrementValue));
        } catch (Exception e) {}
    }
    private void handleLoadBidHistory(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    // Dịch JSON thành mảng các lượt Bid
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<dto.BidRequest>>(){}.getType();
                    java.util.List<dto.BidRequest> history = gson.fromJson(gson.toJson(response.getData()), listType);

                    if (history != null) {
                        currentProduct.getBidHistory().clear();
                        for (dto.BidRequest b : history) {
                            String bidderName = "User#" + b.bidderId + (b.autoBid ? " (AUTO)" : "");
                            currentProduct.addBid(new Bid(bidderName, b.amount));
                        }
                        updateBidHistory(); // Cập nhật ListView
                        loadChartHistory(); // Vẽ lại biểu đồ
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }
}

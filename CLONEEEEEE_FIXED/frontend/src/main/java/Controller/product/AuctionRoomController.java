package Controller.product;

import Model.Bid;
import Model.Product;
import Service.auction.AuctionRoomManager;
import Service.core.SceneNavigator;
import Session.Session;
import network.SocketClient;
import dto.AuctionDTO;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.BidRequest;
import dto.AutoBidRequest;
import com.google.gson.Gson;
import util.VietnamTime;
import util.ImageUtil;

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

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private String lastRealtimeBidKey = "";

    @FXML
    public void initialize() {
        SocketClient socketClient = SocketClient.getInstance();

        for (String action : List.of(
                "PLACE_BID_RESPONSE",
                "BID_UPDATE",
                "SET_AUTO_BID_RESPONSE",
                "GET_BID_HISTORY_RESPONSE",
                "GET_ACTIVE_AUCTIONS_RESPONSE",
                "BALANCE_UPDATE",
                "GET_BALANCE_RESPONSE",
                "NEW_BID_EVENT",
                "NEW_AUCTION_EVENT",
                "AUCTION_TIME_EXTENDED"
        )) {
            socketClient.clearListeners(action);
        }

        /*
         * AuctionRoomController chỉ xử lý BID_UPDATE.
         * Không nghe NEW_BID_EVENT ở đây để tránh cùng một bid bị xử lý 2 lần.
         */
        socketClient.on("BID_UPDATE", this::handleRealtimeBid);
        socketClient.on("PLACE_BID_RESPONSE", this::handleBidResponse);

        socketClient.on("AUCTION_TIME_EXTENDED", this::handleAuctionTimeExtended);
        socketClient.on("SET_AUTO_BID_RESPONSE", this::handleAutoBidResponse);
        socketClient.on("BALANCE_UPDATE", response -> updateBalance());
        socketClient.on("GET_BALANCE_RESPONSE", response -> updateBalance());
        socketClient.on("GET_BID_HISTORY_RESPONSE", this::handleLoadBidHistory);
        socketClient.on("NEW_AUCTION_EVENT", this::handleAuctionCatalogChanged);
        socketClient.on("GET_ACTIVE_AUCTIONS_RESPONSE", this::handleAuctionRefreshResponse);
    }
    public void setData(Product p) {
        currentProduct = p;
        imageView.setImage(ImageUtil.loadImage(p.getImageBase64(), p.getImagePath(), false));

        setupChart();
        loadChartHistory();
        updateAllUI();
        SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));

        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(1000), e -> updateAllUI()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        Platform.runLater(() -> {
            Stage stage = (Stage) nameLabel.getScene().getWindow();
            AuctionRoomManager.joinRoom(currentProduct.getId());
            roomJoined = true;
            stage.setOnCloseRequest(e -> cleanupRoom());
        });
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
        if (Session.getCurrentUser() == null) {
            return;
        }
        double held = Math.max(0, Session.getCurrentUser().getBalance() - Session.getCurrentUser().getAvailableBalance());
        balanceLabel.setText("BALANCE: $"
                + String.format("%.2f", Session.getCurrentUser().getBalance())
                + " | AVAILABLE: $"
                + String.format("%.2f", Session.getCurrentUser().getAvailableBalance())
                + " | HELD: $"
                + String.format("%.2f", held));
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
    private void handleAuctionTimeExtended(ResponsePayload response) {
        try {
            if (currentProduct == null || response.getData() == null) {
                return;
            }

            com.google.gson.JsonObject data =
                    gson.fromJson(gson.toJson(response.getData()), com.google.gson.JsonObject.class);

            long auctionId = data.get("auctionId").getAsLong();

            if (auctionId != currentProduct.getId()) {
                return;
            }

            String newEndTime = data.get("newEndTime").getAsString();

            currentProduct.setEndTime(java.time.LocalDateTime.parse(newEndTime));

            updateAllUI();

        } catch (Exception e) {
            e.printStackTrace();
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
            String winner = currentProduct.getHighestBidder();
            winnerLabel.setText((winner == null || winner.isEmpty()) ? "No winner" : "Winner: " + winner);
            if (winner == null || winner.isEmpty()) {
                lblMessage.setText("Auction has ended. No winner.");
            } else if (isCurrentUserWinner(winner)) {
                lblMessage.setText("Auction has ended. You won. Final price: $" + String.format("%.2f", currentProduct.getCurrentPrice()));
            } else {
                lblMessage.setText("Auction has ended. You did not win.");
            }
        } else {
            winnerLabel.setText("");
        }
    }

    private boolean isCurrentUserWinner(String winner) {
        if (Session.getCurrentUser() == null || winner == null) {
            return false;
        }
        String normalizedWinner = winner.replace(" ", "");
        return normalizedWinner.equalsIgnoreCase("User#" + Session.getCurrentUser().getId());
    }


    @FXML
    private void handleBid() {
        try {
            if (Session.getCurrentUser() == null) {
                lblMessage.setText("You need to log in before bidding");
                return;
            }
            if (!(currentProduct.getStatus().equals("OPEN") || currentProduct.getStatus().equals("RUNNING"))) {
                lblMessage.setText("Auction is not open");
                return;
            }

            double bidAmount = Double.parseDouble(txtBid.getText());
            if (bidAmount > Session.getCurrentUser().getAvailableBalance()) {
                lblMessage.setText("Not enough available balance");
                return;
            }
            lblMessage.setText("Sending bid...");

            BidRequest req = new BidRequest();
            req.auctionId = (long) currentProduct.getId();
            req.bidderId = Session.getCurrentUser().getId();
            req.amount = bidAmount;

            RequestPayload payload = new RequestPayload("PLACE_BID", gson.toJson(req));
            SocketClient.getInstance().sendRequest(payload);
        } catch (Exception e) {
            lblMessage.setText("Invalid bid amount");
        }
    }


    @FXML
    private void handleAutoBid() {
        try {
            if (currentProduct == null || Session.getCurrentUser() == null) {
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
            req.bidderId = Session.getCurrentUser().getId();
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

            SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));

            if (currentProduct != null) {
                SocketClient.getInstance().sendRequest(
                        new RequestPayload("GET_BID_HISTORY", "{\"auctionId\":" + currentProduct.getId() + "}")
                );
            }
        } else {
            lblMessage.setText(response.getMessage());
        }
    }


    private void handleAutoBidResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            lblMessage.setText("Auto bid enabled");
            txtAutoMax.clear();
            txtAutoStep.clear();
            SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));
        } else {
            lblMessage.setText(response.getMessage());
        }
    }

    private void handleRealtimeBid(ResponsePayload response) {
        try {
            if (currentProduct == null || response == null || response.getData() == null) {
                return;
            }

            BidRequest newBid = gson.fromJson(gson.toJson(response.getData()), BidRequest.class);

            if (newBid == null || newBid.auctionId == null || newBid.bidderId == null) {
                return;
            }

            if (!newBid.auctionId.equals((long) currentProduct.getId())) {
                return;
            }

            /*
             * Chống xử lý trùng cùng một bid.
             * Phòng đấu giá chỉ nghe BID_UPDATE, nhưng vẫn giữ key này để an toàn
             * nếu backend hoặc socket gửi lặp lại cùng payload.
             */
            String bidKey = newBid.auctionId + "-" + newBid.bidderId + "-" + newBid.amount + "-" + newBid.autoBid;

            if (bidKey.equals(lastRealtimeBidKey)) {
                return;
            }

            lastRealtimeBidKey = bidKey;

            Platform.runLater(() -> {
                currentProduct.setCurrentPrice(newBid.amount);

                String bidderName = "User#" + newBid.bidderId + (newBid.autoBid ? " (AUTO)" : "");
                currentProduct.setHighestBidder(bidderName);

                /*
                 * Realtime đã tự thêm bid mới vào history.
                 * Không gọi GET_BID_HISTORY ngay tại đây nữa, vì sẽ làm UI bị reload
                 * và dễ tạo cảm giác auto bid nhảy nhiều lần.
                 */
                currentProduct.addBid(new Bid(bidderName, newBid.amount));

                updateAllUI();
                updateChartIfNeeded();

                SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAuctionCatalogChanged(ResponsePayload response) {
        if (currentProduct == null) {
            return;
        }
        SocketClient.getInstance().sendRequest(new RequestPayload("GET_ACTIVE_AUCTIONS", "{}"));
    }

    private void handleAuctionRefreshResponse(ResponsePayload response) {
        if (!"SUCCESS".equals(response.getStatus()) || currentProduct == null) {
            return;
        }

        try {
            Type listType = new com.google.gson.reflect.TypeToken<List<AuctionDTO>>() {}.getType();
            List<AuctionDTO> auctions = gson.fromJson(gson.toJson(response.getData()), listType);
            if (auctions == null) {
                return;
            }

            for (AuctionDTO auction : auctions) {
                if (auction.id != null && auction.id == currentProduct.getId()) {
                    updateCurrentProduct(auction);
                    Platform.runLater(this::updateAllUI);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    private void updateCurrentProduct(AuctionDTO auction) {
        if (auction.item != null) {
            currentProduct.setTitle(auction.item.name);
            currentProduct.setDescription(auction.item.description);
            currentProduct.setCategory(auction.item.category);
            currentProduct.setCondition(auction.item.condition);
            currentProduct.setImagePath(auction.item.imagePath);
            currentProduct.setImageBase64(auction.item.imageBase64);
            imageView.setImage(ImageUtil.loadImage(auction.item.imageBase64, auction.item.imagePath, true));
        }

        currentProduct.setCurrentPrice(auction.currentPrice);
        currentProduct.setSeller("Seller#" + auction.sellerId);
        currentProduct.setStatus(auction.status);
        if (auction.highestBidderId != null && auction.highestBidderId > 0) {
            currentProduct.setHighestBidder("User #" + auction.highestBidderId);
        } else {
            currentProduct.setHighestBidder("");
        }
        if (auction.startTime != null) {
            currentProduct.setStartTime(LocalDateTime.parse(auction.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (auction.endTime != null) {
            currentProduct.setEndTime(LocalDateTime.parse(auction.endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
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
            String timestamp = VietnamTime.timeNow().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
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
        if (currentProduct == null || response == null) {
            return;
        }

        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    java.lang.reflect.Type listType =
                            new com.google.gson.reflect.TypeToken<java.util.List<dto.BidRequest>>(){}.getType();

                    java.util.List<dto.BidRequest> history =
                            gson.fromJson(gson.toJson(response.getData()), listType);

                    if (history != null) {
                        currentProduct.getBidHistory().clear();

                        for (dto.BidRequest b : history) {
                            String bidderName = "User#" + b.bidderId + (b.autoBid ? " (AUTO)" : "");
                            currentProduct.addBid(new Bid(bidderName, b.amount));
                        }

                        updateBidHistory();
                        loadChartHistory();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}


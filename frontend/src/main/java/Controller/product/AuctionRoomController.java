package Controller.product;

import FakeDB.FakeDB;

import Model.Bid;
import Model.Product;
import Model.User;

import Service.auction.AuctionRoomManager;

import Session.Session;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import javafx.application.Platform;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

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

    // =====================================================
    // FXML
    // =====================================================

    @FXML
    private Label countdownLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label viewerCountLabel;

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
    private Label winnerLabel;

    @FXML
    private Label viewerLabel;

    @FXML
    private TextField txtBid;

    @FXML
    private Button bidButton;

    @FXML
    private ListView<String> bidHistoryList;

    @FXML
    private ImageView imageView;

    @FXML
    private LineChart<String, Number> priceChart;

    // 🎯 ĐÃ BỔ SUNG: Khai báo idLabel để khớp với fx:id="idLabel" trong FXML
    @FXML
    private Label idLabel;

    // =====================================================
    // DATA
    // =====================================================

    private Product currentProduct;

    private Timeline refreshTimeline;

    private XYChart.Series<String, Number> bidSeries;

    private int lastBidCount = 0;

    // =====================================================
    // SET DATA
    // =====================================================

    public void setData(Product p) {

        currentProduct = p;

        // IMAGE
        if (p.getImagePath() != null
                && !p.getImagePath().isEmpty()) {

            imageView.setImage(
                    new Image(p.getImagePath())
            );
        }

        // CHART
        setupChart();

        loadChartHistory();

        // UPDATE UI
        updateAllUI();

        // REALTIME REFRESH
        refreshTimeline = new Timeline(

                new KeyFrame(

                        Duration.millis(500),

                        e -> {

                            updateAllUI();

                            updateChartIfNeeded();
                        }
                )
        );

        refreshTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        refreshTimeline.play();

        // WAIT UNTIL SCENE READY
        Platform.runLater(() -> {

            Stage stage = (Stage)

                    nameLabel
                            .getScene()
                            .getWindow();

            // JOIN ROOM
            AuctionRoomManager.joinRoom(
                    currentProduct.getId(),
                    stage
            );

            // AUTO CLEANUP
            stage.setOnCloseRequest(e -> {

                cleanupRoom();
            });

        });
    }

    // =====================================================
    // UPDATE ALL UI
    // =====================================================

    private void updateAllUI() {

        nameLabel.setText(
                currentProduct.getTitle()
        );

        priceLabel.setText(

                "$ " + currentProduct.getCurrentPrice()
        );

        sellerLabel.setText(

                "SELLER: "
                        + currentProduct.getSeller()
        );

        descriptionArea.setText(
                currentProduct.getDescription()
        );

        // 🎯 ĐÃ SỬA: Lấy chính xác ID sản phẩm thay vì lấy nhầm status như lúc trước
        if (idLabel != null) {
            idLabel.setText(
                    "PRODUCT ID: #" + currentProduct.getId()
            );
        }

        statusLabel.setText(
                currentProduct.getStatus()
        );

        countdownLabel.setText(
                currentProduct.getTimeRemaining()
        );

        updateHighestBidder();

        updateBalance();

        updateBidHistory();

        updateAuctionStatus();

        updateViewerCount();
    }

    // =====================================================
    // VIEWERS
    // =====================================================

    private void updateViewerCount() {

        int viewers =

                AuctionRoomManager.getViewerCount(
                        currentProduct.getId()
                );

        viewerLabel.setText(
                "LIVE VIEWERS"
        );

        viewerCountLabel.setText(
                viewers + " watching"
        );
    }

    // =====================================================
    // BALANCE
    // =====================================================

    private void updateBalance() {

        balanceLabel.setText(

                "YOUR BALANCE: $"

                        +

                        String.format(

                                "%.2f",

                                Session.currentUser.getBalance()
                        )
        );
    }

    // =====================================================
    // HIGHEST BIDDER
    // =====================================================

    private void updateHighestBidder() {

        String bidder =
                currentProduct.getHighestBidder();

        if (bidder == null
                || bidder.isEmpty()) {

            highestBidderLabel.setText(
                    "HIGHEST BIDDER: None"
            );

        } else {

            highestBidderLabel.setText(

                    "HIGHEST BIDDER: "

                            + bidder
            );
        }
    }

    // =====================================================
    // BID HISTORY
    // =====================================================

    private void updateBidHistory() {

        bidHistoryList.getItems().clear();

        for (Bid bid :

                currentProduct.getBidHistory()) {

            bidHistoryList.getItems().add(

                    bid.toString()
            );
        }

        if (!bidHistoryList.getItems().isEmpty()) {

            bidHistoryList.scrollTo(
                    bidHistoryList.getItems().size() - 1
            );
        }
    }

    // =====================================================
    // AUCTION STATUS
    // =====================================================

    private void updateAuctionStatus() {

        String status =
                currentProduct.getStatus();

        boolean open =
                status.equals("OPEN");

        txtBid.setDisable(!open);

        bidButton.setDisable(!open);

        if (status.equals("SCHEDULED")) {

            lblMessage.setText(
                    "Auction has not started yet"
            );

        } else if (status.equals("SOLD")) {

            lblMessage.setText(
                    "Auction has ended"
            );

            String winner =
                    currentProduct.getHighestBidder();

            winnerLabel.setText(

                    winner == null
                            || winner.isEmpty()

                            ?

                            "No winner"

                            :

                            "Winner: " + winner
            );

        } else {

            lblMessage.setText("");

            winnerLabel.setText("");
        }
    }

    // =====================================================
    // HANDLE BID
    // =====================================================

    @FXML
    private void handleBid() {

        try {

            if (!currentProduct.getStatus()
                    .equals("OPEN")) {

                lblMessage.setText(
                        "Auction is not open"
                );

                return;
            }

            double bidAmount =
                    Double.parseDouble(

                            txtBid.getText()
                    );

            // PRICE CHECK
            if (bidAmount <=
                    currentProduct.getCurrentPrice()) {

                lblMessage.setText(
                        "Bid amount must be higher"
                );

                return;
            }

            // BALANCE CHECK
            if (Session.currentUser.getBalance()
                    < bidAmount) {

                lblMessage.setText(
                        "Insufficient balance"
                );

                return;
            }

            // OWN PRODUCT CHECK
            if (currentProduct.getSeller()
                    .equals(

                            Session.currentUser
                                    .getUsername()
                    )) {

                lblMessage.setText(
                        "You cannot bid on your own product"
                );

                return;
            }

            refundOldBidder();

            Session.currentUser.deductMoney(
                    bidAmount
            );

            currentProduct.setCurrentPrice(
                    bidAmount
            );

            currentProduct.setHighestBidder(

                    Session.currentUser.getUsername()
            );

            currentProduct.addBid(

                    new Bid(

                            Session.currentUser
                                    .getUsername(),

                            bidAmount
                    )
            );

            applyAntiSniping();

            lblMessage.setText(
                    "Bid successful!"
            );

            txtBid.clear();

            updateAllUI();

            updateChartIfNeeded();

        } catch (Exception e) {

            lblMessage.setText(
                    "Invalid bid"
            );
        }
    }

    // =====================================================
    // REFUND
    // =====================================================

    private void refundOldBidder() {

        String oldBidder =
                currentProduct.getHighestBidder();

        if (oldBidder == null
                || oldBidder.isEmpty()) {

            return;
        }

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

    // =====================================================
    // ANTI SNIPING
    // =====================================================

    private void applyAntiSniping() {

        java.time.Duration remaining =

                java.time.Duration.between(

                        java.time.LocalDateTime.now(),

                        currentProduct.getEndTime()
                );

        if (remaining.getSeconds() <= 30) {

            currentProduct.setEndTime(

                    currentProduct.getEndTime()
                            .plusSeconds(30)
            );

            lblMessage.setText(

                    "Bid successful! +30s anti-sniping"
            );
        }
    }

    // =====================================================
    // CHART SETUP
    // =====================================================

    private void setupChart() {

        bidSeries = new XYChart.Series<>();

        priceChart.getData().clear();

        priceChart.getData().add(bidSeries);

        priceChart.setAnimated(false);

        CategoryAxis xAxis =
                (CategoryAxis) priceChart.getXAxis();

        NumberAxis yAxis =
                (NumberAxis) priceChart.getYAxis();

        yAxis.setForceZeroInRange(false);
        yAxis.setAutoRanging(true);
    }

    // =====================================================
    // LOAD CHART HISTORY
    // =====================================================

    private void loadChartHistory() {

        bidSeries.getData().clear();

        int count = 1;

        for (Bid bid : currentProduct.getBidHistory()) {

            bidSeries.getData().add(

                    new XYChart.Data<>(

                            "Bid #" + (count++),

                            bid.getAmount()
                    )
            );
        }

        lastBidCount =
                currentProduct.getBidHistory().size();
    }

    // =====================================================
    // UPDATE CHART ONLY WHEN NEW BID
    // =====================================================

    private void updateChartIfNeeded() {

        int currentSize =
                currentProduct.getBidHistory().size();

        if (currentSize > lastBidCount) {

            Bid latest = currentProduct
                    .getBidHistory()
                    .get(currentSize - 1);

            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            Platform.runLater(() -> {
                bidSeries.getData().add(
                        new XYChart.Data<>(
                                timestamp,
                                latest.getAmount()
                        )
                );

                if (bidSeries.getData().size() > 10) {
                    bidSeries.getData().remove(0);
                }
            });

            lastBidCount = currentSize;
        }
    }

    // =====================================================
    // BACK
    // =====================================================
    @FXML
    private void goBack() throws Exception {
        cleanupRoom();

        Stage stage = (Stage) nameLabel.getScene().getWindow();

        Parent root = FXMLLoader.load(
                getClass().getResource("/ui/product/AuctionMain.fxml")
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/style/pages/auction-main.css").toExternalForm()
        );

        stage.setScene(scene);

        stage.setWidth(1280);
        stage.setHeight(750);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    // =====================================================
    // CLEANUP
    // =====================================================

    private void cleanupRoom() {

        if (currentProduct != null) {

            AuctionRoomManager.leaveRoom(
                    currentProduct.getId()
            );
        }

        if (refreshTimeline != null) {

            refreshTimeline.stop();
        }
    }

    // =====================================================
    // HANDLE QUICK BID
    // =====================================================
    public void handleQuickBid(ActionEvent event) {
        try {
            Button clickedButton = (Button) event.getSource();

            String buttonText = clickedButton.getText().replace("+", "").trim();
            double incrementValue = Double.parseDouble(buttonText);

            double currentBidAmount = 0;
            if (txtBid.getText() != null && !txtBid.getText().trim().isEmpty()) {
                currentBidAmount = Double.parseDouble(txtBid.getText().trim());
            } else {
                currentBidAmount = currentProduct.getCurrentPrice();
            }

            double newBidAmount = currentBidAmount + incrementValue;
            txtBid.setText(String.format("%.0f", newBidAmount));

        } catch (NumberFormatException e) {
            lblMessage.setText("Lỗi định dạng số khi tính toán giá thầu!");
        }
    }
}
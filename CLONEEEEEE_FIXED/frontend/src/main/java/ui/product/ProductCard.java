package ui.product;

import Controller.product.AuctionRoomController;
import Model.Product;
import Model.User;
import Session.Session;
import dto.RequestPayload;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.SocketClient;

public class ProductCard {

    private Product product;
    private VBox root;
    private Label idLabel;
    private Label titleLabel;
    private Label priceLabel;
    private Label statusLabel;
    private Label timerLabel;
    private ImageView imageView;
    private Button watchBtn;
    private Timeline refreshTimeline;

    public ProductCard(Product product) {
        this.product = product;

        buildUI();
        loadImage();
        update();

        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> update())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void buildUI() {
        root = new VBox();
        root.setSpacing(10);
        root.setPrefWidth(220);
        root.getStyleClass().add("auction-card");

        imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);

        idLabel = new Label();
        idLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px; -fx-font-weight: bold;");

        titleLabel = new Label();
        priceLabel = new Label();
        statusLabel = new Label();
        timerLabel = new Label();

        watchBtn = new Button();
        watchBtn.getStyleClass().add("secondary-button");

        watchBtn.setOnAction(e -> {
            e.consume();
            handleWatchButtonClick();
        });

        root.getChildren().addAll(
                imageView,
                idLabel,
                titleLabel,
                priceLabel,
                statusLabel,
                timerLabel,
                watchBtn
        );

        root.setOnMouseClicked(e -> openAuctionRoom());
    }

    private void loadImage() {
        try {
            if (product == null) {
                return;
            }

            String imagePath = product.getImagePath();

            if (imagePath != null && !imagePath.isBlank()) {
                imageView.setImage(new Image(imagePath, true));
            }

        } catch (Exception e) {
            System.out.println("Khong nap duoc anh cho san pham ID: " + product.getId());
        }
    }

    private void handleWatchButtonClick() {
        User currentUser = Session.getCurrentUser();

        if (currentUser == null || product == null) {
            return;
        }

        int auctionId = product.getId();
        long userId = currentUser.getId();

        boolean currentlyWatching = currentUser.isWatching(auctionId);

        String action = currentlyWatching ? "REMOVE_WATCHLIST" : "ADD_WATCHLIST";

        String data = "{"
                + "\"userId\":" + userId + ","
                + "\"auctionId\":" + auctionId
                + "}";

        SocketClient.getInstance().sendRequest(
                new RequestPayload(action, data)
        );

        /*
         * Cap nhat tam thoi tren UI cho muot.
         * Server van la nguon du lieu chinh.
         */
        if (currentlyWatching) {
            currentUser.removeFromWatchlist(auctionId);
        } else {
            currentUser.addToWatchlist(auctionId);
        }

        updateWatchButtonState();
    }

    private void openAuctionRoom() {
        try {
            if (refreshTimeline != null) {
                refreshTimeline.stop();
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/product/AuctionRoom.fxml")
            );

            Parent room = loader.load();

            AuctionRoomController controller = loader.getController();
            controller.setData(product);

            Stage stage = (Stage) root.getScene().getWindow();
            Scene scene = new Scene(room);

            if (getClass().getResource("/style/main.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/style/main.css").toExternalForm()
                );
            }

            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();

        } catch (Exception ex) {
            System.out.println("Loi khi mo phong dau gia: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void update() {
        if (product == null) {
            return;
        }

        if (idLabel != null) {
            idLabel.setText("ID: #" + product.getId());
        }

        if (titleLabel != null) {
            titleLabel.setText(product.getTitle());
        }

        if (priceLabel != null) {
            priceLabel.setText(product.getCurrentPrice() + " VND");
        }

        if (statusLabel != null) {
            statusLabel.setText(product.getStatus());
        }

        if (timerLabel != null) {
            timerLabel.setText(product.getTimeRemaining());
        }

        updateWatchButtonState();
    }

    private void updateWatchButtonState() {
        if (watchBtn == null || product == null) {
            return;
        }

        User currentUser = Session.getCurrentUser();

        if (currentUser == null) {
            watchBtn.setText("☆ Watch");
            watchBtn.setDisable(true);
            return;
        }

        watchBtn.setDisable(false);

        if (currentUser.isWatching(product.getId())) {
            watchBtn.setText("★ Watching");
            watchBtn.setStyle("-fx-text-fill: #ff3b30; -fx-font-weight: bold;");
        } else {
            watchBtn.setText("☆ Watch");
            watchBtn.setStyle("");
        }
    }

    public void updateProduct(Product product) {
        this.product = product;
        loadImage();
        update();
    }

    public void stopTimeline() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    public VBox getRoot() {
        return root;
    }

    public Product getProduct() {
        return product;
    }
}
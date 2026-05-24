package ui.product;

import Controller.product.AuctionRoomController;

import Model.Product;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Label;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import javafx.util.Duration;

public class ProductCard {

    private Product product;

    private VBox root;

    private Label titleLabel;

    private Label priceLabel;

    private Label statusLabel;

    private Label timerLabel;

    private ImageView imageView;

    private Timeline refreshTimeline;

    public ProductCard(Product product) {

        this.product = product;

        buildUI();

        update();

        // REALTIME UPDATE
        refreshTimeline = new Timeline(

                new KeyFrame(

                        Duration.seconds(1),

                        e -> update()
                )
        );

        refreshTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        refreshTimeline.play();
    }

    private void buildUI() {

        root = new VBox();

        root.setSpacing(10);

        root.setPrefWidth(220);

        root.getStyleClass().add("auction-card");

        // =========================
        // IMAGE
        // =========================

        imageView = new ImageView();

        imageView.setFitWidth(180);

        imageView.setFitHeight(140);

        imageView.setPreserveRatio(true);

        // =========================
        // LABELS
        // =========================

        titleLabel = new Label();

        priceLabel = new Label();

        statusLabel = new Label();

        timerLabel = new Label();

        // =========================
        // ADD UI
        // =========================

        root.getChildren().addAll(

                imageView,

                titleLabel,

                priceLabel,

                statusLabel,

                timerLabel
        );

        // =========================
        // CLICK EVENT
        // =========================

        root.setOnMouseClicked(e -> {

            try {

                FXMLLoader loader = new FXMLLoader(

                        getClass().getResource(
                                "/ui/product/AuctionRoom.fxml"
                        )
                );

                Parent room = loader.load();

                AuctionRoomController controller =
                        loader.getController();

                controller.setData(product);

                Stage stage = (Stage)

                        root.getScene()
                                .getWindow();

                Scene scene = new Scene(room);

                scene.getStylesheets().add(

                        getClass()
                                .getResource("/style/main.css")
                                .toExternalForm()
                );

                stage.setScene(scene);

                stage.setResizable(true);

                stage.centerOnScreen();

            } catch (Exception ex) {

                ex.printStackTrace();
            }
        });
    }

    public void update() {

        titleLabel.setText(
                product.getTitle()
        );

        priceLabel.setText(
                product.getCurrentPrice() + " VND"
        );

        statusLabel.setText(
                product.getStatus()
        );

        timerLabel.setText(
                product.getTimeRemaining()
        );

        if (product.getImagePath() != null
                && !product.getImagePath().isEmpty()) {

            imageView.setImage(

                    new Image(
                            product.getImagePath(),
                            true
                    )
            );
        }
    }

    public VBox getRoot() {

        return root;
    }

    public Product getProduct() {

        return product;
    }
}
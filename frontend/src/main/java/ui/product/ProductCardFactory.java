package ui.product;

import Controller.product.AuctionRoomController;

import Model.Product;

import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Label;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.VBox;

import javafx.stage.Stage;

public class ProductCardFactory {

    private static final String DEFAULT_IMAGE =

            "https://via.placeholder.com/180";

    public static VBox create(
            Product p,
            Stage stage
    ) {

        VBox card = new VBox();

        // =========================
        // CARD CONFIG
        // =========================

        card.setSpacing(10);

        card.setPrefWidth(220);

        card.getStyleClass().add(
                "auction-card"
        );

        // =========================
        // HOVER EFFECT
        // =========================

        card.setOnMouseEntered(e -> {

            card.setScaleX(1.03);

            card.setScaleY(1.03);
        });

        card.setOnMouseExited(e -> {

            card.setScaleX(1);

            card.setScaleY(1);
        });

        // =========================
        // IMAGE
        // =========================

        ImageView img = new ImageView();

        img.setFitWidth(180);

        img.setFitHeight(140);

        img.setPreserveRatio(true);

        img.getStyleClass().add(
                "product-image"
        );

        try {

            if (p.getImagePath() != null
                    && !p.getImagePath().isEmpty()) {

                img.setImage(

                        new Image(
                                p.getImagePath(),
                                true
                        )
                );

            } else {

                img.setImage(

                        new Image(
                                DEFAULT_IMAGE,
                                true
                        )
                );
            }

        } catch (Exception e) {

            img.setImage(

                    new Image(
                            DEFAULT_IMAGE,
                            true
                    )
            );
        }

        // =========================
        // PRODUCT ID
        // =========================

        Label id = new Label(
                "#" + p.getId()
        );

        id.getStyleClass().add(
                "product-id"
        );

        // =========================
        // TITLE
        // =========================

        Label title = new Label(
                p.getTitle()
        );

        title.getStyleClass().add(
                "product-title"
        );

        // =========================
        // PRICE
        // =========================

        Label price = new Label(
                p.getCurrentPrice() + " VND"
        );

        price.getStyleClass().add(
                "product-price"
        );

        // =========================
        // CATEGORY
        // =========================

        Label category = new Label(
                "Category: " + p.getCategory()
        );

        category.getStyleClass().add(
                "normal-label"
        );

        // =========================
        // CONDITION
        // =========================

        Label condition = new Label(
                "Condition: " + p.getCondition()
        );

        condition.getStyleClass().add(
                "normal-label"
        );

        // =========================
        // TIMER
        // =========================

        Label timer = new Label(
                p.getTimeRemaining()
        );

        timer.getStyleClass().add(
                "timer-label"
        );

        // =========================
        // VIEWERS
        // =========================

        Label viewers = new Label(
                "Live Auction"
        );

        viewers.getStyleClass().add(
                "normal-label"
        );

        // =========================
        // STATUS
        // =========================

        Label status = new Label(
                p.getStatus()
        );

        status.getStyleClass().add(
                "status-label"
        );

        switch (p.getStatus()) {

            case "OPEN":

                status.getStyleClass().add(
                        "product-status-open"
                );

                break;

            case "SOLD":

                status.getStyleClass().add(
                        "product-status-sold"
                );

                break;

            default:

                status.getStyleClass().add(
                        "product-status-scheduled"
                );
        }

        // =========================
        // CLICK EVENT
        // =========================

        card.setOnMouseClicked(e -> {

            try {

                FXMLLoader loader = new FXMLLoader(

                        ProductCardFactory.class.getResource(
                                "/ui/product/AuctionRoom.fxml"
                        )
                );

                Parent root = loader.load();

                AuctionRoomController controller =
                        loader.getController();

                controller.setData(p);

                Scene scene = new Scene(root);

                scene.getStylesheets().add(

                        ProductCardFactory.class
                                .getResource(
                                        "/style/main.css"
                                )
                                .toExternalForm()
                );

                stage.setScene(scene);

                stage.setResizable(true);

                stage.centerOnScreen();

            } catch (Exception ex) {

                ex.printStackTrace();
            }
        });

        // =========================
        // ADD COMPONENTS
        // =========================

        card.getChildren().addAll(

                id,

                img,

                title,

                price,

                category,

                condition,

                timer,

                viewers,

                status
        );

        return card;
    }
}

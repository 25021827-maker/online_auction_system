package Controller.product;

import FakeDB.FakeDB;

import Model.Product;

import Session.Session;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.time.Duration;
import java.time.LocalDateTime;

public class MyProductsController {

    @FXML
    private FlowPane productsContainer;

    // =====================================================
    // INIT
    // =====================================================

    @FXML
    public void initialize() {

        loadProducts();
    }

    // =====================================================
    // LOAD PRODUCTS
    // =====================================================

    private void loadProducts() {

        productsContainer.getChildren().clear();

        for (Product p :

                FakeDB.getProductsBySeller(
                        Session.currentUser.getUsername()
                )

        ) {

            addProductCard(p);
        }
    }

    // =====================================================
    // PRODUCT CARD
    // =====================================================

    private void addProductCard(Product p) {

        VBox card = new VBox();

        card.setSpacing(12);

        card.setPrefWidth(230);

        card.getStyleClass().add(
                "auction-card"
        );

        // =====================================================
        // IMAGE
        // =====================================================

        ImageView imageView = new ImageView();

        imageView.setFitWidth(200);

        imageView.setFitHeight(140);

        imageView.setPreserveRatio(true);

        if (

                p.getImagePath() != null
                        &&
                        !p.getImagePath().isEmpty()

        ) {

            imageView.setImage(

                    new Image(
                            p.getImagePath(),
                            true
                    )

            );
        }

        // =====================================================
        // NAME
        // =====================================================

        Label name = new Label(
                p.getTitle()
        );

        name.getStyleClass().add(
                "product-title"
        );

        // =====================================================
        // PRICE
        // =====================================================

        Label price = new Label(

                "Price: "
                        + p.getCurrentPrice()
                        + " VND"

        );

        price.getStyleClass().add(
                "product-price"
        );

        // =====================================================
        // STATUS
        // =====================================================

        Label status = new Label(

                "Status: "
                        + p.getStatus()

        );

        status.getStyleClass().add(
                "normal-label"
        );

        // =====================================================
        // TIMER
        // =====================================================

        Label timer = new Label(

                p.getTimeRemaining()

        );

        timer.getStyleClass().add(
                "normal-label"
        );

        // =====================================================
        // CATEGORY
        // =====================================================

        Label category = new Label(

                "Category: "
                        + p.getCategory()

        );

        category.getStyleClass().add(
                "normal-label"
        );

        // =====================================================
        // CONDITION
        // =====================================================

        Label condition = new Label(

                "Condition: "
                        + p.getCondition()

        );

        condition.getStyleClass().add(
                "normal-label"
        );

        // =====================================================
        // BUTTONS
        // =====================================================

        Button editBtn = new Button(
                "Edit"
        );

        editBtn.getStyleClass().add(
                "secondary-button"
        );

        Button deleteBtn = new Button(
                "Delete"
        );

        deleteBtn.getStyleClass().add(
                "danger-button"
        );

        // =====================================================
        // LOCK RULE
        // Không cho sửa/xóa nếu còn <= 20 phút
        // hoặc auction đã OPEN/SOLD
        // =====================================================

        boolean locked = false;

        Duration untilStart = Duration.between(

                LocalDateTime.now(),

                p.getStartTime()
        );

        if (

                p.getStatus().equals("OPEN")
                        ||
                        p.getStatus().equals("SOLD")
                        ||
                        untilStart.toMinutes() <= 20

        ) {

            locked = true;
        }

        if (locked) {

            editBtn.setDisable(true);

            deleteBtn.setDisable(true);
        }

        // =====================================================
        // EDIT
        // =====================================================

        editBtn.setOnAction(e -> {

            System.out.println(
                    "TODO: edit product"
            );
        });

        // =====================================================
        // DELETE
        // =====================================================

        deleteBtn.setOnAction(e -> {

            FakeDB.removeProduct(p);

            loadProducts();
        });

        // =====================================================
        // BUTTON ROW
        // =====================================================

        FlowPane buttonPane = new FlowPane();

        buttonPane.setHgap(10);

        buttonPane.getChildren().addAll(

                editBtn,
                deleteBtn
        );

        // =====================================================
        // ADD UI
        // =====================================================

        card.getChildren().addAll(

                imageView,

                name,

                price,

                status,

                timer,

                category,

                condition,

                buttonPane
        );

        productsContainer.getChildren()
                .add(card);
    }

    // =====================================================
    // BACK
    // =====================================================

    // =====================================================
    // BACK
    // =====================================================
    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) productsContainer.getScene().getWindow();

            Parent root = FXMLLoader.load(
                    getClass().getResource("/ui/product/AuctionMain.fxml")
            );

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/style/pages/auction-main.css").toExternalForm()
            );

            stage.setScene(scene);

            // Đồng bộ kích thước cố định và khóa phóng to
            stage.setWidth(1280);
            stage.setHeight(750);
            stage.setResizable(false);
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
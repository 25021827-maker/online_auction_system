package Controller.product;

import Model.Product;
import FakeDB.FakeDB;
import Session.Session;

import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;

import javafx.event.ActionEvent;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import java.util.stream.Collectors;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

public class AuctionController {

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane productsContainer;

    @FXML
    private ComboBox<String> sortBox;

    @FXML
    private ComboBox<String> priceFilterBox;

    // =========================
    // INIT
    // =========================
    @FXML
    public void initialize() {

        loadProducts(
                FakeDB.getProducts()
        );

        // DEFAULT FILTER
        priceFilterBox.setValue("Tất cả");

        // REALTIME SEARCH
        searchField.textProperty().addListener(

                (observable, oldValue, newValue) -> {

                    applyFilters();

                }

        );

        // SORT
        sortBox.valueProperty().addListener(

                (observable, oldValue, newValue) -> {

                    applyFilters();

                }

        );

        // PRICE FILTER
        priceFilterBox.valueProperty().addListener(

                (observable, oldValue, newValue) -> {

                    applyFilters();

                }

        );

        // PRELOAD
        new Thread(() -> {

            try {

                FXMLLoader.load(

                        getClass().getResource(
                                "/ui/product/ProductForm.fxml"
                        )

                );

            } catch (Exception ignored) {}

        }).start();
    }

    // =========================
    // APPLY FILTERS
    // =========================
    private void applyFilters() {

        List<Product> filtered =
                new ArrayList<>(FakeDB.getProducts());

        // SEARCH
        String keyword =
                searchField.getText().toLowerCase();

        filtered = filtered.stream()

                .filter(product ->

                        product.getTitle()
                                .toLowerCase()
                                .contains(keyword)

                )

                .collect(Collectors.toList());

        // PRICE FILTER
        String priceFilter =
                priceFilterBox.getValue();

        if (priceFilter != null) {

            switch (priceFilter) {

                case "Dưới 500":

                    filtered = filtered.stream()

                            .filter(p ->
                                    p.getCurrentPrice() < 500
                            )

                            .collect(Collectors.toList());

                    break;

                case "500 - 1000":

                    filtered = filtered.stream()

                            .filter(p ->

                                    p.getCurrentPrice() >= 500
                                            &&
                                            p.getCurrentPrice() <= 1000

                            )

                            .collect(Collectors.toList());

                    break;

                case "Trên 1000":

                    filtered = filtered.stream()

                            .filter(p ->
                                    p.getCurrentPrice() > 1000
                            )

                            .collect(Collectors.toList());

                    break;
            }
        }

        // SORT
        String sortType =
                sortBox.getValue();

        if (sortType != null) {

            switch (sortType) {

                case "Giá thấp → cao":

                    filtered.sort(

                            Comparator.comparingDouble(
                                    Product::getCurrentPrice
                            )

                    );

                    break;

                case "Giá cao → thấp":

                    filtered.sort(

                            Comparator.comparingDouble(
                                    Product::getCurrentPrice
                            ).reversed()

                    );

                    break;
            }
        }

        // LOAD UI
        loadProducts(filtered);
    }

    // =========================
    // LOAD PRODUCTS
    // =========================
    private void loadProducts(List<Product> list) {

        productsContainer.getChildren().clear();

        for (Product p : list) {

            addProduct(p);

        }
    }

    // =========================
    // PRODUCT CARD
    // =========================
    private void addProduct(Product p) {

        VBox card = new VBox();

        card.setSpacing(5);

        card.setPrefWidth(150);

        card.setStyle(

                "-fx-background-color: white;" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #eeeeee;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1),5,0,0,2);"

        );

        // IMAGE
        ImageView img = new ImageView();

        img.setFitWidth(120);

        img.setFitHeight(100);

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
                            "https://via.placeholder.com/120",
                            true
                    )
            );
        }

        // NAME
        Label name = new Label(
                p.getTitle()
        );

        // PRICE
        Label price = new Label(
                p.getCurrentPrice() + " VND"
        );

        // TIMER
        Label timerLabel = new Label(
                p.getTimeRemaining()
        );

        timerLabel.setStyle(
                "-fx-font-weight: bold;"
        );

        // STATUS
        Label status = new Label(
                p.getStatus()
        );

        updateStatusColor(status, p);

        // ADD UI
        card.getChildren().addAll(

                img,
                name,
                price,
                timerLabel,
                status

        );

        // CLICK DETAIL
        card.setOnMouseClicked(e -> {

            try {

                FXMLLoader loader = new FXMLLoader(

                        getClass().getResource(
                                "/ui/product/ProductDetail.fxml"
                        )

                );

                Parent root = loader.load();

                ProductController controller =
                        loader.getController();

                controller.setData(p);

                Stage stage = (Stage)

                        productsContainer
                                .getScene()
                                .getWindow();

                stage.setScene(
                        new Scene(root)
                );

            } catch (Exception ex) {

                ex.printStackTrace();

            }
        });

        productsContainer.getChildren()
                .add(card);

        // REALTIME TIMER
        Timeline timeline = new Timeline(

                new KeyFrame(

                        Duration.seconds(1),

                        e -> {

                            timerLabel.setText(
                                    p.getTimeRemaining()
                            );

                            status.setText(
                                    p.getStatus()
                            );

                            updateStatusColor(status, p);
                        }

                )
        );

        timeline.setCycleCount(
                Timeline.INDEFINITE
        );

        timeline.play();
    }
    private void updateStatusColor(Label status, Product p) {

        if (p.getStatus().equals("OPEN")) {

            status.setStyle(
                    "-fx-text-fill: orange;"
            );

        } else {

            status.setStyle(
                    "-fx-text-fill: green;"
            );
        }
    }


    // =========================
    // FILTER STATUS
    // =========================
    @FXML
    private void showAll() {

        loadProducts(
                FakeDB.getProducts()
        );
    }

    @FXML
    private void showOpen() {

        loadProducts(
                FakeDB.getByStatus("OPEN")
        );
    }

    @FXML
    private void showSold() {

        loadProducts(
                FakeDB.getByStatus("SOLD")
        );
    }

    // =========================
    // FILTER POPUP
    // =========================
    @FXML
    private void openFilter(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(

                    getClass().getResource(
                            "/ui/product/FilterView.fxml"
                    )
            );

            Parent root = loader.load();

            Stage filterStage = new Stage();

            filterStage.setScene(
                    new Scene(root)
            );

            filterStage.setTitle(
                    "Bộ lọc sản phẩm"
            );

            Stage primaryStage = (Stage)

                    ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            filterStage.initOwner(primaryStage);

            filterStage.initModality(
                    Modality.APPLICATION_MODAL
            );

            filterStage.initStyle(
                    StageStyle.UTILITY
            );

            filterStage.showAndWait();

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    // =========================
    // NAVIGATION
    // =========================
    @FXML
    private void handlePostProduct(ActionEvent event) {

        loadSceneAsync(

                event,
                "/ui/product/ProductForm.fxml",
                "Đăng sản phẩm"

        );
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {

        loadSceneAsync(

                event,
                "/ui/user/Profile.fxml",
                "Hồ sơ cá nhân"

        );
    }

    // =========================
    // ASYNC LOAD
    // =========================
    private void loadSceneAsync(
            ActionEvent event,
            String path,
            String title
    ) {

        new Thread(() -> {

            try {

                FXMLLoader loader = new FXMLLoader(

                        getClass().getResource(path)

                );

                Parent root = loader.load();

                Platform.runLater(() -> {

                    Stage stage = (Stage)

                            ((Node) event.getSource())
                                    .getScene()
                                    .getWindow();

                    stage.setScene(
                            new Scene(root)
                    );

                    stage.setTitle(title);

                });

            } catch (Exception e) {

                e.printStackTrace();

            }
        }).start();
    }

    // =========================
    // LOGOUT
    // =========================
    @FXML
    private void handleLogout(ActionEvent event) {

        try {

            Session.currentUser = null;

            FXMLLoader loader = new FXMLLoader(

                    getClass().getResource(
                            "/ui/auth/Login.fxml"
                    )

            );

            Parent root = loader.load();

            Stage stage = (Stage)

                    ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.setScene(
                    new Scene(root)
            );

            stage.setTitle(
                    "Đăng nhập"
            );

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}
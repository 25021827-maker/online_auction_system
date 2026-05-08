package Controller.product;

import Model.Product;
import FakeDB.FakeDB;
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
import javafx.scene.control.TextField;
import javafx.stage.StageStyle;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import java.util.List;

public class AuctionController {

    @FXML private TextField searchField;
    @FXML private FlowPane productsContainer;

    // ===== INIT =====
    @FXML
    public void initialize() {
        loadProducts(FakeDB.getProducts());

        // preload để tránh lag
        new Thread(() -> {
            try {
                FXMLLoader.load(getClass().getResource("/ui/product/ProductForm.fxml"));
            } catch (Exception ignored) {}
        }).start();
    }

    // ===== LOAD DATA =====
    private void loadProducts(List<Product> list) {
        productsContainer.getChildren().clear();

        for (Product p : list) {
            addProduct(p);
        }
    }

    // ===== CARD UI =====
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

        // IMAGE (không block UI)
        ImageView img = new ImageView();
        img.setFitWidth(120);
        img.setFitHeight(100);

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            img.setImage(new Image(p.getImagePath(), true));
        } else {
            img.setImage(new Image("https://via.placeholder.com/120", true));
        }

        Label name = new Label(p.getTitle());
        Label price = new Label(p.getCurrentPrice() + " VND");

        Label status = new Label(p.getStatus());
        if (p.getStatus().equals("OPEN")) {
            status.setStyle("-fx-text-fill: orange;");
        } else {
            status.setStyle("-fx-text-fill: green;");
        }

        card.getChildren().addAll(img, name, price, status);

        // CLICK → DETAIL
        card.setOnMouseClicked(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/product/ProductDetail.fxml"));
                Parent root = loader.load();

                ProductController controller = loader.getController();
                controller.setData(p);

                Stage stage = (Stage) productsContainer.getScene().getWindow();
                stage.setScene(new Scene(root));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        productsContainer.getChildren().add(card);
    }

    // ===== FILTER =====
    @FXML
    private void showAll() {
        loadProducts(FakeDB.getProducts());
    }

    @FXML
    private void showOpen() {
        loadProducts(FakeDB.getByStatus("OPEN"));
    }

    @FXML
    private void showSold() {
        loadProducts(FakeDB.getByStatus("SOLD"));
    }

    // ===== FILTER POPUP =====
    @FXML
    private void openFilter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/product/FilterView.fxml"));
            Parent root = loader.load();

            Stage filterStage = new Stage();
            filterStage.setScene(new Scene(root));
            filterStage.setTitle("Bộ lọc sản phẩm");

            Stage primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            filterStage.initOwner(primaryStage);
            filterStage.initModality(Modality.APPLICATION_MODAL);
            filterStage.initStyle(StageStyle.UTILITY);

            filterStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== NAVIGATION (FIX LAG) =====
    @FXML
    private void handlePostProduct(ActionEvent event) {
        loadSceneAsync(event, "/ui/product/ProductForm.fxml", "Đăng sản phẩm");
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        loadSceneAsync(event, "/ui/user/Profile.fxml", "Hồ sơ cá nhân");
    }

    private void loadSceneAsync(ActionEvent event, String path, String title) {

        new Thread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent root = loader.load();

                Platform.runLater(() -> {
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle(title);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class MyProductsController {

    @FXML
    private FlowPane productsContainer;

    // 🎯 MỚI BỔ SUNG: Luồng quản lý cập nhật thời gian liên tục cho màn hình My Products
    private Timeline liveTimeline;

    // =====================================================
    // INIT
    // =====================================================
    @FXML
    public void initialize() {
        // 1. Nạp danh sách sản phẩm lần đầu tiên khi mở màn hình
        loadProducts();

        // 2. 🎯 MỚI BỔ SUNG: Chạy luồng ngầm quét mỗi 1 giây để tự động cập nhật thời gian đếm ngược
        setupLiveUpdater();
    }

    /**
     * 🎯 MỚI BỔ SUNG: Hàm điều khiển bộ đếm giây chạy ngầm liên tục
     */
    private void setupLiveUpdater() {
        liveTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    // Quét lại toàn bộ giao diện để làm mới thời gian và check lại luật khóa nút 20 phút
                    loadProducts();
                })
        );
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        liveTimeline.play();
    }

    // =====================================================
    // LOAD PRODUCTS (Tối ưu hóa nạp liên tục)
    // =====================================================
    private void loadProducts() {
        if (productsContainer == null) return;

        // Vì số lượng sản phẩm của một Seller thường không quá nhiều, việc clear và dựng lại mỗi giây
        // giúp đồng hồ nhảy chữ cực kỳ chuẩn xác và dễ quản lý trạng thái nút Disable
        productsContainer.getChildren().clear();

        List<Product> myProducts = FakeDB.getProductsBySeller(Session.currentUser.getUsername());
        for (Product p : myProducts) {
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
        card.getStyleClass().add("auction-card");

        // =====================================================
        // IMAGE
        // =====================================================
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try {
                // Nạp ảnh tĩnh (không nạp lại ngầm) để tránh giật khung hình
                imageView.setImage(new Image(p.getImagePath(), true));
            } catch (Exception e) {
                System.out.println("Không nạp được ảnh cho sản phẩm ID: " + p.getId());
            }
        }

        // =====================================================
        // NAME
        // =====================================================
        Label name = new Label(p.getTitle());
        name.getStyleClass().add("product-title");

        // =====================================================
        // PRICE
        // =====================================================
        Label price = new Label("Price: " + p.getCurrentPrice() + " VND");
        price.getStyleClass().add("product-price");

        // =====================================================
        // STATUS
        // =====================================================
        Label status = new Label("Status: " + p.getStatus());
        status.getStyleClass().add("normal-label");

        // =====================================================
        // TIMER (Sẽ tự nhảy lùi lách tách nhờ bộ setupLiveUpdater)
        // =====================================================
        Label timer = new Label(p.getTimeRemaining());
        timer.getStyleClass().add("normal-label");

        // =====================================================
        // CATEGORY
        // =====================================================
        Label category = new Label("Category: " + p.getCategory());
        category.getStyleClass().add("normal-label");

        // =====================================================
        // CONDITION
        // =====================================================
        Label condition = new Label("Condition: " + p.getCondition());
        condition.getStyleClass().add("normal-label");

        // =====================================================
        // BUTTONS
        // =====================================================
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("secondary-button");

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("danger-button");

        // =====================================================
        // LOCK RULE (20 PHÚT)
        // Hệ thống quét liên tục, chạm mốc 20 phút tự động ẩn/khóa nút lập tức
        // =====================================================
        boolean locked = false;
        java.time.Duration untilStart = java.time.Duration.between(LocalDateTime.now(), p.getStartTime());

        if (p.getStatus().equals("OPEN") || p.getStatus().equals("SOLD") || untilStart.toMinutes() <= 20) {
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
            try {
                // Tắt luồng ngầm trước khi chuyển màn hình để tránh lãng phí RAM
                stopLiveTimeline();

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/ui/product/ProductForm.fxml")
                );
                Parent formRoot = loader.load();

                ProductFormController formController = loader.getController();
                formController.setEditProduct(p);

                Stage stage = (Stage) productsContainer.getScene().getWindow();
                Scene scene = new Scene(formRoot);

                if (getClass().getResource("/style/main.css") != null) {
                    scene.getStylesheets().add(getClass().getResource("/style/main.css").toExternalForm());
                }

                stage.setScene(scene);
                stage.setResizable(false);
                stage.centerOnScreen();

            } catch (Exception ex) {
                System.out.println("Lỗi khi mở form sửa sản phẩm: " + ex.getMessage());
            }
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
        buttonPane.getChildren().addAll(editBtn, deleteBtn);

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

        productsContainer.getChildren().add(card);
    }

    /**
     * 🎯 MỚI BỔ SUNG: Dập luồng chạy ngầm khi chuyển màn hình
     */
    private void stopLiveTimeline() {
        if (liveTimeline != null) {
            liveTimeline.stop();
        }
    }

    // =====================================================
    // BACK
    // =====================================================
    @FXML
    private void goBack() {
        try {
            // Tắt luồng ngầm màn hình này trước khi về trang chủ
            stopLiveTimeline();

            Stage stage = (Stage) productsContainer.getScene().getWindow();
            Parent root = FXMLLoader.load(
                    getClass().getResource("/ui/product/AuctionMain.fxml")
            );

            Scene scene = new Scene(root);
            if (getClass().getResource("/style/pages/auction-main.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/style/pages/auction-main.css").toExternalForm()
                );
            }

            stage.setScene(scene);
            stage.setWidth(1280);
            stage.setHeight(750);
            stage.setResizable(false);
            stage.centerOnScreen();

        } catch (Exception e) {
            System.out.println("Lỗi khi quay lại trang chủ: " + e.getMessage());
        }
    }
}
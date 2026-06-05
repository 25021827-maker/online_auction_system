package ui.product;

import Controller.product.AuctionRoomController;
import Model.Product;
import Service.core.SceneNavigator;
import Session.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import util.ImageUtil;

import java.util.Objects;

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
    private String loadedImagePath = "";

    public ProductCard(Product product) {
        this.product = product;

        buildUI();

        loadImageIfChanged();

        // Cập nhật các thông tin bằng chữ lần đầu tiên
        update();

        // REALTIME UPDATE - Giữ bộ đếm chạy nội bộ cho từng Card để tự nhảy giây đếm ngược
        refreshTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        e -> update()
                )
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void buildUI() {
        root = new VBox();
        root.setSpacing(12);
        root.setPrefWidth(260); // Độ rộng 260px giúp bố cục card thoáng và đẹp mắt hơn
        root.setPadding(new Insets(16));

        // 🔥 ÉP ĐỒNG BỘ CSS: Tự động nạp file css dành riêng cho card ngay khi component được khởi tạo
        try {
            root.getStylesheets().add(getClass().getResource("/style/pages/product-card.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("Cảnh báo: Chưa tìm thấy file product-card.css tại thư mục resources/style/pages/");
        }

        // Gán class định danh chính cho Card
        root.getStyleClass().add("product-card");

        // =====================================================
        // IMAGE CONTAINER (Khung chứa ảnh bo góc chống lộ viền thô)
        // =====================================================
        imageView = new ImageView();
        imageView.setFitWidth(220);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("image-container");
        imageContainer.setPrefHeight(160);
        imageContainer.setAlignment(Pos.CENTER);

        // =====================================================
        // LABELS & STYLE CLASSES (Chuyển hoàn toàn sang CSS)
        // =====================================================
        idLabel = new Label();
        idLabel.getStyleClass().add("product-id");

        titleLabel = new Label();
        titleLabel.getStyleClass().add("product-name");
        titleLabel.setWrapText(true);

        priceLabel = new Label();
        priceLabel.getStyleClass().add("product-price");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("product-status");

        timerLabel = new Label();
        timerLabel.getStyleClass().add("product-countdown");

        // Nhóm các nhãn chữ thông tin lại thành một block gọn gàng
        VBox infoContainer = new VBox(6);
        infoContainer.getChildren().addAll(idLabel, titleLabel, priceLabel, statusLabel, timerLabel);

        // =====================================================
        // 🎯 NÚT WATCHLIST & BẮT SỰ KIỆN
        // =====================================================
        watchBtn = new Button();
        watchBtn.setMaxWidth(Double.MAX_VALUE); // Cho nút kéo dãn full chiều rộng card nhìn vuông vắn và hiện đại
        watchBtn.getStyleClass().add("watch-button");

        watchBtn.setOnAction(e -> {
            e.consume(); // Ngăn chặn sự kiện click lan ra ngoài làm mở màn hình chi tiết đấu giá

            if (Session.getCurrentUser() != null) {
                int pId = product.getId();
                if (Session.getCurrentUser().isWatching(pId)) {
                    Session.getCurrentUser().removeFromWatchlist(pId);
                } else {
                    Session.getCurrentUser().addToWatchlist(pId);
                }
                updateWatchButtonState();
            }
        });

        // =====================================================
        // ADD CÁC THÀNH PHẦN VÀO ROOT
        // =====================================================
        root.getChildren().addAll(
                imageContainer,
                infoContainer,
                watchBtn
        );

        // =====================================================
        // CLICK EVENT - MỞ PHÒNG ĐẤU GIÁ CHI TIẾT
        // =====================================================
        root.setOnMouseClicked(e -> {
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
                SceneNavigator.showFixedFullScreen(stage, room, "Auction Room");

            } catch (Exception ex) {
                System.out.println("Lỗi khi mở phòng đấu giá: " + ex.getMessage());
            }
        });
    }

    /**
     * Hàm chịu trách nhiệm cập nhật các thông tin ĐỘNG thay đổi theo thời gian
     */
    public void update() {
        if (idLabel != null) {
            idLabel.setText("ID: #" + product.getId());
        }

        // 🔥 ĐÃ SỬA CHUẨN: Lấy chính xác getTitle() theo đúng Model gốc của bạn
        if (titleLabel != null) {
            titleLabel.setText(product.getTitle());
        }

        if (priceLabel != null) {
            priceLabel.setText(String.format("%,.1f VND", product.getCurrentPrice()));
        }

        if (statusLabel != null) {
            statusLabel.setText(product.getStatus());
        }

        if (timerLabel != null) {
            timerLabel.setText(product.getTimeRemaining());
        }

        updateWatchButtonState();
    }

    public void updateProduct(Product product) {
        if (product == null) return;
        this.product = product;
        loadImageIfChanged();
        update();
    }

    private void loadImageIfChanged() {
        String imagePath = product.getImagePath() == null ? "" : product.getImagePath();
        String imageKey = ImageUtil.imageKey(product.getImageBase64(), imagePath);
        if (Objects.equals(loadedImagePath, imageKey)) {
            return;
        }

        loadedImagePath = imageKey;
        imageView.setImage(ImageUtil.loadImage(product.getImageBase64(), imagePath, true));
    }

    /**
     * Cập nhật trạng thái màu sắc, chữ hiển thị của nút Watch bằng cách thay đổi class CSS động
     */
    private void updateWatchButtonState() {
        if (watchBtn == null || Session.getCurrentUser() == null) return;

        if (Session.getCurrentUser().isWatching(product.getId())) {
            watchBtn.setText("★ Watching");
            watchBtn.getStyleClass().removeAll("watch-button");
            if (!watchBtn.getStyleClass().contains("watching-active-button")) {
                watchBtn.getStyleClass().add("watching-active-button"); // Đổi sang trạng thái đỏ hoạt động khi nhấn
            }
        } else {
            watchBtn.setText("☆ Watch");
            watchBtn.getStyleClass().removeAll("watching-active-button");
            if (!watchBtn.getStyleClass().contains("watch-button")) {
                watchBtn.getStyleClass().add("watch-button"); // Trả lại viền vàng outline mặc định
            }
        }
    }

    public void stopTimeline() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    public VBox getRoot() { return root; }
    public Product getProduct() { return product; }
}

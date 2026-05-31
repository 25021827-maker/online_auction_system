package ui.product;

import Controller.product.AuctionRoomController;
import Model.Product;
import Session.Session;
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

public class ProductCard {

    private Product product;
    private VBox root;
    private Label idLabel;
    private Label titleLabel;
    private Label priceLabel;
    private Label statusLabel;
    private Label timerLabel;
    private ImageView imageView;
    private Button watchBtn; // 🎯 MỚI BỔ SUNG: Nút bấm theo dõi sản phẩm
    private Timeline refreshTimeline;

    public ProductCard(Product product) {
        this.product = product;

        buildUI();

        // 🎯 MỚI: Nạp ảnh một lần duy nhất tại đây khi khởi tạo Card để CHỐNG NHÁY
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            try {
                // Sử dụng cấu hình nạp ảnh giữ nguyên tỷ lệ và mượt mà
                imageView.setImage(new Image(product.getImagePath(), true));
            } catch (Exception e) {
                System.out.println("Không nạp được ảnh cho sản phẩm ID: " + product.getId());
            }
        }

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
        idLabel = new Label();
        idLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px; -fx-font-weight: bold;");

        titleLabel = new Label();
        priceLabel = new Label();
        statusLabel = new Label();
        timerLabel = new Label();

        // =====================================================
        // 🎯 MỚI BỔ SUNG: KHỞI TẠO NÚT WATCHLIST & BẮT SỰ KIỆN
        // =====================================================
        watchBtn = new Button();
        watchBtn.getStyleClass().add("secondary-button"); // Bạn có thể thêm css riêng cho nút này

        watchBtn.setOnAction(e -> {
            // Ngăn chặn sự kiện click lan ra ngoài làm mở màn hình đấu giá (Mẹo JavaFX)
            e.consume();

            if (Session.currentUser != null) {
                int pId = product.getId();
                if (Session.currentUser.isWatching(pId)) {
                    // Nếu đang theo dõi thì bấm vào sẽ hủy theo dõi
                    Session.currentUser.removeFromWatchlist(pId);
                } else {
                    // Nếu chưa theo dõi thì bấm vào sẽ thêm vào danh sách
                    Session.currentUser.addToWatchlist(pId);
                }
                // Cập nhật giao diện nút bấm ngay lập tức
                updateWatchButtonState();
            }
        });

        // =========================
        // ADD UI (Đã thêm nút watchBtn vào cuối card)
        // =========================
        root.getChildren().addAll(
                imageView,
                idLabel,
                titleLabel,
                priceLabel,
                statusLabel,
                timerLabel,
                watchBtn
        );

        // =========================
        // CLICK EVENT
        // =========================
        root.setOnMouseClicked(e -> {
            try {
                // Trước khi chuyển màn hình, dừng bộ đếm ngầm của chiếc Card này để tránh rò rỉ bộ nhớ
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
                System.out.println("Lỗi khi mở phòng đấu giá: " + ex.getMessage());
            }
        });
    }

    /**
     * Hàm này chỉ chịu trách nhiệm cập nhật các thông tin ĐỘNG thay đổi theo thời gian
     */
    public void update() {
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

        // Đồng bộ trạng thái giao diện nút bấm theo dõi mỗi giây theo nhịp Timeline
        updateWatchButtonState();
    }

    /**
     * 🎯 MỚI BỔ SUNG: Hàm cập nhật trạng thái màu sắc, chữ hiển thị của nút Watch
     */
    private void updateWatchButtonState() {
        if (watchBtn == null || Session.currentUser == null) return;

        if (Session.currentUser.isWatching(product.getId())) {
            watchBtn.setText("★ Watching");
            // Gắn mã màu đỏ rực rỡ trực tiếp nếu css nhóm chưa khai báo kịp
            watchBtn.setStyle("-fx-text-fill: #ff3b30; -fx-font-weight: bold;");
        } else {
            watchBtn.setText("☆ Watch");
            watchBtn.setStyle(""); // Trả về style mặc định của hệ thống
        }
    }

    /**
     * Hàm tiện ích giúp giải phóng bộ đếm thời gian khi tắt màn hình chính
     */
    public void stopTimeline() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    public VBox getRoot() { return root; }
    public Product getProduct() { return product; }
}
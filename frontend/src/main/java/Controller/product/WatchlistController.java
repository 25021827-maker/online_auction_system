package Controller.product;

import FakeDB.FakeDB;
import Model.Product;
import Session.Session;
import ui.product.ProductCard;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

public class WatchlistController {

    @FXML
    private FlowPane watchlistContainer;

    private Timeline liveTimeline;
    private List<ProductCard> activeCards = new ArrayList<>();

    @FXML
    public void initialize() {
        // 🎯 MỚI BỔ SUNG: Ép chiều rộng tự động xuống dòng cho FlowPane bằng Java code để tránh lỗi FXML
        if (watchlistContainer != null) {
            watchlistContainer.setPrefWidth(1160.0);
        }

        // Nạp danh sách đồ theo dõi lần đầu
        loadWatchlist();

        // Chạy luồng làm mới realtime cho đồng hồ đếm ngược của các Card
        liveTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    for (ProductCard card : activeCards) {
                        card.update();
                    }
                })
        );
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        liveTimeline.play();
    }

    private void loadWatchlist() {
        if (watchlistContainer == null || Session.currentUser == null) return;

        // Giải phóng các card cũ để tránh rò rỉ bộ đếm ngầm
        for (ProductCard card : activeCards) {
            card.stopTimeline();
        }
        activeCards.clear();
        watchlistContainer.getChildren().clear();

        // Lấy danh sách ID mà user đang theo dõi
        List<Integer> watchedIds = Session.currentUser.getWatchlistProductIds();

        for (Product p : FakeDB.getProducts()) {
            if (watchedIds.contains(p.getId())) {
                ProductCard card = new ProductCard(p);
                activeCards.add(card);
                watchlistContainer.getChildren().add(card.getRoot());
            }
        }
    }

    @FXML
    private void goBack() {
        try {
            // Tắt luồng ngầm trước khi chuyển cảnh
            if (liveTimeline != null) liveTimeline.stop();
            for (ProductCard card : activeCards) card.stopTimeline();

            Stage stage = (Stage) watchlistContainer.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/product/AuctionMain.fxml"));
            Scene scene = new Scene(root);

            if (getClass().getResource("/style/pages/auction-main.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/style/pages/auction-main.css").toExternalForm());
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
package Controller.product;

import FakeDB.FakeDB;
import Model.Product;
import Service.product.ProductFilterService;
import ui.product.ProductCard;
import Service.core.SceneNavigator;
import Session.Session;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Rectangle;
import javafx.event.ActionEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class AuctionController {

    // =========================
    // SEARCH + FILTER
    // =========================
    @FXML private TextField searchField;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<String> sortBox;
    @FXML private ComboBox<String> statusBox;
    @FXML private ComboBox<String> categoryFilterBox;
    @FXML private ComboBox<String> conditionFilterBox;
    @FXML private Label lblResultCount;

    // =====================================================
    // USER AVATAR ON HEADER
    // =====================================================
    @FXML private ImageView imgAvatarHeader;

    // =========================
    // PRODUCTS CONTAINER
    // =========================
    @FXML private FlowPane productsContainer;

    // =========================
    // CARD MAP
    // =========================
    private final Map<Integer, ProductCard> cardMap = new HashMap<>();

    // 🎯 MỚI BỔ SUNG: Luồng quản lý cập nhật Realtime cho toàn bộ màn hình chính
    private Timeline liveTimeline;

    // =========================
    // INITIALIZE
    // =========================
    @FXML
    public void initialize() {
        setupDefaults();
        setupListeners();
        setupUserAvatar();

        if (searchField != null) {
            searchField.setPromptText("Search by product name or ID...");
        }

        Platform.runLater(() -> {
            if (productsContainer != null) {
                applyFilters(); // Chạy bộ lọc lần đầu để nạp danh sách
            }
        });

        // 🎯 MỚI BỔ SUNG: Kích hoạt mạch luồng ngầm quét mỗi 1 giây ngoài màn hình chính
        setupLiveUpdater();
    }

    /**
     * 🎯 MỚI BỔ SUNG: Khởi tạo luồng ngầm đồng bộ với hoạt động giả lập của FakeDB
     */
    private void setupLiveUpdater() {
        liveTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    // 1. Gọi FakeDB tự động nhảy giá ngẫu nhiên ngầm (giả lập user khác bid)
                    FakeDB.simulateRealtimeBids();

                    // 2. Tự động lấy lại danh sách đã qua bộ lọc hiện tại để kiểm tra xem có thay đổi gì không
                    String status = (statusBox.getValue() == null || "STATUS".equals(statusBox.getValue())) ? "All" : statusBox.getValue();
                    String category = (categoryFilterBox.getValue() == null || "CATEGORY".equals(categoryFilterBox.getValue())) ? "All" : categoryFilterBox.getValue();
                    String condition = (conditionFilterBox.getValue() == null || "CONDITION".equals(conditionFilterBox.getValue())) ? "All" : conditionFilterBox.getValue();

                    List<Product> currentFiltered = ProductFilterService.filter(
                            FakeDB.getProducts(),
                            searchField.getText(),
                            status,
                            category,
                            condition,
                            parseDouble(minPriceField.getText()),
                            parseDouble(maxPriceField.getText()),
                            sortBox.getValue()
                    );

                    // 3. Đẩy danh sách mới vào hàm load tối ưu để ép card đổi thông tin
                    loadProducts(currentFiltered);
                })
        );
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        liveTimeline.play();
    }

    private void setupUserAvatar() {
        if (Session.currentUser != null) {
            String userAvatarPath = Session.currentUser.getAvatarPath();
            if (userAvatarPath != null && !userAvatarPath.isEmpty()) {
                imgAvatarHeader.setImage(new Image(userAvatarPath));
            } else {
                String defaultAvatarUrl = getClass().getResource("/images/defaultavatar.png").toExternalForm();
                imgAvatarHeader.setImage(new Image(defaultAvatarUrl));
            }

            Rectangle clip = new Rectangle(42, 42);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            imgAvatarHeader.setClip(clip);
        }
    }

    private void setupDefaults() {
        statusBox.setValue("STATUS");
        categoryFilterBox.setValue("CATEGORY");
        conditionFilterBox.setValue("CONDITION");
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        minPriceField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        maxPriceField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        sortBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        statusBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        categoryFilterBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        conditionFilterBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void applyFilters() {
        String status = (statusBox.getValue() == null || "STATUS".equals(statusBox.getValue())) ? "All" : statusBox.getValue();
        String category = (categoryFilterBox.getValue() == null || "CATEGORY".equals(categoryFilterBox.getValue())) ? "All" : categoryFilterBox.getValue();
        String condition = (conditionFilterBox.getValue() == null || "CONDITION".equals(conditionFilterBox.getValue())) ? "All" : conditionFilterBox.getValue();

        List<Product> filtered = ProductFilterService.filter(
                FakeDB.getProducts(),
                searchField.getText(),
                status,
                category,
                condition,
                parseDouble(minPriceField.getText()),
                parseDouble(maxPriceField.getText()),
                sortBox.getValue()
        );

        loadProducts(filtered);
    }

    /**
     * 🎯 ĐÃ SỬA: Thuật toán nạp Card thông minh, không clear thô bạo -> CHỐNG NHẤP NHÁY
     */
    private void loadProducts(List<Product> products) {
        if (productsContainer == null) {
            return;
        }

        if (lblResultCount != null) {
            lblResultCount.setText(products.size() + " products found");
        }

        // Lưu danh sách ID sản phẩm xuất hiện trong đợt quét này
        Set<Integer> activeIds = new HashSet<>();

        for (Product p : products) {
            activeIds.add(p.getId());

            if (cardMap.containsKey(p.getId())) {
                // 1. Nếu Card đã có trên UI: Chỉ ép nó cập nhật lại chữ và số (Mượt mà, không nháy)
                cardMap.get(p.getId()).update();
            } else {
                // 2. Nếu là món đồ mới: Tạo mới Card và add vào FlowPane
                ProductCard card = new ProductCard(p);
                cardMap.put(p.getId(), card);
                productsContainer.getChildren().add(card.getRoot());
            }
        }

        // 3. Nếu món đồ nào đã bị xóa (Người bán gỡ đồ), gỡ bỏ Card đó khỏi giao diện chính
        cardMap.entrySet().removeIf(entry -> {
            int id = entry.getKey();
            ProductCard card = entry.getValue();
            if (!activeIds.contains(id)) {
                productsContainer.getChildren().remove(card.getRoot());
                card.stopTimeline(); // Dừng luồng chạy ngầm của riêng Card đó
                return true;
            }
            return false;
        });
    }

    public void updateProductCard(Product product) {
        ProductCard card = cardMap.get(product.getId());
        if (card != null) {
            card.update();
        }
    }

    public void removeProductCard(Product product) {
        ProductCard card = cardMap.remove(product.getId());
        if (card != null) {
            productsContainer.getChildren().remove(card.getRoot());
            card.stopTimeline();
        }
    }

    private Double parseDouble(String text) {
        try {
            if (text == null || text.isEmpty()) return null;
            return Double.parseDouble(text);
        } catch (Exception e) {
            return null;
        }
    }

    // Tiện ích tắt luồng tổng khi đổi màn hình điều hướng
    private void stopLiveTimeline() {
        if (liveTimeline != null) {
            liveTimeline.stop();
        }
    }

    @FXML
    private void handlePostProduct(ActionEvent event) {
        stopLiveTimeline();
        SceneNavigator.load(event, "/ui/product/ProductForm.fxml", "Create Auction");
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        stopLiveTimeline();
        SceneNavigator.load(event, "/ui/user/Profile.fxml", "Profile");
    }

    @FXML
    private void handleMyProducts(ActionEvent event) {
        stopLiveTimeline();
        SceneNavigator.load(event, "/ui/product/MyProducts.fxml", "My Products");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        stopLiveTimeline();
        SceneNavigator.load(event, "/ui/auth/Login.fxml", "Login");
    }
    @FXML
    private void handleWatchlist(ActionEvent event) {
        // Tắt luồng ngầm Realtime của trang chủ trước khi chuyển màn hình để tránh nặng RAM
        stopLiveTimeline();

        // Điều hướng sang màn hình Watchlist của anh em mình
        SceneNavigator.load(event, "/ui/product/Watchlist.fxml", "My Watchlist");
    }
}
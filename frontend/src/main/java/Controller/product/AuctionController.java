<<<<<<< HEAD
package Controller.auction;
=======
package Controller.product;

import FakeDB.FakeDB;
import Model.Product;
import Service.product.ProductFilterService;
import ui.product.ProductCard;
import Service.core.SceneNavigator;
import Session.Session; // Đảm bảo đã import Session để lấy User hiện tại
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
<<<<<<< HEAD
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import java.util.List;

public class AuctionController {

    @FXML private ListView<String> auctionListView;

    private Gson gson = new Gson();
    private AuctionClient auctionClient = AuctionClient.getInstance();

    @FXML
    public void initialize() {
        loadActiveAuctions();
    }

    private void loadActiveAuctions() {
        auctionClient.sendRequest("GET_ACTIVE_AUCTIONS", null)
                .thenAccept(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        // Chuyển đổi data thành List<AuctionDTO>
                        List<AuctionDTO> auctions = gson.fromJson(
                                gson.toJson(response.getData()),
                                new TypeToken<List<AuctionDTO>>(){}.getType()
                        );
                        Platform.runLater(() -> displayAuctions(auctions));
                    } else {
                        Platform.runLater(() -> showAlert("Lỗi", "Không thể tải danh sách đấu giá"));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert("Lỗi", ex.getMessage()));
                    return null;
                });
    }

    private void displayAuctions(List<AuctionDTO> auctions) {
        auctionListView.getItems().clear();
        for (AuctionDTO a : auctions) {
            auctionListView.getItems().add(
                    a.getItemName() + " | Giá: " + a.getCurrentPrice() + " | Kết thúc: " + a.getEndTime()
            );
        }
=======
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Rectangle;
import javafx.event.ActionEvent;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AuctionController {

    // =========================
    // SEARCH + FILTER
    // =========================

    @FXML
    private TextField searchField;

    @FXML
    private TextField minPriceField;

    @FXML
    private TextField maxPriceField;

    @FXML
    private ComboBox<String> sortBox;

    @FXML
    private ComboBox<String> statusBox;

    @FXML
    private ComboBox<String> categoryFilterBox;

    @FXML
    private ComboBox<String> conditionFilterBox;

    // =====================================================
    // USER AVATAR ON HEADER (MỚI BỔ SUNG)
    // =====================================================

    @FXML
    private ImageView imgAvatarHeader;

    // =========================
    // PRODUCTS CONTAINER
    // =========================

    @FXML
    private FlowPane productsContainer;

    // =========================
    // CARD MAP
    // =========================

    private final Map<Integer, ProductCard> cardMap
            = new HashMap<>();

    // =========================
    // INITIALIZE
    // =========================

    @FXML
    public void initialize() {

        setupDefaults();

        setupListeners();

        // Tải thông tin người dùng và bo góc Avatar
        setupUserAvatar();

        Platform.runLater(() -> {

            if (productsContainer != null) {

                loadProducts(
                        FakeDB.getProducts()
                );
            }
        });
    }

    // =====================================================
    // SETUP USER AVATAR (MỚI BỔ SUNG LOGIC BO GÓC)
    // =====================================================
    private void setupUserAvatar() {
        if (Session.currentUser != null) {
            // 2. Kiểm tra và nạp ảnh đại diện công nghệ
            String userAvatarPath = Session.currentUser.getAvatarPath();
            if (userAvatarPath != null && !userAvatarPath.isEmpty()) {
                imgAvatarHeader.setImage(new Image(userAvatarPath));
            } else {
                // Sử dụng đường dẫn tương đối an toàn từ tài nguyên hệ thống
                String defaultAvatarUrl = getClass().getResource("/images/defaultavatar.png").toExternalForm();
                imgAvatarHeader.setImage(new Image(defaultAvatarUrl));
            }

            // 3. Thực hiện mặt nạ hình chữ nhật bo góc thay vì hình tròn xoe
            // Kích thước 42x42 khớp hoàn toàn với fitHeight/fitWidth trong FXML
            Rectangle clip = new Rectangle(42, 42);

            // Bo góc vừa phải (12px) để đồng bộ với cấu trúc nút vuông Cyberpunk
            clip.setArcWidth(12);
            clip.setArcHeight(12);

            imgAvatarHeader.setClip(clip);
        }
    }

    // =========================
    // DEFAULT FILTER VALUES
    // =========================

    private void setupDefaults() {

        statusBox.setValue("All");

        categoryFilterBox.setValue("All");

        conditionFilterBox.setValue("All");
    }

    // =========================
    // LISTENERS
    // =========================

    private void setupListeners() {

        searchField.textProperty().addListener(
                (obs, oldV, newV) ->
                        applyFilters()
        );

        minPriceField.textProperty().addListener(
                (obs, oldV, newV) ->
                        applyFilters()
        );

        maxPriceField.textProperty().addListener(
                (obs, oldV, newV) ->
                        applyFilters()
        );

        sortBox.valueProperty().addListener(
                (obs, oldV, newV) ->
                        applyFilters()
        );

        statusBox.valueProperty().addListener(
                (obs, oldV, newV) ->
                        applyFilters()
        );

        categoryFilterBox.valueProperty().addListener(
                (obs, oldV, newV) ->
                        applyFilters()
        );

        conditionFilterBox.valueProperty().addListener(
                (obs, oldV, newV) ->
                        applyFilters()
        );
    }

    // =========================
    // APPLY FILTERS
    // =========================

    private void applyFilters() {

        List<Product> filtered =

                ProductFilterService.filter(

                        FakeDB.getProducts(),

                        searchField.getText(),

                        statusBox.getValue(),

                        categoryFilterBox.getValue(),

                        conditionFilterBox.getValue(),

                        parseDouble(
                                minPriceField.getText()
                        ),

                        parseDouble(
                                maxPriceField.getText()
                        ),

                        sortBox.getValue()
                );

        loadProducts(filtered);
    }

    // =========================
    // LOAD PRODUCTS
    // =========================

    private void loadProducts(
            List<Product> products
    ) {

        if (productsContainer == null) {

            return;
        }

        productsContainer.getChildren().clear();

        cardMap.clear();

        for (Product p : products) {

            ProductCard card =
                    new ProductCard(p);

            cardMap.put(
                    p.getId(),
                    card
            );

            productsContainer
                    .getChildren()
                    .add(
                            card.getRoot()
                    );
        }
    }

    // =========================
    // UPDATE SINGLE CARD
    // =========================

    public void updateProductCard(
            Product product
    ) {

        ProductCard card =
                cardMap.get(
                        product.getId()
                );

        if (card != null) {

            card.update();
        }
    }

    // =========================
    // REMOVE SINGLE CARD
    // =========================

    public void removeProductCard(
            Product product
    ) {

        ProductCard card =
                cardMap.remove(
                        product.getId()
                );

        if (card != null) {

            productsContainer
                    .getChildren()
                    .remove(
                            card.getRoot()
                    );
        }
    }

    // =========================
    // PARSE DOUBLE
    // =========================

    private Double parseDouble(
            String text
    ) {

        try {

            if (text == null
                    || text.isEmpty()) {

                return null;
            }

            return Double.parseDouble(text);

        } catch (Exception e) {

            return null;
        }
    }

    // =========================
    // CREATE PRODUCT
    // =========================

    @FXML
    private void handlePostProduct(
            ActionEvent event
    ) {

        SceneNavigator.load(

                event,

                "/ui/product/ProductForm.fxml",

                "Create Auction"
        );
    }

    // =====================================================
    // PROFILE (Hàm kích hoạt khi nhấn vào ảnh Avatar trên Header)
    // =====================================================
    @FXML
    private void handleViewProfile(
            ActionEvent event
    ) {

        SceneNavigator.load(

                event,

                "/ui/user/Profile.fxml",

                "Profile"
        );
    }

    // =========================
    // MY PRODUCTS
    // =========================

    @FXML
    private void handleMyProducts(
            ActionEvent event
    ) {

        SceneNavigator.load(

                event,

                "/ui/product/MyProducts.fxml",

                "My Products"
        );
    }

    // =========================
    // LOGOUT
    // =========================

    @FXML
    private void handleLogout(
            ActionEvent event
    ) {

        SceneNavigator.load(

                event,

                "/ui/auth/Login.fxml",

                "Login"
        );
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
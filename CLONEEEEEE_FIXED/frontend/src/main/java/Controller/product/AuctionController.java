package Controller.product;

import Model.Product;
import Service.product.ProductFilterService;
import ui.product.ProductCard;
import Service.core.SceneNavigator;
import Session.Session;
import network.SocketClient;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.AuctionDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Rectangle;
import javafx.event.ActionEvent;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AuctionController {

    @FXML private TextField searchField;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<String> sortBox;
    @FXML private ComboBox<String> statusBox;
    @FXML private ComboBox<String> categoryFilterBox;
    @FXML private ComboBox<String> conditionFilterBox;
    @FXML private Label lblResultCount;
    @FXML private ImageView imgAvatarHeader;
    @FXML private FlowPane productsContainer;

    @FXML private javafx.scene.control.Button btnPostProduct;
    @FXML private javafx.scene.control.Button btnMyProducts;

    private final Map<Integer, ProductCard> cardMap = new HashMap<>();
    private final Gson gson = new Gson();

    // Nguồn dữ liệu thật (thay cho FakeDB)
    private List<Product> serverProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        if (Session.currentUser != null
                && "ADMIN".equalsIgnoreCase(Session.currentUser.getRole())) {
            SceneNavigator.loadFromNode(productsContainer, "/ui/user/AdminView.fxml", "Admin Dashboard");
            return;
        }
        setupDefaults();
        setupListeners();
        setupUserAvatar();

        if (searchField != null) {
            searchField.setPromptText("Tìm kiếm sản phẩm...");
        }

        SocketClient.getInstance().on("GET_ACTIVE_AUCTIONS_RESPONSE", this::handleLoadAuctions);
        SocketClient.getInstance().on("NEW_BID_EVENT", this::handleRealtimeBidUpdate);
        SocketClient.getInstance().on("NEW_AUCTION_EVENT", e -> fetchAuctionsFromServer());

        fetchAuctionsFromServer();

        // ---- ĐOẠN MỚI THÊM: ẨN NÚT VỚI BIDDER ----
        if (Session.currentUser != null && "BIDDER".equalsIgnoreCase(Session.currentUser.getRole())) {
            if (btnPostProduct != null) {
                btnPostProduct.setVisible(false);
                btnPostProduct.setManaged(false); // Ẩn hoàn toàn khỏi Layout
            }
            if (btnMyProducts != null) {
                btnMyProducts.setVisible(false);
                btnMyProducts.setManaged(false);
            }
        }
    }

    private void fetchAuctionsFromServer() {
        RequestPayload req = new RequestPayload("GET_ACTIVE_AUCTIONS", "{}");
        SocketClient.getInstance().sendRequest(req);
    }

    private void handleLoadAuctions(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            try {
                // Dịch JSON thành danh sách AuctionDTO
                Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
                String dataJson = gson.toJson(response.getData());
                List<AuctionDTO> dtos = gson.fromJson(dataJson, listType);

                // Chuyển đổi DTO sang Model Product của JavaFX
                serverProducts.clear();
                if (dtos != null) {
                    for (AuctionDTO dto : dtos) {
                        serverProducts.add(mapToProduct(dto));
                    }
                }

                // Cập nhật lại giao diện lưới
                applyFilters();

            } catch (Exception e) {
                System.err.println("Lỗi Parse dữ liệu màn hình chính: " + e.getMessage());
            }
        }
    }

    // Nhận sự kiện có người vừa đặt giá từ luồng mạng
    private void handleRealtimeBidUpdate(ResponsePayload response) {
        // Tải lại toàn bộ danh sách để đảm bảo tính đồng bộ tuyệt đối
        fetchAuctionsFromServer();
    }

    // Phiên dịch DTO (Backend) thành Product (Frontend)
    private Product mapToProduct(AuctionDTO dto) {
        // Bóc dữ liệu an toàn
        String title = (dto.item != null && dto.item.name != null) ? dto.item.name : "Sản phẩm ẩn danh";
        double price = dto.currentPrice > 0 ? dto.currentPrice : (dto.item != null ? dto.item.startingPrice : 0);
        String image = (dto.item != null && dto.item.imagePath != null) ? dto.item.imagePath : "";
        String desc = (dto.item != null && dto.item.description != null) ? dto.item.description : "";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime st = dto.startTime != null ? LocalDateTime.parse(dto.startTime, formatter) : LocalDateTime.now();
        LocalDateTime et = dto.endTime != null ? LocalDateTime.parse(dto.endTime, formatter) : LocalDateTime.now().plusHours(1);

        Product p = new Product(title, price, image, "Seller#" + dto.sellerId, st, et, desc);
        // Ép kiểu ID từ Long xuống int cho khớp Frontend
        try {
            java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, dto.id.intValue());
        } catch (Exception e) {}

        p.setStatus(dto.status);
        if (dto.highestBidderId != null && dto.highestBidderId > 0) {
            p.setHighestBidder("User #" + dto.highestBidderId);
        } else {
            p.setHighestBidder("");
        }
        if (dto.item != null) {
            p.setCategory(dto.item.category);
            p.setCondition(dto.item.condition);
        }
        return p;
    }

    private void setupUserAvatar() {
        if (Session.currentUser != null) {
            String userAvatarPath = Session.currentUser.getAvatarPath();
            if (userAvatarPath != null && !userAvatarPath.isEmpty()) {
                imgAvatarHeader.setImage(new Image(userAvatarPath));
            } else {
                imgAvatarHeader.setImage(new Image(getClass().getResource("/images/defaultavatar.png").toExternalForm()));
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
                serverProducts, // Dùng dữ liệu thật thay vì FakeDB
                searchField.getText(), status, category, condition,
                parseDouble(minPriceField.getText()), parseDouble(maxPriceField.getText()), sortBox.getValue()
        );

        loadProducts(filtered);
    }

    private void loadProducts(List<Product> products) {
        if (productsContainer == null) return;
        if (lblResultCount != null) lblResultCount.setText(products.size() + " products found");

        Set<Integer> activeIds = new HashSet<>();
        for (Product p : products) {
            activeIds.add(p.getId());
            if (cardMap.containsKey(p.getId())) {
                cardMap.get(p.getId()).update();
            } else {
                ProductCard card = new ProductCard(p);
                cardMap.put(p.getId(), card);
                productsContainer.getChildren().add(card.getRoot());
            }
        }

        cardMap.entrySet().removeIf(entry -> {
            int id = entry.getKey();
            ProductCard card = entry.getValue();
            if (!activeIds.contains(id)) {
                productsContainer.getChildren().remove(card.getRoot());
                card.stopTimeline();
                return true;
            }
            return false;
        });
    }

    private Double parseDouble(String text) {
        try {
            if (text == null || text.isEmpty()) return null;
            return Double.parseDouble(text);
        } catch (Exception e) { return null; }
    }

    // Các hàm chuyển trang
    @FXML private void handlePostProduct(ActionEvent event) { SceneNavigator.load(event, "/ui/product/ProductForm.fxml", "Create Auction"); }
    @FXML private void handleViewProfile(ActionEvent event) { SceneNavigator.load(event, "/ui/user/Profile.fxml", "Profile"); }
    @FXML private void handleMyProducts(ActionEvent event) { SceneNavigator.load(event, "/ui/product/MyProducts.fxml", "My Products"); }
    @FXML private void handleWatchlist(ActionEvent event) { SceneNavigator.load(event, "/ui/product/Watchlist.fxml", "My Watchlist"); }
    @FXML private void handleLogout(ActionEvent event) { SceneNavigator.load(event, "/ui/auth/Login.fxml", "Login"); }
}
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
import util.VietnamTime;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
    @FXML private Label lblHeaderBalance;
    @FXML private Label lblHeaderAvailableBalance;
    @FXML private Label lblHeaderHeldBalance;
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
        if (Session.getCurrentUser() != null
                && "ADMIN".equalsIgnoreCase(Session.getCurrentUser().getRole())) {
            SceneNavigator.loadFromNode(productsContainer, "/ui/user/AdminView.fxml", "Admin Dashboard");
            return;
        }
        setupDefaults();
        setupListeners();
        setupUserAvatar();

        if (searchField != null) {
            searchField.setPromptText("Search products...");
        }

        SocketClient socketClient = SocketClient.getInstance();

        socketClient.clearListeners("GET_ACTIVE_AUCTIONS_RESPONSE");
        socketClient.clearListeners("NEW_BID_EVENT");
        socketClient.clearListeners("AUCTION_PRICE_CHANGED");
        socketClient.clearListeners("NEW_AUCTION_EVENT");
        socketClient.clearListeners("BALANCE_UPDATE");
        socketClient.clearListeners("GET_BALANCE_RESPONSE");
        socketClient.clearListeners("AUCTION_TIME_EXTENDED");
        socketClient.clearListeners("GET_WATCHLIST_IDS_RESPONSE");
        socketClient.clearListeners("ADD_WATCHLIST_RESPONSE");
        socketClient.clearListeners("REMOVE_WATCHLIST_RESPONSE");

        socketClient.on("AUCTION_TIME_EXTENDED", e -> fetchAuctionsFromServer());
        socketClient.on("GET_ACTIVE_AUCTIONS_RESPONSE", this::handleLoadAuctions);
        socketClient.on("NEW_BID_EVENT", this::handleRealtimeBidUpdate);
        socketClient.on("AUCTION_PRICE_CHANGED", this::handleRealtimeBidUpdate);
        socketClient.on("NEW_AUCTION_EVENT", e -> fetchAuctionsFromServer());
        socketClient.on("BALANCE_UPDATE", this::handleBalanceUpdate);
        socketClient.on("GET_BALANCE_RESPONSE", this::handleBalanceUpdate);

        socketClient.on("GET_WATCHLIST_IDS_RESPONSE", this::handleWatchlistIdsResponse);
        socketClient.on("ADD_WATCHLIST_RESPONSE", this::handleWatchlistActionResponse);
        socketClient.on("REMOVE_WATCHLIST_RESPONSE", this::handleWatchlistActionResponse);

        fetchAuctionsFromServer();
        fetchWatchlistIdsFromServer();
        requestBalanceRefresh();
        updateHeaderBalance();

        // ---- ĐOẠN MỚI THÊM: ẨN NÚT VỚI BIDDER ----
        if (Session.getCurrentUser() != null && "BIDDER".equalsIgnoreCase(Session.getCurrentUser().getRole())) {
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

    private void requestBalanceRefresh() {
        if (Session.getCurrentUser() != null) {
            SocketClient.getInstance().sendRequest(new RequestPayload("GET_BALANCE", "{}"));
        }
    }

    private void handleBalanceUpdate(ResponsePayload response) {
        updateHeaderBalance();
    }

    private void updateHeaderBalance() {
        if (lblHeaderBalance == null || Session.getCurrentUser() == null) {
            return;
        }
        double balance = Session.getCurrentUser().getBalance();
        double available = Session.getCurrentUser().getAvailableBalance();
        double held = Math.max(0, Session.getCurrentUser().getBalance() - Session.getCurrentUser().getAvailableBalance());
        lblHeaderBalance.setText("$" + String.format("%.2f", balance));
        if (lblHeaderAvailableBalance != null) {
            lblHeaderAvailableBalance.setText("$" + String.format("%.2f", available));
        }
        if (lblHeaderHeldBalance != null) {
            lblHeaderHeldBalance.setText("$" + String.format("%.2f", held));
        }
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
        String title = (dto.item != null && dto.item.name != null) ? dto.item.name : "Untitled product";
        double price = dto.currentPrice > 0 ? dto.currentPrice : (dto.item != null ? dto.item.startingPrice : 0);
        String image = (dto.item != null && dto.item.imagePath != null) ? dto.item.imagePath : "";
        String desc = (dto.item != null && dto.item.description != null) ? dto.item.description : "";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime st = dto.startTime != null ? LocalDateTime.parse(dto.startTime, formatter) : VietnamTime.now();
        LocalDateTime et = dto.endTime != null ? LocalDateTime.parse(dto.endTime, formatter) : VietnamTime.now().plusHours(1);

        Product p = new Product(title, price, image, "Seller#" + dto.sellerId, st, et, desc);

        if (dto.serverTime != null && !dto.serverTime.isBlank()) {
            try {
                p.syncServerTime(LocalDateTime.parse(dto.serverTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (Exception ignored) {
            }
        }

        if (dto.item != null) {
            p.setImageBase64(dto.item.imageBase64);
        }

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
        if (Session.getCurrentUser() != null) {
            String userAvatarPath = Session.getCurrentUser().getAvatarPath();
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
        if (sortBox != null) {
            sortBox.setValue("SORT");
        }

        if (statusBox != null) {
            statusBox.setValue("All");
        }

        if (categoryFilterBox != null) {
            categoryFilterBox.setValue("All");
        }

        if (conditionFilterBox != null) {
            conditionFilterBox.setValue("All");
        }
    }

    private void setupListeners() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }

        if (minPriceField != null) {
            minPriceField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }

        if (maxPriceField != null) {
            maxPriceField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }

        if (sortBox != null) {
            sortBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }

        if (statusBox != null) {
            statusBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }

        if (categoryFilterBox != null) {
            categoryFilterBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }

        if (conditionFilterBox != null) {
            conditionFilterBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
    }

    private void applyFilters() {
        String status = getComboValue(statusBox, "All");
        String category = getComboValue(categoryFilterBox, "All");
        String condition = getComboValue(conditionFilterBox, "All");
        String sortType = getComboValue(sortBox, "SORT");

        String keyword = searchField == null ? "" : searchField.getText();

        Double minPrice = parseDouble(minPriceField == null ? null : minPriceField.getText());
        Double maxPrice = parseDouble(maxPriceField == null ? null : maxPriceField.getText());

        List<Product> filtered = ProductFilterService.filter(
                serverProducts,
                keyword,
                status,
                category,
                condition,
                minPrice,
                maxPrice,
                sortType
        );

        loadProducts(filtered);
    }

    private String getComboValue(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null || comboBox.getValue() == null || comboBox.getValue().trim().isEmpty()) {
            return defaultValue;
        }

        return comboBox.getValue();
    }

    private void fetchWatchlistIdsFromServer() {
        if (Session.getCurrentUser() == null) {
            return;
        }

        RequestPayload req = new RequestPayload(
                "GET_WATCHLIST_IDS",
                "{\"userId\":" + Session.getCurrentUser().getId() + "}"
        );

        SocketClient.getInstance().sendRequest(req);
    }

    private void handleWatchlistIdsResponse(ResponsePayload response) {
        if (!"SUCCESS".equals(response.getStatus()) || Session.getCurrentUser() == null) {
            return;
        }

        try {
            Type listType = new TypeToken<List<Integer>>() {}.getType();
            List<Integer> ids = gson.fromJson(gson.toJson(response.getData()), listType);

            Session.getCurrentUser().setWatchlistProductIds(ids);

            Platform.runLater(() -> {
                for (ProductCard card : cardMap.values()) {
                    card.update();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleWatchlistActionResponse(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            fetchWatchlistIdsFromServer();
        }
    }

    private void loadProducts(List<Product> products) {
        if (productsContainer == null) return;
        if (products == null) products = List.of();
        if (lblResultCount != null) lblResultCount.setText(products.size() + " products found");

        Set<Integer> activeIds = new HashSet<>();
        List<Node> orderedNodes = new ArrayList<>();

        for (Product p : products) {
            activeIds.add(p.getId());
            ProductCard card = cardMap.get(p.getId());

            if (card != null) {
                card.updateProduct(p);
            } else {
                card = new ProductCard(p);
                cardMap.put(p.getId(), card);
            }

            orderedNodes.add(card.getRoot());
        }

        cardMap.entrySet().removeIf(entry -> {
            int id = entry.getKey();
            ProductCard card = entry.getValue();
            if (!activeIds.contains(id)) {
                card.stopTimeline();
                return true;
            }
            return false;
        });

        productsContainer.getChildren().setAll(orderedNodes);
    }

    private Double parseDouble(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }

            return Double.parseDouble(text.trim());

        } catch (Exception e) {
            return null;
        }
    }

    // Các hàm chuyển trang
    @FXML private void handlePostProduct(ActionEvent event) { SceneNavigator.load(event, "/ui/product/ProductForm.fxml", "Create Auction"); }
    @FXML private void handleViewProfile(ActionEvent event) { SceneNavigator.load(event, "/ui/user/Profile.fxml", "Profile"); }
    @FXML private void handleMyProducts(ActionEvent event) { SceneNavigator.load(event, "/ui/product/MyProducts.fxml", "My Products"); }
    @FXML private void handleWatchlist(ActionEvent event) { SceneNavigator.load(event, "/ui/product/Watchlist.fxml", "My Watchlist"); }
    @FXML private void handleLogout(ActionEvent event) { SceneNavigator.load(event, "/ui/auth/Login.fxml", "Login"); }
}

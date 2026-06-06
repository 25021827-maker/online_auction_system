package Controller.product;

import Model.Product;
import Session.Session;
import Service.core.SceneNavigator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;
import dto.RequestPayload;
import dto.ResponsePayload;
import util.ImageUtil;
import util.VietnamTime;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
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
import network.SocketClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyProductsController {

    @FXML private FlowPane productsContainer;

    private Timeline liveTimeline;
    private final Gson gson = new Gson();
    private final List<Product> myServerProducts = new ArrayList<>();
    private final Map<Integer, MyProductCard> productCards = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        SocketClient socketClient = SocketClient.getInstance();
        socketClient.clearListeners("GET_ACTIVE_AUCTIONS_RESPONSE");
        socketClient.clearListeners("NEW_BID_EVENT");
        socketClient.clearListeners("GET_MY_PRODUCTS_RESPONSE");
        socketClient.clearListeners("DELETE_PRODUCT_RESPONSE");
        socketClient.clearListeners("NEW_AUCTION_EVENT");
        socketClient.on("GET_MY_PRODUCTS_RESPONSE", this::handleLoadMyProducts);
        socketClient.on("DELETE_PRODUCT_RESPONSE", this::handleDeleteResponse);
        socketClient.on("NEW_BID_EVENT", response -> fetchMyProductsFromServer());
        socketClient.on("NEW_AUCTION_EVENT", response -> fetchMyProductsFromServer());

        fetchMyProductsFromServer();
        setupLiveUpdater();
    }

    private void fetchMyProductsFromServer() {
        if (Session.getCurrentUser() == null) {
            return;
        }
        RequestPayload req = new RequestPayload(
                "GET_MY_PRODUCTS",
                "{\"sellerId\":" + Session.getCurrentUser().getId() + "}"
        );
        SocketClient.getInstance().sendRequest(req);
    }

    private void handleLoadMyProducts(ResponsePayload response) {
        if (!"SUCCESS".equals(response.getStatus())) {
            return;
        }

        try {
            Type listType = new TypeToken<List<AuctionDTO>>() {}.getType();
            List<AuctionDTO> dtos = gson.fromJson(gson.toJson(response.getData()), listType);

            myServerProducts.clear();
            if (dtos != null) {
                for (AuctionDTO dto : dtos) {
                    myServerProducts.add(mapToProduct(dto));
                }
            }
            Platform.runLater(this::renderProductsUI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderProductsUI() {
        if (productsContainer == null) {
            return;
        }

        Set<Integer> activeIds = new HashSet<>();
        for (Product product : myServerProducts) {
            int productId = product.getId();
            activeIds.add(productId);

            MyProductCard card = productCards.get(productId);
            if (card == null) {
                card = new MyProductCard(product);
                productCards.put(productId, card);
                productsContainer.getChildren().add(card.root);
            } else {
                card.updateProduct(product);
            }
        }

        productCards.entrySet().removeIf(entry -> {
            if (activeIds.contains(entry.getKey())) {
                return false;
            }
            productsContainer.getChildren().remove(entry.getValue().root);
            return true;
        });

        updateLiveFields();
    }

    private void setupLiveUpdater() {
        liveTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateLiveFields()));
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        liveTimeline.play();
    }

    private void updateLiveFields() {
        for (MyProductCard card : productCards.values()) {
            card.updateLiveFields();
        }
    }

    private void handleDeleteResponse(ResponsePayload response) {
        Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
                fetchMyProductsFromServer();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Loi khi xoa: " + response.getMessage());
                alert.show();
            }
        });
    }

    private Product mapToProduct(AuctionDTO dto) {
        String title = (dto.item != null) ? dto.item.name : "";
        double price = dto.currentPrice;
        String image = (dto.item != null) ? dto.item.imagePath : "";
        String desc = (dto.item != null) ? dto.item.description : "";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime st = dto.startTime != null ? LocalDateTime.parse(dto.startTime, formatter) : VietnamTime.now();
        LocalDateTime et = dto.endTime != null ? LocalDateTime.parse(dto.endTime, formatter) : VietnamTime.now();

        Product product = new Product(title, price, image, "Seller#" + dto.sellerId, st, et, desc);

        if (dto.serverTime != null && !dto.serverTime.isBlank()) {
            try {
                product.syncServerTime(
                        LocalDateTime.parse(dto.serverTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
            } catch (Exception ignored) {
            }
        }

        if (dto.item != null) {
            product.setImageBase64(dto.item.imageBase64);
        }
        try {
            java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, dto.id.intValue());
        } catch (Exception ignored) {}

        product.setStatus(resolveSellerDisplayStatus(dto));
        if (dto.item != null) {
            product.setCategory(dto.item.category);
            product.setCondition(dto.item.condition);
        }
        return product;
    }

    private String resolveSellerDisplayStatus(AuctionDTO dto) {
        if (dto.item != null && dto.item.approvalStatus != null) {
            if ("PENDING".equalsIgnoreCase(dto.item.approvalStatus)) {
                return "PENDING_APPROVAL";
            }
            if ("REJECTED".equalsIgnoreCase(dto.item.approvalStatus)) {
                return "REJECTED";
            }
        }
        return dto.status == null ? "" : dto.status;
    }

    private void stopLiveTimeline() {
        if (liveTimeline != null) {
            liveTimeline.stop();
        }
    }

    @FXML
    private void goBack() {
        stopLiveTimeline();
        try {
            SceneNavigator.loadFromNode(productsContainer, "/ui/product/AuctionMain.fxml", "San dau gia");
        } catch (Exception ignored) {}
    }

    private final class MyProductCard {
        private Product product;
        private final VBox root;
        private final ImageView imageView;
        private final Label nameLabel;
        private final Label priceLabel;
        private final Label statusLabel;
        private final Label timerLabel;
        private final Label categoryLabel;
        private final Label conditionLabel;
        private final Button editButton;
        private final Button deleteButton;
        private String loadedImagePath = "";

        private MyProductCard(Product product) {
            this.product = product;
            this.root = new VBox();
            this.imageView = new ImageView();
            this.nameLabel = new Label();
            this.priceLabel = new Label();
            this.statusLabel = new Label();
            this.timerLabel = new Label();
            this.categoryLabel = new Label();
            this.conditionLabel = new Label();
            this.editButton = new Button("Edit");
            this.deleteButton = new Button("Delete");

            buildUI();
            updateProduct(product);
        }

        private void buildUI() {
            root.setSpacing(8); // Khoảng cách giữa các dòng chữ vừa phải, không quá thưa
            root.setPrefWidth(240); // Tăng nhẹ chiều rộng để layout thở hơn
            root.setPrefHeight(380); // Đặt chiều cao tối thiểu cố định để card không bị bóp nghẹt
            root.getStyleClass().add("product-card");
            // Thêm padding cho ruột của card để chữ/nút không bị chạm sát mép viền
            root.setPadding(new javafx.geometry.Insets(12));

            // 1. Xử lý Image Container chống móp méo ảnh
            imageView.setFitWidth(216);
            imageView.setFitHeight(140);
            imageView.setPreserveRatio(false); // Hoặc true tùy bạn, nhưng nên cố định khung chứa

            // Tạo một HBox hoặc Pane bọc ngoài ImageView nếu trong CSS `.image-container` là vùng chứa
            javafx.scene.layout.StackPane imgContainer = new javafx.scene.layout.StackPane(imageView);
            imgContainer.getStyleClass().add("image-container");
            imgContainer.setPrefSize(216, 140);

            // 2. Định dạng Style Class chuẩn theo file product-card.css
            nameLabel.getStyleClass().add("product-name");
            priceLabel.getStyleClass().add("product-price");
            statusLabel.getStyleClass().add("product-status");
            timerLabel.getStyleClass().add("product-countdown");

            // Các nhãn phụ dùng tạm class product-id để có màu xám tinh tế, không bị chìm
            categoryLabel.getStyleClass().add("product-id");
            conditionLabel.getStyleClass().add("product-id");

            // 3. Xử lý các nút bấm (Edit / Delete)
            editButton.getStyleClass().add("secondary-button"); // Hoặc dùng yellow-button tùy ý bạn
            deleteButton.getStyleClass().add("danger-button");

            editButton.setOnAction(e -> openEditForm());
            deleteButton.setOnAction(e -> deleteProduct());

            // Dùng HBox thay vì FlowPane để 2 nút nằm ngay ngắn trên một hàng
            javafx.scene.layout.HBox buttonPane = new javafx.scene.layout.HBox(10);
            buttonPane.setAlignment(javafx.geometry.Pos.CENTER);
            buttonPane.getChildren().addAll(editButton, deleteButton);

            // MẸO QUAN TRỌNG: Tạo một vùng đệm co giãn để tự động đẩy buttonPane xuống đáy Card
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            // 4. Add các thành phần vào VBox theo thứ tự chuẩn
            root.getChildren().addAll(
                    imgContainer,
                    nameLabel,
                    priceLabel,
                    statusLabel,
                    timerLabel,
                    categoryLabel,
                    conditionLabel,
                    spacer, // Đẩy nút xuống
                    buttonPane
            );
        }

        private void updateProduct(Product product) {
            this.product = product;
            loadImageIfNeeded();
            nameLabel.setText(product.getTitle());
            priceLabel.setText("Price: " + product.getCurrentPrice() + " USD");
            categoryLabel.setText("Category: " + product.getCategory());
            conditionLabel.setText("Condition: " + product.getCondition());
            updateLiveFields();
        }

        private void updateLiveFields() {
            statusLabel.setText("Status: " + product.getStatus());
            timerLabel.setText(product.getTimeRemaining());
            updateActionButtonState();
        }

        private void loadImageIfNeeded() {
            String imagePath = product.getImagePath() == null ? "" : product.getImagePath();
            String imageKey = ImageUtil.imageKey(product.getImageBase64(), imagePath);
            if (loadedImagePath.equals(imageKey)) {
                return;
            }

            loadedImagePath = imageKey;
            imageView.setImage(ImageUtil.loadImage(product.getImageBase64(), imagePath, true));
        }

        private void updateActionButtonState() {
            boolean locked = !product.canModify()
                    || product.getStatus().equals("RUNNING")
                    || product.getStatus().equals("FINISHED")
                    || product.getStatus().equals("SOLD");

            editButton.setDisable(locked);
            deleteButton.setDisable(locked);
        }

        private void openEditForm() {
            stopLiveTimeline();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/product/ProductForm.fxml"));
                Parent root = loader.load();

                ProductFormController controller = loader.getController();
                controller.setEditData(product);

                Stage stage = (Stage) productsContainer.getScene().getWindow();
                SceneNavigator.showFixedFullScreen(stage, root, "Create Auction");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void deleteProduct() {
            RequestPayload req = new RequestPayload("DELETE_PRODUCT", "{\"auctionId\":" + product.getId() + "}");
            SocketClient.getInstance().sendRequest(req);
        }
    }
}

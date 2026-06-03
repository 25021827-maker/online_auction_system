package Controller.product;

import Model.Product;
import Session.Session;
import network.SocketClient;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.AuctionDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import Service.core.SceneNavigator;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MyProductsController {

    @FXML private FlowPane productsContainer;
    private Timeline liveTimeline;
    private final Gson gson = new Gson();
    private List<Product> myServerProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        // Đăng ký nghe phản hồi từ luồng mạng Socket
        SocketClient.getInstance().on("GET_MY_PRODUCTS_RESPONSE", this::handleLoadMyProducts);
        SocketClient.getInstance().on("DELETE_PRODUCT_RESPONSE", this::handleDeleteResponse);

        // Gọi lệnh lấy đồ từ Server về
        fetchMyProductsFromServer();

        // Chạy luồng ngầm đếm ngược thời gian lách tách trên UI
        setupLiveUpdater();
    }

    private void fetchMyProductsFromServer() {
        if (Session.currentUser == null) return;
        // Gửi ID của chính mình lên để lọc danh sách sản phẩm mình đăng bán
        RequestPayload req = new RequestPayload("GET_MY_PRODUCTS", "{\"sellerId\":" + Session.currentUser.getId() + "}");
        SocketClient.getInstance().sendRequest(req);
    }

    private void handleLoadMyProducts(ResponsePayload response) {
        if ("SUCCESS".equals(response.getStatus())) {
            try {
                Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
                String dataJson = gson.toJson(response.getData());
                List<AuctionDTO> dtos = gson.fromJson(dataJson, listType);

                myServerProducts.clear();
                if (dtos != null) {
                    for (AuctionDTO dto : dtos) {
                        myServerProducts.add(mapToProduct(dto));
                    }
                }
                // Vẽ lại giao diện trên luồng UI chính
                Platform.runLater(this::renderProductsUI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void renderProductsUI() {
        if (productsContainer == null) return;
        productsContainer.getChildren().clear();
        for (Product p : myServerProducts) {
            addProductCard(p);
        }
    }

    private void setupLiveUpdater() {
        liveTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            renderProductsUI(); // Re-render mỗi giây để đồng hồ nhảy chữ chuẩn
        }));
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        liveTimeline.play();
    }

    private void addProductCard(Product p) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setPrefWidth(230);
        card.getStyleClass().add("auction-card");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try { imageView.setImage(new Image(p.getImagePath(), true)); } catch (Exception e) {}
        }

        Label name = new Label(p.getTitle());
        name.getStyleClass().add("product-title");

        Label price = new Label("Price: " + p.getCurrentPrice() + " USD");
        price.getStyleClass().add("product-price");

        Label status = new Label("Status: " + p.getStatus());
        status.getStyleClass().add("normal-label");

        Label timer = new Label(p.getTimeRemaining());
        timer.getStyleClass().add("normal-label");

        Label category = new Label("Category: " + p.getCategory());
        category.getStyleClass().add("normal-label");

        Label condition = new Label("Condition: " + p.getCondition());
        condition.getStyleClass().add("normal-label");

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("secondary-button");
        // BỎ DÒNG editBtn.setDisable(true); ĐI

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("danger-button");

        // Luật: Chỉ cho sửa/xóa khi phiên chưa bắt đầu (còn hơn 20 phút)
        java.time.Duration untilStart = java.time.Duration.between(LocalDateTime.now(), p.getStartTime());
        if (p.getStatus().equals("RUNNING") || p.getStatus().equals("FINISHED") || p.getStatus().equals("SOLD") || untilStart.toMinutes() <= 20) {
            editBtn.setDisable(true);
            deleteBtn.setDisable(true);
        }

        // BẮT SỰ KIỆN CLICK NÚT EDIT
        editBtn.setOnAction(e -> {
            stopLiveTimeline(); // Tắt đồng hồ ngầm
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/product/ProductForm.fxml"));
                javafx.scene.Parent root = loader.load();

                // Truyền dữ liệu sang form
                ProductFormController controller = loader.getController();
                controller.setEditData(p); // Ta sẽ viết hàm này ở bước sau

                javafx.stage.Stage stage = (javafx.stage.Stage) productsContainer.getScene().getWindow();
                SceneNavigator.showFixedFullScreen(stage, root, "Create Auction");
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        deleteBtn.setOnAction(e -> {
            RequestPayload req = new RequestPayload("DELETE_PRODUCT", "{\"auctionId\":" + p.getId() + "}");
            SocketClient.getInstance().sendRequest(req);
        });

        FlowPane buttonPane = new FlowPane();
        buttonPane.setHgap(10);
        buttonPane.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(imageView, name, price, status, timer, category, condition, buttonPane);
        productsContainer.getChildren().add(card);
    }

    private void handleDeleteResponse(ResponsePayload response) {
        Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
                fetchMyProductsFromServer(); // Tải lại danh sách sau khi xóa thành công
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi khi xóa: " + response.getMessage());
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
        LocalDateTime st = dto.startTime != null ? LocalDateTime.parse(dto.startTime, formatter) : LocalDateTime.now();
        LocalDateTime et = dto.endTime != null ? LocalDateTime.parse(dto.endTime, formatter) : LocalDateTime.now();

        Product p = new Product(title, price, image, "Seller#" + dto.sellerId, st, et, desc);
        try {
            java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, dto.id.intValue());
        } catch (Exception e) {}
        p.setStatus(dto.status);
        if (dto.item != null) {
            p.setCategory(dto.item.category);
            p.setCondition(dto.item.condition);
        }
        return p;
    }

    private void stopLiveTimeline() {
        if (liveTimeline != null) liveTimeline.stop();
    }

    @FXML
    private void goBack() {
        stopLiveTimeline();
        try { SceneNavigator.loadFromNode(productsContainer, "/ui/product/AuctionMain.fxml", "Sàn đấu giá"); } catch (Exception e) {}
    }
}

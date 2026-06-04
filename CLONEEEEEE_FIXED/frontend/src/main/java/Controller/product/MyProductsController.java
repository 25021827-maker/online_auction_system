package Controller.product;

import Model.Product;
import Session.Session;
import Service.core.SceneNavigator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;
import dto.RequestPayload;
import dto.ResponsePayload;
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
        SocketClient.getInstance().on("GET_MY_PRODUCTS_RESPONSE", this::handleLoadMyProducts);
        SocketClient.getInstance().on("DELETE_PRODUCT_RESPONSE", this::handleDeleteResponse);

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
        LocalDateTime st = dto.startTime != null ? LocalDateTime.parse(dto.startTime, formatter) : LocalDateTime.now();
        LocalDateTime et = dto.endTime != null ? LocalDateTime.parse(dto.endTime, formatter) : LocalDateTime.now();

        Product product = new Product(title, price, image, "Seller#" + dto.sellerId, st, et, desc);
        try {
            java.lang.reflect.Field idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, dto.id.intValue());
        } catch (Exception ignored) {}

        product.setStatus(dto.status);
        if (dto.item != null) {
            product.setCategory(dto.item.category);
            product.setCondition(dto.item.condition);
        }
        return product;
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
            root.setSpacing(12);
            root.setPrefWidth(230);
            root.getStyleClass().add("auction-card");

            imageView.setFitWidth(200);
            imageView.setFitHeight(140);
            imageView.setPreserveRatio(true);

            nameLabel.getStyleClass().add("product-title");
            priceLabel.getStyleClass().add("product-price");
            statusLabel.getStyleClass().add("normal-label");
            timerLabel.getStyleClass().add("normal-label");
            categoryLabel.getStyleClass().add("normal-label");
            conditionLabel.getStyleClass().add("normal-label");

            editButton.getStyleClass().add("secondary-button");
            deleteButton.getStyleClass().add("danger-button");

            editButton.setOnAction(e -> openEditForm());
            deleteButton.setOnAction(e -> deleteProduct());

            FlowPane buttonPane = new FlowPane();
            buttonPane.setHgap(10);
            buttonPane.getChildren().addAll(editButton, deleteButton);

            root.getChildren().addAll(
                    imageView,
                    nameLabel,
                    priceLabel,
                    statusLabel,
                    timerLabel,
                    categoryLabel,
                    conditionLabel,
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
            if (imageView.getImage() != null) {
                return;
            }
            if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                try {
                    imageView.setImage(new Image(product.getImagePath(), true));
                } catch (Exception ignored) {}
            }
        }

        private void updateActionButtonState() {
            java.time.Duration untilStart = java.time.Duration.between(LocalDateTime.now(), product.getStartTime());
            boolean locked = product.getStatus().equals("RUNNING")
                    || product.getStatus().equals("FINISHED")
                    || product.getStatus().equals("SOLD")
                    || untilStart.toMinutes() <= 20;
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
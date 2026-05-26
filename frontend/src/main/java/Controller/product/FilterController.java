package Controller.product;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;                        // ← sửa import
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class FilterController {

    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button applyButton;
    @FXML private Button resetButton;

    private FlowPane productsContainer;
    private Gson gson = new Gson();
    private Runnable onFilterApplied;

    @FXML
    public void initialize() {
        categoryCombo.getItems().addAll("Tất cả", "ELECTRONICS", "ART", "VEHICLE", "OTHER");
        categoryCombo.setValue("Tất cả");
        statusCombo.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED");
        statusCombo.setValue("Tất cả");
    }

    public void setProductsContainer(FlowPane container) {
        this.productsContainer = container;
    }

    public void setOnFilterApplied(Runnable callback) {
        this.onFilterApplied = callback;
    }

    @FXML
    private void applyFilter() {
        String category = categoryCombo.getValue();
        if ("Tất cả".equals(category)) category = null;
        String minPriceStr = minPriceField.getText();
        String maxPriceStr = maxPriceField.getText();
        String status = statusCombo.getValue();
        if ("Tất cả".equals(status)) status = null;

        Double minPrice = null, maxPrice = null;
        try {
            if (minPriceStr != null && !minPriceStr.isEmpty()) minPrice = Double.parseDouble(minPriceStr);
            if (maxPriceStr != null && !maxPriceStr.isEmpty()) maxPrice = Double.parseDouble(maxPriceStr);
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Giá phải là số");
            return;
        }

        Map<String, Object> filter = Map.of(
                "category", category,
                "minPrice", minPrice,
                "maxPrice", maxPrice,
                "status", status
        );

        AuctionClient.getInstance().sendRequest("FILTER_AUCTIONS", filter)
                .thenAccept(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        // Chuyển đổi dữ liệu thành List<AuctionDTO>
                        Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
                        List<AuctionDTO> auctions = gson.fromJson(gson.toJson(response.getData()), listType);
                        Platform.runLater(() -> {
                            if (productsContainer != null) {
                                displayAuctions(auctions);
                            }
                            if (onFilterApplied != null) onFilterApplied.run();
                            closeWindow();
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Lỗi", response.getData().toString()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert("Lỗi kết nối", ex.getMessage()));
                    return null;
                });
    }

    // Sửa kiểu tham số thành List<AuctionDTO>
    private void displayAuctions(List<AuctionDTO> auctions) {
        productsContainer.getChildren().clear();
        for (AuctionDTO auction : auctions) {
            VBox card = new VBox();
            card.setSpacing(5);
            card.setPrefWidth(200);
            card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 10; " +
                    "-fx-border-color: #eeeeee; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1),5,0,0,2);");

            // Hiển thị tên sản phẩm (itemName) thay vì auctionId
            String itemName = auction.getItemName() != null ? auction.getItemName() : "Sản phẩm #" + auction.getItemId();
            Label nameLabel = new Label(itemName);
            Label priceLabel = new Label("Giá hiện tại: " + auction.getCurrentPrice());
            Label statusLabel = new Label("Trạng thái: " + auction.getStatus());
            Button detailButton = new Button("Chi tiết");
            detailButton.setOnAction(e -> openAuctionDetail(auction.getAuctionId()));
            card.getChildren().addAll(nameLabel, priceLabel, statusLabel, detailButton);
            productsContainer.getChildren().add(card);
        }
    }

    // Implement mở màn hình chi tiết
    private void openAuctionDetail(Long auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/product/AuctionDetail.fxml"));
            Parent root = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionId(auctionId);
            Stage stage = new Stage();
            stage.setTitle("Chi tiết phiên đấu giá");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở chi tiết: " + e.getMessage());
        }
    }

    @FXML
    private void resetFilter() {
        categoryCombo.setValue("Tất cả");
        minPriceField.clear();
        maxPriceField.clear();
        statusCombo.setValue("Tất cả");
        applyFilter();
    }

    private void closeWindow() {
        Stage stage = (Stage) applyButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
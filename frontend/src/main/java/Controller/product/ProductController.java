package Controller.product;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.List;

public class ProductController {

    @FXML private ListView<String> productListView;

    private Gson gson = new Gson();
    private AuctionClient auctionClient = AuctionClient.getInstance();

    @FXML
    public void initialize() {
        loadProducts();
    }

    private void loadProducts() {
        // Giả sử server có action "GET_ACTIVE_AUCTIONS" trả về List<AuctionDTO>
        auctionClient.sendRequest("GET_ACTIVE_AUCTIONS", null)
                .thenAccept(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        List<AuctionDTO> products = gson.fromJson(
                                gson.toJson(response.getData()),
                                new TypeToken<List<AuctionDTO>>(){}.getType()
                        );
                        Platform.runLater(() -> displayProducts(products));
                    } else {
                        Platform.runLater(() -> showError("Không thể tải sản phẩm: " + response.getData()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Lỗi kết nối: " + ex.getMessage()));
                    return null;
                });
    }

    private void displayProducts(List<AuctionDTO> products) {
        productListView.getItems().clear();
        for (AuctionDTO a : products) {
            // Sử dụng getItemName(), fallback nếu null
            String displayName = (a.getItemName() != null && !a.getItemName().isEmpty())
                    ? a.getItemName()
                    : "Auction " + a.getAuctionId();
            productListView.getItems().add(displayName + " - " + a.getCurrentPrice() + " VND");
        }
    }

    private void showError(String msg) {
        System.err.println(msg);
        // Có thể hiện Alert nếu muốn
    }
}
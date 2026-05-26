package Controller.auction;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
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
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
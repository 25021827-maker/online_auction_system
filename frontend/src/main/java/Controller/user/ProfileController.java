package Controller.user;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import util.UserSession;

import java.util.List;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label balanceLabel;
    @FXML private ListView<String> myAuctionsListView;

    private Gson gson = new Gson();
    private AuctionClient auctionClient = AuctionClient.getInstance();

    @FXML
    public void initialize() {
        // Sử dụng UserSession để lấy thông tin người dùng hiện tại
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() != null) {
            usernameLabel.setText(session.getUsername());
            balanceLabel.setText(String.format("%.2f VND", session.getBalance()));
        } else {
            usernameLabel.setText("Chưa đăng nhập");
            balanceLabel.setText("0.00 VND");
        }
        loadMyAuctions();
    }

    private void loadMyAuctions() {
        // Gửi yêu cầu lấy danh sách đấu giá của seller hiện tại
        // Server cần có action "GET_MY_AUCTIONS"
        auctionClient.sendRequest("GET_MY_AUCTIONS", null)
                .thenAccept(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        List<AuctionDTO> auctions = gson.fromJson(
                                gson.toJson(response.getData()),
                                new TypeToken<List<AuctionDTO>>(){}.getType()
                        );
                        Platform.runLater(() -> displayMyAuctions(auctions));
                    } else {
                        Platform.runLater(() -> showError("Không thể tải danh sách đấu giá: " + response.getMessage()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Lỗi kết nối: " + ex.getMessage()));
                    return null;
                });
    }

    private void displayMyAuctions(List<AuctionDTO> auctions) {
        myAuctionsListView.getItems().clear();
        if (auctions == null || auctions.isEmpty()) {
            myAuctionsListView.getItems().add("Bạn chưa có phiên đấu giá nào.");
            return;
        }
        for (AuctionDTO a : auctions) {
            String displayName = (a.getItemName() != null && !a.getItemName().isEmpty())
                    ? a.getItemName()
                    : "Auction " + a.getAuctionId();
            myAuctionsListView.getItems().add(
                    displayName + " | Giá: " + a.getCurrentPrice() + " | Trạng thái: " + a.getStatus()
            );
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
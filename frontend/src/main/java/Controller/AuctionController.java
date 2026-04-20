package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

public class AuctionController {

    @FXML private TextField searchField;

    // Hàm này giúp nút "Bộ lọc" hết báo đỏ
    @FXML
    private void openFilter(ActionEvent event) {
        System.out.println("Đã nhấn nút Bộ lọc!");
        // Bạn có thể thêm logic hiện popup ở đây sau
    }

    @FXML
    private void handlePostProduct(ActionEvent event) {
        switchScene(event, "/ui/ProductForm.fxml", "Đăng sản phẩm");
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        switchScene(event, "/ui/Profile.fxml", "Hồ sơ cá nhân");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception e) {
            System.err.println("Lỗi load file: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
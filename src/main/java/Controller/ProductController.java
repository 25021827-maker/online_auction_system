package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

public class ProductController {

    @FXML private TextField txtName;
    @FXML private TextField txtPrice;

    @FXML
    private void saveProduct(ActionEvent event) {
        System.out.println("Đã đăng sản phẩm thành công!");
        goBack(event); // Lưu xong tự động quay về trang chủ
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/AuctionMain.fxml"));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package Controller.product;

import Model.Product;
import FakeDB.FakeDB;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.File;

public class ProductFormController {

    @FXML private TextField txtName;
    @FXML private TextArea txtDesc;
    @FXML private TextField txtPrice;
    @FXML private String imagePath = "";

    @FXML
    private void saveProduct() {
        try {
            String name = txtName.getText();
            double price = Double.parseDouble(txtPrice.getText());

            Product p = new Product(name, price,"Đang đấu", imagePath);
            FakeDB.addProduct(p);

            goBack();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            imagePath = file.toURI().toString(); // ❗ quan trọng
        }
    }

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) txtName.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/ui/product/AuctionMain.fxml"));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

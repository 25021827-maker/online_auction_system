package Controller.product;

import Model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class ProductController {

    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private ImageView imageView;
    @FXML private Product currentProduct;
    @FXML private TextField txtBid;
    @FXML private Label lblMessage;

    // nhận dữ liệu từ màn trước
    public void setData(Product p) {
        this.currentProduct = p;

        nameLabel.setText(p.getTitle());
        priceLabel.setText("Giá: " + p.getCurrentPrice());

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            imageView.setImage(new Image(p.getImagePath()));
        }
    }
    @FXML
    private void handleBid() {
        try {
            double newPrice = Double.parseDouble(txtBid.getText());

            if (newPrice <= currentProduct.getCurrentPrice()) {
                lblMessage.setText("Giá phải lớn hơn giá hiện tại");
                return;
            }

            currentProduct.setCurrentPrice(newPrice);

            priceLabel.setText("Giá: " + newPrice);
            lblMessage.setText("Đặt giá thành công!");

        } catch (Exception e) {
            lblMessage.setText("Giá không hợp lệ");
        }
    }

    @FXML
    private void goBack() throws Exception {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/ui/product/AuctionMain.fxml"));
        stage.setScene(new Scene(root));
    }
}

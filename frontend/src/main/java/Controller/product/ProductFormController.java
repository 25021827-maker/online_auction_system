package Controller.product;

import Model.Product;
import Session.Session;
import client.AuctionClient;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class ProductFormController {

    @FXML private TextField txtName;
    @FXML private TextArea txtDesc;
    @FXML private TextField txtPrice;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startHourField;
    @FXML private TextField startMinuteField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endHourField;
    @FXML private TextField endMinuteField;
    @FXML private Label lblMessage;

    private String imagePath = "";

    @FXML
    private void saveProduct() {
        if (lblMessage != null) lblMessage.setText("");

        if (!validateInputs()) return;

        try {
            String name = txtName.getText().trim();
            String description = txtDesc.getText();
            double price = Double.parseDouble(txtPrice.getText());

            LocalDateTime startTime = LocalDateTime.of(
                    startDatePicker.getValue(),
                    Integer.parseInt(startHourField.getText()),
                    Integer.parseInt(startMinuteField.getText())
            );
            LocalDateTime endTime = LocalDateTime.of(
                    endDatePicker.getValue(),
                    Integer.parseInt(endHourField.getText()),
                    Integer.parseInt(endMinuteField.getText())
            );

            if (endTime.isBefore(startTime)) {
                showMessage("Thời gian kết thúc phải sau thời gian bắt đầu.");
                return;
            }
            if (startTime.isBefore(LocalDateTime.now())) {
                showMessage("Không thể chọn thời gian bắt đầu trong quá khứ.");
                return;
            }

            Map<String, Object> payload = Map.of(
                    "name", name,
                    "description", description,
                    "startingPrice", price,
                    "imageUrl", imagePath,
                    "startTime", startTime.toString(),
                    "endTime", endTime.toString(),
                    "minBidStep", 10000,
                    "category", "OTHER"
            );

            AuctionClient.getInstance().sendRequest("CREATE_AUCTION", payload)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(response.getStatus())) {
                                showMessage("Đăng sản phẩm thành công!");
                                goBack();
                            } else {
                                showMessage("Lỗi: " + response.getData());
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showMessage("Lỗi kết nối: " + ex.getMessage()));
                        ex.printStackTrace();
                        return null;
                    });

        } catch (NumberFormatException e) {
            showMessage("Giá khởi điểm phải là số hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Lỗi: " + e.getMessage());
        }
    }

    private boolean validateInputs() {
        if (txtName.getText().trim().isEmpty()) { showMessage("Tên sản phẩm không được để trống."); return false; }
        if (txtPrice.getText().trim().isEmpty()) { showMessage("Giá khởi điểm không được để trống."); return false; }
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showMessage("Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc.");
            return false;
        }
        if (startHourField.getText().trim().isEmpty() || startMinuteField.getText().trim().isEmpty() ||
                endHourField.getText().trim().isEmpty() || endMinuteField.getText().trim().isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ giờ phút.");
            return false;
        }
        return true;
    }

    private void showMessage(String msg) {
        if (lblMessage != null) lblMessage.setText(msg);
        else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
            alert.showAndWait();
        }
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            imagePath = file.toURI().toString();
            if (lblMessage != null) lblMessage.setText("Đã chọn ảnh: " + file.getName());
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
            showMessage("Lỗi điều hướng: " + e.getMessage());
        }
    }
}
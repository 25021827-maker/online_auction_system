package Controller.product;

import Model.Product;
<<<<<<< HEAD
=======

import FakeDB.FakeDB;

>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
import Session.Session;
import client.AuctionClient;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
<<<<<<< HEAD
import javafx.scene.control.*;
=======

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.collections.FXCollections;

>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class ProductFormController {

<<<<<<< HEAD
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

=======
// =====================================================
// FXML
// =====================================================

    @FXML
    private TextField txtName;

    @FXML
    private TextArea txtDesc;

    @FXML
    private TextField txtPrice;

    // CATEGORY
    @FXML
    private ComboBox<String> categoryBox;

    // CONDITION
    @FXML
    private ComboBox<String> conditionBox;

    // START TIME
    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startHourField;

    @FXML
    private TextField startMinuteField;

    // END TIME
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField endHourField;

    @FXML
    private TextField endMinuteField;

// =====================================================
// IMAGE
// =====================================================

    private String imagePath = "";

// =====================================================
// INITIALIZE
// =====================================================

    @FXML
    public void initialize() {

        categoryBox.setItems(

                FXCollections.observableArrayList(

                        "Electronics",

                        "Fashion",

                        "Book",

                        "Furniture",

                        "Gaming",

                        "Vehicle",

                        "Other"
                )
        );

        conditionBox.setItems(

                FXCollections.observableArrayList(

                        "New",

                        "Like New",

                        "Used",

                        "Damaged"
                )
        );
    }

// =====================================================
// SAVE PRODUCT
// =====================================================

>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
    @FXML
    private void saveProduct() {
        if (lblMessage != null) lblMessage.setText("");

        if (!validateInputs()) return;

        try {
            String name = txtName.getText().trim();
            String description = txtDesc.getText();
            double price = Double.parseDouble(txtPrice.getText());

<<<<<<< HEAD
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

=======
            // =========================
            // BASIC DATA
            // =========================

            String name =
                    txtName.getText();

            String description =
                    txtDesc.getText();

            double price =
                    Double.parseDouble(
                            txtPrice.getText()
                    );

            String seller =
                    Session.currentUser.getUsername();

            String category =
                    categoryBox.getValue();

            String condition =
                    conditionBox.getValue();

            // =========================
            // VALIDATION
            // =========================

            if (name.isEmpty()) {

                System.out.println(
                        "Name không được để trống"
                );

                return;
            }

            if (description.isEmpty()) {

                System.out.println(
                        "Description không được để trống"
                );

                return;
            }

            if (price <= 0) {

                System.out.println(
                        "Price phải lớn hơn 0"
                );

                return;
            }

            // =========================
            // START TIME
            // =========================

            LocalDate startDate =
                    startDatePicker.getValue();

            int startHour =
                    Integer.parseInt(
                            startHourField.getText()
                    );

            int startMinute =
                    Integer.parseInt(
                            startMinuteField.getText()
                    );

            LocalDateTime startTime =
                    startDate.atTime(
                            startHour,
                            startMinute
                    );

            // =========================
            // END TIME
            // =========================

            LocalDate endDate =
                    endDatePicker.getValue();

            int endHour =
                    Integer.parseInt(
                            endHourField.getText()
                    );

            int endMinute =
                    Integer.parseInt(
                            endMinuteField.getText()
                    );

            LocalDateTime endTime =
                    endDate.atTime(
                            endHour,
                            endMinute
                    );

            // =========================
            // TIME VALIDATION
            // =========================

>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
            if (endTime.isBefore(startTime)) {
                showMessage("Thời gian kết thúc phải sau thời gian bắt đầu.");
                return;
            }
            if (startTime.isBefore(LocalDateTime.now())) {
                showMessage("Không thể chọn thời gian bắt đầu trong quá khứ.");
                return;
            }

<<<<<<< HEAD
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
=======
            if (startTime.isBefore(
                    LocalDateTime.now()
            )) {

                System.out.println(
                        "Không thể chọn thời gian trong quá khứ"
                );

                return;
            }

            // =========================
            // CREATE PRODUCT
            // =========================

            Product p = new Product(

                    name,

                    price,

                    imagePath,

                    seller,

                    startTime,

                    endTime,

                    description
            );

            // CATEGORY
            p.setCategory(category);

            // CONDITION
            p.setCondition(condition);

            // SAVE
            FakeDB.addProduct(p);

            System.out.println(
                    "Đăng sản phẩm thành công"
            );

            goBack();
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e

        } catch (NumberFormatException e) {
            showMessage("Giá khởi điểm phải là số hợp lệ.");
        } catch (Exception e) {
            e.printStackTrace();
<<<<<<< HEAD
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

=======
        }
    }

// =====================================================
// CHOOSE IMAGE
// =====================================================

>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
<<<<<<< HEAD
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
=======

                new FileChooser.ExtensionFilter(

                        "Image Files",

                        "*.png",

                        "*.jpg",

                        "*.jpeg"
                )
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            imagePath = file.toURI().toString();
            if (lblMessage != null) lblMessage.setText("Đã chọn ảnh: " + file.getName());
        }
    }

<<<<<<< HEAD
=======
// =====================================================
// GO BACK
// =====================================================

    // =====================================================
// GO BACK
// =====================================================
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) txtName.getScene().getWindow();
<<<<<<< HEAD
            Parent root = FXMLLoader.load(getClass().getResource("/ui/product/AuctionMain.fxml"));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Lỗi điều hướng: " + e.getMessage());
        }
    }
}
=======

            Parent root = FXMLLoader.load(
                    getClass().getResource("/ui/product/AuctionMain.fxml")
            );

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/style/pages/auction-main.css").toExternalForm()
            );

            stage.setScene(scene);

            // Phóng to lại kích thước chính khi quay về trang chủ và khóa resizable
            stage.setWidth(1280);
            stage.setHeight(750);
            stage.setResizable(false);
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e

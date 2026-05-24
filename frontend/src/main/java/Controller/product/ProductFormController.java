package Controller.product;

import Model.Product;

import FakeDB.FakeDB;

import Session.Session;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.collections.FXCollections;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductFormController {

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

    @FXML
    private void saveProduct() {

        try {

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

            if (endTime.isBefore(startTime)) {

                System.out.println(
                        "End time phải sau start time"
                );

                return;
            }

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

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

// =====================================================
// CHOOSE IMAGE
// =====================================================

    @FXML
    private void chooseImage() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Chọn ảnh"
        );

        fileChooser.getExtensionFilters().add(

                new FileChooser.ExtensionFilter(

                        "Image Files",

                        "*.png",

                        "*.jpg",

                        "*.jpeg"
                )
        );

        File file =
                fileChooser.showOpenDialog(null);

        if (file != null) {

            imagePath =
                    file.toURI().toString();
        }
    }

// =====================================================
// GO BACK
// =====================================================

    // =====================================================
// GO BACK
// =====================================================
    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) txtName.getScene().getWindow();

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

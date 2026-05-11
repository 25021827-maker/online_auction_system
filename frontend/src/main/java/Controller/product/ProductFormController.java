package Controller.product;

import Model.Product;
import FakeDB.FakeDB;
import Session.Session;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductFormController {

    @FXML
    private TextField txtName;

    @FXML
    private TextArea txtDesc;

    @FXML
    private TextField txtPrice;

    // =========================
    // START TIME
    // =========================
    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startHourField;

    @FXML
    private TextField startMinuteField;

    // =========================
    // END TIME
    // =========================
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField endHourField;

    @FXML
    private TextField endMinuteField;

    // =========================
    // IMAGE
    // =========================
    private String imagePath = "";

    // =========================
    // SAVE PRODUCT
    // =========================
    @FXML
    private void saveProduct() {

        try {

            String name =
                    txtName.getText();

            double price =
                    Double.parseDouble(
                            txtPrice.getText()
                    );

            String seller =
                    Session.currentUser.getUsername();

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
            // VALIDATION
            // =========================

            // END > START
            if (endTime.isBefore(startTime)) {

                System.out.println(
                        "End time phải sau start time"
                );

                return;
            }

            // START >= NOW
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
                    endTime

            );

            FakeDB.addProduct(p);

            System.out.println(
                    "Đăng sản phẩm thành công"
            );

            goBack();

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    // =========================
    // CHOOSE IMAGE
    // =========================
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

    // =========================
    // GO BACK
    // =========================
    @FXML
    private void goBack() {

        try {

            Stage stage =
                    (Stage) txtName
                            .getScene()
                            .getWindow();

            Parent root = FXMLLoader.load(

                    getClass().getResource(
                            "/ui/product/AuctionMain.fxml"
                    )

            );

            stage.setScene(
                    new Scene(root)
            );

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}

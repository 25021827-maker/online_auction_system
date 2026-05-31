package Controller.product;

import Model.Product;
import FakeDB.FakeDB;
import Session.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

    @FXML
    private ComboBox<String> categoryBox;

    @FXML
    private ComboBox<String> conditionBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startHourField;

    @FXML
    private TextField startMinuteField;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField endHourField;

    @FXML
    private TextField endMinuteField;

    // 🎯 MỚI BỔ SUNG: Ánh xạ nút xác nhận từ FXML để đổi chữ (Hãy chắc chắn nút trong FXML có fx:id="btnSubmit")
    @FXML
    private Button btnSubmit;

    // =====================================================
    // IMAGE
    // =====================================================
    private String imagePath = "";

    // =====================================================
    // 🎯 MỚI BỔ SUNG: BIẾN CỜ THEO DÕI CHẾ ĐỘ SỬA
    // =====================================================
    private Product editingProduct = null;

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
    // 🎯 MỚI BỔ SUNG: HÀM ĐỔ DATA CŨ LÊN FORM KHI SỬA
    // =====================================================
    public void setEditProduct(Product product) {
        this.editingProduct = product;

        // 1. Đổ thông tin cơ bản
        txtName.setText(product.getTitle());
        txtDesc.setText(product.getDescription());
        txtPrice.setText(String.valueOf(product.getCurrentPrice()));
        categoryBox.setValue(product.getCategory());
        conditionBox.setValue(product.getCondition());
        this.imagePath = product.getImagePath();

        // 🎯 BIẾN HÌNH NÚT: Đổi chữ nút bấm từ "CREATE" thành "SAVE CHANGES" để tránh nhầm lẫn
        if (btnSubmit != null) {
            btnSubmit.setText("SAVE CHANGES");
        }

        // 2. Bóc tách thời gian bắt đầu (LocalDateTime -> Date, Hour, Minute)
        if (product.getStartTime() != null) {
            startDatePicker.setValue(product.getStartTime().toLocalDate());
            startHourField.setText(String.format("%02d", product.getStartTime().getHour()));
            startMinuteField.setText(String.format("%02d", product.getStartTime().getMinute()));
        }

        // 3. Bóc tách thời gian kết thúc (LocalDateTime -> Date, Hour, Minute)
        if (product.getEndTime() != null) {
            endDatePicker.setValue(product.getEndTime().toLocalDate());
            endHourField.setText(String.format("%02d", product.getEndTime().getHour()));
            endMinuteField.setText(String.format("%02d", product.getEndTime().getMinute()));
        }
    }

    // =====================================================
    // SAVE PRODUCT (Đã sửa để tương thích cả Thêm và Sửa)
    // =====================================================
    @FXML
    private void saveProduct() {
        try {
            // =========================
            // BASIC DATA
            // =========================
            String name = txtName.getText();
            String description = txtDesc.getText();
            double price = Double.parseDouble(txtPrice.getText());
            String seller = Session.currentUser.getUsername();
            String category = categoryBox.getValue();
            String condition = conditionBox.getValue();

            // =========================
            // VALIDATION
            // =========================
            if (name.isEmpty()) {
                System.out.println("Name không được để trống");
                return;
            }

            if (description.isEmpty()) {
                System.out.println("Description không được để trống");
                return;
            }

            if (price <= 0) {
                System.out.println("Price phải lớn hơn 0");
                return;
            }

            // =========================
            // START TIME
            // =========================
            LocalDate startDate = startDatePicker.getValue();
            int startHour = Integer.parseInt(startHourField.getText());
            int startMinute = Integer.parseInt(startMinuteField.getText());
            LocalDateTime startTime = startDate.atTime(startHour, startMinute);

            // =========================
            // END TIME
            // =========================
            LocalDate endDate = endDatePicker.getValue();
            int endHour = Integer.parseInt(endHourField.getText());
            int endMinute = Integer.parseInt(endMinuteField.getText());
            LocalDateTime endTime = endDate.atTime(endHour, endMinute);

            // =========================
            // TIME VALIDATION
            // =========================
            if (endTime.isBefore(startTime)) {
                System.out.println("End time phải sau start time");
                return;
            }

            // Chỉ check lỗi thời gian quá khứ khi tạo mới (tránh lỗi khi sửa phòng đang chạy)
            if (editingProduct == null && startTime.isBefore(LocalDateTime.now())) {
                System.out.println("Không thể chọn thời gian trong quá khứ");
                return;
            }

            // =====================================================
            // XỬ LÝ LƯU (PHÂN TÁCH LUỒNG THÊM / SỬA)
            // =====================================================
            if (editingProduct == null) {
                // LUỒNG 1: TẠO MỚI SẢN PHẨM
                Product p = new Product(
                        name,
                        price,
                        imagePath,
                        seller,
                        startTime,
                        endTime,
                        description
                );
                p.setCategory(category);
                p.setCondition(condition);

                FakeDB.addProduct(p);
                System.out.println("Đăng sản phẩm thành công");
            } else {
                // =====================================================
                // 🎯 LUỒNG 2: ĐÃ CẬP NHẬT HOÀN CHỈNH THÔNG TIN SỬA
                // =====================================================
                editingProduct.setTitle(name); // Cập nhật Tên mới
                editingProduct.setCurrentPrice(price); // Cập nhật Giá khởi điểm mới
                editingProduct.setImagePath(imagePath); // Cập nhật Ảnh mới (nếu có)
                editingProduct.setCategory(category);
                editingProduct.setCondition(condition);
                editingProduct.setStartTime(startTime);
                editingProduct.setEndTime(endTime);
                editingProduct.setDescription(description);

                FakeDB.updateProduct(editingProduct);
                System.out.println("Cập nhật sản phẩm thành công");
            }

            // Quay lại trang chính sau khi hoàn thành công việc
            goBack();

        } catch (Exception e) {
            System.out.println("Có lỗi xảy ra khi lưu sản phẩm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =====================================================
    // CHOOSE IMAGE
    // =====================================================
    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            imagePath = file.toURI().toString();
        }
    }

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
            if (getClass().getResource("/style/pages/auction-main.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/style/pages/auction-main.css").toExternalForm()
                );
            }

            stage.setScene(scene);

            stage.setWidth(1280);
            stage.setHeight(750);
            stage.setResizable(false);
            stage.centerOnScreen();

        } catch (Exception e) {
            System.out.println("Lỗi khi điều hướng quay lại: " + e.getMessage());
        }
    }
}
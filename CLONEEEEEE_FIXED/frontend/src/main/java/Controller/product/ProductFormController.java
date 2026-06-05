package Controller.product;

import Service.core.SceneNavigator;
import Session.Session;
import network.SocketClient;
import dto.RequestPayload;
import dto.ResponsePayload;
import dto.CreateAuctionRequest;
import com.google.gson.Gson;
import util.VietnamTime;

import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ProductFormController {
    private static final long MIN_AUCTION_DURATION_MINUTES = 5;

    // Khớp 100% với fx:id trong ProductForm.fxml
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

    @FXML
    private Button btnSubmit;

    private final Gson gson = new Gson();
    private Model.Product editingProduct = null;
    private String selectedImagePath = "";
    private File selectedImageFile = null;


    @FXML
    public void initialize() {
        // Nạp dữ liệu cho ComboBox
        if (categoryBox != null) {
            categoryBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE", "OTHER");
            categoryBox.getSelectionModel().selectFirst();
        }
        if (conditionBox != null) {
            conditionBox.getItems().addAll("New", "Like New", "Good", "Used", "For Parts");
            conditionBox.getSelectionModel().selectFirst();
        }

        SocketClient socketClient = SocketClient.getInstance();
        socketClient.clearListeners("GET_ACTIVE_AUCTIONS_RESPONSE");
        socketClient.clearListeners("NEW_BID_EVENT");
        socketClient.clearListeners("NEW_AUCTION_EVENT");
        socketClient.clearListeners("CREATE_AUCTION_RESPONSE");
        socketClient.clearListeners("UPDATE_AUCTION_RESPONSE");
        socketClient.on("CREATE_AUCTION_RESPONSE", this::handleCreateResponse);
    }

    // Hàm gắn với onAction="#chooseImage" trong FXML
    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            selectedImageFile = file;
            selectedImagePath = file.toURI().toString(); // Lưu dạng URI để JavaFX đọc được
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã nạp ảnh thành công!");
        }
    }

    // Hàm gắn với onAction="#saveProduct" trong FXML
    @FXML
    private void saveProduct(ActionEvent event) {
        String name = txtName.getText();
        String desc = txtDesc.getText();
        String priceStr = txtPrice.getText();

        if (name == null || name.trim().isEmpty() || priceStr == null || priceStr.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Tên và giá khởi điểm không được để trống!");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Giá khởi điểm phải là một số lớn hơn 0!");
            return;
        }

        // Lấy thời gian từ các DatePicker và TextField
        LocalDateTime startDateTime = parseDateTime(startDatePicker, startHourField, startMinuteField);
        LocalDateTime endDateTime = parseDateTime(endDatePicker, endHourField, endMinuteField);

        if (startDateTime == null || endDateTime == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ và đúng định dạng ngày/giờ!");
            return;
        }

        if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
            return;
        }

        if (startDateTime.isBefore(VietnamTime.now().minusMinutes(1))) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Thời gian bắt đầu không được nằm trong quá khứ.");
            return;
        }

        if (Duration.between(startDateTime, endDateTime).toMinutes() < MIN_AUCTION_DURATION_MINUTES) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Phiên đấu giá phải kéo dài tối thiểu " + MIN_AUCTION_DURATION_MINUTES + " phút.");
            return;
        }

        if (Session.getCurrentUser() == null || !"SELLER".equals(Session.getCurrentUser().getRole())) {
            showAlert(Alert.AlertType.ERROR, "Từ chối truy cập", "Chỉ tài khoản người bán (SELLER) mới được tạo phiên đấu giá!");
            return;
        }

        btnSubmit.setDisable(true); // Tránh spam click
        btnSubmit.setText("ĐANG XỬ LÝ...");

        // Đóng gói DTO
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.sellerId = (long) Session.getCurrentUser().getId();
        req.auctionId = (editingProduct != null) ? (long) editingProduct.getId() : null;
        req.itemName = name;
        req.description = desc;
        req.startingPrice = price;
        // Bắt lỗi null ở ComboBox để tránh sập Server
        req.category = categoryBox.getValue() != null ? categoryBox.getValue() : "OTHER";
        req.condition = conditionBox.getValue() != null ? conditionBox.getValue() : "New";
        req.startTime = startDateTime.toString();
        req.endTime = endDateTime.toString();
        req.imagePath = selectedImagePath;

        if (selectedImageFile != null) {
            try {
                req.imageBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(selectedImageFile.toPath()));
                req.imageFileName = selectedImageFile.getName();
                req.imagePath = "";
            } catch (IOException e) {
                btnSubmit.setDisable(false);
                btnSubmit.setText(editingProduct != null ? "UPDATE PRODUCT" : "CREATE");
                showAlert(Alert.AlertType.ERROR, "Lỗi ảnh", "Không thể đọc file ảnh đã chọn.");
                return;
            }
        }

        String actionName = (editingProduct != null) ? "UPDATE_AUCTION" : "CREATE_AUCTION";
        RequestPayload payload = new RequestPayload(actionName, gson.toJson(req));
        SocketClient.getInstance().sendRequest(payload);
    }

    // Tiện ích gom DatePicker và TextField thành LocalDateTime
    private LocalDateTime parseDateTime(DatePicker datePicker, TextField hourField, TextField minField) {
        LocalDate date = datePicker.getValue();
        if (date == null) return null;
        try {
            String hourText = hourField.getText() == null ? "" : hourField.getText().trim();
            String minText = minField.getText() == null ? "" : minField.getText().trim();
            int hour = hourText.isEmpty() ? 0 : Integer.parseInt(hourText);
            int min = minText.isEmpty() ? 0 : Integer.parseInt(minText);
            if (hour < 0 || hour > 23 || min < 0 || min > 59) return null;
            return LocalDateTime.of(date, LocalTime.of(hour, min));
        } catch (Exception e) {
            return null;
        }
    }

    private void handleCreateResponse(ResponsePayload response) {
        Platform.runLater(() -> {
            btnSubmit.setDisable(false);
            btnSubmit.setText(editingProduct != null ? "UPDATE PRODUCT" : "CREATE");

            if ("SUCCESS".equals(response.getStatus())) {
                if (editingProduct != null) {
                    showAlert(Alert.AlertType.INFORMATION, "Update", "Product updated successfully.");
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Chờ admin duyệt", "Phiên đấu giá đã được gửi và sẽ hiển thị sau khi admin phê duyệt.");
                }
                try {
                    SceneNavigator.loadFromNode(txtName, "/ui/product/AuctionMain.fxml", "Sàn đấu giá");
                } catch (Exception e) {
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi Server", response.getMessage());
            }
        });
    }

    // Hàm gắn với onAction="#goBack"
    @FXML
    private void goBack(ActionEvent event) {
        SceneNavigator.load(event, "/ui/product/AuctionMain.fxml", "Sàn đấu giá");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    public void setEditData(Model.Product p) {
        this.editingProduct = p;

        this.selectedImagePath = p.getImagePath() == null ? "" : p.getImagePath();
        this.selectedImageFile = null;

        txtName.setText(p.getTitle());
        txtDesc.setText(p.getDescription());
        txtPrice.setText(String.valueOf(p.getCurrentPrice()));

        if (categoryBox != null) {
            categoryBox.setValue(p.getCategory());
        }

        if (conditionBox != null) {
            conditionBox.setValue(p.getCondition());
        }

        startDatePicker.setValue(p.getStartTime().toLocalDate());
        startHourField.setText(String.valueOf(p.getStartTime().getHour()));
        startMinuteField.setText(String.valueOf(p.getStartTime().getMinute()));

        endDatePicker.setValue(p.getEndTime().toLocalDate());
        endHourField.setText(String.valueOf(p.getEndTime().getHour()));
        endMinuteField.setText(String.valueOf(p.getEndTime().getMinute()));

        btnSubmit.setText("UPDATE PRODUCT");

        SocketClient.getInstance().on("UPDATE_AUCTION_RESPONSE", this::handleCreateResponse);
    }
}

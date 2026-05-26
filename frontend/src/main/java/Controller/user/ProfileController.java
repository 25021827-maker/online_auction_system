package Controller.user;

<<<<<<< HEAD
import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dto.AuctionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import util.UserSession;

import java.util.List;
=======
import FakeDB.FakeDB;
import Model.Product;
import Session.Session;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle; // Cần dùng để bo góc ảnh

import java.io.File;
import java.util.Optional;
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label balanceLabel;
    @FXML private ListView<String> myAuctionsListView;

<<<<<<< HEAD
    private Gson gson = new Gson();
    private AuctionClient auctionClient = AuctionClient.getInstance();

    @FXML
    public void initialize() {
        // Sử dụng UserSession để lấy thông tin người dùng hiện tại
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() != null) {
            usernameLabel.setText(session.getUsername());
            balanceLabel.setText(String.format("%.2f VND", session.getBalance()));
        } else {
            usernameLabel.setText("Chưa đăng nhập");
            balanceLabel.setText("0.00 VND");
=======
    @FXML
    private Label lblBalance;

    @FXML
    private Label proofLabel;

    @FXML
    private VBox myProductsBox;

    @FXML
    private ImageView qrImageView;

    // =====================================================
    // USER AVATAR (MỚI BỔ SUNG)
    // =====================================================
    @FXML
    private ImageView imgProfileAvatar; // ImageView hiển thị avatar lớn trong profile

    // ảnh xác minh nạp tiền
    private String proofImagePath = "";

    // số tiền pending
    private double pendingAmount = 0;

    @FXML
    public void initialize() {

        // USERNAME
        lblUsername.setText(
                Session.currentUser.getUsername()
        );

        // BALANCE
        updateBalance();

        // PRODUCTS
        loadMyProducts();

        // LOAD USER AVATAR (MỚI BỔ SUNG)
        setupProfileAvatar();

        // LOAD QR IMAGE
        try {
            qrImageView.setImage(
                    new Image(
                            getClass().getResourceAsStream(
                                    "/images/myqr.png"
                            )
                    )
            );
        } catch (Exception e) {
            System.out.println("Không tìm thấy QR");
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
        }
        loadMyAuctions();
    }

<<<<<<< HEAD
    private void loadMyAuctions() {
        // Gửi yêu cầu lấy danh sách đấu giá của seller hiện tại
        // Server cần có action "GET_MY_AUCTIONS"
        auctionClient.sendRequest("GET_MY_AUCTIONS", null)
                .thenAccept(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        List<AuctionDTO> auctions = gson.fromJson(
                                gson.toJson(response.getData()),
                                new TypeToken<List<AuctionDTO>>(){}.getType()
                        );
                        Platform.runLater(() -> displayMyAuctions(auctions));
                    } else {
                        Platform.runLater(() -> showError("Không thể tải danh sách đấu giá: " + response.getMessage()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Lỗi kết nối: " + ex.getMessage()));
                    return null;
                });
    }

    private void displayMyAuctions(List<AuctionDTO> auctions) {
        myAuctionsListView.getItems().clear();
        if (auctions == null || auctions.isEmpty()) {
            myAuctionsListView.getItems().add("Bạn chưa có phiên đấu giá nào.");
            return;
        }
        for (AuctionDTO a : auctions) {
            String displayName = (a.getItemName() != null && !a.getItemName().isEmpty())
                    ? a.getItemName()
                    : "Auction " + a.getAuctionId();
            myAuctionsListView.getItems().add(
                    displayName + " | Giá: " + a.getCurrentPrice() + " | Trạng thái: " + a.getStatus()
            );
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
=======
    // =====================================================
    // SETUP PROFILE AVATAR (MỚI BỔ SUNG)
    // =====================================================
    private void setupProfileAvatar() {
        if (Session.currentUser != null && imgProfileAvatar != null) {
            String userAvatarPath = Session.currentUser.getAvatarPath();

            if (userAvatarPath != null && !userAvatarPath.isEmpty()) {
                imgProfileAvatar.setImage(new Image(userAvatarPath));
            } else {
                // Nếu chưa có ảnh, nạp ảnh mặc định từ resources
                String defaultAvatarUrl = getClass().getResource("/images/defaultavatar.png").toExternalForm();
                imgProfileAvatar.setImage(new Image(defaultAvatarUrl));
            }

            // Bo góc chữ nhật 15px cho ảnh đại diện to trong trang cá nhân (fit 100x100 hoặc 120x120 tùy FXML)
            // Lấy trực tiếp fitWidth/fitHeight từ FXML để tạo Rectangle cắt chuẩn xác nhất
            double width = imgProfileAvatar.getFitWidth() > 0 ? imgProfileAvatar.getFitWidth() : 100;
            double height = imgProfileAvatar.getFitHeight() > 0 ? imgProfileAvatar.getFitHeight() : 100;

            Rectangle clip = new Rectangle(width, height);
            clip.setArcWidth(15);
            clip.setArcHeight(15);
            imgProfileAvatar.setClip(clip);
        }
    }

    // =====================================================
    // HANDLE UPLOAD AVATAR (MỚI BỔ SUNG)
    // =====================================================
    @FXML
    private void handleUploadAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện của bạn");

        // Chỉ hiển thị các định dạng file ảnh
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // Mở hộp thoại chọn file lấy Stage từ event gốc
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Chuyển đổi file thành chuỗi URL hợp lệ cho JavaFX Image đọc
                String newAvatarPath = selectedFile.toURI().toString();

                // Lưu đường dẫn ảnh mới vào Session của User hiện tại
                if (Session.currentUser != null) {
                    Session.currentUser.setAvatarPath(newAvatarPath);
                    // Nếu bạn có hàm lưu DB như FakeDB.save() thì gọi ở đây
                }

                // Cập nhật lại giao diện trang Profile ngay tức thì
                imgProfileAvatar.setImage(new Image(newAvatarPath));

            } catch (Exception e) {
                System.out.println("Không thể nạp file ảnh avatar mới: " + e.getMessage());
            }
        }
    }

    // =========================
    // UPDATE BALANCE
    // =========================
    private void updateBalance() {
        lblBalance.setText(
                "$" + String.format("%.2f", Session.currentUser.getBalance())
        );
    }

    // =========================
    // INPUT MONEY
    // =========================
    @FXML
    private void handleAddMoney() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nhập số tiền muốn nạp");
        dialog.setContentText("Amount:");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            try {
                double amount = Double.parseDouble(result.get());
                if (amount > 0) {
                    pendingAmount = amount;
                    proofLabel.setText("Đang chờ xác minh cho " + amount + "$");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // =========================
    // CHOOSE PROOF IMAGE
    // =========================
    @FXML
    private void chooseProofImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh xác minh");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // Sửa truyền null thành lấy Stage của app để tránh lỗi focus cửa sổ
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            proofImagePath = file.toURI().toString();
            proofLabel.setText("Đã tải ảnh xác minh");
        }
    }

    // =========================
    // FAKE PAYMENT
    // =========================
    @FXML
    private void handleFakePayment() {
        if (pendingAmount <= 0) {
            proofLabel.setText("Hãy nhập số tiền trước");
            return;
        }

        if (proofImagePath.isEmpty()) {
            proofLabel.setText("Phải tải ảnh xác minh");
            return;
        }

        Session.currentUser.addMoney(pendingAmount);
        updateBalance();
        proofLabel.setText("Nạp " + pendingAmount + "$ thành công");

        pendingAmount = 0;
        proofImagePath = "";
    }

    // =========================
    // LOAD PRODUCTS
    // =========================
    private void loadMyProducts() {
        myProductsBox.getChildren().clear();
        for (Product p : FakeDB.getProducts()) {
            if (p.getSeller().equals(Session.currentUser.getUsername())) {
                Label label = new Label(
                        p.getTitle() + " - $" + p.getCurrentPrice()
                );
                myProductsBox.getChildren().add(label);
            }
        }
    }

    // =========================
    // BACK HOME
    // =========================
    @FXML
    private void goHome(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(
                    getClass().getResource("/ui/product/AuctionMain.fxml")
            );
            stage.setScene(new Scene(root));
            stage.setWidth(1280);
            stage.setHeight(750);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
>>>>>>> b7d3a129137e941cebe93c46ef4ee705c7f2ac2e
    }
}
package Controller.user;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

public class AdminController {
    @FXML private Label lblTitle;
    @FXML private StackPane contentArea;
    @FXML private Label lblWelcome;
    @FXML private TableView<Object> tableCommon;

    @FXML
    private void switchDashboard() {
        lblTitle.setText("8. Thống kê hệ thống");
        lblWelcome.setText("Biểu đồ doanh thu và chỉ số người dùng...");
        tableCommon.setVisible(false);
    }

    @FXML
    private void switchUsers() {
        lblTitle.setText("9. Danh sách người dùng");
        lblWelcome.setText("");
        tableCommon.setVisible(true);
        // Sau này bạn sẽ thêm code đổ dữ liệu User vào bảng này
    }

    @FXML
    private void switchProducts() {
        lblTitle.setText("10. Kiểm duyệt sản phẩm");
        lblWelcome.setText("Danh sách sản phẩm đang chờ Admin phê duyệt...");
        tableCommon.setVisible(true);
    }

    @FXML
    private void switchAuctions() {
        lblTitle.setText("11. Quản lý các phiên đấu giá");
        lblWelcome.setText("Can thiệp hoặc đóng các phiên đang chạy...");
        tableCommon.setVisible(true);
    }
}

package Service.core;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class SceneNavigator {

    // Định nghĩa kích thước cố định theo cấu trúc chuẩn của bạn
    private static final double MAIN_WIDTH = 1280;
    private static final double MAIN_HEIGHT = 750;

    private static final double FORM_WIDTH = 850;
    private static final double FORM_HEIGHT = 700;

    private static final double AUTH_WIDTH = 450;
    private static final double AUTH_HEIGHT = 600;

    public static void load(ActionEvent event, String path, String title) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            loadScreen(stage, path, title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadFromNode(Node node, String path, String title) {
        try {
            Stage stage = (Stage) node.getScene().getWindow();
            loadScreen(stage, path, title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadScreen(Stage stage, String path, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(path));
        Parent root = loader.load();

        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        stage.setTitle(title);

        // PHÂN CHIA KÍCH THƯỚC THEO TÊN FILE FXML
        if (path.contains("ProductForm.fxml")) {
            // Màn đăng sản phẩm nhỏ gọn
            stage.setWidth(FORM_WIDTH);
            stage.setHeight(FORM_HEIGHT);
        } else if (path.contains("Login.fxml") || path.contains("Register.fxml")) {
            // Các màn hình Auth bé hơn
            stage.setWidth(AUTH_WIDTH);
            stage.setHeight(AUTH_HEIGHT);
        } else {
            // Tất cả các màn hình chức năng còn lại bằng nhau chằn chặn
            stage.setWidth(MAIN_WIDTH);
            stage.setHeight(MAIN_HEIGHT);
        }

        // KHÓA CỨNG: Không cho phép kéo dãn khung hoặc bấm nút phóng to
        stage.setResizable(false);

        // ĐƯA RA CHÍNH GIỮA MÀN HÌNH LAPTOP
        stage.centerOnScreen();

        stage.show();
    }
}
package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Sửa dòng này để app luôn bật màn hình đăng nhập đầu tiên
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/Login.fxml"));
        Scene scene = new Scene(loader.load());

        stage.setTitle("Hệ thống Đấu giá");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/auth/Login.fxml"));
        Scene scene = new Scene(loader.load());

        stage.setTitle("Hệ thống Đấu giá");
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icon đấu giá.png")));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

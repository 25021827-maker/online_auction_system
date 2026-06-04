package Main;

import Service.core.SceneNavigator;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import util.VietnamTime;

import java.util.TimeZone;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("BVBID");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon dau gia.png")));
        SceneNavigator.loadInitial(stage, "/ui/auth/Login.fxml", "BVBID");
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(VietnamTime.ZONE));
        launch();
    }
}

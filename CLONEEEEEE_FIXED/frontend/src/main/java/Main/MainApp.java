package Main;

import Service.core.SceneNavigator;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("BVBID");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon dau gia.png")));
        SceneNavigator.loadInitial(stage, "/ui/auth/Login.fxml", "BVBID");
    }

    public static void main(String[] args) {
        launch();
    }
}

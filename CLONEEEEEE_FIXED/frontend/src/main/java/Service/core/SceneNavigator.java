package Service.core;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

public class SceneNavigator {

    public static void loadInitial(Stage stage, String path, String title) {
        try {
            loadScreen(stage, path, title);
        } catch (Exception e) {
            System.err.println("Loi khi load FXML: " + path);
            e.printStackTrace();
        }
    }

    public static void load(ActionEvent event, String path, String title) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            loadScreen(stage, path, title);
        } catch (Exception e) {
            System.err.println("Loi khi load FXML: " + path);
            e.printStackTrace();
        }
    }

    public static void loadFromNode(Node node, String fxml, String title) {
        try {
            Stage stage = findStage(node);
            if (stage != null) {
                loadScreen(stage, fxml, title);
            } else {
                System.err.println("Khong tim thay Stage nao de chuyen trang.");
            }
        } catch (Exception e) {
            System.err.println("Loi khi load FXML: " + fxml);
            e.printStackTrace();
        }
    }

    public static void showFixedFullScreen(Stage stage, Parent root, String title) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        stage.setTitle(title);
        applyFixedFullScreenSize(stage);
        stage.show();
    }

    private static void loadScreen(Stage stage, String path, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(path));
        Parent root = loader.load();
        showFixedFullScreen(stage, root, title);
    }

    private static Stage findStage(Node node) {
        if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage) {
            return (Stage) node.getScene().getWindow();
        }

        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window instanceof Stage) {
                return (Stage) window;
            }
        }
        return null;
    }

    private static void applyFixedFullScreenSize(Stage stage) {
        Rectangle2D bounds = getCurrentScreenBounds(stage);

        stage.setFullScreen(false);
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.setMinWidth(bounds.getWidth());
        stage.setMinHeight(bounds.getHeight());
        stage.setMaxWidth(bounds.getWidth());
        stage.setMaxHeight(bounds.getHeight());
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private static Rectangle2D getCurrentScreenBounds(Stage stage) {
        double width = Math.max(stage.getWidth(), 1);
        double height = Math.max(stage.getHeight(), 1);

        return Screen.getScreensForRectangle(stage.getX(), stage.getY(), width, height)
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();
    }
}

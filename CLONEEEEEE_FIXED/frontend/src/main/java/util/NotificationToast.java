package util;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public final class NotificationToast {
    private static final double TOAST_WIDTH = 340;
    private static final double EDGE_MARGIN = 24;
    private static final double TOP_OFFSET = 64;

    private NotificationToast() {
    }

    public static void show(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            showOnFxThread(title, message);
        } else {
            Platform.runLater(() -> showOnFxThread(title, message));
        }
    }

    private static void showOnFxThread(String title, String message) {
        Window owner = Window.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);

        if (owner == null) {
            return;
        }

        Label titleLabel = new Label(safeText(title, "Notification"));
        titleLabel.setWrapText(true);
        titleLabel.setStyle("""
                -fx-text-fill: #1f2933;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                """);

        Label messageLabel = new Label(safeText(message, ""));
        messageLabel.setWrapText(true);
        messageLabel.setStyle("""
                -fx-text-fill: #39434d;
                -fx-font-size: 13px;
                """);

        VBox root = new VBox(6, titleLabel, messageLabel);
        root.setPadding(new Insets(14, 16, 14, 16));
        root.setPrefWidth(TOAST_WIDTH);
        root.setStyle("""
                -fx-background-color: #fff8d7;
                -fx-background-radius: 14;
                -fx-border-color: #d2a900;
                -fx-border-radius: 14;
                -fx-border-width: 1;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0.2, 0, 6);
                """);

        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoFix(true);
        popup.setAutoHide(true);

        double x = owner.getX() + owner.getWidth() - TOAST_WIDTH - EDGE_MARGIN;
        double y = owner.getY() + TOP_OFFSET;
        popup.show(owner, Math.max(owner.getX() + EDGE_MARGIN, x), y);

        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

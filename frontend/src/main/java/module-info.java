module demo {
    requires javafx.controls;
    requires javafx.fxml;

    opens Main to javafx.graphics, javafx.fxml;
    opens Model to javafx.base;

    opens Controller.auth to javafx.fxml;
    opens Controller.product to javafx.fxml;
    opens Controller.user to javafx.fxml;

    exports Main;
}
module demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.desktop;


    opens Main to javafx.graphics, javafx.fxml;
    opens Model to javafx.base, com.google.gson;

    opens Controller.auth to javafx.fxml;
    opens Controller.product to javafx.fxml;
    opens Controller.user to javafx.fxml;

    exports Main;
}
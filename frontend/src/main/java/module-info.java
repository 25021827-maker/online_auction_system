module demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.logging;

    opens Main to javafx.graphics, javafx.fxml;
    opens Model to javafx.base, com.google.gson;
    opens Controller.auth to javafx.fxml;
    opens Controller.product to javafx.fxml;
    opens Controller.user to javafx.fxml;
    opens dto to com.google.gson;
    opens network to com.google.gson;
    opens client to com.google.gson;   // thêm
    exports Main;
}
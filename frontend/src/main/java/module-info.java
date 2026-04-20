module demo {
    requires javafx.controls;
    requires javafx.fxml;

    opens Main to javafx.graphics, javafx.fxml;
    opens Controller to javafx.fxml;
    opens Model to javafx.base; // Cần thiết để TableView hiển thị dữ liệu từ Model

    exports Main;
}
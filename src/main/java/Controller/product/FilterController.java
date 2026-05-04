package Controller.product;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;

public class FilterController {

    @FXML
    private void handleApply(ActionEvent event) {
        // Sau này bạn code logic lọc dữ liệu ở đây
        handleClose(event);
    }

    @FXML
    private void handleClose(ActionEvent event) {
        // Lấy cửa sổ hiện tại và đóng nó lại
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
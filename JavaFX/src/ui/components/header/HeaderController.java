package ui.components.header;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;


public class HeaderController {
    @FXML
    private Button loadFileBtn;

    @FXML
    private Label loadFileLbl;

    @FXML
    private void initialize() {
        loadFileLbl.prefHeightProperty().bind(loadFileBtn.heightProperty());
    }
}

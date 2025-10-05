package ui.runner.components.center_left;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class CenterLeftController {
    @FXML private Button expandBtn;
    @FXML private Label currDegreeLbl;
    @FXML private ChoiceBox chooseVarBox;

    @FXML
    private void initialize(){
        currDegreeLbl.prefHeightProperty().bind(expandBtn.heightProperty());
    }
}

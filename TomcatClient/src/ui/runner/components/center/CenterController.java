// java
package ui.runner.components.center;

import javafx.fxml.FXML;
import ui.runner.components.instruction_table.InstructionTableController;

public class CenterController {
    @FXML private InstructionTableController instructionTopController;
    @FXML private InstructionTableController instructionBottomController;

    @FXML
    private void initialize() {
        if (instructionTopController != null) {
            instructionTopController.init(true);   // load selected program here
        }
        if (instructionBottomController != null) {
            instructionBottomController.init(false); // keep bottom table empty
        }
    }
}

package ui.runner.components.center;

import javafx.fxml.FXML;
import ui.runner.SelectedProgram;
import ui.runner.components.instruction_table.InstructionTableController;

public class CenterController {

    @FXML private InstructionTableController instructionTopController;
    @FXML private InstructionTableController instructionBottomController;

    @FXML
    private void initialize() {
        String selected = SelectedProgram.get();  // fetch currently selected program name
        if (instructionTopController != null && selected != null) {
            instructionTopController.loadInstructions(selected);
        }
        if (instructionBottomController != null) {
            // leave empty (no instructions loaded)
        }
    }
}

// java
package ui.runner.components.center_left;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import ui.runner.components.instruction_table.InstructionTableController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CenterLeftController {
    @FXML private Button expandBtn;
    @FXML private Label currDegreeLbl;
    @FXML private ChoiceBox<String> chooseVarBox;

    // Must match fx:id + "Controller" for <fx:include fx:id="instructionTable"/> and "ancestryTable"
    @FXML private InstructionTableController instructionTableController;
    @FXML private InstructionTableController ancestryTableController;

    private String programName;
    private int maxDegree = 0;
    private int currDegree = 0;

    @FXML
    private void initialize() {
        updateDegreeLabel();
    }

    // Called by CenterController after injection
    public void init(String program, int maxDegree) {
        this.programName = program;
        this.maxDegree = Math.max(0, maxDegree);
        if (currDegree > this.maxDegree) currDegree = this.maxDegree;
        updateDegreeLabel();

        // Ensure initial load reflects the current program + degree
        if (instructionTableController != null && programName != null && !programName.isBlank()) {
            instructionTableController.loadInstructions(programName, currDegree);
        }
    }

    @FXML
    private void onExpand() {
        if (programName == null || programName.isBlank()) return;

        List<String> options = new ArrayList<>();
        for (int d = 0; d <= maxDegree; d++) {
            options.add(d + (d == currDegree ? " (current)" : ""));
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currDegree + " (current)", options);
        dialog.setTitle("Pick degree");
        dialog.setHeaderText(null);
        dialog.setContentText("Degree:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(sel -> {
            try {
                int d = Integer.parseInt(sel.replace(" (current)", "").trim());
                if (d < 0 || d > maxDegree || d == currDegree) return;

                currDegree = d;
                updateDegreeLabel();

                if (instructionTableController != null) {
                    instructionTableController.loadInstructions(programName, currDegree);
                }
                // ancestryTableController can be populated similarly if/when ancestry data exists
            } catch (NumberFormatException ignore) {
                // ignore invalid selection
            }
        });
    }

    private void updateDegreeLabel() {
        if (currDegreeLbl != null) {
            currDegreeLbl.setText("Degree " + currDegree + " / " + maxDegree);
        }
    }
}

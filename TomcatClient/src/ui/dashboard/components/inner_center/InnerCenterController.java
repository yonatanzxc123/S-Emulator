// java
package ui.dashboard.components.inner_center;

import javafx.fxml.FXML;
import ui.dashboard.components.inner_center_right.InnerCenterRightController;
import ui.dashboard.components.function_table.FunctionTableController;
import ui.dashboard.components.program_table.ProgramTableController;

public class InnerCenterController {
    // Left side is present in FXML, but not needed for this wiring
    @FXML private InnerCenterRightController innerCenterRightController;

    public InnerCenterRightController rightController() {
        return innerCenterRightController;
    }

    // Convenience accessors for callers that don't care about the right wrapper
    public ProgramTableController programTableController() {
        return innerCenterRightController != null ? innerCenterRightController.programTableController() : null;
    }

    public FunctionTableController functionTableController() {
        return innerCenterRightController != null ? innerCenterRightController.functionTableController() : null;
    }
}

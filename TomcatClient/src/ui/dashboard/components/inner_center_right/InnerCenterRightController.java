// java
package ui.dashboard.components.inner_center_right;

import javafx.fxml.FXML;
import ui.dashboard.components.function_table.FunctionTableController;
import ui.dashboard.components.program_table.ProgramTableController;

public class InnerCenterRightController {
    @FXML private ProgramTableController programsTableController;
    @FXML private FunctionTableController functionsTableController;

    public ProgramTableController programTableController() { return programsTableController; }
    public FunctionTableController functionTableController() { return functionsTableController; }
}

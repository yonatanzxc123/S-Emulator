// java
package ui.dashboard.components.center;

import javafx.fxml.FXML;
import ui.AppContext;
import ui.dashboard.components.file_line.FileLineController;
import ui.dashboard.components.inner_center.InnerCenterController;
import ui.dashboard.components.inner_center_right.InnerCenterRightController;
import ui.dashboard.components.function_table.FunctionTableController;
import ui.dashboard.components.program_table.ProgramTableController;

public class CenterController {
    @FXML private FileLineController fileLineController;
    @FXML private InnerCenterController innerCenterController;

    public void init(AppContext ctx) {
        if (innerCenterController == null) return;

        InnerCenterRightController right = innerCenterController.rightController();
        if (right == null) return;

        ProgramTableController program = right.programTableController();
        FunctionTableController functions = right.functionTableController();

        if (program != null) program.init();
        if (functions != null) functions.init();
        if (fileLineController != null) fileLineController.init(ctx, program, functions);
    }
}

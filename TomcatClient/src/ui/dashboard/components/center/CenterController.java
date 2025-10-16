// java
package ui.dashboard.components.center;

import javafx.application.Platform;
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

        if (functions != null) functions.init();
        if (fileLineController != null) fileLineController.init(ctx, program, functions);

        // Load existing catalog so tables show already-uploaded programs/functions
        if (program != null && functions != null && ctx != null) {
            Thread t = new Thread(() -> {
                try {
                    var catalog = ctx.api().listPrograms();
                    Platform.runLater(() -> {
                        for (var p : catalog) {
                            program.addProgram(p.name, p.owner, p.instrDeg0, p.maxDegree);
                            if (p.functions != null && !p.functions.isEmpty()) {
                                functions.addFunctions(p.name, p.owner, p.functions);
                            }
                        }
                    });
                } catch (Exception ignored) {
                    // keep UI responsive even if server is empty/down
                }
            }, "load-catalog");
            t.setDaemon(true);
            t.start();
        }

    }
}

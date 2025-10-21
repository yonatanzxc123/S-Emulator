// java
package ui.dashboard.components.inner_center_right;

import javafx.fxml.FXML;
import ui.AppContext;
import ui.ClientApp;
import ui.dashboard.components.function_table.FunctionTableController;
import ui.dashboard.components.program_table.ProgramTableController;

public class InnerCenterRightController {
    @FXML private ProgramTableController programsTableController;
    @FXML private FunctionTableController functionsTableController;

    public void setClientApp(ClientApp app) {
        if (programsTableController != null) {
            programsTableController.setClientApp(app);
        }
        if (functionsTableController != null) {
            functionsTableController.setClientApp(app);
        }
    }

    public void setAppContext(AppContext ctx) {
        if (programsTableController != null) {
            programsTableController.setAppContext(ctx);
        }
        if (functionsTableController != null) {
            functionsTableController.setAppContext(ctx);
        }
    }
    public ProgramTableController programTableController() { return programsTableController; }
    public FunctionTableController functionTableController() { return functionsTableController; }
}

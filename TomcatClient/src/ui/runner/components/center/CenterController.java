// java
package ui.runner.components.center;

import javafx.application.Platform;
import javafx.fxml.FXML;
import ui.AppContext;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;
import ui.runner.components.center_left.CenterLeftController;
import ui.runner.components.center_right.CenterRightController;

public class CenterController {
    @FXML private CenterLeftController centerLeftController;
    @FXML private CenterRightController centerRightController;
    private AppContext ctx;


    public CenterRightController getCenterRightController() {
        return centerRightController;
    }

    public CenterLeftController getCenterLeftController() {
        return centerLeftController;
    }

    public void init(AppContext ctx) {
        this.ctx = ctx;
        if (centerRightController != null) {
            centerRightController.setAppContext(ctx);
        } else {
            // Defer until injected
            javafx.application.Platform.runLater(() -> {
                if (centerRightController != null) centerRightController.setAppContext(ctx);
            });
        }
    }

    @FXML
    private void initialize() {
        centerRightController.setAppContext(ctx);
        final String program = SelectedProgram.get();
        if (program == null || program.isBlank()) {
            // Defer once if SelectedProgram is not ready yet
            Platform.runLater(this::initialize);
            return;
        }

        Thread t = new Thread(() -> {
            int max = 0;
            try { max = ApiClient.get().programMaxDegree(program); } catch (Exception ignored) {}
            final int maxDegree = Math.max(0, max);
            Platform.runLater(() -> ensureCenterLeftInitialized(program, maxDegree));
        }, "load-degree");
        t.setDaemon(true);
        t.start();
    }

    // Retry until the included controller is injected, then init it
    private void ensureCenterLeftInitialized(String program, int maxDegree) {
        if (centerLeftController != null) {
            centerLeftController.init(program, maxDegree);
        } else {
            Platform.runLater(() -> ensureCenterLeftInitialized(program, maxDegree));
        }
    }
}

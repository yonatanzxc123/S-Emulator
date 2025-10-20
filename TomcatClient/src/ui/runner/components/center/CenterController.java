// java
package ui.runner.components.center;

import javafx.application.Platform;
import javafx.fxml.FXML;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;
import ui.runner.components.center_left.CenterLeftController;

public class CenterController {
    @FXML private CenterLeftController centerLeftController;

    @FXML
    private void initialize() {
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

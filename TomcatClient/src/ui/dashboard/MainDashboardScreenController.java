package ui.dashboard;

import javafx.fxml.FXML;
import ui.AppContext;
import ui.components.header.HeaderController;
import ui.dashboard.components.center.CenterController;

public class MainDashboardScreenController {
    private final AppContext ctx;

    public MainDashboardScreenController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML private HeaderController headerController;
    @FXML private CenterController centerController;

    @FXML
    private void initialize() {
        if (headerController != null && ctx != null) {
            headerController.bindUsername(ctx.usernameProperty());
        }
        if (centerController != null && ctx != null) {
            centerController.init(ctx);
        }
    }
}

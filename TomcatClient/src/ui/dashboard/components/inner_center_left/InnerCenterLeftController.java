package ui.dashboard.components.inner_center_left;

import javafx.fxml.FXML;
import ui.dashboard.components.history_table.HistoryTableController;
import ui.dashboard.components.user_table.UserTableController;

public class InnerCenterLeftController {

    @FXML
    private UserTableController usersTableController;
    @FXML private HistoryTableController historyTableController;

    @FXML
    private void initialize() {
        // Pass the reference after both are injected
        if (usersTableController != null && historyTableController != null) {
            usersTableController.setHistoryTableController(historyTableController);
        }
    }
}

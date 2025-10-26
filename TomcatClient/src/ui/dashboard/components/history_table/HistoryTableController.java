package ui.dashboard.components.history_table;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ui.net.ApiClient;

public class HistoryTableController {
    @FXML
    private TableView<ApiClient.RunHistoryEntry> table;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> runNoColumn;
    @FXML
    private TableColumn<ApiClient.RunHistoryEntry, String> mainProgramColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, String> nameColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, String> archColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> degreeColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> yColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> cyclesColumn;

    private final ObservableList<ApiClient.RunHistoryEntry> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        runNoColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().runNo));
        mainProgramColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().isMainProgram ? "Program" : "Function"));
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().name));
        archColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().arch));
        degreeColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().degree));
        yColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().y));
        cyclesColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cycles));
        table.setItems(items);

        // Fetch and show history for current user
        new Thread(() -> {
            try {
                var history = ApiClient.get().fetchOwnRunHistory();
                Platform.runLater(() -> items.setAll(history));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

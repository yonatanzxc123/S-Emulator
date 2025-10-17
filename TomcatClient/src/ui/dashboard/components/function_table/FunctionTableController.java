// java
package ui.dashboard.components.function_table;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ui.net.ApiClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FunctionTableController {
    @FXML private TableView<FunctionRow> table;
    @FXML private TableColumn<FunctionRow, String> programCol;
    @FXML private TableColumn<FunctionRow, String> functionCol;
    @FXML private TableColumn<FunctionRow, String> userCol;
    @FXML private TableColumn<FunctionRow, Number> instrCol;
    @FXML private TableColumn<FunctionRow, Number> degreeCol;

    private final ObservableList<FunctionRow> items = FXCollections.observableArrayList();

    public void init() {
        // Bind columns to FunctionRow getters/properties
        programCol.setCellValueFactory(new PropertyValueFactory<>("programName"));
        functionCol.setCellValueFactory(new PropertyValueFactory<>("functionName"));
        userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        instrCol.setCellValueFactory(new PropertyValueFactory<>("instrCount"));
        degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));

        // Bind items to table
        table.setItems(items);
    }

    public void addFunctions(String programName, String owner, Collection<ApiClient.FunctionInfo> functions) {
        if (functions == null || functions.isEmpty()) return;

        // Build rows off-thread, then append on FX thread
        List<FunctionRow> rows = new ArrayList<>(functions.size());
        for (ApiClient.FunctionInfo f : functions) {
            rows.add(new FunctionRow(programName, f.name, owner, f.instr, f.maxDegree));
        }

        if (Platform.isFxApplicationThread()) {
            items.addAll(rows);
        } else {
            Platform.runLater(() -> items.addAll(rows));
        }
    }
}

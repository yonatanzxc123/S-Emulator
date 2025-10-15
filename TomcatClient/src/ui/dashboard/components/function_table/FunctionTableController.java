// java
package ui.dashboard.components.function_table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ui.net.ApiClient;

import java.util.Collection;

public class FunctionTableController {
    @FXML private TableView<FunctionRow> table;
    @FXML private TableColumn<FunctionRow, String> programCol;
    @FXML private TableColumn<FunctionRow, String> functionCol;
    @FXML private TableColumn<FunctionRow, String> userCol;
    @FXML private TableColumn<FunctionRow, Number> instrCol;
    @FXML private TableColumn<FunctionRow, Number> degreeCol;

    private final ObservableList<FunctionRow> items = FXCollections.observableArrayList();

    public void init() {
        if (table != null) table.setItems(items);
        if (programCol != null) programCol.setCellValueFactory(new PropertyValueFactory<>("programName"));
        if (functionCol != null) functionCol.setCellValueFactory(new PropertyValueFactory<>("functionName"));
        if (userCol != null) userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (instrCol != null) instrCol.setCellValueFactory(new PropertyValueFactory<>("instrCount"));
        if (degreeCol != null) degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
    }

    public void addFunctions(String programName, String owner, Collection<ApiClient.FunctionInfo> functions) {
        if (functions == null) return;
        for (ApiClient.FunctionInfo f : functions) {
            items.add(new FunctionRow(programName, f.name, owner, f.instr, f.maxDegree));
        }
    }
}

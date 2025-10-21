// java
package ui.dashboard.components.function_table;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import ui.AppContext;
import ui.ClientApp;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;

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
    @FXML private Button executeFunctionBtn;

    private final ObservableList<FunctionRow> items = FXCollections.observableArrayList();

    private AppContext ctx;
    public void setAppContext(AppContext ctx) { this.ctx = ctx; }

    private ClientApp app;
    public void setClientApp(ClientApp app) { this.app = app; }

    @FXML public void initialize() {
        if (programCol != null) programCol.setCellValueFactory(new PropertyValueFactory<>("programName"));
        if (functionCol != null) functionCol.setCellValueFactory(new PropertyValueFactory<>("functionName"));
        if (userCol != null)    userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (instrCol != null)   instrCol.setCellValueFactory(new PropertyValueFactory<>("instrCount"));
        if (degreeCol != null)  degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));

        if (table != null) table.setItems(items);

        if (executeFunctionBtn != null && table != null) {
            executeFunctionBtn.disableProperty().bind(
                    table.getSelectionModel().selectedItemProperty().isNull()
            );
        }
        new Thread(() -> {
            try {
                var list = ApiClient.get().listAllFunctions();
                Platform.runLater(() -> {
                    items.clear();
                    for (ApiClient.FunctionSummary f : list) {
                        items.add(new FunctionRow(f.program, f.name, f.owner, f.instr, f.maxDegree));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "functions-load").start();
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

    @FXML
    private void onExecute() {
        if (table == null || ctx == null || app == null) return;
        FunctionRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        SelectedProgram.set(sel.getFunctionName());
        try {
            app.showRunScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

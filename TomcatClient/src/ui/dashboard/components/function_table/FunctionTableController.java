// java
package ui.dashboard.components.function_table;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import ui.AppContext;
import ui.ClientApp;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    private final Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(2), e -> refresh()));
    private volatile boolean fetching = false;

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

        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
        refresh();
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

    private void refresh() {
        if (fetching) return;
        fetching = true;

        // Remember selected function name
        String selectedName;
        if (table != null && table.getSelectionModel().getSelectedItem() != null) {
            selectedName = table.getSelectionModel().getSelectedItem().getFunctionName();
        } else {
            selectedName = null;
        }

        CompletableFuture.runAsync(() -> {
            try {
                var list = ApiClient.get().listAllFunctions();
                Platform.runLater(() -> {
                    items.clear();
                    for (ApiClient.FunctionSummary f : list) {
                        items.add(new FunctionRow(f.program, f.name, f.owner, f.instr, f.maxDegree));
                    }
                    // Restore selection
                    if (selectedName != null) {
                        for (FunctionRow row : items) {
                            if (row.getFunctionName().equals(selectedName)) {
                                table.getSelectionModel().select(row);
                                break;
                            }
                        }
                    }
                    fetching = false;
                });
            } catch (Exception e) {
                fetching = false;
            }
        });
    }
}

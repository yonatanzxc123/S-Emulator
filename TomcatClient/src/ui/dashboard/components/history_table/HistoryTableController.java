package ui.dashboard.components.history_table;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import ui.net.ApiClient;

import java.io.IOException;

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
    @FXML private Button rerunBtn;
    @FXML private Button showBtn;

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

    @FXML
    private void onRerunClicked() {
        ApiClient.RunHistoryEntry entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) return;

        ui.runner.SelectedProgram.set(entry.name);
        ui.runner.SelectedProgram.setSelectedDegree(entry.degree);
        ui.runner.SelectedProgram.setInputs(entry.inputs);

        try {
            ui.ClientApp.get().showRunScreen();
            Platform.runLater(() -> {
                var runScreenCtrl = ui.ClientApp.get().getRunScreenController();
                if (runScreenCtrl != null) {
                    runScreenCtrl.setMainProgram(entry.isMainProgram);
                    try {
                        runScreenCtrl.prepareRerun();
                        // Set architecture choice box to recorded arch
                        var centerRightCtrl = runScreenCtrl.getCenterController().getCenterRightController();
                        if (centerRightCtrl != null && centerRightCtrl.getArchitectureChoiceBox() != null) {
                            centerRightCtrl.getArchitectureChoiceBox().setValue(entry.arch);
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onActionShow() {
        ApiClient.RunHistoryEntry entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/dashboard/components/history_table/VarsPopup.fxml"));
            Scene scene = new Scene(loader.load());
            VarsPopupController controller = loader.getController();

            java.util.List<VarsPopupController.VarEntry> vars = new java.util.ArrayList<>();
            entry.vars.forEach((k, v) -> vars.add(new VarsPopupController.VarEntry(k, v)));
            controller.setVariables(vars);

            Stage popup = new Stage();
            popup.setTitle("Variables for Run #" + entry.runNo);
            popup.setScene(scene);
            popup.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

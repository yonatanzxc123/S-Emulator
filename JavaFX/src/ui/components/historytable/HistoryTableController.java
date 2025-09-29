package ui.components.historytable;

import javafx.beans.binding.Bindings;
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
import system.api.EmulatorEngine;
import system.api.HistoryEntry;
import system.api.view.CommandView;
import system.api.view.ProgramView;
import ui.EngineInjector;


import java.util.*;

public class HistoryTableController implements EngineInjector {
    private EmulatorEngine engine;
    @Override
    public void setEngine(EmulatorEngine engine) {
        this.engine=engine;
    }

    @FXML
    private TableView<HistoryEntry> table;
    @FXML private TableColumn<HistoryEntry, Integer> runNoColumn;
    @FXML private TableColumn<HistoryEntry, Integer> degreeColumn;
    @FXML private TableColumn<HistoryEntry, String> outputColumn;
    @FXML private TableColumn<HistoryEntry, Long> cyclesColumn;
    @FXML private Button rerunBtn;
    @FXML private Button showBtn;

    private VarsPopupController varsPopupController;

    private final ObservableList<HistoryEntry> entries = FXCollections.observableArrayList();

    private Runnable rerunCallback;

    @FXML
    private void initialize() {
        runNoColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().runNo()));
        degreeColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().degree()));
        outputColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper("y=" + cd.getValue().y()));
        cyclesColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cycles()));
        table.setItems(entries);

        rerunBtn.disableProperty().bind(Bindings.createBooleanBinding(
                entries::isEmpty,
                entries
        ));

        showBtn.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull()
        );



    }

    public void onActionShow() {
        HistoryEntry entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/components/historytable/VarsPopup.fxml"));
            Scene scene = new Scene(loader.load());
            VarsPopupController controller = loader.getController();

            List<VarsPopupController.VarEntry> vars = new ArrayList<>();

            // Add y value first
            vars.add(new VarsPopupController.VarEntry("y", entry.y()));

            // Add x values
            List<Long> inputs = entry.inputs();
            for (int i = 0; i < inputs.size(); i++) {
                vars.add(new VarsPopupController.VarEntry("x" + (i + 1), inputs.get(i)));
            }

            // Add z variables from stored final variables
            Map<String, Long> finalVars = entry.finalVariables();
            if (finalVars != null) {
                List<String> zVars = finalVars.keySet().stream()
                        .filter(this::isZVar)
                        .sorted((a, b) -> Integer.compare(intSuffix(a), intSuffix(b)))
                        .toList();

                for (String zVar : zVars) {
                    vars.add(new VarsPopupController.VarEntry(zVar, finalVars.get(zVar)));
                }
            }

            controller.setVariables(vars);

            Stage popup = new Stage();
            popup.setTitle("Variables for Run #" + entry.runNo());
            popup.setScene(scene);
            popup.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean isZVar(String t) {
        if (t.length() >= 2 && t.charAt(0) == 'z') {
            for (int k = 1; k < t.length(); k++) {
                if (!Character.isDigit(t.charAt(k))) return false;
            }
            return true;
        }
        return false;
    }

    private int intSuffix(String s) {
        int i = 1, n = s.length(), val = 0;
        while (i < n && Character.isDigit(s.charAt(i))) {
            val = val * 10 + (s.charAt(i) - '0');
            i++;
        }
        return val;
    }


    public void setOnRerun(Runnable callback) {
        this.rerunCallback = callback;
    }

    @FXML
    private void onRerunClicked() {
        if (rerunCallback != null) {
            rerunCallback.run();
        }
    }

    public HistoryEntry getSelectedEntry() {
        return table.getSelectionModel().getSelectedItem();
    }

    public void addEntry(HistoryEntry entry) {
        entries.add(entry);
    }

    public ObservableList<HistoryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<HistoryEntry> entries) {
        this.entries.clear();
        this.entries.addAll(entries);
    }
    public void clear() {
        entries.clear();
    }
}


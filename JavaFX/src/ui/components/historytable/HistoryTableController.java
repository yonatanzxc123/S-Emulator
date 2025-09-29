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

            // Get actual final z values by re-running the execution
            if (engine != null) {
                // Re-run the program with the same parameters to get final variable states
                var result = engine.run(entry.degree(), entry.inputs());

                if (result != null && result.variablesOrdered() != null) {
                    Map<String, Long> finalVars = result.variablesOrdered();

                    // Get all z variables from the program
                    ProgramView pv = (entry.degree() == 0) ? engine.getProgramView()
                            : engine.getExpandedProgramView(entry.degree());

                    if (pv != null) {
                        Set<String> zVars = new LinkedHashSet<>();
                        if (pv.commands() != null) {
                            for (CommandView c : pv.commands()) {
                                collectZVarsFromText(c.text(), zVars);
                            }
                        }

                        // Add z variables with their actual final values
                        List<String> sortedZVars = new ArrayList<>(zVars);
                        sortedZVars.sort((a, b) -> Integer.compare(intSuffix(a), intSuffix(b)));

                        for (String zVar : sortedZVars) {
                            Long finalValue = finalVars.getOrDefault(zVar, 0L);
                            vars.add(new VarsPopupController.VarEntry(zVar, finalValue));
                        }
                    }
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


    // Add these helper methods to HistoryTableController
    private void collectZVarsFromText(String text, Set<String> out) {
        if (text == null) return;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                int j = i + 1;
                while (j < n && Character.isLetterOrDigit(text.charAt(j))) j++;
                String tok = text.substring(i, j);
                if (isZVar(tok)) out.add(tok);
                i = j - 1;
            }
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


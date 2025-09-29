package ui.components.vartable;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import system.api.EmulatorEngine;
import ui.EngineInjector;

import java.util.*;

public class VarTableController implements EngineInjector {
    private EmulatorEngine engine;

    @Override
    public void setEngine(EmulatorEngine engine) {
       this.engine = engine;
    }

    @FXML
    private TableView<VarRow> varTable;
    @FXML private TableColumn<VarRow,String> varColumn;
    @FXML private TableColumn<VarRow,Number> valueColumn;

    private final ObservableList<VarRow> rows = FXCollections.observableArrayList();
    private Map<String, Long> previousValues = new HashMap<>();
    private Set<String> changedVars = new HashSet<>();
    private Map<String, Long> allEncounteredVars = new HashMap<>();

    @FXML
    private void initialize() {
        varTable.setItems(rows);
        varTable.setPlaceholder(new Label("No content in table"));
        varTable.setEditable(false);

        varColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        valueColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getValue()));
        varTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        varTable.setRowFactory(tv -> new TableRow<VarRow>() {
            @Override
            protected void updateItem(VarRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    boolean isChanged = changedVars.contains(item.getName());
                    setStyle(isChanged ? "-fx-background-color: #FFE4B3;" : "");
                }
            }
        });
    }

    public void showSnapshot(Map<String, Long> vars) {
        if (vars == null) {
            clear();
            return;
        }

        // Track changes
        changedVars.clear();
        for (Map.Entry<String, Long> entry : vars.entrySet()) {
            String varName = entry.getKey();
            Long currentValue = entry.getValue();
            Long previousValue = previousValues.get(varName);

            if (previousValue != null && !previousValue.equals(currentValue)) {
                changedVars.add(varName);
            }
        }

        // Update previous values
        allEncounteredVars.putAll(vars);
        previousValues.putAll(vars);

        var names = new ArrayList<>(allEncounteredVars.keySet());
        Collections.sort(names);
        rows.setAll(names.stream().map(n -> new VarRow(n, allEncounteredVars.get(n))).toList());
    }

    public void showSnapshotWithInstruction(Map<String, Long> vars, String currentInstruction) {
        if (vars == null) {
            clear();
            return;
        }

        // Extract variables from current instruction and add missing ones
        if (currentInstruction != null) {
            Set<String> varsInInstruction = new HashSet<>();
            collectVarsFromText(currentInstruction, varsInInstruction);

            // Add any missing variables with default value 0
            for (String varName : varsInInstruction) {
                if (!allEncounteredVars.containsKey(varName)) {
                    allEncounteredVars.put(varName, 0L);
                }
            }
        }

        // Continue with normal processing
        showSnapshot(vars);
    }

    private void collectVarsFromText(String text, Set<String> out) {
        if (text == null) return;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                int j = i + 1;
                while (j < n && Character.isLetterOrDigit(text.charAt(j))) j++;
                String tok = text.substring(i, j);
                if (isVarToken(tok)) out.add(tok);
                i = j - 1;
            }
        }
    }

    private boolean isVarToken(String t) {
        if (t.equals("y")) return true;
        if (t.length() >= 2 && (t.charAt(0) == 'x' || t.charAt(0) == 'z')) {
            for (int k = 1; k < t.length(); k++) {
                if (!Character.isDigit(t.charAt(k))) return false;
            }
            return true;
        }
        return false;
    }

    public void clear() {
        rows.clear();
        previousValues.clear();
        changedVars.clear();
        allEncounteredVars.clear();
    }
    public void resetChangeTracking() {
        changedVars.clear();
        previousValues.clear();
        allEncounteredVars.clear();
    }

}

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
        previousValues.clear();
        previousValues.putAll(vars);

        var names = new ArrayList<>(vars.keySet());
        Collections.sort(names);
        rows.setAll(names.stream().map(n -> new VarRow(n, vars.getOrDefault(n, 0L))).toList());
    }

    public void clear() {
        rows.clear();
        previousValues.clear();
        changedVars.clear();
    }
    public void resetChangeTracking() {
        changedVars.clear();
        previousValues.clear();
    }

}

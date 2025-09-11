package ui.components.vartable;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import system.api.EmulatorEngine;
import ui.EngineInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

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


    @FXML
    private void initialize() {
        varTable.setItems(rows);
        varTable.setPlaceholder(new Label("No content in table"));
        varTable.setEditable(false);

        varColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        valueColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getValue()));
        varTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public void showSnapshot(Map<String, Long> vars) {
        if (vars == null) { clear(); return; }
        var names = new ArrayList<>(vars.keySet());
        Collections.sort(names);
        rows.setAll(names.stream().map(n -> new VarRow(n, vars.getOrDefault(n, 0L))).toList());
    }

    public void clear() { rows.clear(); }

}

package ui.runner.components.var_table;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Map;

public class VarTableController {
    @FXML private TableView<VarEntry> varTable;
    @FXML private TableColumn<VarEntry, String> varColumn;
    @FXML private TableColumn<VarEntry, Long> valueColumn;

    private final ObservableList<VarEntry> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        varColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        valueColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty().asObject());
        varTable.setItems(items);
    }

    public void setVars(Map<String, Long> vars) {
        items.clear();
        if (vars != null) {
            vars.forEach((k, v) -> items.add(new VarEntry(k, v)));
        }
    }

    public static class VarEntry {
        private final StringProperty name;
        private final LongProperty value;
        public VarEntry(String name, long value) {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleLongProperty(value);
        }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.LongProperty valueProperty() { return value; }
    }
}

package ui.dashboard.components.history_table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class VarsPopupController {
    @FXML private TableView<VarEntry> varsTable;
    @FXML private TableColumn<VarEntry, String> nameCol;
    @FXML private TableColumn<VarEntry, String> valueCol;

    private final ObservableList<VarEntry> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().name));
        valueCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().value)));
        varsTable.setItems(items);
    }

    public void setVariables(java.util.List<VarEntry> vars) {
        items.setAll(vars);
    }

    public static class VarEntry {
        public final String name;
        public final Object value;
        public VarEntry(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
}

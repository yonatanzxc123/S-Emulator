package ui.components.historytable;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class VarsPopupController {
    @FXML private TableView<VarEntry> varsTable;
    @FXML private TableColumn<VarEntry, String> varNameColumn;
    @FXML private TableColumn<VarEntry, String> varValueColumn;

    private final ObservableList<VarEntry> variables = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        varNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
        varValueColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().value)));
        varsTable.setItems(variables);
    }

    public void setVariables(List<VarEntry> vars) {
        variables.setAll(vars);
    }

    public static class VarEntry {
        private final String name;
        private final Long value;

        public VarEntry(String name, Long value) {
            this.name = name;
            this.value = value;
        }
    }
}

package ui.components.historytable;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import system.api.HistoryEntry;

public class HistoryTableController {
    @FXML
    private TableView<HistoryEntry> table;
    @FXML private TableColumn<HistoryEntry, Integer> runNoColumn;
    @FXML private TableColumn<HistoryEntry, Integer> degreeColumn;
    @FXML private TableColumn<HistoryEntry, String> outputColumn;
    @FXML private TableColumn<HistoryEntry, Long> cyclesColumn;
    @FXML private Button rerunButton;

    private final ObservableList<HistoryEntry> entries = FXCollections.observableArrayList();

    private Runnable rerunCallback;

    @FXML
    private void initialize() {
        runNoColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().runNo()));
        degreeColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().degree()));
        outputColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper("y=" + cd.getValue().y()));
        cyclesColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cycles()));
        table.setItems(entries);

        rerunButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> entries.isEmpty(),
                entries
        ));

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
    public void clear() {
        entries.clear();
    }
}


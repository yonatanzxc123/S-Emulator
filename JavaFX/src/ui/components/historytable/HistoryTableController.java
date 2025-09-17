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
import system.api.HistoryEntry;


import java.util.ArrayList;
import java.util.List;

public class HistoryTableController {
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

            // Add z values based on the degree
            // Each degree has z1 through zN where N is the degree
            for (int i = 1; i <= entry.degree(); i++) {
                vars.add(new VarsPopupController.VarEntry("z" + i, 0L)); // Default to 0 since we don't have z values stored
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


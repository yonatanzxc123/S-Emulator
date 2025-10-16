// java
// File: 'TomcatClient/src/ui/dashboard/components/program_table/ProgramTableController.java'
package ui.dashboard.components.program_table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import ui.runner.SelectedProgram;

public class ProgramTableController {

    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row, String> nameCol;
    @FXML private TableColumn<Row, String> uploaderCol;
    @FXML private TableColumn<Row, String> instrCol;
    @FXML private TableColumn<Row, String> degreeCol;
    @FXML private TableColumn<Row, String> runsCol;
    @FXML private TableColumn<Row, String> creditsCol;
    @FXML private Button executeBtn;

    private final ObservableList<Row> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (nameCol != null)      nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (uploaderCol != null)  uploaderCol.setCellValueFactory(new PropertyValueFactory<>("uploader"));
        if (instrCol != null)     instrCol.setCellValueFactory(new PropertyValueFactory<>("instr"));
        if (degreeCol != null)    degreeCol.setCellValueFactory(new PropertyValueFactory<>("degree"));
        if (runsCol != null)      runsCol.setCellValueFactory(new PropertyValueFactory<>("runs"));
        if (creditsCol != null)   creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));

        if (table != null) {
            table.setItems(items);
            // Optional: double-click row to execute
            table.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) onExecute();
            });
        }

        if (executeBtn != null) {
            executeBtn.disableProperty().bind(
                    table.getSelectionModel().selectedItemProperty().isNull()
            );
        }
    }

    // Called by CenterController to populate the table
    public void addProgram(String name, String owner, int instrDeg0, int maxDegree) {
        items.add(new Row(name, owner, instrDeg0, maxDegree));
    }

    @FXML
    private void onExecute() {
        Row sel = (table == null) ? null : table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // 1) remember selected program for the run screen
        SelectedProgram.set(sel.getName());

        // 2) navigate to the run screen
        try {
            var url = ProgramTableController.class.getResource("/ui/runner/MainRunScreen.fxml");
            if (url == null) return;
            var loader = new FXMLLoader(url);
            var scene = new Scene(loader.load());
            Stage stage = (Stage) table.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception ignore) {
            // optionally log
        }
    }

    // Row bean for the table
    public static final class Row {
        private final String name;
        private final String uploader;
        private final String instr;
        private final String degree;
        private final String runs;
        private final String credits;

        public Row(String name, String uploader, int instrDeg0, int maxDegree) {
            this.name = name;
            this.uploader = uploader == null ? "" : uploader;
            this.instr = String.valueOf(instrDeg0);
            this.degree = String.valueOf(maxDegree);
            this.runs = "0";     // unknown here
            this.credits = "0";  // unknown here
        }

        public String getName() { return name; }
        public String getUploader() { return uploader; }
        public String getInstr() { return instr; }
        public String getDegree() { return degree; }
        public String getRuns() { return runs; }
        public String getCredits() { return credits; }
    }
}

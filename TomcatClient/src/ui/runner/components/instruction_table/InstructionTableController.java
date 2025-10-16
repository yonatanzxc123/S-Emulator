package ui.runner.components.instruction_table;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ui.net.ApiClient;
import ui.runner.SelectedProgram;

import java.util.ArrayList;
import java.util.List;

public class InstructionTableController {

    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row, String> lineColumn;
    @FXML private TableColumn<Row, String> bsColumn;
    @FXML private TableColumn<Row, String> labelColumn;
    @FXML private TableColumn<Row, String> instructionColumn;
    @FXML private TableColumn<Row, String> cyclesColumn;
    @FXML private TableColumn<Row, String> ArchitectureColumn;

    private final ObservableList<Row> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (lineColumn != null)        lineColumn.setCellValueFactory(new PropertyValueFactory<>("line"));
        if (bsColumn != null)          bsColumn.setCellValueFactory(new PropertyValueFactory<>("bs"));
        if (labelColumn != null)       labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        if (instructionColumn != null) instructionColumn.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        if (cyclesColumn != null)      cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));
        if (ArchitectureColumn != null) ArchitectureColumn.setCellValueFactory(new PropertyValueFactory<>("arch"));

        if (table != null) {
            table.setItems(items);
        }
    }

    // Called by parent to initialize top/bottom table; when loadProgram==true populate from server
    public void init(boolean loadProgram) {
        if (!loadProgram) return;

        String prog = SelectedProgram.get();
        if (prog == null || prog.isBlank()) return;

        // background thread for IO
        Thread t = new Thread(() -> {
            try {
                String base = System.getProperty("api.base", "http://localhost:8080/server_Web_exploded");
                ApiClient api = new ApiClient(base, true);
                List<ApiClient.ProgramInstruction> body = api.programBody(prog);

                List<Row> rows = new ArrayList<>();
                for (ApiClient.ProgramInstruction pi : body) {
                    rows.add(new Row(
                            String.valueOf(pi.index),
                            pi.bs,
                            pi.label == null ? "" : pi.label,
                            pi.op == null ? "" : pi.op,
                            String.valueOf(pi.cycles),
                            pi.level == null ? "" : pi.level
                    ));
                }

                Platform.runLater(() -> {
                    items.setAll(rows);
                });
            } catch (Exception e) {
                // keep UI responsive; optionally log
            }
        }, "load-program-body");
        t.setDaemon(true);
        t.start();
    }

    // simple row bean
    public static final class Row {
        private final String line;
        private final String bs;
        private final String label;
        private final String instruction;
        private final String cycles;
        private final String arch;

        public Row(String line, String bs, String label, String instruction, String cycles, String arch) {
            this.line = line;
            this.bs = bs;
            this.label = label;
            this.instruction = instruction;
            this.cycles = cycles;
            this.arch = arch;
        }

        public String getLine() { return line; }
        public String getBs() { return bs; }
        public String getLabel() { return label; }
        public String getInstruction() { return instruction; }
        public String getCycles() { return cycles; }
        public String getArch() { return arch; }
    }
}

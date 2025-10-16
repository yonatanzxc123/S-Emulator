package ui.runner.components.instruction_table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ui.net.ApiClient;
import ui.net.ApiClient.ProgramInstruction;
import ui.runner.SelectedProgram;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstructionTableController {

    @FXML private TableView<ProgramInstruction> table;
    @FXML private TableColumn<ProgramInstruction, Integer> lineCol;
    @FXML private TableColumn<ProgramInstruction, String>  labelCol;
    @FXML private TableColumn<ProgramInstruction, String>  instructionCol;
    @FXML private TableColumn<ProgramInstruction, Integer> cyclesCol;
    @FXML private TableColumn<ProgramInstruction, String>  bsCol;
    @FXML private TableColumn<ProgramInstruction, String>  archCol;

    private final ObservableList<ProgramInstruction> items = FXCollections.observableArrayList();
    private static final AtomicBoolean AUTOLOAD_ONCE = new AtomicBoolean(true);

    @FXML
    private void initialize() {
        lineCol.setCellValueFactory(new PropertyValueFactory<>("index"));
        labelCol.setCellValueFactory(new PropertyValueFactory<>("label"));
        instructionCol.setCellValueFactory(new PropertyValueFactory<>("op"));
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("cycles"));
        bsCol.setCellValueFactory(new PropertyValueFactory<>("bs"));
        archCol.setCellValueFactory(new PropertyValueFactory<>("level"));

        table.setItems(items);

        // Only the first included table (top one) will autoload.
        if (AUTOLOAD_ONCE.getAndSet(false)) {
            String program = SelectedProgram.get();
            if (program != null && !program.isEmpty()) {
                loadInstructions(program);
            }
        }
    }

    public void loadInstructions(String programName) {
        try {
            items.clear();
            List<ProgramInstruction> list = ApiClient.get().programBody(programName);
            if (list != null) items.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

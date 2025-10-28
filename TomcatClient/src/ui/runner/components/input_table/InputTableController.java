package ui.runner.components.input_table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.LongStringConverter;
import ui.net.ApiClient;

import java.util.List;

public class InputTableController {
    @FXML
    private TableView<InputVar> inputTable;
    @FXML private TableColumn<InputVar, String> varColumn;
    @FXML private TableColumn<InputVar, Long> valueColumn;

    private final ObservableList<InputVar> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        inputTable.setEditable(true);
        valueColumn.setEditable(true);

        varColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        valueColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty().asObject());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn(new LongStringConverter()));
        inputTable.setItems(items);

        valueColumn.setOnEditCommit(event -> {
            InputVar var = event.getRowValue();
            if (var != null) {
                var.setValue(event.getNewValue() != null ? event.getNewValue() : 0L);
            }
        });

        inputTable.setItems(items);
    }

    public void setInputs(List<ApiClient.InputVarInfo> inputVars) {
        items.clear();
        for (var iv : inputVars) {
            items.add(new InputVar(iv.name));
        }
    }
    public List<Long> getInputValues() {
        return items.stream().map(InputVar::getValue).toList();
    }

    public void setInputValues(List<Long> values) {
        for (int i = 0; i < items.size() && i < values.size(); i++) {
            items.get(i).setValue(values.get(i));
        }
    }
}

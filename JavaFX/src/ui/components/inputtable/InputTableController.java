package ui.components.inputtable;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.LongStringConverter;
import system.api.EmulatorEngine;
import system.api.view.ProgramView;
import ui.EngineInjector;

import java.util.List;
import java.util.stream.Collectors;

public class InputTableController implements EngineInjector {
    private EmulatorEngine engine;
    @Override
    public void setEngine(EmulatorEngine engine) {
        this.engine = engine;
    }

    @FXML
    private TableView<InputsRow> inputsTable;
    @FXML
    private TableColumn<InputsRow,String> varColumn;
    @FXML
    private TableColumn<InputsRow,Long> valueColumn;

    private final ObservableList<InputsRow> items = FXCollections.observableArrayList();

   @FXML
    public void initialize() {
       inputsTable.setItems(items);
       inputsTable.setEditable(true);
       inputsTable.setPlaceholder(new Label("No content in table"));

       varColumn.setCellValueFactory(data -> data.getValue().nameProperty());
       // not editable
       varColumn.setEditable(false);

       valueColumn.setCellValueFactory(data -> data.getValue().valueProperty().asObject());
       valueColumn.setEditable(true);
       valueColumn.setCellFactory(TextFieldTableCell.forTableColumn(new LongStringConverter()));
       valueColumn.setOnEditCommit(evt -> {
           InputsRow row = evt.getRowValue();
           Long nv = evt.getNewValue() == null ? 0L : evt.getNewValue().longValue();
           row.setValue(nv);
       });
   }
    public void showInputs(List<String> inputNames) {
        items.setAll(inputNames.stream()
                .map(n -> new InputsRow(n, 0))
                .collect(Collectors.toList()));
        inputsTable.getSelectionModel().clearSelection();
    }
    public List<Long> readValues() {
        return items.stream().map(InputsRow::getValue).collect(Collectors.toList());
    }

    public void clear() { items.clear(); }

}

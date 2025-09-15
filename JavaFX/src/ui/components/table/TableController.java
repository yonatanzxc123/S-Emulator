package ui.components.table;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import system.api.EmulatorEngine;
import system.api.view.CommandView;
import system.api.view.ProgramView;
import ui.EngineInjector;

import java.util.List;
import java.util.stream.Collectors;

public class TableController implements EngineInjector {
    private EmulatorEngine engine;

    @Override public void setEngine(EmulatorEngine engine) { this.engine = engine; }

    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row, Number> lineColumn;
    @FXML private TableColumn<Row, String> bsColumn;
    @FXML private TableColumn<Row, String> labelColumn;
    @FXML private TableColumn<Row, String> instructionColumn;
    @FXML private TableColumn<Row, Number> cyclesColumn;

    private List<CommandView> lastCommands = List.of();

    private final ReadOnlyObjectWrapper<Row> selectedRow = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<CommandView> selectedCmd = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyIntegerWrapper selectedLine = new ReadOnlyIntegerWrapper(-1);

    @FXML
    private void initialize() {
        table.setPlaceholder(new Label("No program to display"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        lineColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getLine()));
        bsColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBs()));
        labelColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getLabel()));
        instructionColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getInstruction()));
        cyclesColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getCycles()));

        selectedRow.bind(table.getSelectionModel().selectedItemProperty());
        selectedLine.bind(Bindings.createIntegerBinding(
                () -> selectedRow.get() == null ? -1 : selectedRow.get().getLine(),
                selectedRow
        ));
        // map current selected Row -> underlying CommandView
        selectedCmd.bind(Bindings.createObjectBinding(() -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= lastCommands.size()) return null;
            return lastCommands.get(idx);
        }, table.getSelectionModel().selectedIndexProperty()));
    }

    public void showDegree(int degree) {
        if (engine == null) { clear(); return; }
        ProgramView pv = (degree == 0) ? engine.getProgramView()
                : engine.getExpandedProgramView(degree);
        showProgramView(pv);
    }

    public void showProgramView(ProgramView pv) {
        if (pv == null || pv.commands() == null) { clear(); return; }
        lastCommands = pv.commands();
        var rows = lastCommands.stream()
                .map(Row::new) // display real line numbers
                .collect(Collectors.toList());
        table.setItems(FXCollections.observableArrayList(rows));
        table.getSelectionModel().clearSelection();
    }

    public void showProgramViewRenumbered(ProgramView pv) {
        if (pv == null || pv.commands() == null) { clear(); return; }
        lastCommands = pv.commands();
        var rows = java.util.stream.IntStream.range(0, lastCommands.size())
                .mapToObj(i -> new Row(lastCommands.get(i), i + 1)) // displayLine = 1..n
                .collect(Collectors.toList());
        table.setItems(FXCollections.observableArrayList(rows));
        table.getSelectionModel().clearSelection();
    }

    public void selectByLineNumber(int engineLineNumber) {
        if (table.getItems() == null) return;
        for (int i = 0; i < table.getItems().size(); i++) {
            // match against the underlying command’s true number
            if (table.getItems().get(i).getCommand().number() == engineLineNumber) {
                final int rowIdx = i;
                table.getSelectionModel().clearAndSelect(rowIdx);
                table.scrollTo(Math.max(0, rowIdx - 3));
                return;
            }
        }
        table.getSelectionModel().clearSelection();
    }

    public void clearSelection() {
        if (table != null) table.getSelectionModel().clearSelection();
    }

    public void clear() {
        lastCommands = List.of();
        if (table.getItems() != null) table.getItems().clear();
        clearSelection();
    }

    // keep CenterController API the same:
    public ReadOnlyObjectProperty<CommandView> selectedCommandProperty() { return selectedCmd.getReadOnlyProperty(); }
    public CommandView getSelectedCommand() { return selectedCmd.get(); }
}



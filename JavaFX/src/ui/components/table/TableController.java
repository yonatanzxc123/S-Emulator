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

public class TableController implements EngineInjector {
    private EmulatorEngine engine;

    @Override public void setEngine(EmulatorEngine engine) { this.engine = engine; }

    @FXML private TableView<CommandView> table;
    @FXML private TableColumn<CommandView, Number> lineColumn;
    @FXML private TableColumn<CommandView, String> bsColumn;
    @FXML private TableColumn<CommandView, String> labelColumn;
    @FXML private TableColumn<CommandView, String> instructionColumn;
    @FXML private TableColumn<CommandView, Number> cyclesColumn;

    private final ReadOnlyObjectWrapper<CommandView> selected = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyIntegerWrapper selectedLine = new ReadOnlyIntegerWrapper(-1);

    @FXML
    private void initialize() {
        table.setPlaceholder(new Label("No program to display"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        lineColumn.setCellValueFactory(
                (TableColumn.CellDataFeatures<CommandView, Number> cd) ->
                        new ReadOnlyObjectWrapper<Number>(cd.getValue().number())
        );

        bsColumn.setCellValueFactory(
                (TableColumn.CellDataFeatures<CommandView, String> cd) ->
                        new ReadOnlyStringWrapper(cd.getValue().basic() ? "B" : "S")
        );

        labelColumn.setCellValueFactory(
                (TableColumn.CellDataFeatures<CommandView, String> cd) -> {
                    String lbl = cd.getValue().labelOrEmpty();
                    return new ReadOnlyStringWrapper(lbl == null ? "" : lbl);
                }
        );

        instructionColumn.setCellValueFactory(
                (TableColumn.CellDataFeatures<CommandView, String> cd) ->
                        new ReadOnlyStringWrapper(cd.getValue().text())
        );

        cyclesColumn.setCellValueFactory(
                (TableColumn.CellDataFeatures<CommandView, Number> cd) ->
                        new ReadOnlyObjectWrapper<Number>(cd.getValue().cycles())
        );

        selected.bind(table.getSelectionModel().selectedItemProperty());
        selectedLine.bind(Bindings.createIntegerBinding(
                () -> selected.get() == null ? -1 : selected.get().number(),
                selected
        ));
        selected.bind(table.getSelectionModel().selectedItemProperty());
    }


    public void showDegree(int degree) {
        if (engine == null) { clear(); return; }
        ProgramView pv = (degree == 0) ? engine.getProgramView()
                : engine.getExpandedProgramView(degree);
        showProgramView(pv);
    }

    public void selectByLineNumber(int number) {
        if (table.getItems() == null) return;
        for (int i = 0; i < table.getItems().size(); i++) {
            if (table.getItems().get(i).number() == number) {
                final int rowIdx = i;
                table.getSelectionModel().clearAndSelect(rowIdx);
                table.scrollTo(Math.max(0, rowIdx - 3));
                return;
            }
        }
        table.getSelectionModel().clearSelection();
    }

    public void showProgramView(ProgramView pv) {
        if (pv == null || pv.commands() == null) { clear(); return; }
        table.setItems(FXCollections.observableArrayList(pv.commands()));
        table.getSelectionModel().clearSelection();
    }
    public void clearSelection() {
        if (table != null) {
            table.getSelectionModel().clearSelection();
        }
    }

    public void clear() { table.getItems().clear(); }
    public ReadOnlyObjectProperty<CommandView> selectedCommandProperty() { return selected.getReadOnlyProperty(); }
    public CommandView getSelectedCommand() { return selected.get(); }
}



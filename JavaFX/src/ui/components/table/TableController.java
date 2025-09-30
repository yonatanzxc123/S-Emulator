package ui.components.table;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import system.api.EmulatorEngine;
import system.api.view.CommandView;
import system.api.view.ProgramView;
import ui.EngineInjector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final Set<Integer> breakpoints = new HashSet<>();
    private BreakpointClickHandler breakpointHandler;

    public interface BreakpointClickHandler {
        void onBreakpointToggle(int lineNumber, boolean isSet);
    }

    public void setBreakpointHandler(BreakpointClickHandler handler) {
        this.breakpointHandler = handler;
    }

    private final ReadOnlyObjectWrapper<Row> selectedRow = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<CommandView> selectedCmd = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyIntegerWrapper selectedLine = new ReadOnlyIntegerWrapper(-1);
    private final StringProperty highlightVar = new SimpleStringProperty();
    public StringProperty highlightVarProperty() { return highlightVar; }
    public void setHighlightVar(String v) { highlightVar.set(v); }
    public String getHighlightVar() { return highlightVar.get(); }

    @FXML
    private void initialize() {
        table.setPlaceholder(new Label("No program to display"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        lineColumn.setCellValueFactory(cd -> {
            Row row = cd.getValue();
            return new ReadOnlyObjectWrapper<>(row.shouldShowLineNumber() ? row.getLine() : null);
        });
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

        highlightVar.addListener((obs, oldV, newV) -> table.refresh());

        table.setRowFactory((TableView<Row> tv) ->
                new TableRow<Row>() {
                    @Override
                    protected void updateItem(Row item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setStyle("");
                            return;
                        }
                        String hv = highlightVar.get();
                        if (hv == null || hv.isBlank()) {
                            setStyle("");
                        } else {
                            boolean uses = usesVar(item.getInstruction(), hv);
                            setStyle(uses ? "-fx-background-color: #FFF1B3;" : "");
                        }
                    }
                }
        );

        lineColumn.setCellFactory(column -> new TableCell<Row, Number>() {
            private Circle dot;

            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);

                // Clear styles and graphics
                getStyleClass().removeAll("line-column", "breakpoint");
                setGraphic(null);

                if (empty || item == null) {
                    setText("");
                    return;
                }

                Row row = getTableRow().getItem();
                if (row != null && row.shouldShowLineNumber()) {
                    int lineNum = row.getLine();
                    boolean hasBreakpoint = breakpoints.contains(lineNum);

                    getStyleClass().add("line-column");

                    if (hasBreakpoint) {
                        getStyleClass().add("breakpoint");
                        setText("");
                        // Create red dot
                        if (dot == null) {
                            dot = new Circle(4);
                        }
                        dot.setFill(Color.RED);
                        setGraphic(dot);
                    } else {
                        setText(item.toString());
                    }
                } else {
                    setText("");
                }
            }

            // Mouse handlers for hover effect
            {
                setOnMouseEntered(e -> {
                    Row row = getTableRow().getItem();
                    if (row != null && row.shouldShowLineNumber() && !breakpoints.contains(row.getLine())) {
                        setText("");
                        if (dot == null) {
                            dot = new Circle(4);
                        }
                        dot.setFill(Color.RED.deriveColor(0, 1, 1, 0.3)); // Semi-transparent
                        setGraphic(dot);
                    }
                });

                setOnMouseExited(e -> {
                    Row row = getTableRow().getItem();
                    if (row != null && row.shouldShowLineNumber() && !breakpoints.contains(row.getLine())) {
                        setText(getItem().toString());
                        setGraphic(null);
                    }
                });

                setOnMouseClicked(e -> {
                    Row row = getTableRow().getItem();
                    if (row != null && row.shouldShowLineNumber()) {
                        int lineNum = row.getLine();
                        boolean isSet = breakpoints.contains(lineNum);

                        if (isSet) {
                            breakpoints.remove(lineNum);
                        } else {
                            breakpoints.add(lineNum);
                        }

                        updateItem(getItem(), isEmpty());

                        if (breakpointHandler != null) {
                            breakpointHandler.onBreakpointToggle(lineNum, !isSet);
                        }
                    }
                    e.consume();
                });
            }
        });


        highlightVar.addListener((obs, o, n) -> table.refresh());
    }

    public void setHighlightVariable(String var) {
        highlightVar.set(var == null ? "" : var.trim());
    }

    public void setBreakpoint(int lineNumber, boolean set) {
        if (set) {
            breakpoints.add(lineNumber);
        } else {
            breakpoints.remove(lineNumber);
        }
        table.refresh();
    }

    public Set<Integer> getBreakpoints() {
        return new HashSet<>(breakpoints);
    }

    private static boolean usesVar(String text, String var) {
        if (text == null || var == null || var.isEmpty()) return false;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                int j = i + 1;
                while (j < n && Character.isLetterOrDigit(text.charAt(j))) j++;
                String tok = text.substring(i, j);
                if (tok.equals(var)) return true;   // exact token match
                i = j - 1;
            }
        }
        return false;
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

        rows.add(new Row(createBlankCommand(),0));
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

    private CommandView createBlankCommand() {
        return new CommandView(0, true, "", "", 0, "");
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

    public ReadOnlyObjectProperty<CommandView> selectedCommandProperty() { return selectedCmd.getReadOnlyProperty(); }
    public CommandView getSelectedCommand() { return selectedCmd.get(); }

    public List<CommandView> getLastCommands() {
        return lastCommands;
    }

    public TableView<Row> getTable() {
        return table;
    }
    public void clearBreakpoints() {
        breakpoints.clear();
        table.refresh();
    }
}





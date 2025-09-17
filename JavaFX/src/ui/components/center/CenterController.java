package ui.components.center;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import system.api.DebugStep;
import system.api.EmulatorEngine;
import system.api.HistoryEntry;
import system.api.view.CommandView;
import system.api.view.ProgramView;
import system.core.exec.debugg.Debugger;
import ui.EngineInjector;
import ui.components.historytable.HistoryTableController;
import ui.components.inputtable.InputTableController;
import ui.components.table.Row;
import ui.components.table.TableController;
import ui.components.vartable.VarTableController;

import java.util.*;

import static javafx.application.Platform.runLater;


public class CenterController implements EngineInjector {
    private EmulatorEngine engine;

    @Override
    public void setEngine(EmulatorEngine engine) {
        this.engine = engine;
    }

    @FXML
    private Button programSelectorBtn;
    @FXML
    private Button collapseBtn;
    @FXML
    private Label currDegreeLbl;
    @FXML
    private Button expandBtn;
    @FXML
    private ChoiceBox<String> chooseVarBtn;
    @FXML
    private Button newRunBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Button debugBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Button resumeBtn;
    @FXML
    private Button stepOverBtn;
    @FXML
    private Button stepBackBtn;
    @FXML
    private Label cyclesLbl;



    @FXML
    private TableController instructionTableController;
    @FXML
    private TableController historyTableController;
    @FXML
    private HistoryTableController runHistoryTableController;
    @FXML
    private InputTableController inputTableController;
    @FXML
    private VarTableController varTableController;

    private SimpleIntegerProperty currDegree = new SimpleIntegerProperty(0);
    private SimpleIntegerProperty maxDegree = new SimpleIntegerProperty(0);
    private SimpleListProperty<ProgramView> selectedProgram = new SimpleListProperty<>();
    private BooleanProperty canStart = new SimpleBooleanProperty(false);
    private BooleanProperty inDebug = new SimpleBooleanProperty(false);
    private final StringProperty highlightVar = new SimpleStringProperty("");

    private Debugger debugga;

    @FXML
    private void initialize() {
        currDegreeLbl.prefHeightProperty().bind(programSelectorBtn.heightProperty());
        chooseVarBtn.prefHeightProperty().bind(programSelectorBtn.heightProperty());
        programSelectorBtn.setDisable(true);
        collapseBtn.setDisable(true);
        expandBtn.setDisable(true);
        currDegreeLbl.setText("Current / Maximum degree");

        chooseVarBtn.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (instructionTableController != null) {
                instructionTableController.setHighlightVar(nv == null ? "" : nv.trim());
            }
        });

        instructionTableController.selectedCommandProperty().addListener((obs, oldSel, sel) -> {
            if (sel == null) {
                historyTableController.clear();
                return;
            }
            int line = sel.number();
            lineSelected(line, sel);
        });

        if (runHistoryTableController != null) {
            runHistoryTableController.setOnRerun(this::onActionRerun);
        }

    }

    public void bindToHeaderLoader(ObservableBooleanValue loaded) {
        programSelectorBtn.disableProperty().bind(Bindings.not(loaded));
        collapseBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), currDegree.isEqualTo(0))
        );
        expandBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), currDegree.greaterThanOrEqualTo(maxDegree))
        );
        chooseVarBtn.disableProperty().bind(Bindings.not(loaded));

        StringBinding labelText = Bindings.createStringBinding(
                () -> loaded.get()
                        ? (currDegree.get() + " / " + maxDegree.get())
                        : "Current / Maximum degree",
                loaded, currDegree, maxDegree
        );
        currDegreeLbl.textProperty().bind(labelText);

        loaded.addListener((obs, was, isNowLoaded) -> {
            if (isNowLoaded) {
                int md = (engine == null) ? 0 : engine.getMaxDegree();
                maxDegree.set(md);
                currDegree.set(Math.min(currDegree.get(), md));
                if (instructionTableController != null) {
                    instructionTableController.showDegree(currDegree.get());
                }
                refreshVariableChoices();
                canStart.set(false);
            } else {
                maxDegree.set(0);
                currDegree.set(0);
                canStart.set(false);
                inDebug.set(false);
                if (varTableController != null) varTableController.clear();
                chooseVarBtn.getItems().clear();
                if (instructionTableController != null) instructionTableController.setHighlightVar("");
            }
        });

        loaded.addListener((obs, was, isNowLoaded) -> {
            if (isNowLoaded) {
                int md = (engine == null) ? 0 : engine.getMaxDegree();
                maxDegree.set(md);
                currDegree.set(Math.min(currDegree.get(), md));
                if (instructionTableController != null) {
                    instructionTableController.showDegree(currDegree.get());
                }
            } else {
                maxDegree.set(0);
                currDegree.set(0);
            }
        });
        loaded.addListener((o, was, isNow) -> {
            canStart.set(false);
            if (!isNow) {
                inDebug.set(false);
                varTableController.clear();
                if (runHistoryTableController != null) runHistoryTableController.clear(); // <-- add this line
            }
        });

        startBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), canStart.not())
        );

        loaded.addListener((obs, was, isNowLoaded) -> {
            if (isNowLoaded) {
                canStart.set(false);
            } else {
                canStart.set(false);
            }
        });

        startBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), canStart.not()).or(inDebug)
        );

        // Debug is enabled when ready and not already debugging.
        debugBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), canStart.not()).or(inDebug)
        );

        // Debug controls only while in debug:
        stopBtn.disableProperty().bind(inDebug.not());
        resumeBtn.disableProperty().bind(inDebug.not());
        stepOverBtn.disableProperty().bind(inDebug.not());
        stepBackBtn.disableProperty().bind(inDebug.not());

        // Reset startable when a new file loads
        loaded.addListener((o, was, isNow) -> {
            canStart.set(false);
            if (!isNow) {
                inDebug.set(false);
                varTableController.clear();
            }
        });
        newRunBtn.disableProperty().bind(
                Bindings.or(Bindings.not(loaded), inDebug)
        );
    }

    public void showDegree(int degree) {
        if (instructionTableController != null) {
            instructionTableController.showDegree(degree);
            currDegree.set(degree);

        }
    }

    public void onActionExpansion() {
        if (engine.getMaxDegree() >= currDegree.get()) {
            currDegree.set(currDegree.get() + 1);
            instructionTableController.showDegree(currDegree.get());
            refreshVariableChoices();
        }
    }

    public void onActionCollapse() {
        if (currDegree.get() > 0) {
            currDegree.set(currDegree.get() - 1);
            instructionTableController.showDegree(currDegree.get());
            refreshVariableChoices();
        }

    }
    public void onActionNewRun(){
        ProgramView pv = (currDegree.get() == 0)
                ? engine.getProgramView()
                : engine.getExpandedProgramView(currDegree.get());

        if (pv == null || pv.inputsUsed() == null || pv.inputsUsed().isEmpty()) {
            inputTableController.clear();
            return;
        }
        inputTableController.showInputs(pv.inputsUsed());
        canStart.set(true);
    }
    public void onActionDebug(){
        if (engine == null) return;
        var inputs = (inputTableController == null) ? List.<Long>of() : inputTableController.readValues();

        debugga = engine.startDebug(currDegree.get(), inputs);
        if (debugga == null) return;

        inDebug.set(true);

        // Show initial snapshot
        pushSnapshotToUI();
    }
    public void render(DebugStep s) {
        if (s == null) return;

        if (cyclesLbl != null) {
            cyclesLbl.setText(String.valueOf(s.cycles()));
        }

        // Vars table
        if (varTableController != null) {
            varTableController.showSnapshot(s.vars());
        }

        // Instruction highlight
        if (instructionTableController != null) {
            instructionTableController.selectByLineNumber(s.pc() + 1);
        }
    }

    public void onActionStepOver() {
        if (debugga == null) return;
        DebugStep s = debugga.step();
        render(s);
        if (s.finished()) exitDebug();
    }


    public void onActionStepBack() {
        if (debugga == null) return;
        DebugStep s = debugga.stepBack();
        render(s);
    }


    public void onActionResume() {
        if (debugga == null) return;

        Thread t = new Thread(() -> {
            DebugStep s = debugga.resume();

            runLater(() -> {
                render(s);
                if (s.finished()) {
                    exitDebug();
                }
            });
        }, "debug-resume-fast");

        t.setDaemon(true);
        t.start();
    }


    public void onActionStop() {
        if (debugga == null) { inDebug.set(false); return; }
        DebugStep s = debugga.stop();
        render(s);
        if (instructionTableController != null) {
            instructionTableController.clearSelection();
        }
        exitDebug();
    }
    private void exitDebug() {
        inDebug.set(false);

        if (debugga != null && runHistoryTableController != null) {
            DebugStep lastStep = debugga.peek();
            if (lastStep != null && lastStep.finished()) {
                int runNo = runHistoryTableController.getEntries().size() + 1;
                int degree = currDegree.get();
                List<Long> inputs = (inputTableController == null) ? List.of() : inputTableController.readValues();
                Map<String, Long> vars = lastStep.vars();
                long y = (vars != null && vars.containsKey("y")) ? vars.get("y") : 0L;
                long cycles = lastStep.cycles();
                system.api.HistoryEntry entry = new system.api.HistoryEntry(runNo, degree, inputs, y, cycles);
                runHistoryTableController.addEntry(entry);
            }
            debugga = null;
        }
    }

    private void pushSnapshotToUI() {
        if (debugga == null) return;
        render(debugga.peek());
    }

    private void onActionRerun() {
        if (runHistoryTableController == null || inputTableController == null) return;

        HistoryEntry entry = runHistoryTableController.getSelectedEntry();
        if (entry == null) return;

        // Set the degree
        showDegree(entry.degree());

        // Trigger new run setup
        onActionNewRun();

        // Set the input values
        if (inputTableController != null) {
            List<String> inputVars = engine.getProgramView().inputsUsed();
            List<Long> inputs = entry.inputs();

            for (int i = 0; i < Math.min(inputVars.size(), inputs.size()); i++) {
                inputTableController.setValue(inputVars.get(i), inputs.get(i));
            }
        }
    }

    public void onActionStart(){
        if (engine == null) return;

        // Ensure a program is loaded & degree is current
        final int degree = currDegree.get();
        ProgramView pv = (degree == 0) ? engine.getProgramView()
                : engine.getExpandedProgramView(degree);
        inDebug.set(false);
        if (pv == null) {

            return;

        }

        var inputs = (inputTableController == null) ? java.util.List.<Long>of()
                : inputTableController.readValues();


        var result = engine.run(degree, inputs);
        if (result == null) return;

        if (result.executedProgram() != null && instructionTableController != null) {
            instructionTableController.showProgramView(result.executedProgram());
        }

        if (result.executedProgram() != null && instructionTableController != null) {
            instructionTableController.showProgramView(result.executedProgram());
        }


        if (varTableController != null) {
            varTableController.showSnapshot(result.variablesOrdered());

        }

        if (cyclesLbl != null) {
            cyclesLbl.setText(String.valueOf(result.cycles()));
        }

        if (result != null && runHistoryTableController != null) {
            int runNo = runHistoryTableController.getEntries().size() + 1;
            long y = result.y();
            long cycles = result.cycles();
            system.api.HistoryEntry entry = new system.api.HistoryEntry(runNo, degree, inputs, y, cycles);
            runHistoryTableController.addEntry(entry);
        }
    }

    public void lineSelected(int line, CommandView selected) {
        List<CommandView> rows = buildAncestryRows(selected);
        rows = renumber(rows);
        historyTableController.showProgramView(
                new ProgramView("Ancestry", List.of(), List.of(), rows)
        );
    }

    private static List<CommandView> renumber(List<CommandView> rows) {
        List<CommandView> out = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CommandView c = rows.get(i);
            out.add(new CommandView(
                    i + 1,                // <- 1, 2, 3, ...
                    c.basic(),
                    c.labelOrEmpty(),
                    c.text(),
                    c.cycles(),
                    c.originChain()
            ));
        }
        return out;
    }

    private List<CommandView> buildAncestryRows(CommandView selected) {
        List<CommandView> out = new ArrayList<>();

        String chain = selected.originChain();
        if (chain == null || chain.isBlank()) {
            return out;
        }

        String[] parts = chain.split("  >>>  ");
        List<String> links = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) links.add(s);
        }
        Collections.reverse(links);

        int rowNum = 1;
        for (String link : links) {
            out.add(parseLinkToCommandView(link, rowNum++));
        }
        return out;
    }

    private CommandView parseLinkToCommandView(String link, int fallbackNumber) {
        try {
            String s = link.trim();
            int pHash = s.indexOf('#');
            int pSp1 = s.indexOf(' ', pHash + 1);
            int number = Integer.parseInt(s.substring(pHash + 1, pSp1).trim());

            int pParL = s.indexOf('(', pSp1);
            int pParR = s.indexOf(')', pParL + 1);
            boolean basic = s.substring(pParL + 1, pParR).trim().equalsIgnoreCase("B");

            int pLbL = s.indexOf('[', pParR);
            int pLbR = (pLbL >= 0) ? s.indexOf(']', pLbL + 1) : -1;
            String label = "";
            int afterLabel = pParR + 1;
            if (pLbL >= 0 && pLbR > pLbL) {
                label = s.substring(pLbL + 1, pLbR).trim();
                afterLabel = pLbR + 1;
            }

            int pLastL = s.lastIndexOf('(');
            int pLastR = s.lastIndexOf(')');
            int cycles = Integer.parseInt(s.substring(pLastL + 1, pLastR).trim());

            String text = s.substring(afterLabel).trim();
            if (pLastL > afterLabel) {
                text = s.substring(afterLabel, pLastL).trim();
            }

            return new CommandView(number, basic, label, text, cycles, /*originChain*/ "");
        } catch (Exception ex) {
            return new CommandView(fallbackNumber, true, "", link, 0, "");
        }
    }

    // all of the choice box methods
    private void refreshVariableChoices() {
        if (chooseVarBtn == null) return;

        ProgramView pv = (currDegree.get() == 0) ? engine.getProgramView()
                : engine.getExpandedProgramView(currDegree.get());
        if (pv == null) {
            chooseVarBtn.getItems().clear();
            if (instructionTableController != null) instructionTableController.setHighlightVar("");
            return;
        }

        List<String> vars = computeVars(pv);
        chooseVarBtn.getItems().setAll(vars);
        chooseVarBtn.getSelectionModel().clearSelection();     // no highlight by default
        if (instructionTableController != null) instructionTableController.setHighlightVar("");
    }

    private static List<String> computeVars(ProgramView pv) {
        Set<String> out = new LinkedHashSet<>();
        // declared inputs (x#)
        if (pv.inputsUsed() != null) out.addAll(pv.inputsUsed());
        // scan command texts
        if (pv.commands() != null) {
            for (CommandView c : pv.commands()) collectVarsFromText(c.text(), out);
        }
        // sort y, x1..xn, z1..zn
        List<String> sorted = new ArrayList<>(out);
        sorted.sort((a, b) -> {
            if (a.equals(b)) return 0;
            if (a.equals("y")) return -1;
            if (b.equals("y")) return 1;
            boolean ax = a.startsWith("x"), bx = b.startsWith("x");
            boolean az = a.startsWith("z"), bz = b.startsWith("z");
            if (ax && bz) return -1; if (az && bx) return 1;
            if (ax && bx) return Integer.compare(intSuffix(a), intSuffix(b));
            if (az && bz) return Integer.compare(intSuffix(a), intSuffix(b));
            return a.compareTo(b); // fallback
        });
        return sorted;
    }

    private static void collectVarsFromText(String text, Set<String> out) {
        if (text == null) return;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                int j = i + 1;
                while (j < n && Character.isLetterOrDigit(text.charAt(j))) j++;
                String tok = text.substring(i, j);
                if (isVarToken(tok)) out.add(tok);
                i = j - 1;
            }
        }
    }

    private static boolean isVarToken(String t) {
        if (t.equals("y")) return true;
        if (t.length() >= 2 && (t.charAt(0) == 'x' || t.charAt(0) == 'z')) {
            for (int k = 1; k < t.length(); k++) if (!Character.isDigit(t.charAt(k))) return false;
            return true;
        }
        return false;
    }

    private static int intSuffix(String s) {
        int i = 1, n = s.length(), val = 0;
        while (i < n && Character.isDigit(s.charAt(i))) {
            val = val * 10 + (s.charAt(i) - '0');
            i++;
        }
        return val;
    }
}





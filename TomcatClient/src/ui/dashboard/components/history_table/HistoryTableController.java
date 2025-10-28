package ui.dashboard.components.history_table;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import ui.net.ApiClient;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HistoryTableController {
    @FXML private TableView<ApiClient.RunHistoryEntry> table;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> runNoColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, String> mainProgramColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, String> nameColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, String> archColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> degreeColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> yColumn;
    @FXML private TableColumn<ApiClient.RunHistoryEntry, Number> cyclesColumn;
    @FXML private Button rerunBtn;
    @FXML private Button showBtn;

    private final ObservableList<ApiClient.RunHistoryEntry> items = FXCollections.observableArrayList();

    private Long lastSelectedRunNo = null;
    private final Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(2), e -> refresh()));
    private volatile boolean fetching = false;
    private volatile boolean suppressSelectionListener = false;
    private String currentUsername = null;

    @FXML
    private void initialize() {
        runNoColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().runNo));
        mainProgramColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().isMainProgram ? "Program" : "Function"));
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().name));
        archColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().arch));
        degreeColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().degree));
        yColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().y));
        cyclesColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cycles));

        table.setItems(items);
        table.setPlaceholder(new Pane()); // avoid placeholder flicker

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            if (suppressSelectionListener) return;
            lastSelectedRunNo = (sel != null) ? sel.runNo : null;
        });

        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();

        refresh();
    }

    public void loadHistoryForUser(String username) {
        currentUsername = username;
        refresh();
    }

    private void refresh() {
        if (fetching || userInteracting()) return;
        fetching = true;

        final String username = currentUsername;

        CompletableFuture<List<ApiClient.RunHistoryEntry>> fut = CompletableFuture.supplyAsync(() -> {
            try {
                return (username == null || username.isBlank())
                        ? ApiClient.get().fetchOwnRunHistory()
                        : ApiClient.get().fetchUserRunHistory(username);
            } catch (Exception e) {
                return Collections.emptyList();
            }
        });

        fut.whenComplete((history, err) -> Platform.runLater(() -> {
            try {
                List<ApiClient.RunHistoryEntry> fresh =
                        (history == null) ? Collections.emptyList() : history;

                if (sameContent(items, fresh)) return; // no-op -> no flicker

                Long rememberRunNo = (lastSelectedRunNo != null)
                        ? lastSelectedRunNo
                        : (table.getSelectionModel().getSelectedItem() != null
                        ? table.getSelectionModel().getSelectedItem().runNo
                        : null);

                suppressSelectionListener = true;
                try {
                    applyDiffImmutable(fresh); // reuse instances when rows are identical

                    if (rememberRunNo != null) {
                        ApiClient.RunHistoryEntry selected = null;
                        for (ApiClient.RunHistoryEntry e2 : items) {
                            if (e2.runNo == rememberRunNo) { selected = e2; break; }
                        }
                        if (selected != null) {
                            table.getSelectionModel().select(selected);
                            safeScrollTo(selected);
                            lastSelectedRunNo = selected.runNo;
                        } else {
                            table.getSelectionModel().clearSelection();
                        }
                    }
                } finally {
                    suppressSelectionListener = false;
                }
            } finally {
                fetching = false;
            }
        }));
    }

    private boolean userInteracting() {
        return table.isPressed() || table.isHover() || table.isFocused();
    }

    // Compare lists by key + displayed fields
    private boolean sameContent(List<ApiClient.RunHistoryEntry> a, List<ApiClient.RunHistoryEntry> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            var x = a.get(i);
            var y = b.get(i);
            if (x.runNo != y.runNo) return false;
            if (x.isMainProgram != y.isMainProgram) return false;
            if (!Objects.equals(x.name, y.name)) return false;
            if (!Objects.equals(x.arch, y.arch)) return false;
            if (x.degree != y.degree) return false;
            if (x.y != y.y) return false;
            if (x.cycles != y.cycles) return false;
        }
        return true;
    }

    /**
     * Immutable-friendly diff:
     * - If a fresh row has the exact same displayed fields as an existing row with the same runNo,
     *   reuse the existing instance (keeps cell reuse, reduces churn).
     * - Otherwise, use the new instance.
     */
    private void applyDiffImmutable(List<ApiClient.RunHistoryEntry> fresh) {
        Map<Long, ApiClient.RunHistoryEntry> existingById = new HashMap<>();
        for (var e : items) existingById.put(e.runNo, e);

        List<ApiClient.RunHistoryEntry> merged = new ArrayList<>(fresh.size());
        for (var nf : fresh) {
            var old = existingById.get(nf.runNo);
            if (old != null && rowsEqualOnDisplayedFields(old, nf)) {
                merged.add(old);    // reuse old immutable instance
            } else {
                merged.add(nf);     // take new instance (changed row or brand new row)
            }
        }
        items.setAll(merged);
    }

    private boolean rowsEqualOnDisplayedFields(ApiClient.RunHistoryEntry a, ApiClient.RunHistoryEntry b) {
        return a.runNo == b.runNo
                && a.isMainProgram == b.isMainProgram
                && Objects.equals(a.name, b.name)
                && Objects.equals(a.arch, b.arch)
                && a.degree == b.degree
                && a.y == b.y
                && a.cycles == b.cycles;
    }

    private void safeScrollTo(ApiClient.RunHistoryEntry entry) {
        int idx = items.indexOf(entry);
        if (idx < 0) return;
        if (table.getFocusModel().getFocusedIndex() != idx) {
            table.scrollTo(idx);
        }
    }

    @FXML
    private void onRerunClicked() {
        ApiClient.RunHistoryEntry entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) return;

        ui.runner.SelectedProgram.set(entry.name);
        ui.runner.SelectedProgram.setSelectedDegree(entry.degree);
        ui.runner.SelectedProgram.setInputs(entry.inputs);

        try {
            ui.ClientApp.get().showRunScreen();
            Platform.runLater(() -> {
                var runScreenCtrl = ui.ClientApp.get().getRunScreenController();
                if (runScreenCtrl != null) {
                    runScreenCtrl.setMainProgram(entry.isMainProgram);
                    try {
                        runScreenCtrl.prepareRerun();
                        var centerRightCtrl = runScreenCtrl.getCenterController().getCenterRightController();
                        if (centerRightCtrl != null && centerRightCtrl.getArchitectureChoiceBox() != null) {
                            centerRightCtrl.getArchitectureChoiceBox().setValue(entry.arch);
                        }
                        if (!entry.isMainProgram) {
                            new Thread(() -> {
                                try {
                                    // Find parent program for the function
                                    String parentProgram = entry.name;
                                    String functionName = entry.name;
                                    var functions = ui.net.ApiClient.get().listAllFunctions();
                                    for (var f : functions) {
                                        if (f.name.equals(entry.name)) {
                                            parentProgram = f.program;
                                            functionName = f.name;
                                            break;
                                        }
                                    }
                                    var inputsInfo = ui.net.ApiClient.get().fetchInputsForProgram(parentProgram, functionName);
                                    System.out.println("entry.inputs: " + entry.inputs);
                                    System.out.println("inputsInfo: " + inputsInfo);
                                    Platform.runLater(() -> {
                                        centerRightCtrl.getInputTableController().setInputs(inputsInfo);
                                        List<Long> values = entry.inputs;
                                        for (int i = 0; i < Math.min(inputsInfo.size(), values.size()); i++) {
                                            centerRightCtrl.getInputTableController().setValue(inputsInfo.get(i).name, values.get(i));
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onActionShow() {
        ApiClient.RunHistoryEntry entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/dashboard/components/history_table/VarsPopup.fxml"));
            Scene scene = new Scene(loader.load());
            VarsPopupController controller = loader.getController();

            List<VarsPopupController.VarEntry> vars = new ArrayList<>();
            entry.vars.forEach((k, v) -> vars.add(new VarsPopupController.VarEntry(k, v)));
            controller.setVariables(vars);

            Stage popup = new Stage();
            popup.setTitle("Variables for Run #" + entry.runNo);
            popup.setScene(scene);
            popup.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

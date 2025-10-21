// java
package ui.dashboard.components.user_table;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import ui.net.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserTableController {
    @FXML private TableView<UserRow> table;
    @FXML private TableColumn<UserRow, String> nameCol;
    @FXML private TableColumn<UserRow, String> mainUploadedCol;
    @FXML private TableColumn<UserRow, String> helperContribCol;
    @FXML private TableColumn<UserRow, String> creditsCol;
    @FXML private TableColumn<UserRow, String> spentCol;
    @FXML private TableColumn<UserRow, String> runsCol;
    @FXML private Button uncheckBtn;

    private final ObservableList<UserRow> items = FXCollections.observableArrayList();
    private final Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(2), e -> refresh()));
    private volatile boolean fetching = false;

    @FXML
    private void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        mainUploadedCol.setCellValueFactory(new PropertyValueFactory<>("mainUploaded"));
        helperContribCol.setCellValueFactory(new PropertyValueFactory<>("helperContrib"));
        creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));
        spentCol.setCellValueFactory(new PropertyValueFactory<>("creditsSpent"));
        runsCol.setCellValueFactory(new PropertyValueFactory<>("runsCount"));

        table.setItems(items);

        if (uncheckBtn != null) {
            uncheckBtn.setOnAction(e -> table.getSelectionModel().clearSelection());
        }

        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();

        // Initial population
        refresh();

        // Stop timer when window closes
        Platform.runLater(() -> {
            if (table.getScene() != null && table.getScene().getWindow() != null) {
                table.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_HIDDEN, ev -> autoRefresh.stop());
                table.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, ev -> autoRefresh.stop());
            }
        });
    }

    // Async refresh; applies updates on FX thread; prevents overlaps
    private void refresh() {
        if (fetching) return;
        fetching = true;

        String selectedName;
        if (table != null && table.getSelectionModel().getSelectedItem() != null) {
            selectedName = table.getSelectionModel().getSelectedItem().getName();
        } else {
            selectedName = null;
        }

        CompletableFuture<List<UserRow>> fut = CompletableFuture.supplyAsync(() -> {
            try {
                List<ApiClient.UserOnline> users = ApiClient.get().usersOnline();
                List<UserRow> rows = new ArrayList<>(users.size());
                for (ApiClient.UserOnline u : users) rows.add(UserRow.from(u));
                return rows;
            } catch (Exception ex) {
                return java.util.Collections.<UserRow>emptyList();
            }
        });

        fut.whenComplete((rows, err) -> Platform.runLater(() -> {
            items.setAll(rows == null ? java.util.Collections.<UserRow>emptyList() : rows);
            if (selectedName != null) {
                for (UserRow row : items) {
                    if (row.getName().equals(selectedName)) {
                        table.getSelectionModel().select(row);
                        break;
                    }
                }
            }
            fetching = false;
        }));
    }

    // Bean for TableView
    public static final class UserRow {
        private final String name;
        private final String mainUploaded;
        private final String helperContrib;
        private final String credits;
        private final String creditsSpent;
        private final String runsCount;

        public UserRow(String name, long mainUploaded, long helperContrib,
                       long credits, long creditsSpent, long runsCount) {
            this.name = name;
            this.mainUploaded = String.valueOf(mainUploaded);
            this.helperContrib = String.valueOf(helperContrib);
            this.credits = String.valueOf(credits);
            this.creditsSpent = String.valueOf(creditsSpent);
            this.runsCount = String.valueOf(runsCount);
        }

        public static UserRow from(ApiClient.UserOnline u) {
            return new UserRow(u.name, u.mainUploaded, u.helperContrib, u.credits, u.creditsSpent, u.runsCount);
        }

        public String getName() { return name; }
        public String getMainUploaded() { return mainUploaded; }
        public String getHelperContrib() { return helperContrib; }
        public String getCredits() { return credits; }
        public String getCreditsSpent() { return creditsSpent; }
        public String getRunsCount() { return runsCount; }
    }
}

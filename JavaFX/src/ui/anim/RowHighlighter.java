package ui.anim;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Duration;

public final class RowHighlighter {
    private RowHighlighter(){}

    public static void pulseRow(TableView<?> table, int rowIndex) {
        if (!AnimationSettings.isEnabled()) return;
        // ensure the row is in the viewport, then animate it if visible
        Platform.runLater(() -> {
            // scroll so the row is visible (optional)
            table.scrollTo(Math.max(0, rowIndex - 2));

            for (Node n : table.lookupAll(".table-row-cell")) {
                if (n instanceof TableRow<?> r && r.getIndex() == rowIndex) {
                    FadeTransition ft = new FadeTransition(Duration.millis(900), r);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.6);
                    ft.setAutoReverse(true);
                    ft.setCycleCount(2); // 0.9s total
                    ft.play();
                    break;
                }
            }
        });
    }
}

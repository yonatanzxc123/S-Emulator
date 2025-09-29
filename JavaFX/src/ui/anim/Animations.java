package ui.anim;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class Animations {
    private Animations(){}

    // ===== Bold neon glow + tiny scale pop (≤ ~1.2s) =====
    public static void glow(Node node) {
        if (!AnimationSettings.isEnabled() || node == null) return;
        // Visible on light UIs; switch to cyan/violet for dark themes
        glow(node, Color.web("#39FF14"), /*maxRadius*/48, /*spread*/0.85, /*riseMs*/220, /*totalMs*/1200);
    }

    /** Customizable glow: accent color, halo size/spread, timing. */
    public static void glow(Node node, Color accent, double maxRadius, double spread, int riseMs, int totalMs) {
        if (!AnimationSettings.isEnabled() || node == null) return;

        Glow glow = new Glow(0.0);
        DropShadow halo = new DropShadow();
        halo.setColor(accent);
        halo.setRadius(0);                 // animated
        halo.setSpread(Math.max(0, Math.min(1, spread)));
        halo.setInput(glow);
        node.setEffect(halo);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(halo.radiusProperty(), 0),
                        new KeyValue(glow.levelProperty(), 0.00)
                ),
                new KeyFrame(Duration.millis(Math.max(1, riseMs)),   // quick rise
                        new KeyValue(halo.radiusProperty(), Math.max(0, maxRadius)),
                        new KeyValue(glow.levelProperty(), 0.80)
                ),
                new KeyFrame(Duration.millis(Math.max(riseMs + 1, totalMs)),  // smooth fall
                        new KeyValue(halo.radiusProperty(), 0),
                        new KeyValue(glow.levelProperty(), 0.00)
                )
        );

        // Tiny scale "pop"
        ScaleTransition st = new ScaleTransition(Duration.millis(Math.min(riseMs, 260)), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.04);  st.setToY(1.04);
        st.setAutoReverse(true);
        st.setCycleCount(2);

        ParallelTransition pt = new ParallelTransition(tl, st);
        pt.setOnFinished(e -> node.setEffect(null)); // clean up
        pt.play();
    }

    // ===== Reusable scale "pop" (for Step Over button, cycles label, etc.) =====
    public static void pop(Node node) {
        pop(node, 1.06, 180); // default: 6% zoom, 180ms up and back
    }

    public static void pop(Node node, double toScale, int ms) {
        if (!AnimationSettings.isEnabled() || node == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(Math.max(1, ms)), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(Math.max(1.0, toScale)); st.setToY(Math.max(1.0, toScale));
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    // ===== Cycles counter tween (≤ ~0.7s) =====
    public static void countTo(javafx.scene.control.Label label, long target) {
        if (label == null) return;
        if (!AnimationSettings.isEnabled()) { label.setText(Long.toString(target)); return; }

        long start = 0;
        try { start = Long.parseLong(label.getText()); } catch (Exception ignore) {}
        final long from = start, to = target;

        javafx.beans.property.DoubleProperty p = new javafx.beans.property.SimpleDoubleProperty(from);
        p.addListener((obs, ov, nv) -> label.setText(Long.toString(Math.round(nv.doubleValue()))));

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(p, (double) from)),
                new KeyFrame(Duration.millis(700), new KeyValue(p, (double) to))
        );
        tl.play();
    }
}

package ui.anim;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.prefs.Preferences;

public final class AnimationSettings {
    private static final Preferences PREFS =
            Preferences.userRoot().node("ui/anim/AnimationSettings");

    private static final BooleanProperty enabled = new SimpleBooleanProperty(true);

    static {
        boolean saved = PREFS.getBoolean("enabled", true);
        enabled.set(saved);
        enabled.addListener((obs, oldV, newV) -> {
            PREFS.putBoolean("enabled", newV);
        });
    }

    private AnimationSettings(){}

    public static BooleanProperty enabledProperty(){ return enabled; }
    public static boolean isEnabled(){ return enabled.get(); }
    public static void setEnabled(boolean v){ enabled.set(v); }
}

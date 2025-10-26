// java
package ui.runner;

import java.util.List;

public final class SelectedProgram {
    private static volatile String program;
    private static int selectedDegree = 0;
    private static List<Long> inputs = List.of();
    public static void setInputs(List<Long> in) { inputs = in == null ? List.of() : List.copyOf(in); }
    public static List<Long> getInputs() { return inputs; }
    public static int getSelectedDegree() { return selectedDegree; }
    public static void setSelectedDegree(int degree) { selectedDegree = degree; }

    private SelectedProgram() {}

    public static void set(String name) { program = name; }
    public static String get() { return program; }
}

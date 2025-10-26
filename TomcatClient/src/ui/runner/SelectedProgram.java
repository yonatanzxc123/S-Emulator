// java
package ui.runner;

public final class SelectedProgram {
    private static volatile String program;
    private static int selectedDegree = 0;
    public static int getSelectedDegree() { return selectedDegree; }
    public static void setSelectedDegree(int degree) { selectedDegree = degree; }

    private SelectedProgram() {}

    public static void set(String name) { program = name; }
    public static String get() { return program; }
}

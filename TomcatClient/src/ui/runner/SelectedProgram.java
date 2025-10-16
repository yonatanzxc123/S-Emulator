// java
package ui.runner;

public final class SelectedProgram {
    private static volatile String program;

    private SelectedProgram() {}

    public static void set(String name) { program = name; }
    public static String get() { return program; }
}

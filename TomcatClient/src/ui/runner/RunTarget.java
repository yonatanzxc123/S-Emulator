// java
package ui.runner;

import java.util.Objects;

public final class RunTarget {
    public enum Kind { PROGRAM, FUNCTION }

    public final Kind kind;
    public final String programName;
    public final String functionName;

    private static volatile RunTarget CURRENT;

    public RunTarget(Kind kind, String programName, String functionName) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.programName = Objects.requireNonNull(programName, "programName");
        this.functionName = functionName;
    }

    public static void setProgram(String programName) {
        CURRENT = new RunTarget(Kind.PROGRAM, programName, null);
    }

    public static RunTarget getAndClear() {
        RunTarget t = CURRENT;
        CURRENT = null;
        return t;
    }
}

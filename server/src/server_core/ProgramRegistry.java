// java
package server_core;

import system.core.model.Program;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small central registry for uploaded programs and function metadata.
 * ProgramsServlet should call registerProgram(...) when a program is accepted.
 */
public final class ProgramRegistry {

    private ProgramRegistry() {}

    // programName -> main Program
    private static final Map<String, Program> MAIN_PROGRAMS = new LinkedHashMap<>();
    // programName -> owner username
    private static final Map<String, String> PROGRAM_OWNER = new LinkedHashMap<>();
    // programName -> instrDeg0, maxDegree
    private static final Map<String, ProgramsServlet.Stats> STATS = new LinkedHashMap<>();
    // functionName -> FunctionMeta (reuse existing class from ProgramsServlet package)
    private static final Map<String, FunctionMeta> FUNCTIONS = new LinkedHashMap<>();

    public static synchronized void registerProgram(String programName, String owner, Program mainProgram,
                                                    int instrDeg0, int maxDegree,
                                                    Map<String, Program> providedFns) {
        MAIN_PROGRAMS.put(programName, mainProgram);
        PROGRAM_OWNER.put(programName, owner);
        STATS.put(programName, new ProgramsServlet.Stats(instrDeg0, maxDegree));

        // register detailed functions
        for (Map.Entry<String, Program> e : providedFns.entrySet()) {
            String fnName = e.getKey();
            Program body = e.getValue();
            int instr = body.instructions().size();
            int maxDeg = EngineUtil.instructionsJson(body) == null ? 0 : FunctionEnvWithMaxDegree(body); // fallback
            FunctionMeta meta = new FunctionMeta(fnName, programName, owner, instr, maxDeg);
            FUNCTIONS.put(fnName, meta);
        }
    }

    // helper to compute max degree for a Program (reuse logic similar to ProgramsServlet.computeMaxDegree)
    private static int FunctionEnvWithMaxDegree(Program p) {
        final int CAP = 1000;
        var expander = new system.core.expand.ExpanderImpl();
        int d = 0;
        Program cur = p;
        while (d < CAP && containsSynthetic(cur)) {
            cur = expander.expandToDegree(cur, 1);
            d++;
        }
        return d;
    }

    private static boolean containsSynthetic(Program p) {
        for (var ins : p.instructions()) {
            if (!ins.isBasic()) return true;
        }
        return false;
    }

    public static synchronized Program getMainProgram(String programName) {
        return MAIN_PROGRAMS.get(programName);
    }

    public static synchronized String getOwner(String programName) {
        return PROGRAM_OWNER.get(programName);
    }

    public static synchronized List<FunctionMeta> functionsOfProgram(String programName) {
        var out = new java.util.ArrayList<FunctionMeta>();
        for (FunctionMeta fm : FUNCTIONS.values()) {
            if (programName.equals(fm.definedInProgram())) out.add(fm);
        }
        return out;
    }

    public static synchronized Map<String, Program> copyMainPrograms() {
        return new LinkedHashMap<>(MAIN_PROGRAMS);
    }

    public static synchronized boolean containsProgram(String programName) {
        return MAIN_PROGRAMS.containsKey(programName);
    }
}

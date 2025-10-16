package system.api.view;

import system.core.model.Program;
import java.util.Map;

/** Summary returned by the engine after an XML ingest/validation. */
public record IngestReport(
        String programName,
        Program mainProgram,
        Map<String, Program> providedFunctions,     // functions defined in the XML
        Map<String, Integer> functionInstrDeg0,     // per-function degree-0 instr count
        Map<String, Integer> functionMaxDegree,     // per-function max expansion degree
        int mainInstrDeg0,
        int mainMaxDegree
) {}

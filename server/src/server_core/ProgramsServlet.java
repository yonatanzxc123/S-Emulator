package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import system.api.view.IngestReport;
import system.api.view.ProgramView;
import system.core.EmulatorEngineImpl;
import system.core.exec.FunctionEnv;
import system.core.expand.ExpanderImpl;
import system.core.io.ArchTierMap;
import system.core.io.ProgramMapper;
import system.core.model.Instruction;
import system.core.model.Program;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles listing of programs and uploading new programs via XML.
 * Also serves program data to clients (e.g., program instruction bodies).
 */
@WebServlet(name = "ProgramsServlet", urlPatterns = {"/api/programs/*"}, loadOnStartup = 1)
public class ProgramsServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sp = subPath(req);
        if (sp == null) sp = "";
        switch (sp) {
            case "/upload" -> handleProgramUpload(req, resp);
            default        -> json(resp, 404, "{\"error\":\"not_found\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sp = subPath(req);
        if (sp == null || sp.isBlank() || "/".equals(sp)) {
            listPrograms(resp);
        } else if ("/functions".equals(sp)) {
            listFunctions(resp);
        } else if (sp.endsWith("/body") && sp.length() > "/body".length() + 1) {
            String name = sp.substring(1, sp.length() - "/body".length());
            programBody(req, resp, name);
        } else {
            json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(sp) + "\"}");
        }
    }


    // --- POST /api/programs/upload ---
    private void handleProgramUpload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;
        final String owner = u.name();

        final String body = readBody(req);
        final String xml  = jStr(body, "xml");
        if (xml == null || xml.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_xml\"}");
            return;
        }

        // Use a new engine instance to parse and ingest the program XML
        final EmulatorEngineImpl engine = new EmulatorEngineImpl();
        final IngestReport report;
        try {
            // Provide a snapshot of current global helper functions to the engine for linking
            report = engine.ingestFromXml(xml, new LinkedHashMap<>(FUNCTION_BODIES));
        } catch (IllegalArgumentException ex) {
            json(resp, 400, "{\"error\":\"" + esc(ex.getMessage()) + "\"}");
            return;
        }

        // Validate that the program and any provided helper functions are unique
        final String programName = report.programName();
        if (PROGRAMS.containsKey(programName)) {
            json(resp, 409, "{\"error\":\"duplicate_program\",\"name\":\"" + esc(programName) + "\"}");
            return;
        }
        for (String fn : report.providedFunctions().keySet()) {
            if (FUNCTIONS.containsKey(fn)) {
                json(resp, 409, "{\"error\":\"duplicate_function\",\"name\":\"" + esc(fn) + "\"}");
                return;
            }
        }

        // Everything is valid, register the new program
        PROGRAMS.put(programName, new ProgramMeta(
                programName,
                owner,
                engine.getFunctions().keySet(),
                report.mainProgram(),
                report.mainInstrDeg0(),
                report.mainMaxDegree(),
                engine
        ));
        // Register all newly provided helper functions
        for (Map.Entry<String, Program> entry : report.providedFunctions().entrySet()) {
            final String fnName = entry.getKey();
            FUNCTION_BODIES.put(fnName, entry.getValue());
            FUNCTIONS.put(fnName, new FunctionMeta(
                    fnName,
                    programName,
                    owner,
                    report.functionInstrDeg0().getOrDefault(fnName, 0),
                    report.functionMaxDegree().getOrDefault(fnName, 0)
            ));
        }

        // Update user stats and global version
        u.mainUploaded.incrementAndGet();
        u.helperContrib.addAndGet(report.providedFunctions().size());
        VERSION.incrementAndGet();



        // Send response with details of the new program
        final String resJson = "{"
                + "\"ok\":true,"
                + "\"programName\":\"" + esc(programName) + "\","
                + "\"owner\":\"" + esc(owner) + "\","
                + "\"instrDeg0\":" + report.mainInstrDeg0() + ","
                + "\"maxDegree\":" + report.mainMaxDegree() + ","
                + "\"functions\":" + toJsonArray(report.providedFunctions().keySet()) + ","
                + "\"functionsDetailed\":" + functionsDetailedJson(report)
                + "}";
        json(resp, 200, resJson);
    }

    // --- GET /api/programs (list all programs) ---
    private void listPrograms(HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder("{\"programs\":[");
        boolean first = true;
        for (ProgramMeta pm : PROGRAMS.values()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{")
                    .append("\"name\":\"").append(esc(pm.name)).append("\",")
                    .append("\"owner\":\"").append(esc(pm.ownerUser)).append("\",")
                    .append("\"instrDeg0\":").append(pm.instrCountDeg0).append(',')
                    .append("\"maxDegree\":").append(pm.maxDegree).append(',')
                    .append("\"timesRun\":").append(pm.runsCount.get()).append(',')
                    .append("\"avgCredits\":").append(Math.round(pm.avgCreditsCost))
                    .append("}");
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }

    // --- GET /api/programs/functions ---
    private void listFunctions(HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder("{\"functions\":[");
        boolean first = true;
        for (FunctionMeta fm : FUNCTIONS.values()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{")
                    .append("\"name\":\"").append(esc(fm.name())).append("\",")
                    .append("\"definedInProgram\":\"").append(esc(fm.definedInProgram())).append("\",")
                    .append("\"owner\":\"").append(esc(fm.ownerUser())).append("\",")
                    .append("\"instr\":").append(fm.instrCount()).append(',')
                    .append("\"maxDegree\":").append(fm.maxDegree())
                    .append("}");
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }

    private static String functionsDetailedJson(IngestReport report) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String fn : report.providedFunctions().keySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"name\":\"").append(esc(fn)).append("\",")
                    .append("\"instr\":").append(report.functionInstrDeg0().getOrDefault(fn, 0)).append(',')
                    .append("\"maxDegree\":").append(report.functionMaxDegree().getOrDefault(fn, 0))
                    .append("}");
        }
        sb.append(']');
        return sb.toString();
    }



    private void programBody(HttpServletRequest req, HttpServletResponse resp, String programName) throws IOException {
        final ProgramMeta meta = PROGRAMS.get(programName);
        final boolean isFunction = (meta == null);

        Program base;
        int maxDegree;
        Map<String, Program> fnMap;

        if (isFunction) {
            Program body = FUNCTION_BODIES.get(programName);
            FunctionMeta fm = FUNCTIONS.get(programName);
            if (body == null || fm == null) {
                json(resp, 404, "{\"error\":\"program_not_found\",\"name\":\"" + esc(programName) + "\"}");
                return;
            }
            base = body;
            maxDegree = fm.maxDegree();
            fnMap = FUNCTION_BODIES;
        } else {
            base = meta.mainProgram;
            maxDegree = meta.maxDegree;
            fnMap = meta.engine.getFunctions();
        }

        int degree = parseDegree(req.getParameter("degree"), maxDegree);
        final int useDegree = degree;

        ProgramView view;
        Program p;
        try {
            Object[] result = FunctionEnv.with(new FunctionEnv(fnMap), () -> {
                Program program;
                ProgramView pv;
                if (useDegree == 0) {
                    program = base;
                    pv = ProgramMapper.toView(base);
                } else {
                    var res = new ExpanderImpl().expandToDegreeWithOrigins(base, useDegree);
                    program = res.program();
                    pv = ProgramMapper.toView(program, res.origins());
                }
                return new Object[]{program, pv};
            });
            p = (Program) result[0];
            view = (ProgramView) result[1];
        } catch (Exception e) {
            json(resp, 500, "{\"error\":\"expand_failed\",\"degree\":" + useDegree + "}");
            return;
        }

        StringBuilder sb = new StringBuilder("{\"degree\":").append(useDegree)
                .append(",\"maxDegree\":").append(maxDegree)
                .append(",\"instructions\":[");

        List<Instruction> insns = p.instructions();
        List<system.api.view.CommandView> cmds = view.commands();
        for (int i = 0; i < insns.size(); i++) {
            if (i > 0) sb.append(',');
            var ins = insns.get(i);
            var cv = cmds.get(i);

            sb.append("{\"index\":").append(i + 1)
                    .append(",\"op\":\"").append(esc(ins.asText())).append('"')
                    .append(",\"level\":\"").append(toRoman(ArchTierMap.tierOf(ins.getClass()))).append('"')
                    .append(",\"bs\":\"").append(ins.isBasic() ? "B" : "S").append('"')
                    .append(",\"label\":\"").append(esc(cv.labelOrEmpty() == null ? "" : cv.labelOrEmpty())).append('"')
                    .append(",\"cycles\":").append(Math.max(0, cv.cycles()))
                    .append(",\"originChain\":\"").append(esc(cv.originChain() == null ? "" : cv.originChain())).append("\"}");
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }



    // --- Helpers ---

    private static int parseDegree(String q, int max) {
        try {
            int d = (q == null) ? 0 : Integer.parseInt(q);
            return Math.max(0, Math.min(d, max));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String toRoman(int t) {
        return switch (t) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }





}

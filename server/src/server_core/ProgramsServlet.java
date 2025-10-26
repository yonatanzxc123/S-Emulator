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
import server_core.util.CommonUtils;

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
        }catch (RuntimeException ex) {
            // e.g., missing function during linking/expansion → used to bubble up as 500
            String msg = deepestMessage(ex);
            // Optional: detect the common "Function 'X' not found" and return the name
            String missingFn = extractMissingFunctionName(msg);
            if (missingFn != null) {
                json(resp, 400, "{\"error\":\"missing_functions_to_operate\",\"name\":\"" + esc(missingFn) + "\"}");
            } else {
                json(resp, 400, "{\"error\":\"invalid_program\",\"detail\":\"" + esc(msg) + "\"}");
            }
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
            // If requesting a helper function body
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
            // Requesting a main program body
            base = meta.mainProgram;
            maxDegree = meta.maxDegree;
            fnMap = meta.engine.getFunctions();
        }

        // Parse the requested degree (clamped to maxDegree)
        int degree = CommonUtils.parseDegree(req.getParameter("degree"), maxDegree);
        final int useDegree = degree;


        boolean withOrigins = Boolean.parseBoolean(req.getParameter("withOrigins"));

        // Expand program to the requested degree and map to a view
        ProgramView view;
        Program p;
        try {
            Object[] result = FunctionEnv.with(new FunctionEnv(fnMap), () -> {
                Program program;
                ProgramView pv;
                if (useDegree == 0) {
                    // Degree 0: no expansion
                    program = base;
                    pv = ProgramMapper.toView(base);
                } else {
                    // Expand the program to the given degree, with origin tracking
                    var res = new ExpanderImpl().expandToDegreeWithOrigins(base, useDegree);
                    program = res.program();
                    pv = ProgramMapper.toView(program, withOrigins ? res.origins() : null);
                }
                return new Object[]{program, pv};
            });
            p = (Program) result[0];
            view = (ProgramView) result[1];
        } catch (Exception e) {
            json(resp, 500, "{\"error\":\"expand_failed\",\"degree\":" + useDegree + "}");
            return;
        }

        // Build response JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        var out = resp.getWriter();

        out.print("{\"degree\":" + useDegree + ",\"maxDegree\":" + maxDegree + ",\"instructions\":[");


        List<Instruction> insns = p.instructions();
        List<system.api.view.CommandView> cmds = view.commands();

        for (int i = 0; i < insns.size(); i++) {
            if (i > 0) out.print(",");
            var ins = insns.get(i);
            var cv = cmds.get(i);

            out.print("{\"index\":" + (i + 1));
            out.print(",\"op\":\"" + esc(ins.asText()) + "\"");
            out.print(",\"level\":\"" + CommonUtils.toRoman(ArchTierMap.tierOf(ins.getClass())) + "\"");
            out.print(",\"bs\":\"" + (ins.isBasic() ? "B" : "S") + "\"");
            out.print(",\"label\":\"" + esc(nullToEmpty(cv.labelOrEmpty())) + "\"");
            out.print(",\"cycles\":" + Math.max(0, cv.cycles()));
            out.print(",\"originChain\":\"" + esc(withOrigins ? nullToEmpty(cv.originChain()) : "") + "\"}");
        }

        out.print("]}");
        out.flush();
    }


    // --- Helpers ---

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
    private static String deepestMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage() == null ? t.toString() : cur.getMessage();
    }

    private static String extractMissingFunctionName(String msg) {
        // Match: Function 'foo' not found
        if (msg == null) return null;
        int i = msg.indexOf("Function '");
        int j = msg.indexOf("' not found");
        if (i >= 0 && j > i + 10) {
            return msg.substring(i + 10, j);
        }
        return null;
    }


}

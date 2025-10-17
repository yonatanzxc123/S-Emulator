package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import system.api.view.IngestReport;
import system.core.EmulatorEngineImpl;
import system.core.model.Program;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lists programs, handles upload, and serves /api/programs/{name}/body
 * so the JavaFX client can populate the instruction table.
 */
@WebServlet(name = "ProgramsServlet", urlPatterns = {"/api/programs/*"}, loadOnStartup = 1)
public class ProgramsServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sp = subPath(req);
        if (sp == null) sp = "";
        switch (sp) {
            case "/upload" -> handleProgramUpload(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sp = subPath(req);
        if (sp == null) sp = "/";
        switch (sp) {
            case "/", "" -> { listPrograms(resp); return; }
            case "/functions" -> { listFunctions(resp); return; }
            default -> json(resp, 404, "{\"error\":\"not_found\"}");
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

        // Let the ENGINE do the heavy work (parse, merge functions, validate, compute degrees)
        final EmulatorEngineImpl engine = new EmulatorEngineImpl();
        final IngestReport report;
        try {
            // Pass a snapshot of the current global helper functions
            report = engine.ingestFromXml(xml, new LinkedHashMap<>(FUNCTION_BODIES));
        } catch (IllegalArgumentException ex) {
            json(resp, 400, "{\"error\":\"" + esc(ex.getMessage()) + "\"}");
            return;
        }

        // API-level uniqueness checks (pure validation; still no persistence)
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

        // Persist program meta (functions snapshot is the engine's merged view)
        PROGRAMS.put(programName, new ProgramMeta(
                programName,
                owner,
                engine.getFunctions().keySet(),
                report.mainProgram(),
                report.mainInstrDeg0(),
                report.mainMaxDegree(),
                engine
        ));

        // Persist the newly provided functions: both body and summarized meta
        for (Map.Entry<String, Program> e : report.providedFunctions().entrySet()) {
            final String fn = e.getKey();
            FUNCTION_BODIES.put(fn, e.getValue());
            FUNCTIONS.put(fn, new FunctionMeta(
                    fn,
                    programName,
                    owner,
                    report.functionInstrDeg0().getOrDefault(fn, 0),
                    report.functionMaxDegree().getOrDefault(fn, 0)
            ));
        }

        // Stats + version bump
        u.mainUploaded.incrementAndGet();
        u.helperContrib.addAndGet(report.providedFunctions().size());
        VERSION.incrementAndGet();

        // Build functionsDetailed array for client
        StringBuilder fd = new StringBuilder("[");
        boolean first = true;
        for (String fn : report.providedFunctions().keySet()) {
            if (!first) fd.append(',');
            first = false;
            int instr0 = report.functionInstrDeg0().getOrDefault(fn, 0);
            int maxDeg = report.functionMaxDegree().getOrDefault(fn, 0);
            fd.append("{\"name\":\"").append(esc(fn)).append("\",")
                    .append("\"instr\":").append(instr0).append(',')
                    .append("\"maxDegree\":").append(maxDeg).append('}');
        }
        fd.append(']');

        // Response aligned with ApiClient.uploadProgram()
        final String res = "{"
                + "\"ok\":true,"
                + "\"programName\":\"" + esc(programName) + "\","
                + "\"owner\":\"" + esc(owner) + "\","
                + "\"instrDeg0\":" + report.mainInstrDeg0() + ","
                + "\"maxDegree\":" + report.mainMaxDegree() + ","
                + "\"functions\":" + toJsonArray(report.providedFunctions().keySet()) + ","
                + "\"functionsDetailed\":" + fd
                + "}";
        json(resp, 200, res);

    }

    // --- GET /api/programs ---
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
}

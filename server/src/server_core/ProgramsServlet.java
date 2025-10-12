// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import system.core.EmulatorEngineImpl;
import system.core.io.ProgramLoaderJaxb;
import system.core.model.Program;

import java.io.IOException;

@WebServlet(name = "ProgramsServlet", urlPatterns = {"/api/programs/*"}, loadOnStartup = 1)
public class ProgramsServlet extends BaseApiServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/upload" -> handleProgramUploadStub(req, resp);
            default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    private void handleProgramUploadStub(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = requireUser(req, resp);
        if (u == null) return;

        String body = readBody(req);
        String xml  = jStr(body, "xml");
        if (xml == null || xml.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_name_or_xml\"}");
            return;
        }

        // 1) Parse via your engine — in-memory (uses the new loadFromString)
        var loader = new ProgramLoaderJaxb();
        var outcome = loader.loadFromString(xml);
        if (!outcome.ok()) {
            json(resp, 400, "{\"error\":\"xml_invalid\",\"details\":\"" + esc(String.join("; ", outcome.errors())) + "\"}");
            return;
        }
        Program main = outcome.program();
        String programName = main.name();

        // 2) Uniqueness of main program
        if (PROGRAMS.containsKey(programName)) {
            json(resp, 409, "{\"error\":\"program_name_exists\"}");
            return;
        }

        // 3) No function redefinitions
        var provided = outcome.functions().keySet();
        for (String fn : provided) {
            if (FUNCTIONS.containsKey(fn)) {
                json(resp, 409, "{\"error\":\"function_redefinition\",\"name\":\"" + esc(fn) + "\"}");
                return;
            }
        }

        // 4) "Used functions must exist" — your model uses FunctionEnv at run time.
        // We can do a conservative check here: if the main program contains a
        // synthetic instruction that refers to a function by *name*, reject when missing.
        // (If your call-site is a specific class, add it here explicitly.)
        // For now, we skip aggressive guessing and rely on run-time to fail if truly missing.
        // >>> If you have a specific instruction class for calls, tell me its name and I’ll add exact checks <<<

        // 5) Create an engine instance for this program & load from XML (no files)
        var engine = new EmulatorEngineImpl();
        var load2 = engine.loadProgramFromString(xml); // uses the new method you added
        if (!load2.ok()) {
            json(resp, 400, "{\"error\":\"engine_load_failed\",\"details\":\"" + esc(String.join("; ", load2.errors())) + "\"}");
            return;
        }

        // 6) Fill registries
        PROGRAMS.put(programName, new ProgramMeta(
                programName, u.name,
                provided, main,
                /*deg0*/ main.instructions().size(),
                /*maxDeg*/ engine.getMaxDegree(),
                engine
        ));
        for (String fn : provided) {
            FUNCTIONS.put(fn, new FunctionMeta(fn, programName, u.name));
            u.helperContrib.incrementAndGet();
        }
        u.mainUploaded.incrementAndGet();
        VERSION.incrementAndGet();

        json(resp, 200, "{\"ok\":true," +
                "\"addedProgram\":{\"name\":\"" + esc(programName) + "\",\"owner\":\"" + esc(u.name) + "\"}," +
                "\"provides\":" + toJsonArray(provided) + "," +
                "\"instrDeg0\":" + main.instructions().size() + "," +
                "\"maxDegree\":" + engine.getMaxDegree() + "}");
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (subPath(req)) {
            case "/" -> listPrograms(resp);
            default -> json(resp, 404, "{\"error\":\"not_found\",\"path\":\"" + esc(subPath(req)) + "\"}");
        }
    }

    private void listPrograms(HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder().append("{\"programs\":[");
        boolean first = true;
        for (ProgramMeta p : PROGRAMS.values()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"name\":\"").append(esc(p.name)).append("\",")
                    .append("\"owner\":\"").append(esc(p.ownerUser)).append("\",")
                    .append("\"runsCount\":").append(p.runsCount.get()).append(",")
                    .append("\"avgCreditsCost\":").append(p.avgCreditsCost).append("}");
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }



}

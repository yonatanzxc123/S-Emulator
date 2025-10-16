// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import system.core.exec.FunctionEnv;
import system.core.expand.ExpanderImpl;
import system.core.model.Instruction;
import system.core.model.Program;
import system.core.io.ProgramLoaderJaxb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ProgramsServlet", urlPatterns = {"/api/programs/*"}, loadOnStartup = 1)
public class ProgramsServlet extends BaseApiServlet {

    private static final Map<String, String> PROGRAM_OWNER = new LinkedHashMap<>();
    private static final Map<String, Stats> STATS = new LinkedHashMap<>();
    private static final Map<String, FunctionMeta> FUNCTIONS = new LinkedHashMap<>();

    static record Stats(int instrDeg0, int maxDegree) {}

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
        if (sp == null || sp.isBlank() || "/".equals(sp)) {
            listPrograms(resp);
        } else {
            json(resp, 404, "{\"error\":\"not_found\"}");
        }
    }

    private void handleProgramUpload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object uAny = requireUser(req, resp);
        if (uAny == null) return;
        String owner = usernameOf(uAny);

        String body = readBody(req);
        String xml  = jStr(body, "xml");
        if (xml == null || xml.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_xml\"}");
            return;
        }

        var loader = new ProgramLoaderJaxb();
        var outcome = loader.loadFromString(xml);
        if (!outcome.ok()) {
            json(resp, 400, "{\"error\":\"parse_failed\",\"details\":\"" + esc(String.join("; ", outcome.errors())) + "\"}");
            return;
        }

        Program main = outcome.program();
        Map<String, Program> providedFns = outcome.functions();

        String programName = main.name();
        if (PROGRAM_OWNER.containsKey(programName)) {
            json(resp, 409, "{\"error\":\"program_exists\",\"name\":\"" + esc(programName) + "\"}");
            return;
        }
        for (String fn : providedFns.keySet()) {
            if (FUNCTIONS.containsKey(fn)) {
                json(resp, 409, "{\"error\":\"function_redefinition\",\"name\":\"" + esc(fn) + "\"}");
                return;
            }
        }

        int programInstr0 = main.instructions().size();
        int programMaxDeg = FunctionEnv.with(new FunctionEnv(providedFns), () -> computeMaxDegree(main));

        PROGRAM_OWNER.put(programName, owner);
        STATS.put(programName, new Stats(programInstr0, programMaxDeg));

        List<String> fnNames = new ArrayList<>();
        StringBuilder fjson = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, Program> e : providedFns.entrySet()) {
            String fnName = e.getKey();
            Program Pbody = e.getValue();
            int instr = Pbody.instructions().size();
            int maxDeg = FunctionEnv.with(new FunctionEnv(providedFns), () -> computeMaxDegree(Pbody));

            FunctionMeta meta = new FunctionMeta(fnName, programName, owner, instr, maxDeg);
            FUNCTIONS.put(fnName, meta);
            fnNames.add(fnName);

            if (!first) fjson.append(',');
            first = false;
            fjson.append("{\"name\":\"").append(esc(fnName)).append("\",")
                    .append("\"instr\":").append(instr).append(',')
                    .append("\"maxDegree\":").append(maxDeg).append('}');
        }
        fjson.append(']');

        // Increment per-user counters so the Users table reflects uploads
        User me = optUser(req);
        if (me != null) {
            me.mainUploaded.incrementAndGet();
            me.helperContrib.addAndGet(providedFns.size());
            me.lastSeenMs = System.currentTimeMillis();
            VERSION.incrementAndGet();
        }

        StringBuilder ok = new StringBuilder();
        ok.append('{')
                .append("\"ok\":true,")
                .append("\"programName\":\"").append(esc(programName)).append("\",")
                .append("\"owner\":\"").append(esc(owner)).append("\",")
                .append("\"instrDeg0\":").append(programInstr0).append(',')
                .append("\"maxDegree\":").append(programMaxDeg).append(',')
                .append("\"functions\":").append(asJsonArrayOfStrings(fnNames)).append(',')
                .append("\"functionsDetailed\":").append(fjson)
                .append('}');
        json(resp, 200, ok.toString());
    }

    private void listPrograms(HttpServletResponse resp) throws IOException {
        StringBuilder sb = new StringBuilder().append("{\"programs\":[");
        boolean firstP = true;

        for (Map.Entry<String, String> e : PROGRAM_OWNER.entrySet()) {
            String programName = e.getKey();
            String owner = e.getValue();
            Stats s = STATS.getOrDefault(programName, new Stats(0, 0));

            if (!firstP) sb.append(',');
            firstP = false;

            List<FunctionMeta> fns = new ArrayList<>();
            for (FunctionMeta fm : FUNCTIONS.values()) {
                if (programName.equals(fm.definedInProgram())) {
                    fns.add(fm);
                }
            }

            sb.append("{\"name\":\"").append(esc(programName)).append("\",")
                    .append("\"owner\":\"").append(esc(owner)).append("\",")
                    .append("\"instrDeg0\":").append(s.instrDeg0()).append(',')
                    .append("\"maxDegree\":").append(s.maxDegree()).append(',')
                    .append("\"functions\":[");
            boolean firstF = true;
            for (FunctionMeta fm : fns) {
                if (!firstF) sb.append(',');
                firstF = false;
                sb.append("{\"name\":\"").append(esc(fm.name())).append("\",")
                        .append("\"instr\":").append(fm.instrCount()).append(',')
                        .append("\"maxDegree\":").append(fm.maxDegree()).append('}');
            }
            sb.append("]}");
        }

        sb.append("]}");
        json(resp, 200, sb.toString());
    }

    private static int computeMaxDegree(Program p) {
        final int CAP = 1000;
        var expander = new ExpanderImpl();
        int d = 0;
        Program cur = p;
        while (d < CAP && containsSynthetic(cur)) {
            cur = expander.expandToDegree(cur, 1);
            d++;
        }
        return d;
    }

    private static boolean containsSynthetic(Program p) {
        for (Instruction ins : p.instructions()) {
            if (!ins.isBasic()) return true;
        }
        return false;
    }

    private static String asJsonArrayOfStrings(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String s : items) {
            if (!first) sb.append(',');
            first = false;
            sb.append("\"").append(esc(s)).append("\"");
        }
        sb.append(']');
        return sb.toString();
    }

    private static String usernameOf(Object u) {
        if (u == null) return "unknown";
        try {
            var m = u.getClass().getMethod("username");
            Object v = m.invoke(u);
            return v == null ? "unknown" : String.valueOf(v);
        } catch (Exception ignore) { }
        try {
            var m = u.getClass().getMethod("name");
            Object v = m.invoke(u);
            return v == null ? "unknown" : String.valueOf(v);
        } catch (Exception ignore) { }
        return "unknown";
    }
}

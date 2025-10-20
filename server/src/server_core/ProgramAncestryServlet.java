// New servlet to compute ancestry on demand
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import system.core.exec.FunctionEnv;
import system.core.expand.ExpanderImpl;
import system.core.io.ProgramMapper;
import system.core.model.Program;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ProgramAncestryServlet", urlPatterns = {"/api/programs/*/ancestry"}, loadOnStartup = 1)
public class ProgramAncestryServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Expected pathInfo: /<programName>/ancestry
        String path = req.getPathInfo(); // e.g. "/Divide/ancestry"
        if (path == null || !path.endsWith("/ancestry")) {
            writeJson(resp, 404, "{\"error\":\"invalid_path\",\"path\":\"" + esc(String.valueOf(path)) + "\"}");
            return;
        }

        // Split: ["", "<program>", "ancestry"]
        String[] parts = path.split("/");
        if (parts.length < 3 || parts[1].isBlank()) {
            writeJson(resp, 400, "{\"error\":\"missing_program\"}");
            return;
        }
        String programName = parts[1];

        // 1-based index coming from the UI
        int index1 = parseInt(req.getParameter("index"), -1);
        if (index1 < 1) {
            writeJson(resp, 400, "{\"error\":\"invalid_index\"}");
            return;
        }

        // Find program/function + function map
        final Program base;
        final Map<String, Program> fnMap;
        final int programMaxDegree;

        ProgramMeta pm = PROGRAMS.get(programName);
        if (pm != null) {
            base = pm.mainProgram;
            fnMap = pm.engine.getFunctions();
            programMaxDegree = pm.maxDegree;
        } else {
            Program body = FUNCTION_BODIES.get(programName);
            FunctionMeta fm = FUNCTIONS.get(programName);
            if (body == null || fm == null) {
                writeJson(resp, 404, "{\"error\":\"program_not_found\"}");
                return;
            }
            base = body;
            fnMap = FUNCTION_BODIES;
            programMaxDegree = fm.maxDegree();
        }

        // Clamp requested degree to this program's max
        int reqDegree = parseInt(req.getParameter("degree"), 0);
        int useDegree = Math.max(0, Math.min(reqDegree, programMaxDegree));

        try {
            String chain = FunctionEnv.with(new FunctionEnv(fnMap), () -> {
                if (useDegree == 0) {
                    // at degree 0 origin = the line itself; we construct a minimal chain
                    // format: "#<idx> (B|S) [<label>] <op> (cycles)"
                    var view = ProgramMapper.toView(base);
                    var cmds = view.commands();
                    int zeroBased = index1 - 1;
                    if (zeroBased < 0 || zeroBased >= cmds.size()) return "";
                    var cv = cmds.get(zeroBased);
                    String label = cv.labelOrEmpty() == null ? "" : cv.labelOrEmpty();
                    String labelPart = label.isEmpty() ? "" : ("[" + label + "] ");
                    String bs = base.instructions().get(zeroBased).isBasic() ? "B" : "S";
                    String op = base.instructions().get(zeroBased).asText();
                    int cyc = Math.max(0, cv.cycles());
                    return "#"+index1+" ("+bs+") "+labelPart+op+" ("+cyc+")";
                } else {
                    // Expand ONCE with origins and return just this line's chain
                    var res = new ExpanderImpl().expandToDegreeWithOrigins(base, useDegree);
                    List<String> origins = res.origins();
                    int zeroBased = index1 - 1;
                    if (zeroBased < 0 || zeroBased >= origins.size()) return "";
                    return origins.get(zeroBased);
                }
            });

            // Explicit JSON write (avoid any wrapper oddities)
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"ok\":true,\"originChain\":\"" + esc(chain) + "\"}");
            resp.getWriter().flush();
        } catch (Exception e) {
            writeJson(resp, 500, "{\"error\":\"ancestry_failed\"}");
        }
    }

    private static int parseInt(String q, int def) {
        if (q == null) return def;
        try { return Integer.parseInt(q); } catch (NumberFormatException e) { return def; }
    }

    private static void writeJson(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(body);
        resp.getWriter().flush();
    }
}

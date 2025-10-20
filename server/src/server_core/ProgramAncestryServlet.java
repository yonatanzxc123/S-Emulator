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

@WebServlet(
        name = "ProgramAncestryServlet",
        urlPatterns = {"/api/programs/ancestry/*"},   // <— VALID prefix mapping
        loadOnStartup = 1
)
public class ProgramAncestryServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // pathInfo looks like: "/Divide"   (because mapping is /api/programs/ancestry/*)
        String path = req.getPathInfo();
        if (path == null || path.length() <= 1) {
            writeJson(resp, 400, "{\"error\":\"missing_program\"}");
            return;
        }
        String programName = path.substring(1); // strip leading '/'

        int index1 = parseInt(req.getParameter("index"), -1);
        if (index1 < 1) {
            writeJson(resp, 400, "{\"error\":\"invalid_index\"}");
            return;
        }

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

        int reqDegree = parseInt(req.getParameter("degree"), 0);
        int useDegree = Math.max(0, Math.min(reqDegree, programMaxDegree));

        try {
            String chain = FunctionEnv.with(new FunctionEnv(fnMap), () -> {
                if (useDegree == 0) {
                    var view = ProgramMapper.toView(base);
                    int z = index1 - 1;
                    if (z < 0 || z >= view.commands().size()) return "";
                    var cv = view.commands().get(z);
                    var ins = base.instructions().get(z);
                    String label = cv.labelOrEmpty() == null ? "" : cv.labelOrEmpty();
                    String labelPart = label.isEmpty() ? "" : ("[" + label + "] ");
                    String bs = ins.isBasic() ? "B" : "S";
                    int cyc = Math.max(0, cv.cycles());
                    return "#" + index1 + " (" + bs + ") " + labelPart + ins.asText() + " (" + cyc + ")";
                } else {
                    var res = new ExpanderImpl().expandToDegreeWithOrigins(base, useDegree);
                    int z = index1 - 1;
                    if (z < 0 || z >= res.origins().size()) return "";
                    return res.origins().get(z);
                }
            });

            writeJson(resp, 200, "{\"ok\":true,\"originChain\":\"" + esc(chain) + "\"}");
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

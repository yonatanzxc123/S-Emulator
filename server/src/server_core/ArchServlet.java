package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import system.api.view.ArchSummary;

@WebServlet(name = "ArchServlet", urlPatterns = {"/api/arch/summary"}, loadOnStartup = 1)
public class ArchServlet extends BaseApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String programName = req.getParameter("program");
        if (programName == null || programName.isBlank()) {
            json(resp, 400, "{\"error\":\"bad_params\"}");
            return;
        }
        final ProgramMeta meta = PROGRAMS.get(programName);
        if (meta == null) {
            json(resp, 404, "{\"error\":\"program_not_found\"}");
            return;
        }

        int requested = 0;
        String dParam = req.getParameter("degree");
        if (dParam != null && !dParam.isBlank()) {
            try { requested = Integer.parseInt(dParam.trim()); } catch (NumberFormatException ignore) {}
        }
        final int clamped = Math.max(0, Math.min(requested, meta.engine.getMaxDegree()));

        final ArchSummary s = meta.engine.getArchSummary(clamped);

        final String out =
                "{"
                        + "\"total\":" + s.total() + ","
                        + "\"byArch\":{"
                        +   "\"I\":"   + s.I()   + ","
                        +   "\"II\":"  + s.II()  + ","
                        +   "\"III\":" + s.III() + ","
                        +   "\"IV\":"  + s.IV()
                        + "}"
                        + "}";

        json(resp, 200, out);
    }
}

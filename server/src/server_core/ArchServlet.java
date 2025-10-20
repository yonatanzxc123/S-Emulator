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

        // Determine the degree (depth of expansion) to summarize, clamped to the program's max degree
        int requestedDegree = 0;
        String degreeParam = req.getParameter("degree");
        if (degreeParam != null && !degreeParam.isBlank()) {
            try {
                requestedDegree = Integer.parseInt(degreeParam.trim());
            } catch (NumberFormatException ignore) {
                // If parsing fails, default to 0 (no expansion)
            }
        }
        final int degree = Math.max(0, Math.min(requestedDegree, meta.engine.getMaxDegree()));

        // Get architecture summary for the given program at the requested degree
        final ArchSummary summary = meta.engine.getArchSummary(degree);
        final String responseJson =
                "{"
                        + "\"total\":" + summary.total() + ","
                        + "\"byArch\":{"
                        +   "\"I\":"   + summary.I()   + ","
                        +   "\"II\":"  + summary.II()  + ","
                        +   "\"III\":" + summary.III() + ","
                        +   "\"IV\":"  + summary.IV()
                        + "}"
                        + "}";

        json(resp, 200, responseJson);
    }
}


package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "FunctionsServlet", urlPatterns = {"/api/functions/*"}, loadOnStartup = 1)
public class FunctionsServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Optional filters (keep it simple and optional)
        String ownerFilter = req.getParameter("owner");
        String programFilter = req.getParameter("program");

        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"functions\":[");
        boolean first = true;

        for (FunctionMeta fm : FUNCTIONS.values()) {
            if (ownerFilter != null && !ownerFilter.isBlank() && !ownerFilter.equals(fm.ownerUser())) continue;
            if (programFilter != null && !programFilter.isBlank() && !programFilter.equals(fm.definedInProgram())) continue;

            if (!first) sb.append(',');
            first = false;

            sb.append('{')
                    .append("\"name\":\"").append(esc(fm.name())).append("\",")
                    .append("\"program\":\"").append(esc(fm.definedInProgram())).append("\",")
                    .append("\"owner\":\"").append(esc(fm.ownerUser())).append("\",")
                    .append("\"instr\":").append(fm.instrCount()).append(',')
                    .append("\"maxDegree\":").append(fm.maxDegree())
                    .append('}');
        }

        sb.append("]}");
        json(resp, 200, sb.toString());
    }
}

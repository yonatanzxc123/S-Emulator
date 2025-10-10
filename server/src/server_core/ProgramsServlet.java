// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        String name = jStr(body, "name");
        String xml  = jStr(body, "xml");
        if (name == null || name.isBlank() || xml == null || xml.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_name_or_xml\"}");
            return;
        }

        u.mainUploaded.incrementAndGet();
        VERSION.incrementAndGet();
        json(resp, 200, "{\"ok\":true,\"addedProgram\":{\"name\":\"" + esc(name.trim()) + "\",\"owner\":\"" + esc(u.name) + "\"}}");
    }
}

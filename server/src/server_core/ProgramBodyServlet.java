// java
package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import system.core.model.Instruction;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ProgramBodyServlet", urlPatterns = {"/api/programs/*/body"}, loadOnStartup = 1)
public class ProgramBodyServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Extract program name from path like /api/programs/NAME/body
        String pathInfo = req.getPathInfo();
        String servletPath = req.getServletPath();
        String path = (pathInfo != null) ? servletPath + pathInfo : servletPath;

        String[] parts = path.split("/");
        String name = null;
        for (int i = 0; i < parts.length - 1; i++) {
            if ("programs".equals(parts[i]) && i + 1 < parts.length && !"body".equals(parts[i + 1])) {
                name = parts[i + 1];
                break;
            }
        }

        if (name == null || name.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_program_name\"}");
            return;
        }

        ProgramMeta meta = PROGRAMS.get(name);
        if (meta == null || meta.mainProgram == null) {
            json(resp, 404, "{\"error\":\"program_not_found\"}");
            return;
        }

        List<Instruction> list = meta.mainProgram.instructions();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"instructions\":[");
        boolean first = true;
        for (int i = 0; i < list.size(); i++) {
            Instruction ins = list.get(i);

            String op = EngineUtil.opText(ins);
            String level = EngineUtil.levelOf(ins);
            String bs = ins.isBasic() ? "B" : "S";
            String label = tryLabel(ins);
            int cycles = cyclesOf(level);

            if (!first) sb.append(',');
            first = false;
            sb.append('{')
                    .append("\"index\":").append(i + 1).append(',')
                    .append("\"op\":\"").append(esc(op)).append("\",")
                    .append("\"level\":\"").append(level).append("\",")
                    .append("\"bs\":\"").append(bs).append("\",")
                    .append("\"label\":\"").append(esc(label)).append("\",")
                    .append("\"cycles\":").append(cycles)
                    .append('}');
        }
        sb.append("]}");
        json(resp, 200, sb.toString());
    }

    private static int cyclesOf(String level) {
        return switch (level) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            default -> 1;
        };
    }

    private static String tryLabel(Object ins) {
        for (String m : new String[] { "label", "getLabel", "name", "getName" }) {
            try {
                java.lang.reflect.Method mm = ins.getClass().getMethod(m);
                Object v = mm.invoke(ins);
                if (v != null) return String.valueOf(v);
            } catch (Exception ignore) { }
        }
        return "";
    }
}

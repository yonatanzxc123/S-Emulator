package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import system.core.model.Instruction;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ProgramDetailsServlet", urlPatterns = {"/api/programDetails"})
public class ProgramDetailsServlet extends BaseApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String program = req.getParameter("program");
        if (program == null || program.isBlank()) {
            json(resp, 400, "{\"error\":\"missing_program\"}");
            return;
        }

        ProgramMeta meta = ProgramRegistry.get(program);
        if (meta == null || meta.mainProgram == null) {
            json(resp, 404, "{\"error\":\"program_not_found\"}");
            return;
        }

        List<Instruction> list = meta.mainProgram.instructions();
        StringBuilder out = new StringBuilder("{\"instructions\":[");
        int i = 1;
        for (Instruction ins : list) {
            if (i > 1) out.append(",");
            out.append("{")
                    .append("\"line\":").append(i).append(",")
                    .append("\"label\":\"").append(esc(ins.label())).append("\",")
                    .append("\"instruction\":\"").append(esc(ins.asText())).append("\",")
                    .append("\"cycles\":").append(ins.cycles()).append(",")
                    .append("\"bs\":\"").append(ins.isBasic() ? "B" : "S").append("\",")
                    .append("\"arch\":\"").append(archLevel(ins)).append("\"")
                    .append("}");
            i++;
        }
        out.append("]}");
        json(resp, 200, out.toString());
    }

    private static String archLevel(Instruction ins) {
        String name = ins.getClass().getSimpleName();
        return switch (name) {
            case "Inc", "Dec", "Nop", "IfGoto" -> "I";
            case "ZeroVariable", "ConstantAssignment", "GotoLabel" -> "II";
            case "Assignment", "JumpZero", "JumpEqualConstant", "JumpEqualVariable" -> "III";
            case "Quote", "JumpEqualFunction" -> "IV";
            default -> "?";
        };
    }
}

package server_core;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import system.core.model.Instruction;
import system.core.model.Program;

// basic ops
import system.core.model.basic.*;

// synthetic level II
import system.core.model.synthetic.ZeroVariable;
import system.core.model.synthetic.ConstantAssignment;
import system.core.model.synthetic.GotoLabel;

// synthetic level III
import system.core.model.synthetic.Assignment;
import system.core.model.synthetic.JumpZero;
import system.core.model.synthetic.JumpEqualConstant;
import system.core.model.synthetic.JumpEqualVariable;

// synthetic level IV
import system.core.model.synthetic.advanced.Quote;
import system.core.model.synthetic.advanced.JumpEqualFunction;

@WebServlet(name = "ArchServlet", urlPatterns = {"/api/arch/summary"}, loadOnStartup = 1)
public class ArchServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String program = req.getParameter("program");
        if (program == null || program.isBlank()) { json(resp, 400, "{\"error\":\"bad_params\"}"); return; }
        ProgramMeta meta = PROGRAMS.get(program);
        if (meta == null) { json(resp, 404, "{\"error\":\"program_not_found\"}"); return; }

        int i = 0, ii = 0, iii = 0, iv = 0;
        for (Instruction ins : meta.mainProgram.instructions()) {
            if (ins instanceof Inc || ins instanceof Dec || ins instanceof Nop || ins instanceof IfGoto) { i++; continue; }
            if (ins instanceof ZeroVariable || ins instanceof ConstantAssignment || ins instanceof GotoLabel) { ii++; continue; }
            if (ins instanceof Assignment || ins instanceof JumpZero || ins instanceof JumpEqualConstant || ins instanceof JumpEqualVariable) { iii++; continue; }
            if (ins instanceof Quote || ins instanceof JumpEqualFunction) { iv++; continue; }
        }

        json(resp, 200, "{"
                + "\"program\":\"" + esc(program) + "\","
                + "\"I\":" + i + ","
                + "\"II\":" + ii + ","
                + "\"III\":" + iii + ","
                + "\"IV\":" + iv
                + "}");
    }
}

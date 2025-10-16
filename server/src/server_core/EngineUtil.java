// java
package server_core;

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

final class EngineUtil {

    private EngineUtil() {}

    static String levelOf(Instruction ins) {
        if (ins instanceof Inc || ins instanceof Dec || ins instanceof Nop || ins instanceof IfGoto) return "I";
        if (ins instanceof ZeroVariable || ins instanceof ConstantAssignment || ins instanceof GotoLabel) return "II";
        if (ins instanceof Assignment || ins instanceof JumpZero || ins instanceof JumpEqualConstant || ins instanceof JumpEqualVariable) return "III";
        if (ins instanceof Quote || ins instanceof JumpEqualFunction) return "IV";
        return ins.isBasic() ? "I" : "III";
    }

    static String opText(Instruction ins) {
        String s = String.valueOf(ins);
        return s == null ? "" : s;
    }

    static String instructionsJson(Program p) {
        StringBuilder sb = new StringBuilder(256 + p.instructions().size() * 48);
        sb.append("{\"instructions\":[");
        boolean first = true;
        int idx = 1;
        for (Instruction ins : p.instructions()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('{')
                    .append("\"index\":").append(idx++).append(',')
                    .append("\"op\":\"").append(BaseApiServlet.esc(opText(ins))).append("\",")
                    .append("\"level\":\"").append(levelOf(ins)).append("\"")
                    .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    static String archSummaryJson(Program p) {
        int i = 0, ii = 0, iii = 0, iv = 0;
        for (Instruction ins : p.instructions()) {
            switch (levelOf(ins)) {
                case "I" -> i++;
                case "II" -> ii++;
                case "III" -> iii++;
                case "IV" -> iv++;
                default -> { if (ins.isBasic()) i++; else iii++; }
            }
        }
        return "{\"I\":" + i + ",\"II\":" + ii + ",\"III\":" + iii + ",\"IV\":" + iv + "}";
    }
}

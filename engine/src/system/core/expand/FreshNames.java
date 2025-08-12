package system.core.expand;

import system.core.model.Instruction;
import system.core.model.Program;
import system.core.model.Var;

import java.util.HashSet;
import java.util.Set;

public final class FreshNames {
    private int nextZ = 1;
    private int nextL = 1;
    private final Set<String> labelsUsed = new HashSet<>();

    private FreshNames() {}

    public static FreshNames from(Program p) {
        FreshNames f = new FreshNames();
        for (var ins : p.instructions) {
            var lab = ins.label();
            if (lab != null && !lab.isEmpty() && !"EXIT".equals(lab)) f.labelsUsed.add(lab);
            // scan z indices seen in text quickly (simple heuristic)
            // proper scan can happen later when we parse XML → model with real Var refs
        }
        return f;
    }

    public String freshLabel() {
        while (true) {
            String l = "L" + nextL++;
            if (!labelsUsed.contains(l)) { labelsUsed.add(l); return l; }
        }
    }

    public Var freshZ() {
        return Var.z(nextZ++); // we’ll improve by scanning actual used z’s later
    }
}

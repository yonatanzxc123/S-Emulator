package system.core.expand.helpers;

import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.Var;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FreshNames {
    private static final String AUTO_LABEL_PREFIX = "L$";

    private int nextLabelId = 1;
    private int nextZ;
    private final Set<String> usedLabels = new HashSet<>();

    public FreshNames(Program base) {
        int maxZ = 0;

        if (base != null && base.instructions != null) {
            for (Instruction ins : base.instructions) {
                // collect existing labels
                String lab = ins.label();
                if (lab != null && !lab.isEmpty()) {
                    usedLabels.add(lab);
                }

                // collect used z-k indices
                List<Var> vars = ins.variablesUsed(); // Instruction should expose this
                if (vars != null) {
                    for (Var v : vars) {
                        if (v.isZ()) {
                            if (v.index() > maxZ) maxZ = v.index();
                        }
                    }
                }
            }
        }

        // start temp z's after the highest used one (at least z1)
        this.nextZ = Math.max(1, maxZ + 1);

        // if user(the one who give me the xml) already used some L$N labels, skip ahead to a free id
        while (usedLabels.contains(AUTO_LABEL_PREFIX + nextLabelId)) {
            nextLabelId++;
        }
    }

    /** Returns a fresh label like L$1, L$2… guaranteed not to clash with existing labels. so we know we are good */
    public String nextLabel() {
        String lab;
        do {
            lab = AUTO_LABEL_PREFIX + nextLabelId++;
        } while (usedLabels.contains(lab));
        usedLabels.add(lab);
        return lab;
    }

    /** Returns a fresh temporary z variable: z{n}, starting at maxZ+1. */
    public Var tempZ() {
        return Var.z(nextZ++);
    }

    /** Optional: mark an externally created label as used. (propbably will need it later) */
    public void markLabelUsed(String label) {
        if (label != null && !label.isEmpty()) {
            usedLabels.add(label);
        }
    }
}

package system.core.exec;

import system.core.model.Instruction;
import system.core.model.Program;

import java.util.HashMap;
import java.util.Map;

public final class LabelIndex implements JumpResolver {
    private final Map<String, Integer> first = new HashMap<>();

    private LabelIndex() {}

    public static LabelIndex build(Program p) {
        LabelIndex idx = new LabelIndex();
        for (int i = 0; i < p.instructions().size(); i++) {
            Instruction ins = p.instructions().get(i);
            String lab = ins.label();
            if (lab == null || lab.isEmpty() || "EXIT".equals(lab)) continue;
            // first occurrence wins
            idx.first.putIfAbsent(lab, i);
        }
        return idx;
    }

    @Override
    public int resolve(String label) {
        if (label == null || label.isEmpty()) return -2; // invalid, won't be used
        if ("EXIT".equals(label)) return -1;
        return first.getOrDefault(label, -2); // -2 means not found (invalid program)
    }
}

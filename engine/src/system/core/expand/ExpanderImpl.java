package system.core.expand;

import system.core.expand.helpers.FreshNames;
import system.core.model.Instruction;
import system.core.model.Program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExpanderImpl implements Expander {

    @Override
    public Program expandToDegree(Program program, int degree) {
        if (program == null || degree <= 0) return program;
        Program cur = program;
        for (int d = 0; d < degree; d++) {
            if (!containsSynthetic(cur)) break;
            cur = expandOne(cur);
        }
        return cur;
    }

    /** New: expand and also return per-line origin chain strings. */
    public ExpandedProgramResult expandToDegreeWithOrigins(Program program, int degree) {
        if (program == null) return new ExpandedProgramResult(null, List.of());
        List<String> origins = new ArrayList<>(Collections.nCopies(program.instructions().size(), ""));
        Program cur = program;

        for (int d = 0; d < degree; d++) {
            if (!containsSynthetic(cur)) break;
            var step = expandOneWithOrigins(cur, origins);
            cur = step.program();
            origins = step.origins();
        }
        return new ExpandedProgramResult(cur, origins);
    }

    /** One-round expansion without origins (existing behavior). */
    private Program expandOne(Program p) {
        FreshNames fresh = new FreshNames(p);
        Program out = new Program(p.name() + " [expanded]");
        for (Instruction ins : p.instructions()) {
            ins.expandTo(out, fresh); // synthetic override; basic copies itself
        }
        return out;
    }

    /** One-round expansion that also computes origin chains. */
    private ExpandedProgramResult expandOneWithOrigins(Program cur, List<String> prevOrigins) {
        FreshNames fresh = new FreshNames(cur);
        Program out = new Program(cur.name() + " [expanded]");
        List<String> outOrigins = new ArrayList<>();

        // pre-compute the display of each current instruction with its index (#i ...)
        List<String> prevDisplays = new ArrayList<>(cur.instructions().size());
        int idx = 1;
        for (Instruction ins : cur.instructions()) {
            prevDisplays.add(displayOf(ins, idx++));
        }

        for (int i = 0; i < cur.instructions().size(); i++) {
            Instruction ins = cur.instructions().get(i);
            String parentChain = (prevOrigins != null && i < prevOrigins.size()) ? prevOrigins.get(i) : "";
            String link = prevDisplays.get(i);

            if (ins.isBasic()) {
                int before = out.instructions().size();
                out.add(ins); // copy as-is
                int after = out.instructions().size();
                String carry = parentChain; // basic keeps its previous chain
                for (int j = before; j < after; j++) outOrigins.add(carry);
            } else {
                int before = out.instructions().size();
                ins.expandTo(out, fresh);
                int after = out.instructions().size();
                String combined = (parentChain == null || parentChain.isBlank()) ? link
                        : parentChain + "  <<<   " + link;
                for (int j = before; j < after; j++) outOrigins.add(combined);
            }
        }
        return new ExpandedProgramResult(out, outOrigins);
    }

    /** True if the program still contains any synthetic instruction. */
    public boolean containsSynthetic(Program p) {
        for (Instruction ins : p.instructions()) {
            if (!ins.isBasic()) return true;
        }
        return false;
    }

    /** Same formatting as your CLI uses for “Show Program”. */
    private static String displayOf(Instruction ins, int number) {
        String lab = ins.label() == null ? "" : ins.label();
        return String.format("#%d (%s) [%-5s] %s (%d)",
                number,
                ins.isBasic() ? "B" : "S",
                lab,
                ins.asText(),
                ins.cycles());
    }
}

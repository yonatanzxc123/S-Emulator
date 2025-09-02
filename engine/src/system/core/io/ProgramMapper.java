package system.core.io;

import system.api.view.CommandView;
import system.api.view.ProgramView;
import system.core.model.Instruction;
import system.core.model.Program;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ProgramMapper {
    private ProgramMapper() {}

    public static ProgramView toView(Program p) {
        return toView(p, null);
    }

    /** New: same as toView, but fills the “source” (origin chain) per line if provided. */
    public static ProgramView toView(Program p, List<String> origins) {
        List<CommandView> lines = new ArrayList<>();
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        LinkedHashSet<String> inputs = new LinkedHashSet<>();

        int idx = 1;
        for (Instruction ins : p.instructions()) {
            if (!ins.label().isEmpty() && !"EXIT".equals(ins.label())) labels.add(ins.label());

            // (quick & dirty input detection; I can later upgrade to use variablesUsed()) if I want
            String t = ins.asText();
            for (int i = 1; i <= 20; i++) {
                if (t.contains("x" + i)) inputs.add("x" + i);
            }

            String origin = (origins != null && origins.size() >= idx) ? origins.get(idx - 1) : "";
            lines.add(new CommandView(idx++, ins.isBasic(), ins.label(), ins.asText(), ins.cycles(), origin));
        }
        if (p.instructions().stream().anyMatch(i -> "EXIT".equals(i.label()))) {
            labels.add("EXIT");
        }

        return new ProgramView(p.name(), new ArrayList<>(inputs), new ArrayList<>(labels), lines);
    }
}

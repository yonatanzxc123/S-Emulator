package system.core.io;


import system.api.view.*;
import system.core.model.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ProgramMapper {
    private ProgramMapper() {}

  public static ProgramView toView(Program p) {
        List<CommandView> lines = new ArrayList<>();
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        LinkedHashSet<String> inputs = new LinkedHashSet<>();

        int idx = 1;
        for (Instruction ins : p.instructions) {
            if (!ins.label().isEmpty() && !"EXIT".equals(ins.label())) labels.add(ins.label());
            // naive way to extract x-inputs present in text
            String t = ins.asText();
            for (int i = 1; i <= 20; i++) { // simple scan; replace later with proper parser
                if (t.contains("x"+i)) inputs.add("x"+i);
            }
            lines.add(new CommandView(idx++, ins.isBasic(), ins.label(), ins.asText(), ins.cycles(), ""));
        }
        // move EXIT to the end if appears anywhere (just for display policy)
        if (p.instructions.stream().anyMatch(i -> "EXIT".equals(i.label()))) {
            labels.add("EXIT");
        }

        return new ProgramView(p.name, new ArrayList<>(inputs), new ArrayList<>(labels), lines);
    }
}

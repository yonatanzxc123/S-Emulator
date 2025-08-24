package system.core.model.synthetic;

import system.core.expand.helpers.FreshNames;
import system.core.model.Program;
import system.core.model.Instruction;
import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.basic.IfGoto;
import system.core.model.basic.Inc;
import system.core.io.LoaderUtil;
import java.util.Map;
import java.util.List;

public final class GotoLabel extends SyntheticInstruction {
    private final String target;
    public GotoLabel(String label, String target) { super(label); this.target = target; }
    public String target() { return target; }

    @Override public int cycles() { return 1; }
    @Override public String asText() { return "GOTO " + target; }

    @Override public List<String> labelTargets() {
        return ("EXIT".equals(target) || target.isBlank()) ? List.of() : List.of(target);
    }
    @Override
    public void expandTo(Program out, FreshNames fresh) {
        Var tmp = fresh.tempZ();
        out.add(new Inc(label(), tmp, 1));          // tmp <- 1
        out.add(new IfGoto("", tmp, target, 2));    // if tmp!=0 goto target
    }

    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        String target = LoaderUtil.need(args.get("gotoLabel"), "gotoLabel", -1, errs);
        return (target == null) ? null : new GotoLabel(label, target);
    }


}

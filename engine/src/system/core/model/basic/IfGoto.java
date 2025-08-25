package system.core.model.basic;

import system.core.model.Instruction;
import system.core.model.BasicInstruction;
import system.core.model.Var;
import system.core.io.LoaderUtil;
import java.util.Map;


import java.util.List;

public final class IfGoto extends BasicInstruction {
    private final Var v;
    private final String target;

    public IfGoto(String label, Var v, String target, int cycles) {
        super(label, cycles);                  // cycles should be 2 for JNZ
        this.v = v;
        this.target = target == null ? "" : target;
    }

    public Var v()          { return v; }
    public String target()  { return target; }

    @Override
    public String asText() { return "IF " + v + " != 0 GOTO " + target; }

    @Override
    public List<String> labelTargets() {
        // EXIT is allowed and blank means “no target”
        if (target == null || target.isBlank() || "EXIT".equals(target)) return List.of();
        return List.of(target);
    }


    @Override
    public List<Var> variablesUsed() { return List.of(v); }

    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        var v = LoaderUtil.parseVar(varToken, errs, -1);
        String target = LoaderUtil.need(args.get("JNZLabel"), "JNZLabel", -1, errs);
        return (v == null || target == null) ? null : new IfGoto(label, v, target, 2);
    }

}

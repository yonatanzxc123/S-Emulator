package system.core.model.synthetic;

import system.core.model.*;
import system.core.model.basic.*;
import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;
import java.util.Map;
import java.util.List;
import java.util.function.UnaryOperator;


public final class JumpZero extends SyntheticInstruction implements Remappable {
    private final Var v;
    private final String target;




    public JumpZero(String label, Var v, String target) { super(label); this.v = v; this.target = target; }
    public Var v() { return v; }
    public String target() { return target; }


    @Override public int cycles() { return 2; }
    @Override public String asText() { return "IF " + v + " = 0 GOTO " + target; }
    @Override public List<Var> variablesUsed() { return java.util.List.of(v); }

    @Override public List<String> labelTargets() {
        return ("EXIT".equals(target) || target.isBlank()) ? List.of() : List.of(target);
    }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        String L1 = fresh.nextLabel();                  // L1 in the slide
        out.add(new IfGoto(label(), v, L1, 2));         // IF v != 0 GOTO SKIP
        out.add(new GotoLabel("", target));               // GOTO L
        out.add(new Nop(L1, v, 0));                     // L1: y <- y  (a no-op)
    }



    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        var v = LoaderUtil.parseVar(varToken, errs, -1);
        String target = LoaderUtil.need(args.get("JZLabel"), "JZLabel", -1, errs);
        return (v == null || target == null) ? null : new JumpZero(label, v, target);
    }


    @Override
    public Instruction remap(UnaryOperator<Var> vm, UnaryOperator<String> lm) {
        return new JumpZero(lm.apply(label()), vm.apply(v), lm.apply(target));
    }




}

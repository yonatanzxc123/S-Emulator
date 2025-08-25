package system.core.model.synthetic;

import system.core.model.Instruction;
import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.Program;
import system.core.model.basic.*;
import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;

import java.util.Map;
import java.util.List;

public final class JumpEqualVariable extends SyntheticInstruction {
    private final Var a;
    private final Var b;
    private final String target;

    public JumpEqualVariable(String label, Var a, Var b, String target) {
        super(label);
        this.a = a;
        this.b = b;
        this.target = (target == null ? "" : target);
    }

    public Var a() { return a; }
    public Var b() { return b; }
    public String target() { return target; }

    @Override public int cycles() { return 2; }  // per spec
    @Override public String asText() { return "IF " + a + " = " + b + " GOTO " + target; }
    @Override public List<Var> variablesUsed() { return List.of(a, b); }

    @Override
    public List<String> labelTargets() {
        return ("EXIT".equals(target) || target.isBlank()) ? List.of() : List.of(target);
    }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        // Working copies
        Var z1 = fresh.tempZ();
        Var z2 = fresh.tempZ();

        // Labels
        String L2 = fresh.nextLabel();   // loop head
        String L3 = fresh.nextLabel();   // reached z1==0; now check z2
        String L1 = fresh.nextLabel();   // not-equal sink

        // z1 <- a ; z2 <- b  (synthetic ASSIGNMENT so degree>1 is meaningful)
        out.add(new Assignment(label(), z1, a));
        out.add(new Assignment("",      z2, b));

        // L2:
        // IF z1 = 0 GOTO L3   (emit explicit join line)
        String CONT1 = fresh.nextLabel();
        out.add(new IfGoto(L2, z1, CONT1, 2));  // z1 != 0 -> CONT1
        out.add(new GotoLabel("", L3));         // z1 == 0 -> L3
        out.add(new Nop(CONT1, z1, 0));         // materialize the join

        // IF z2 = 0 GOTO L1   (emit explicit join line)
        String CONT2 = fresh.nextLabel();
        out.add(new IfGoto("", z2, CONT2, 2));  // z2 != 0 -> CONT2
        out.add(new GotoLabel("", L1));         // z2 == 0 -> not equal
        out.add(new Nop(CONT2, z2, 0));         // materialize the join

        // z1-- ; z2-- ; GOTO L2
        out.add(new Dec("", z1, 1));
        out.add(new Dec("", z2, 1));
        out.add(new GotoLabel("", L2));

        // L3: equal iff z2 == 0
        out.add(new IfGoto(L3, z2, L1, 2));     // z2 != 0 -> not equal
        out.add(new GotoLabel("", target));     // z2 == 0 -> target (equal)

        // L1: y <- y (sink)
        out.add(new Nop(L1, fresh.tempZ(), 0));
    }

    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        var a = LoaderUtil.parseVar(varToken, errs, -1);
        String bTok = LoaderUtil.need(args.get("variableName"), "variableName", -1, errs);
        String target = LoaderUtil.need(args.get("JEVariableLabel"), "JEVariableLabel", -1, errs);
        if (a == null || bTok == null || target == null) return null;
        var b = LoaderUtil.parseVar(bTok, errs, -1);
        return (b == null) ? null : new JumpEqualVariable(label, a, b, target);
    }
}

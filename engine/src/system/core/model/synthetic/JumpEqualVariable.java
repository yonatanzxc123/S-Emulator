package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.Program;
import system.core.model.basic.*;
import system.core.expand.helpers.FreshNames;

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

    @Override public List<String> labelTargets() {
        return ("EXIT".equals(target) || target.isBlank()) ? List.of() : List.of(target);
    }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        // Temps: wa, wb = working copies; ra, rb = restore counters; g = goto helper
        Var wa = fresh.tempZ(), ra = fresh.tempZ();
        Var wb = fresh.tempZ(), rb = fresh.tempZ();
        Var g  = fresh.tempZ();

        String CA = fresh.nextLabel();
        String RA = fresh.nextLabel();
        String CB = fresh.nextLabel();
        String RB = fresh.nextLabel();

        // ---- Copy a -> wa; restore a via ra ----
        out.add(new IfGoto(label(), a, CA, 2));
        out.add(new IfGoto("", ra, RA, 2));
        out.add(new Dec(CA, a, 1));
        out.add(new Inc("", wa, 1));
        out.add(new Inc("", ra, 1));
        out.add(new IfGoto("", a, CA, 2));
        out.add(new Dec(RA, ra, 1));
        out.add(new Inc("", a, 1));
        out.add(new IfGoto("", ra, RA, 2));

        // ---- Copy b -> wb; restore b via rb ----
        out.add(new IfGoto("", b, CB, 2));
        out.add(new IfGoto("", rb, RB, 2));
        out.add(new Dec(CB, b, 1));
        out.add(new Inc("", wb, 1));
        out.add(new Inc("", rb, 1));
        out.add(new IfGoto("", b, CB, 2));
        out.add(new Dec(RB, rb, 1));
        out.add(new Inc("", b, 1));
        out.add(new IfGoto("", rb, RB, 2));

        // ---- Compare wa and wb ----
        String LOOP  = fresh.nextLabel();
        String CONT  = fresh.nextLabel();
        String NOTEQ = fresh.nextLabel();
        String AFTER = fresh.nextLabel();

        // Entry: if wa==0 -> equal iff wb==0
        out.add(new IfGoto("", wa, LOOP, 2));   // if wa!=0 -> LOOP
        out.add(new IfGoto("", wb, NOTEQ, 2));  // wa==0 & wb!=0 -> not equal
        out.add(new Inc("", g, 1));             // both zero -> equal
        out.add(new IfGoto("", g, target, 2));

        // LOOP:
        out.add(new IfGoto(LOOP, wb, CONT, 2)); // if wb!=0 -> CONT else NOTEQ
        out.add(new Inc("", g, 1));
        out.add(new IfGoto("", g, NOTEQ, 2));
        out.add(new Nop(CONT, wb, 0));
        out.add(new Dec("", wa, 1));
        out.add(new Dec("", wb, 1));
        out.add(new IfGoto("", wa, LOOP, 2));   // loop while wa!=0

        // After loop: wa==0; equal iff wb==0
        out.add(new IfGoto(AFTER, wb, NOTEQ, 2)); // if wb!=0 -> not equal
        out.add(new Inc("", g, 1));               // else equal
        out.add(new IfGoto("", g, target, 2));

        // Sinks:
        out.add(new Nop(NOTEQ, wa, 0)); // not equal lands here
    }

}

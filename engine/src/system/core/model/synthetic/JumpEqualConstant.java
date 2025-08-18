package system.core.model.synthetic;

import system.core.expand.helpers.FreshNames;
import system.core.model.Program;
import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.basic.*;

import java.util.List;

public final class JumpEqualConstant extends SyntheticInstruction {
    private final Var v;
    private final long k;
    private final String target;

    public JumpEqualConstant(String label, Var v, long k, String target) {
        super(label);
        this.v = v;
        this.k = k;
        this.target = (target == null ? "" : target);
    }

    public Var v()        { return v; }
    public long k()       { return k; }
    public String target(){ return target; }

    @Override public int cycles() { return 2; }  // per spec
    @Override public String asText() { return "IF " + v + " = " + k + " GOTO " + target; }
    @Override public List<Var> variablesUsed() { return List.of(v); }

    @Override public List<String> labelTargets() {
        return ("EXIT".equals(target) || target.isBlank()) ? List.of() : List.of(target);
    }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        // Temps: w = copy(v), r = restore counter for v, g = goto helper, f = early-zero flag
        Var w = fresh.tempZ();
        Var r = fresh.tempZ();
        Var g = fresh.tempZ();
        Var f = fresh.tempZ();

        String COPY    = fresh.nextLabel();
        String RESTORE = fresh.nextLabel();

        // ---- Copy v -> w (preserve v using r) ----
        out.add(new IfGoto(label(), v, COPY, 2));     // if v!=0 -> COPY
        out.add(new IfGoto("", r, RESTORE, 2));       // else if r!=0 -> RESTORE (first time r==0 so falls through)

        // COPY: while v!=0 { v--; w++; r++; }
        out.add(new Dec(COPY, v, 1));
        out.add(new Inc("", w, 1));
        out.add(new Inc("", r, 1));
        out.add(new IfGoto("", v, COPY, 2));

        // RESTORE: while r!=0 { r--; v++; }
        out.add(new Dec(RESTORE, r, 1));
        out.add(new Inc("", v, 1));
        out.add(new IfGoto("", r, RESTORE, 2));

        // ---- Check w == k ----
        if (k == 0) {
            String SKIP = fresh.nextLabel();
            out.add(new IfGoto("", w, SKIP, 2));   // if w!=0 -> not equal
            out.add(new Inc("", g, 1));            // else equal -> goto target
            out.add(new IfGoto("", g, target, 2));
            out.add(new Nop(SKIP, w, 0));
            return;
        }

        String AFTER = fresh.nextLabel(); // not-equal sink
        // Decrement w exactly k times; if w hits 0 early, it's w<k -> not equal
        for (long i = 0; i < k; i++) {
            String CONT = fresh.nextLabel();
            out.add(new IfGoto("", w, CONT, 2));   // if w!=0 -> CONT
            out.add(new Inc("", f, 1));            // early zero: mark not equal
            out.add(new IfGoto("", f, AFTER, 2));  // goto AFTER
            out.add(new Nop(CONT, w, 0));
            out.add(new Dec("", w, 1));
        }

        // After k decs: equal iff w==0
        String SKIP = fresh.nextLabel();
        out.add(new IfGoto("", w, SKIP, 2));       // if w!=0 -> not equal (w>k)
        out.add(new Inc("", g, 1));                // w==0 => equal
        out.add(new IfGoto("", g, target, 2));
        out.add(new Nop(SKIP, w, 0));
        out.add(new Nop(AFTER, w, 0));             // not-equal path lands here
    }

}

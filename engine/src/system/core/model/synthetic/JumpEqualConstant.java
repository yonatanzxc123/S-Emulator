package system.core.model.synthetic;

import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;
import system.core.model.Instruction;
import system.core.model.Program;
import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.synthetic.Assignment;
import system.core.model.synthetic.GotoLabel;

import system.core.model.basic.*;

import java.util.List;
import java.util.Map;

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

    @Override
    public List<String> labelTargets() {
        return ("EXIT".equals(target) || target.isBlank()) ? List.of() : List.of(target);
    }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        // Working copy so a higher degree can expand ASSIGNMENT further
        Var z1 = fresh.tempZ();
        out.add(new Assignment(label(), z1, v));

        // NOT: sink when z1 != k
        String NOT = fresh.nextLabel();

        // Do this k times:
        // Implement "IF z1 = 0 GOTO NOT" using our primitive "IF z1 != 0 GOTO CONT"
        // and put the CONT label directly on the DEC (no extra NOP line).
        for (long i = 0; i < k; i++) {
            String CONT = fresh.nextLabel();
            out.add(new IfGoto("", z1, CONT, 2));  // z1 != 0 -> CONT
            out.add(new GotoLabel("", NOT));       // z1 == 0 -> NOT
            out.add(new Dec(CONT, z1, 1));         // CONT: z1 <- z1 - 1
        }

        // After k decrements: equal iff z1 == 0
        out.add(new IfGoto("", z1, NOT, 2));       // z1 != 0 -> NOT (z1 > k)
        out.add(new GotoLabel("", target));        // equal -> target
        out.add(new Nop(NOT, z1, 0));              // NOT: y <- y  (sink)
    }

    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        var v = LoaderUtil.parseVar(varToken, errs, -1);
        String kStr   = LoaderUtil.need(args.get("constantValue"), "constantValue", -1, errs);
        String target = LoaderUtil.need(args.get("JEConstantLabel"), "JEConstantLabel", -1, errs);
        if (v == null || kStr == null || target == null) return null;
        long k = LoaderUtil.parseNonNegLong(kStr, "constantValue", -1, errs);
        return new JumpEqualConstant(label, v, k, target);
    }
}

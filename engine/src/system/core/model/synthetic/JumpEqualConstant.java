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
        String NOTEQ = fresh.nextLabel();

        out.add(new Assignment(label(), z1, v));

        // Do this k times:
        // Implement "IF z1 = 0 GOTO NOT" using our primitive "IF z1 != 0 GOTO CONT"
        // and put the CONT label directly on the DEC (no extra NOP line).
        for (long i = 0; i < k; i++) {
            out.add(new JumpZero("",z1,NOTEQ));
            out.add(new Dec("", z1, 1));         // CONT: z1 <- z1 - 1
        }
        out.add(new IfGoto("", z1, NOTEQ,2));
        out.add(new GotoLabel("", target));      // z1 == k, so jump to target
        out.add(new Nop(NOTEQ, v, 0));              // L1:

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

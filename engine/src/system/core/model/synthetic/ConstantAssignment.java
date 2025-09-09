package system.core.model.synthetic;


import system.core.expand.helpers.FreshNames;
import system.core.model.basic.*;
import system.core.model.*;
import system.core.io.LoaderUtil;
import java.util.Map;
import java.util.List;
import java.util.function.UnaryOperator;

public final class ConstantAssignment extends SyntheticInstruction implements Remappable {
    private final Var v;
    private final long k;

    public ConstantAssignment(String label, Var v, long k) {
        super(label);
        this.v = v;
        this.k = k;
    }

    public Var v() { return v; }
    public long k() { return k; }

    @Override public int cycles() { return 2; }
    @Override public String asText() { return v + " <- " + k; }
    @Override public List<Var> variablesUsed() { return java.util.List.of(v); }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        out.add(new ZeroVariable(label(), v));
        for (long i = 0; i < k; i++) {
            out.add(new Inc("", v, 1));
        }
    }

    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        var v = LoaderUtil.parseVar(varToken, errs, -1);
        String kStr = LoaderUtil.need(args.get("constantValue"), "constantValue", -1, errs);
        if (v == null || kStr == null) return null;
        long k = LoaderUtil.parseNonNegLong(kStr, "constantValue", -1, errs);
        return new ConstantAssignment(label, v, k);
    }
    @Override
    public Instruction remap(UnaryOperator<Var> vm, UnaryOperator<String> lm) {
        return new ConstantAssignment(lm.apply(label()), vm.apply(v), k);
    }


}

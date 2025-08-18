package system.core.model.synthetic;

import system.core.expand.helpers.FreshNames;
import system.core.model.Program;
import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.basic.Inc;

import java.util.List;

public final class ConstantAssignment extends SyntheticInstruction {
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
        new ZeroVariable(label(), v).expandTo(out, fresh);
        for (long i = 0; i < k; i++) {
            out.add(new Inc("", v, 1));
        }
    }
}

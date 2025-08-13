package system.core.model.basic;

import system.core.model.BasicInstruction;
import system.core.model.Var;

import java.util.List;

public final class Nop extends BasicInstruction {
    private final Var v;

    public Nop(String label, Var v, int cycles) {
        super(label, cycles);                  // cycles is 0 for NO-OP
        this.v = v;
    }

    public Var v() { return v; }

    @Override
    public String asText() { return v + " <- " + v; }

    @Override
    public List<Var> variablesUsed() { return List.of(v); }
}

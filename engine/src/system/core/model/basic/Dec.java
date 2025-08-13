package system.core.model.basic;

import system.core.model.BasicInstruction;
import system.core.model.Var;

import java.util.List;

public final class Dec extends BasicInstruction {
    private final Var v;

    public Dec(String label, Var v, int cycles) {
        super(label, cycles);
        this.v = v;
    }

    public Var v() { return v; }

    @Override
    public String asText() {
        return v + " <- " + v + " - 1";
    }

    @Override
    public List<Var> variablesUsed() {
        return List.of(v);
    }
}

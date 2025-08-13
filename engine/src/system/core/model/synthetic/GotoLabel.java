package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;   // if the class uses Var, check later


public final class GotoLabel extends SyntheticInstruction {
    private final String target;
    public GotoLabel(String label, String target) { super(label); this.target = target; }
    public String target() { return target; }
    @Override public int cycles() { return 1; }
    @Override public String asText() { return "GOTO " + target; }
    @Override public java.util.List<Var> variablesUsed() { return java.util.List.of(); }
}

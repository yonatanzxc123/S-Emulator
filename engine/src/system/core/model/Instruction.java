package system.core.model;

import system.core.expand.helpers.FreshNames;
import java.util.List;


public sealed interface Instruction
        permits BasicInstruction, SyntheticInstruction {

    String label();
    int cycles();
    boolean isBasic();
    String asText();

    default List<Var> variablesUsed() { return List.of(); }

    /** Label targets referenced by this instruction (default: none). */
    default List<String> labelTargets() { return List.of(); }

    /**
     * Expansion hook. Default = identity (copy this instruction).
     * Synthetic instructions override to emit basic steps.
     */
    default void expandTo(Program out, FreshNames fresh) {
        out.add(this);
    }
}

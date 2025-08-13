package system.core.model;

import java.util.List;

public sealed interface Instruction
        permits BasicInstruction, SyntheticInstruction {

    String label();
    int cycles();
    boolean isBasic();
    String asText();

    default List<Var> variablesUsed() { return List.of(); }
}

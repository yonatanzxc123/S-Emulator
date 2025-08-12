package system.core.model;

public non-sealed interface SyntheticInstruction extends Instruction {
    @Override default boolean isBasic() { return false; }
}

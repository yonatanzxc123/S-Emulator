package system.core.model;

public non-sealed interface BasicInstruction extends Instruction {
    @Override default boolean isBasic() { return true; }
}

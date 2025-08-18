package system.core.model;

import system.core.expand.helpers.FreshNames;

public abstract non-sealed class SyntheticInstruction implements Instruction {
    protected final String label;

    protected SyntheticInstruction(String label) {
        this.label = (label == null ? "" : label);
    }

    @Override public String label()   { return label; }
    @Override public boolean isBasic(){ return false; }

    // expandTo() is overridden by each concrete synthetic instruction so no need here
    // cycles(), asText() are provided by each concrete synthetic instruction so no need here
    // variablesUsed() defaults to Instruction#variablesUsed unless overridden so also no need here
}

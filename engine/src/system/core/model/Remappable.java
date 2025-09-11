package system.core.model;

import java.util.function.UnaryOperator;

/** Minimal contract so QUOTE can rename vars/labels generically. */
public interface Remappable {
    /** Return a copy of this instruction after applying varMap and labelMap. */
    Instruction remap(UnaryOperator<Var> varMap,
                      UnaryOperator<String> labelMap);
}

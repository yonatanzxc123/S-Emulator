package system.core.model;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.Serializable;

public final class Program implements Serializable {
    private final String name;
    private final List<Instruction> instructions;

    //Constructor for a fresh program
    public Program(String name) {
        this(name, new ArrayList<>());
    }
    //Constructor for a loaded program (for example in a program that is being expanded by an expander)
    public Program(String name, List<Instruction> instructions) {
        this.name = Objects.requireNonNull(name, "name");
        this.instructions = new ArrayList<>(Objects.requireNonNull(instructions, "instructions"));
    }

    public String name() { return name; }

    /** Returns a live, read-only view of the instructions.
     *  The view reflects future calls to {@link #add(Instruction)}.
     */
    public List<Instruction> instructions() {
        return Collections.unmodifiableList(instructions);
    }

    /** Controlled mutation point used by loaders/expanders. */
    public void add(Instruction ins) {
        instructions.add(Objects.requireNonNull(ins));
    }



}

package system.core.model;

import java.util.ArrayList;
import java.util.List;

public final class Program {
    public final String name;
    public final List<Instruction> instructions = new ArrayList<>();
    public Program(String name){ this.name = name; }
}

package system.core.model.synthetic;

import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.Program;
import system.core.model.basic.*;
import system.core.expand.helpers.FreshNames;
import java.util.List;

public final class Assignment extends SyntheticInstruction {
    private final Var v;     // destination
    private final Var src;   // source

    public Assignment(String label, Var v, Var src) {
        super(label);
        this.v = v;
        this.src = src;
    }

    public Var v()   { return v; }
    public Var src() { return src; }

    @Override public int cycles() { return 4; }
    @Override public String asText() { return v + " <- " + src; }
    @Override public List<Var> variablesUsed() { return List.of(v, src); }

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        // tmp <- 0 ; dst <- 0
        Var tmp = fresh.tempZ();
        new ZeroVariable(label(), tmp).expandTo(out, fresh);
        new ZeroVariable("", v).expandTo(out, fresh);

        String CONSUME = fresh.nextLabel();
        String RESTORE = fresh.nextLabel();

        // if src!=0 goto CONSUME, else skip to RESTORE gate
        out.add(new IfGoto("", src, CONSUME, 2));
        out.add(new IfGoto("", tmp, RESTORE, 2)); // if tmp!=0 goto RESTORE

        // CONSUME: src-- ; tmp++
        out.add(new Dec(CONSUME, src, 1));
        out.add(new Inc("", tmp, 1));
        out.add(new IfGoto("", src, CONSUME, 2)); // loop

        // RESTORE: while tmp!=0 { tmp-- ; src++ ; dst++ }
        out.add(new Dec(RESTORE, tmp, 1));
        out.add(new Inc("", src, 1));
        out.add(new Inc("", v, 1));
        out.add(new IfGoto("", tmp, RESTORE, 2));
    }


}

package system.core.model.basic;

import system.core.model.Instruction;
import system.core.model.BasicInstruction;
import system.core.model.Remappable;
import system.core.model.Var;
import system.core.io.LoaderUtil;
import java.util.Map;

import java.util.List;
import java.util.function.UnaryOperator;

public final class Dec extends BasicInstruction implements Remappable {
    private final Var v;



    public Dec(String label, Var v, int cycles) {
        super(label, cycles);
        this.v = v;
    }

    public Var v() { return v; }

    @Override
    public String asText() {
        return v + " <- " + v + " - 1";
    }

    @Override
    public List<Var> variablesUsed() {
        return List.of(v);
    }

    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        var v = LoaderUtil.parseVar(varToken, errs, -1);
        return (v == null) ? null : new Dec(label, v, 1);
    }

    @Override
    public Instruction remap(UnaryOperator<Var> vm, UnaryOperator<String> lm) {
        return new Dec(lm.apply(label()), vm.apply(v),this.cycles());
    }




}

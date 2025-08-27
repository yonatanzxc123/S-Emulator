package system.core.model.synthetic;

import system.core.model.Instruction;
import system.core.model.SyntheticInstruction;
import system.core.model.Var;
import system.core.model.Program;
import system.core.model.basic.*;
import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;
import java.util.List;
import java.util.Map;


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
        String L1 = fresh.nextLabel();
        String L2 = fresh.nextLabel();
        String L3 = fresh.nextLabel();
        Var z1 = fresh.tempZ();

        // V <- 0   (keep as synthetic zeroing on pass 1)
        out.add(new ZeroVariable("", v));
        // IF V' != 0 GOTO L1
        out.add(new IfGoto(label(), src, L1, 2));

        // GOTO L3 *****************************************
        out.add(new GotoLabel(null, L3));

        // L1:
        out.add(new Dec(L1, src, 1));         // V' <- V' - 1
        out.add(new Inc("", z1, 1));          // z1 <- z1 + 1
        out.add(new IfGoto("", src, L1, 2));  // IF V' != 0 GOTO L1

        // L2:
        out.add(new Dec(L2, z1, 1));          // z1 <- z1 - 1
        out.add(new Inc("", v, 1));           // V <- V + 1
        out.add(new Inc("", src, 1));         // V' <- V' + 1
        out.add(new IfGoto("", z1, L2, 2));   // IF z1 != 0 GOTO L2

        // L3:
        out.add(new Nop(L3, v, 0));           // V <- V   (NOP)
    }


    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        var dst = LoaderUtil.parseVar(varToken, errs, -1);
        String srcTok = LoaderUtil.need(args.get("assignedVariable"), "assignedVariable", -1, errs);
        if (dst == null || srcTok == null) return null;
        var src = LoaderUtil.parseVar(srcTok, errs, -1);
        return (src == null) ? null : new Assignment(label, dst, src);
    }



}

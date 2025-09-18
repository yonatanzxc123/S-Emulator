package system.core.model.synthetic.advanced;

import system.core.exec.*;
import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;
import system.core.model.*;
import system.core.model.basic.Nop;
import system.core.model.synthetic.Assignment;
import system.core.model.synthetic.advanced.helpers.CallSyntax;

import java.util.*;
import java.util.function.UnaryOperator;

public final class Quote extends SyntheticInstruction
        implements Remappable, SelfExecutable {

    private final Var target;
    private final String functionName;
    /** Always stores INNER args only (no surrounding parentheses). Examples: "", "x1,y", "(+,y)". */
    private final String functionArguments;

    public Quote(String label, Var target, String functionName, String functionArguments) {
        super(label);
        this.target = Objects.requireNonNull(target, "target");
        this.functionName = Objects.requireNonNull(functionName, "functionName");
        this.functionArguments = normalizeInnerArgs(functionArguments);
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    /** Normalize to inner-args-only string; strip outer parens ONLY if they are truly extraneous. */
    private static String normalizeInnerArgs(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.isEmpty()) return s;

        if (!wrapsWholeWithParens(s)) return s;

        // If it's a single call token "(Name, ...)" that spans the entire string, KEEP it.
        if (isSingleCallToken(s)) return s;

        // Otherwise it's just outer grouping; strip if the inner has a top-level comma.
        String inner = s.substring(1, s.length() - 1).trim();
        if (hasTopLevelComma(inner)) return inner;

        // No top-level comma: "()" or "(  something  )" without commas — safest to KEEP.
        return s;
    }

    /** True if s starts with '(' and the matching ')' closes at the final char. */
    private static boolean wrapsWholeWithParens(String s) {
        if (s.charAt(0) != '(' || s.charAt(s.length() - 1) != ')') return false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0 && i != s.length() - 1) return false; // closed early → not wrapping
                if (depth < 0) return false; // malformed
            }
        }
        return depth == 0;
    }

    /** True iff s is exactly one call token of the form "(Name ...)" spanning the whole string. */
    private static boolean isSingleCallToken(String s) {
        if (!wrapsWholeWithParens(s)) return false;
        // Scan inside the first level: at depth==1 we should see a valid function name starting
        // right after '(' until ',' or ')' without leaving depth==1.
        int i = 1, n = s.length() - 1;
        // skip whitespace
        while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        // read function name chars: letters, digits, underscore (tune to your grammar)
        int start = i;
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' ) { i++; continue; }
            break;
        }
        if (i == start) return false; // no name
        // skip whitespace
        while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        // Next must be ',' or ')' (still depth==1)
        if (i >= n) return false;
        char next = s.charAt(i);
        return next == ',' || next == ')';
    }

    /** Returns true if there exists a comma at depth==0 (top-level) in s. */
    private static boolean hasTopLevelComma(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) return true;
        }
        return false;
    }

    private static void collectVars(CallSyntax.Arg a, List<Var> out) {
        if (a instanceof CallSyntax.VarRef vr) {
            out.add(vr.v());
        } else {
            CallSyntax.Call c = (CallSyntax.Call) a;
            for (CallSyntax.Arg sub : c.args()) collectVars(sub, out);
        }
    }

    private static List<CallSyntax.Arg> remapArgs(List<CallSyntax.Arg> args, UnaryOperator<Var> vm) {
        List<CallSyntax.Arg> out = new ArrayList<>(args.size());
        for (CallSyntax.Arg a : args) {
            if (a instanceof CallSyntax.VarRef vr) {
                out.add(new CallSyntax.VarRef(vm.apply(vr.v())));
            } else {
                CallSyntax.Call c = (CallSyntax.Call) a;
                out.add(new CallSyntax.Call(c.name(), remapArgs(c.args(), vm)));
            }
        }
        return out;
    }

    private static String renderArgsOnly(List<CallSyntax.Arg> args) {
        return CallSyntax.renderInnerArgs(args);
    }

    private static Program requireFunction(String name) {
        Program p = FunctionEnv.current().get(name);
        if (p == null) throw new IllegalStateException("Function '" + name + "' not found");
        return p;
    }

    // ------------------------------------------------------------
    // Meta
    // ------------------------------------------------------------

    @Override public int cycles() { return 5; }

    @Override public String asText() {
        String args = functionArguments.isEmpty() ? "" : "," + functionArguments;
        return target + " ← (" + functionName + args + ")";
    }

    /** Report READ variables (from argument expressions). Target is written, not read.
     *  Fail-soft to avoid killing the FX thread if XML is malformed. */
    @Override public List<Var> variablesUsed() {
        try {
            List<Var> used = new ArrayList<>();
            for (CallSyntax.Arg a : CallSyntax.parseArgs(functionArguments)) collectVars(a, used);
            return used;
        } catch (IllegalArgumentException badArgs) {
            // Optionally log: Logger.warn("Bad QUOTE args: '" + functionArguments + "'", badArgs);
            return Collections.emptyList();
        }
    }

    @Override
    public Instruction remap(UnaryOperator<Var> vm, UnaryOperator<String> lm) {
        List<CallSyntax.Arg> ast = CallSyntax.parseArgs(functionArguments);
        List<CallSyntax.Arg> remappedAst = remapArgs(ast, vm);
        String newInner = CallSyntax.renderInnerArgs(remappedAst);
        return new Quote(lm.apply(label()), vm.apply(target), functionName, newInner);
    }

    // ------------------------------------------------------------
    // Run-time (SelfExecutable)
    // ------------------------------------------------------------

    @Override
    public void executeSelf(MachineState s, JumpResolver jr) {
        List<Long> xs = new ArrayList<>();
        for (CallSyntax.Arg a : CallSyntax.parseArgs(functionArguments)) xs.add(evalArg(a, s));

        Program q = requireFunction(functionName);
        Executor ex = new Executor();
        MachineState sub = ex.run(q, xs);

        // account for this instruction and the callee
        s.addCycles(this.cycles());
        s.addCycles((int) Math.min(Integer.MAX_VALUE, sub.cycles()));

        s.set(target, sub.y());
        s.advance();
    }

    /** Evaluate an argument expression. Adds cycles from any nested call into the current state. */
    private long evalArg(CallSyntax.Arg a, MachineState s) {
        if (a instanceof CallSyntax.VarRef vr) {
            return s.get(vr.v());
        }
        // nested call
        CallSyntax.Call call = (CallSyntax.Call) a;
        List<Long> xs = new ArrayList<>();
        for (CallSyntax.Arg sub : call.args()) xs.add(evalArg(sub, s));

        Program subProg = requireFunction(call.name());
        Executor ex = new Executor();
        MachineState sub = ex.run(subProg, xs);

        // add cycles from this nested evaluation to the current state
        s.addCycles((int) Math.min(Integer.MAX_VALUE, sub.cycles()));
        return sub.y();
    }

    // ------------------------------------------------------------
    // Expansion
    // ------------------------------------------------------------

    @Override
    public void expandTo(Program out, FreshNames fresh) {
        Program q = requireFunction(functionName);
        List<Instruction> qBody = q.instructions();

        List<CallSyntax.Arg> args = CallSyntax.parseArgs(functionArguments);

        Map<Integer, Var> xToZ = new HashMap<>();
        Map<Integer, Var> zToZ = new HashMap<>();
        Var zy = fresh.tempZ();
        Set<String> qLabels = new LinkedHashSet<>();

        int maxQz = 0, maxQx = 0;
        for (Instruction ins : qBody) {
            for (Var v : ins.variablesUsed()) {
                if (v.isX()) maxQx = Math.max(maxQx, v.index());
                if (v.isZ()) maxQz = Math.max(maxQz, v.index());
            }
            if (ins.label() != null && !ins.label().isEmpty()) qLabels.add(ins.label());
            for (String t : ins.labelTargets()) if (!"EXIT".equals(t)) qLabels.add(t);
        }

        int needX = Math.max(maxQx, args.size());
        for (int i = 1; i <= needX; i++) xToZ.put(i, fresh.tempZ());
        for (int j = 1; j <= maxQz; j++) zToZ.put(j, fresh.tempZ());

        Map<String, String> labMap = new HashMap<>();
        for (String L : qLabels) labMap.put(L, fresh.nextLabel());
        final String Lend = fresh.nextLabel();

        List<Instruction> seq = new ArrayList<>();

        // keep the QUOTE's original label (blue bar)
        // If Nop can accept a null/ignored var, use that instead of Var.y()
        seq.add(new Nop(label(), Var.y(), 0));

        // compute each argument into its mapped temp (x_i -> xToZ[i])
        for (int i = 0; i < args.size(); i++) {
            Var dst = xToZ.get(i + 1);
            compileArg(args.get(i), dst, seq, fresh);
        }

        // remap Q body: x_i -> xToZ[i], z_j -> zToZ[j], y -> zy, labels -> fresh (EXIT -> Lend)
        UnaryOperator<Var> vm = v -> {
            if (v.isX()) return xToZ.getOrDefault(v.index(), v);
            if (v.isZ()) return zToZ.getOrDefault(v.index(), v);
            if (v.isY()) return zy;
            return v;
        };
        UnaryOperator<String> lm = lab -> {
            if (lab == null || lab.isEmpty()) return lab;
            return "EXIT".equals(lab) ? Lend : labMap.getOrDefault(lab, lab);
        };

        for (Instruction ins : qBody) {
            if (ins instanceof Remappable r) {
                seq.add(r.remap(vm, lm));
            } else {
                throw new IllegalStateException(
                        "Non-remappable instruction inside quoted function: " + ins.getClass().getName());
            }
        }

        // Lend: target <- zy
        seq.add(new Assignment(Lend, target, zy));

        seq.forEach(out::add);
    }

    /** Emit arg expression into dst; nested calls become nested QUOTEs. */
    private void compileArg(CallSyntax.Arg a, Var dst, List<Instruction> out, FreshNames fresh) {
        if (a instanceof CallSyntax.VarRef vr) {
            out.add(new Assignment("", dst, vr.v()));
        } else {
            CallSyntax.Call call = (CallSyntax.Call) a;
            String fargs = renderArgsOnly(call.args()); // inner args only
            out.add(new Quote("", dst, call.name(), fargs));
        }
    }

    // ------------------------------------------------------------
    // Loader hook
    // ------------------------------------------------------------

    public static Instruction fromXml(String label, String varToken, Map<String, String> args, List<String> errs) {
        Var dst = LoaderUtil.parseVar(varToken, errs, -1);
        String fn = LoaderUtil.need(args.get("functionName"), "functionName", -1, errs);
        String fargsRaw = args.getOrDefault("functionArguments", "");
        String fargsInner = normalizeInnerArgs(fargsRaw); // may preserve a single-call "(Func,...)" intact
        if (dst == null || fn == null) return null;
        return new Quote(label, dst, fn, fargsInner);
    }
}

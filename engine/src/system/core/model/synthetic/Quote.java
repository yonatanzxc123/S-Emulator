package system.core.model.synthetic;

import system.core.exec.*;
import system.core.expand.helpers.FreshNames;
import system.core.io.LoaderUtil;
import system.core.model.*;
import system.core.model.basic.Nop;

import java.util.*;
import java.util.function.UnaryOperator;

public final class Quote extends SyntheticInstruction
        implements Remappable, SelfExecutable {

    private final Var target;
    private final String functionName;
    /** Inner arguments list, no surrounding parentheses. Examples: "", "x1,y", "x1,(+,y)". */
    private final String functionArguments;

    public Quote(String label, Var target, String functionName, String functionArguments) {
        super(label);
        this.target = Objects.requireNonNull(target);
        this.functionName = Objects.requireNonNull(functionName);
        this.functionArguments = (functionArguments == null) ? "" : functionArguments.trim();
    }

    @Override public boolean isBasic() { return false; }
    @Override public int cycles() { return 5; }

    @Override public String asText() {
        String args = functionArguments.isEmpty() ? "" : "," + functionArguments;
        return target + " ← (" + functionName + args + ")";
    }

    @Override public List<Var> variablesUsed() { return List.of(target); }
    @Override public List<String> labelTargets() { return List.of(); }

    @Override
    public Instruction remap(UnaryOperator<Var> vm, UnaryOperator<String> lm) {
        return new Quote(lm.apply(label()), vm.apply(target), functionName, functionArguments);
    }

    // ---------- run-time (SelfExecutable) ----------
    @Override
    public void executeSelf(MachineState s, JumpResolver jr) {
        List<Long> xs = new ArrayList<>();
        for (Arg a : Parser.parseArgs(functionArguments)) xs.add(evalArg(a, s));
        Program q = requireFunction(functionName);
        Executor ex = new Executor();
        MachineState sub = ex.run(q, xs);

        s.addCycles(this.cycles());
        s.addCycles((int)Math.min(Integer.MAX_VALUE, sub.cycles()));
        s.set(target, sub.y());
        s.advance();
    }

    private long evalArg(Arg a, MachineState s) {
        return switch (a) {
            case Arg.VarRef vr -> s.get(vr.v());
            case Arg.Call call -> {
                List<Long> xs = new ArrayList<>();
                for (Arg sub : call.args()) xs.add(evalArg(sub, s));
                Program subProg = requireFunction(call.name());
                Executor ex = new Executor();
                MachineState sub = ex.run(subProg, xs);
                yield sub.y();
            }
        };
    }

    private static Program requireFunction(String name) {
        Program p = FunctionEnv.current().get(name);
        if (p == null) throw new IllegalStateException("Function '" + name + "' not found");
        return p;
    }

    // ---------- expansion ----------
    @Override
    public void expandTo(Program out, FreshNames fresh) {
        Program q = requireFunction(functionName);
        List<Instruction> qBody = q.instructions();

        Map<Integer,Var> xToZ = new HashMap<>();
        Map<Integer,Var> zToZ = new HashMap<>();
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
        for (int i = 1; i <= maxQx; i++) xToZ.put(i, fresh.tempZ());
        for (int j = 1; j <= maxQz; j++) zToZ.put(j, fresh.tempZ());

        Map<String,String> labMap = new HashMap<>();
        for (String L : qLabels) labMap.put(L, fresh.nextLabel());
        final String Lend = fresh.nextLabel();

        List<Instruction> seq = new ArrayList<>();

        // keep the QUOTE's original label (blue bar)
        seq.add(new Nop(label(), Var.y(), 0));

        // compute each argument into xToZ[i]
        List<Arg> args = Parser.parseArgs(functionArguments);
        for (int i = 0; i < args.size(); i++) {
            Var dst = xToZ.get(i + 1);
            compileArg(args.get(i), dst, seq, fresh);
        }

        // remap Q body: x_i -> xToZ[i], z_j -> fresh z, y -> zy, labels -> fresh (EXIT -> Lend)
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
            seq.add(((Remappable)ins).remap(vm, lm));
        }

        // Lend: target <- zy
        seq.add(new Assignment(Lend, target, zy));

        seq.forEach(out::add);
    }


    private static String renderArgsOnly(List<Arg> args) {
        if (args.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(args.get(i).render());     // render each sub-arg
        }
        return sb.toString();
    }


    /** emit arg expression into dst; nested calls become nested QUOTEs */
    private void compileArg(Arg a, Var dst, List<Instruction> out, FreshNames fresh) {
        switch (a) {
            case Arg.VarRef vr -> out.add(new Assignment("", dst, vr.v()));
            case Arg.Call call -> {
                String fargs = renderArgsOnly(call.args());   // <-- only inner args
                out.add(new Quote("", dst, call.name(), fargs));
            }
        }
    }




    // ---- Small local AST + parser ---- (Abstract Syntax Tree)
    private sealed interface Arg permits Arg.VarRef, Arg.Call {
        record VarRef(Var v) implements Arg {}
        record Call(String name, List<Arg> args) implements Arg {}
        default String render() {
            return switch (this) {
                case VarRef vr -> vr.v().toString();
                case Call c -> {
                    StringBuilder sb = new StringBuilder("(").append(c.name());
                    for (Arg a : c.args()) sb.append(",").append(a.render());
                    sb.append(")");
                    yield sb.toString();
                }
            };
        }
    }




    private static final class Parser {
        private final String s;
        private int i;

        private Parser(String s) { this.s = s; this.i = 0; }

        /** Accepts: "", "x1,(+,y)", "(+,x1)", "x1,y,(mul,x2,3)". */
        static List<Arg> parseArgs(String raw) {
            if (raw == null) return List.of();
            String s = raw.trim();
            if (s.isEmpty() || s.equals("()")) return List.of();

            // split by top-level commas
            List<String> tokens = new ArrayList<>();
            int depth = 0, start = 0;
            for (int k = 0; k < s.length(); k++) {
                char c = s.charAt(k);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if (c == ',' && depth == 0) {
                    tokens.add(s.substring(start, k).trim());
                    start = k + 1;
                }
            }
            tokens.add(s.substring(start).trim());

            List<Arg> out = new ArrayList<>();
            for (String t : tokens) {
                if (t.isEmpty()) continue;
                Parser p = new Parser(t);
                out.add(p.parseArg());
            }
            return out;
        }

        private Arg parseArg() {
            skipWs();
            if (peek() == '(') {                      // (Name, A1, A2, ...)
                i++;                                  // skip '('
                String name = parseName();            // parse callee name

                List<Arg> args = new ArrayList<>();

                skipWs();
                if (peek() == ')') {                  // empty arg list: ()
                    i++;
                    return new Arg.Call(name, args);
                }

                // We are at either ',' (then first arg follows) or directly at first arg (defensive).
                while (true) {
                    if (peek() == ',') i++;           // <- CONSUME COMMA BEFORE ARG
                    skipWs();

                    // parse one argument (either "(...)" or var token)
                    args.add(parseArg());
                    skipWs();

                    if (peek() == ')') {              // end of an argument list
                        i++;
                        break;
                    }
                    if (peek() != ',') {
                        throw new IllegalArgumentException("Bad nested call near " + i + " in: " + s);
                    }
                    // loop will consume the comma at the top on next iteration
                }
                return new Arg.Call(name, args);
            } else {                                   // variable token: x#, z#, or y
                String name = parseName();
                if ("y".equals(name)) return new Arg.VarRef(Var.y());
                char c = name.charAt(0);
                int idx = (name.length() == 1) ? 1 : Integer.parseInt(name.substring(1));
                return switch (c) {
                    case 'x' -> new Arg.VarRef(Var.x(idx));
                    case 'z' -> new Arg.VarRef(Var.z(idx));
                    default -> throw new IllegalArgumentException("Bad var: " + name);
                };
            }
        }

        private String parseName() {
            skipWs();
            int start = i;
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$') i++; else break;
            }
            if (start == i) throw new IllegalArgumentException("Expected name at " + i + " in: " + s);
            return s.substring(start, i);
        }

        private char peek()   { return i < s.length() ? s.charAt(i) : '\0'; }
        private void skipWs() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    }



    // ---------- loader hook ----------
    public static Instruction fromXml(String label, String varToken, Map<String,String> args, List<String> errs) {
        Var dst   = LoaderUtil.parseVar(varToken, errs, -1);
        String fn = LoaderUtil.need(args.get("functionName"), "functionName", -1, errs);
        // Maybe empty string => zero-arg call
        String fargs = args.getOrDefault("functionArguments", "").trim();
        if (fargs.startsWith("(") && fargs.endsWith(")")) {
            fargs = fargs.substring(1, fargs.length() - 1);   // args only
        }
        if (dst == null || fn == null) return null;
        return new Quote(label, dst, fn, fargs);
    }
}

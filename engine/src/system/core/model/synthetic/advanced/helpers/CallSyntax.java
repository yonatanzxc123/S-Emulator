package system.core.model.synthetic.advanced.helpers;

import system.core.model.Var;

import java.util.ArrayList;
import java.util.List;

/**
 * ---- Small local AST + parser ---- (AST stands for Abstract Syntax Tree)
 *
 * In short this helper parses strings like "(+,x1,(mul,y,z2))" into a tree of
 * Arg.Call nodes and Arg.VarRef leaves. It *only* handles syntax: parsing and
 * rendering. All evaluation/expansion logic is left to the instruction classes
 * (e.g., Quote, JumpEqualFunction).
 *
 * Used in two places:
 *  - at runtime, to evaluate the argument expressions and get their values
 *    (evaluation happens in the instruction classes)
 *  - at expansion time, to compile the argument expressions into sequences of
 *    instructions (again, done by the instruction classes)
 */
public final class CallSyntax {

    // ---- AST
    public sealed interface Arg permits VarRef, Call {}
    public static record VarRef(Var v) implements Arg {}
    public static record Call(String name, List<Arg> args) implements Arg {}

    /** Render a single Arg back to the textual form used by our XML syntax. */
    public static String render(Arg a) {
        if (a instanceof VarRef vr) return vr.v().toString();
        Call c = (Call) a;
        StringBuilder sb = new StringBuilder("(").append(c.name());
        for (Arg sub : c.args()) sb.append(',').append(render(sub));
        sb.append(')');
        return sb.toString();
    }

    /** Render only the comma-separated inner arguments list (no outer parentheses). */
    public static String renderInnerArgs(List<Arg> args) {
        if (args.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(render(args.get(i)));
        }
        return sb.toString();
    }

    // ---- Parser for argument lists (expects *inner* args, not "(x1,y)" wrapper)
    public static List<Arg> parseArgs(String raw) {
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

    private static final class Parser {
        private final String s; private int i;
        private Parser(String s) { this.s = s; this.i = 0; }

        CallSyntax.Arg parseArg() {
            skipWs();
            if (peek() == '(') {                // (Name, a1, a2, ...)
                i++;                            // skip '('
                String name = parseName();
                List<Arg> args = new ArrayList<>();
                skipWs();
                if (peek() == ')') { i++; return new Call(name, args); }
                while (true) {
                    if (peek() == ',') i++;     // consume delimiter before each arg
                    skipWs();
                    args.add(parseArg());
                    skipWs();
                    if (peek() == ')') { i++; break; }
                    if (peek() != ',') {
                        throw new IllegalArgumentException("Bad nested call near " + i + " in: " + s);
                    }
                }
                return new Call(name, args);
            } else {                            // variable token: x#, z#, or y
                String name = parseName();
                if ("y".equals(name)) return new VarRef(Var.y());
                if (name.startsWith("x")) {
                    int idx = (name.length()==1) ? 1 : Integer.parseInt(name.substring(1));
                    return new VarRef(Var.x(idx));
                }
                if (name.startsWith("z")) {
                    int idx = (name.length()==1) ? 1 : Integer.parseInt(name.substring(1));
                    return new VarRef(Var.z(idx));
                }
                throw new IllegalArgumentException("Expected var token or call, got: " + name);
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

    private CallSyntax() {}
}

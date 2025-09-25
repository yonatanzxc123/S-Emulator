package system.core.model.synthetic.advanced.helpers;

import system.core.model.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import system.core.exec.FunctionEnv;


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


    private CallSyntax() {}


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
        List<String> tokens = new ArrayList<>();
        int depth = 0, start = 0;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == '(') depth++;
            else if (c == ')') { depth--; if (depth < 0) throw new IllegalArgumentException("Unbalanced ')' in: " + s); }
            else if (c == ',' && depth == 0) {
                String tok = s.substring(start, k).trim();
                if (!tok.isEmpty()) tokens.add(tok);
                start = k + 1;
            }
        }
        if (depth != 0) throw new IllegalArgumentException("Unbalanced parentheses in: " + s);
        String last = s.substring(start).trim();
        if (!last.isEmpty()) tokens.add(last);

        List<Arg> out = new ArrayList<>();
        for (String t : tokens) {
            // IMPORTANT: do *not* strip here; tokens like "(Foo,...)" must keep their parens
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

    // This helper strips a single outer pair of parentheses if they enclose the whole string.
    // E.g. for me in the future :
    //       "(x1,y)" -> "x1,y" but "(x1,(add,y,z2))" -> "x1,(add,y,z2)" (outer parens stripped)
    //       "((x1,y),z)" -> "((x1,y),z)" (not stripped because inner parens don't match)`
    //       "x1,y" -> "x1,y" (no outer parens)/
    //       "((x1,y))" -> "(x1,y)" (outer parens stripped)

    public static String stripOuterParensIfWhole(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() < 2 || s.charAt(0) != '(' || s.charAt(s.length()-1) != ')') return s;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            // if we ever close back to depth 0 before the last char,
            // outer parens don't enclose the whole string
            if (depth == 0 && i < s.length()-1) return s;
        }
        // got here => one pair encloses the whole string
        return s.substring(1, s.length()-1).trim();
    }


    // ---------- NEW: pretty-name aware rendering ----------

    /** Render a single Arg using a name-mapper (e.g., formal "Minus" -> user "-"). */
    public static String render(Arg a, Function<String,String> namer) {
        if (a instanceof VarRef vr) return vr.v().toString();
        Call c = (Call) a;
        String shown = (namer == null) ? c.name() : namer.apply(c.name());
        StringBuilder sb = new StringBuilder("(").append(shown);
        for (Arg sub : c.args()) sb.append(',').append(render(sub, namer));
        sb.append(')');
        return sb.toString();
    }

    /** Pretty render for inner args (no outer parentheses), applying the mapper to every call name. */
    public static String renderInnerArgsPretty(List<Arg> args, Function<String,String> namer) {
        if (args.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(render(args.get(i), namer));
        }
        return sb.toString();
    }

    /** Returns a name-mapper that uses FunctionEnv (if set) to map formal names to user strings. */
    public static java.util.function.Function<String,String> envNamerOrIdentity() {
        return fn -> {
            try {
                var p = FunctionEnv.current().get(fn);
                return (p == null) ? fn : p.name();   // Program.name() carries the <UserString>
            } catch (IllegalStateException ignore) {  // FunctionEnv not set (e.g., Show Program)
                return fn;                            // fall back to formal name
            }
        };
    }




}

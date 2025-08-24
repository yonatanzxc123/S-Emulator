package system.core.io;

import system.core.model.Var;

import java.util.List;
// helper for all loaders\Instructions to parse common things like variable names, numbers, etc.
public final class LoaderUtil {
    private LoaderUtil() {}

    public static String need(String v, String name, int line, List<String> errs) {
        if (v == null || v.trim().isEmpty()) {
            errs.add("Missing @" + name + (line > 0 ? (" at instruction #" + line) : ""));
            return null;
        }
        return v.trim();
    }

    public static long parseNonNegLong(String v, String name, int line, List<String> errs) {
        try {
            long k = Long.parseLong(v);
            if (k < 0) {
                errs.add("@" + name + " must be >= 0" + (line > 0 ? (" at instruction #" + line) : ""));
                return 0L;
            }
            return k;
        } catch (Exception ex) {
            errs.add("Bad number for @" + name + ": '" + v + "'" + (line > 0 ? (" at instruction #" + line) : ""));
            return 0L;
        }
    }

    /** Parses "xN", "zN", or "y". Returns null and records an error if invalid. */
    public static Var parseVar(String token, List<String> errs, int line) {
        if (token == null || token.isBlank()) {
            errs.add("Missing <S-Variable>" + (line > 0 ? (" at instruction #" + line) : ""));
            return null;
        }
        String s = token.trim();
        if (s.equals("y")) return Var.y();
        if (s.startsWith("x")) {
            try { return Var.x(Integer.parseInt(s.substring(1))); }
            catch (NumberFormatException e){ errs.add("Bad x index: " + s); return null; }
        }
        if (s.startsWith("z")) {
            try { return Var.z(Integer.parseInt(s.substring(1))); }
            catch (NumberFormatException e){ errs.add("Bad z index: " + s); return null; }
        }
        errs.add("Unknown variable token: " + s);
        return null;
    }
}

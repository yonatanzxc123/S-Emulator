package server_core.util;

import java.util.Map;
import server_core.ProgramMeta;
import server_core.User;

public final class Credits {
    private Credits() {}

    public static final Map<String,Integer> ARCH_COST = Map.of(
            "I", 5, "II", 100, "III", 500, "IV", 1000
    );

    /** Check if the architecture tier string is valid. */
    public static boolean validArch(String arch) {
        return arch != null && ARCH_COST.containsKey(arch);
    }

    /** Get the fixed credit cost for a given architecture tier (0 if invalid). */
    public static long archFixed(String arch) {
        return ARCH_COST.getOrDefault(arch, 0);
    }

    /** Calculate the minimum credits required to start a run/debug (fixed cost + avg runtime cost). */
    public static long minRequiredToStart(ProgramMeta meta, String arch) {
        long fixed = archFixed(arch);
        long avg   = Math.round(meta.avgCreditsCost);
        return fixed + Math.max(0, avg);
    }

    /** Attempt to deduct credits from the user; returns false and leaves balance unchanged if insufficient. */
    public static boolean tryCharge(User u, long amount) {
        return u.tryCharge(amount);
    }
}

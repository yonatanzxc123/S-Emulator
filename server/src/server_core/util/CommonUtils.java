package server_core.util;

/**
 * Miscellaneous utility methods shared across server servlets.
 */
public final class CommonUtils {
    private CommonUtils() {}  // prevent instantiation

    /**
     * Parse and clamp a degree value from a string.
     * Accepts a numeric string (or null/blank) and returns an integer between 0 and max (inclusive).
     * Non-numeric or empty input defaults to 0.
     */
    public static int parseDegree(String degreeStr, int maxDegree) {
        int d;
        try {
            d = (degreeStr == null || degreeStr.isBlank()) ? 0 : Integer.parseInt(degreeStr.trim());
        } catch (NumberFormatException e) {
            d = 0;
        }
        // Clamp the result between 0 and maxDegree
        if (d < 0) d = 0;
        if (d > maxDegree) d = maxDegree;
        return d;
    }

    /**
     * Convert an architecture tier number (1–4) to its Roman numeral representation.
     * Returns "I" for 1, "II" for 2, "III" for 3, and "IV" for 4 or any other value (defaults to highest tier).
     */
    public static String toRoman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }



}

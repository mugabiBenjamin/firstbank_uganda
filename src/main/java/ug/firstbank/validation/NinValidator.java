package ug.firstbank.validation;

import java.util.regex.Pattern;

public final class NinValidator {

    private static final Pattern NIN_PATTERN =
            Pattern.compile("^C[MF]\\d{8}[A-Z]{4}$");

    /** Exact required length of a valid NIN. */
    public static final int NIN_LENGTH = 14;

    // Private constructor — utility class, not instantiable.
    private NinValidator() {}

    // ── Public API ───────────────────────────────────────────────────────────

    public static boolean isValid(String nin) {
        if (nin == null) return false;
        return NIN_PATTERN.matcher(nin).matches();
    }

    public static boolean isMale(String nin) {
        return nin != null && nin.startsWith("CM");
    }

    public static boolean isFemale(String nin) {
        return nin != null && nin.startsWith("CF");
    }

    public static String normalise(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase();
    }

    public static String formatDescription() {
        return "Must be exactly 14 characters: CM or CF prefix, "
                + "8 digits, 4 uppercase letters (e.g. CM12345678ABCD).";
    }
}
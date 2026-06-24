package ug.firstbank.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyFormatter {

    private static final NumberFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
        NUMBER_FORMAT.setGroupingUsed(true);
    }

    private CurrencyFormatter() {}

    // ── Public API ───────────────────────────────────────────────────────────

    public static String format(long amount) {
        return "UGX " + NUMBER_FORMAT.format(amount);
    }

    public static String formatPlain(long amount) {
        return NUMBER_FORMAT.format(amount);
    }

    public static long parse(String raw) {
        if (raw == null || raw.isBlank()) return -1L;
        try {
            return Long.parseLong(raw.trim().replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
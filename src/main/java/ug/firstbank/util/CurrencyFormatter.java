package ug.firstbank.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Stateless utility for formatting monetary amounts in Ugandan Shillings (UGX).
 *
 * <p>Produces human-readable strings with thousand separators, e.g.
 * {@code 50000} → {@code "UGX 50,000"} or {@code "50,000"} depending on
 * the caller's needs.</p>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — owns one concern: UGX amount formatting. No parsing,
 *       no validation, no DB interaction.</li>
 *   <li><b>OCP</b> — adding a new format variant (e.g. compact notation
 *       "UGX 1.0M") means adding one method; existing methods are untouched.</li>
 * </ul>
 *
 * <p>All methods are {@code static}; the class is not instantiable.</p>
 */
public final class CurrencyFormatter {

    /**
     * Number formatter using {@link Locale#US} to guarantee comma thousand
     * separators regardless of the JVM's default locale on either Ubuntu or
     * Windows.
     */
    private static final NumberFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
        NUMBER_FORMAT.setGroupingUsed(true);
    }

    private CurrencyFormatter() {}

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Formats {@code amount} with thousand separators and a {@code UGX} prefix.
     *
     * <p>Example: {@code format(1000000)} → {@code "UGX 1,000,000"}</p>
     *
     * @param amount the amount in UGX (non-negative)
     * @return formatted string with UGX prefix
     */
    public static String format(long amount) {
        return "UGX " + NUMBER_FORMAT.format(amount);
    }

    /**
     * Formats {@code amount} with thousand separators and no currency prefix.
     *
     * <p>Use this when the currency context is already clear from the
     * surrounding UI label, e.g. inside a column already headed "UGX".</p>
     *
     * <p>Example: {@code formatPlain(50000)} → {@code "50,000"}</p>
     *
     * @param amount the amount in UGX (non-negative)
     * @return formatted numeric string without prefix
     */
    public static String formatPlain(long amount) {
        return NUMBER_FORMAT.format(amount);
    }

    /**
     * Strips thousand-separator commas and whitespace from a raw user-typed
     * deposit string and parses it as a {@code long}.
     *
     * <p>Returns {@code -1} if the string cannot be parsed, allowing callers
     * to treat a negative result as a parse failure without catching an
     * exception.</p>
     *
     * <p>Example: {@code parse("1,000,000")} → {@code 1000000L}</p>
     *
     * @param raw raw deposit input, possibly containing commas or spaces
     * @return parsed amount, or {@code -1} on parse failure
     */
    public static long parse(String raw) {
        if (raw == null || raw.isBlank()) return -1L;
        try {
            return Long.parseLong(raw.trim().replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
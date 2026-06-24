package ug.firstbank.validation;

import java.util.regex.Pattern;

/**
 * Stateless validator for Ugandan National Identification Numbers (NIN).
 *
 * <p>Expected format:</p>
 * <pre>
 *   CM63738361TYWS   — male   (CM + 8 digits + 4 uppercase letters)
 *   CF63738361TYWS   — female (CF + 8 digits + 4 uppercase letters)
 * </pre>
 * <p>Total: exactly 14 characters, all uppercase alphanumeric.</p>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — this class owns exactly one concern: NIN format
 *       validation. No other validation logic lives here.</li>
 *   <li><b>OCP</b> — if the NIN format ever gains a third prefix (e.g. CX),
 *       only the {@link #NIN_PATTERN} constant changes; no call sites change.</li>
 *   <li><b>DIP</b> — {@code FormValidator} depends on this class, not on any
 *       specific regex string scattered inline. Isolating the pattern here
 *       makes it trivially replaceable or mockable.</li>
 * </ul>
 *
 * <p>All methods are {@code static} because the class is fully stateless —
 * no instance state is needed or desirable.</p>
 */
public final class NinValidator {

    /**
     * Compiled NIN pattern.
     *
     * <p>Breakdown:</p>
     * <ul>
     *   <li>{@code C[MF]} — literal 'C' followed by 'M' (male) or 'F' (female)</li>
     *   <li>{@code \\d{8}} — exactly 8 decimal digits</li>
     *   <li>{@code [A-Z]{4}} — exactly 4 uppercase ASCII letters</li>
     * </ul>
     * <p>The pattern is anchored (^ … $) so partial matches are rejected.</p>
     */
    private static final Pattern NIN_PATTERN =
            Pattern.compile("^C[MF]\\d{8}[A-Z]{4}$");

    /** Exact required length of a valid NIN. */
    public static final int NIN_LENGTH = 14;

    // Private constructor — utility class, not instantiable.
    private NinValidator() {}

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if {@code nin} matches the Ugandan NIN format.
     *
     * <p>The check is case-sensitive: the input must already be uppercase.
     * Use {@link #normalise(String)} to uppercase before validating if
     * the raw input may contain lowercase letters.</p>
     *
     * @param nin the NIN string to validate (must not be {@code null})
     * @return {@code true} if valid
     */
    public static boolean isValid(String nin) {
        if (nin == null) return false;
        return NIN_PATTERN.matcher(nin).matches();
    }

    /**
     * Returns {@code true} if {@code nin} indicates a male applicant
     * (prefix {@code CM}).
     *
     * <p>Callers should only invoke this after {@link #isValid(String)}
     * returns {@code true}; behaviour on invalid input is undefined.</p>
     *
     * @param nin a validated NIN string
     * @return {@code true} for male prefix
     */
    public static boolean isMale(String nin) {
        return nin != null && nin.startsWith("CM");
    }

    /**
     * Returns {@code true} if {@code nin} indicates a female applicant
     * (prefix {@code CF}).
     *
     * @param nin a validated NIN string
     * @return {@code true} for female prefix
     */
    public static boolean isFemale(String nin) {
        return nin != null && nin.startsWith("CF");
    }

    /**
     * Normalises raw NIN input by trimming whitespace and converting to
     * uppercase, matching the auto-uppercase TextFormatter applied in the UI.
     *
     * <p>Callers may pass the result directly to {@link #isValid(String)}.</p>
     *
     * @param raw raw user input, possibly with leading/trailing spaces or
     *            lowercase letters
     * @return trimmed uppercase string, or an empty string if {@code raw}
     *         is {@code null}
     */
    public static String normalise(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase();
    }

    /**
     * Returns a user-facing error message describing the NIN format,
     * used by {@code FormValidator} to populate {@link ValidationResult}.
     *
     * <p>Keeping the message here (not in {@code FormValidator}) means it
     * travels with the rule that defines it — a DRY measure.</p>
     *
     * @return human-readable format description
     */
    public static String formatDescription() {
        return "Must be exactly 14 characters: CM or CF prefix, "
                + "8 digits, 4 uppercase letters (e.g. CM63738361TYWS).";
    }
}
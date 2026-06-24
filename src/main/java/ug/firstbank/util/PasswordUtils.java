package ug.firstbank.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Stateless utility wrapping the bcrypt library for PIN hashing and verification.
 *
 * <p>The raw PIN never leaves {@code AccountService} — this class is the only
 * point in the codebase that touches raw PIN bytes, and it immediately discards
 * them after producing or verifying the hash.</p>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — owns one concern: bcrypt operations. No validation,
 *       no DB, no UI.</li>
 *   <li><b>DIP</b> — {@code AccountService} depends on this utility class
 *       rather than calling the bcrypt library directly, so swapping the
 *       hashing algorithm later requires changing only this file.</li>
 * </ul>
 *
 * <p><b>Cost factor:</b> {@link #BCRYPT_COST} is set to 12, which is a
 * widely recommended baseline for interactive login flows as of 2024.
 * Increase it if the deployment hardware allows without noticeable UI lag.</p>
 *
 * <p>All methods are {@code static}; the class is not instantiable.</p>
 */
public final class PasswordUtils {

    /**
     * bcrypt work factor (cost). Each increment doubles the hashing time.
     * 12 ≈ 250–400 ms on modern hardware — acceptable for a one-time
     * account-opening submission, imperceptible to the user.
     */
    public static final int BCRYPT_COST = 12;

    private PasswordUtils() {}

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Hashes a raw PIN using bcrypt with {@link #BCRYPT_COST}.
     *
     * <p>The returned string is a standard 60-character bcrypt hash that
     * encodes the algorithm version, cost factor, salt, and digest —
     * safe to store directly in the database.</p>
     *
     * @param rawPin the applicant's raw PIN (4–6 digits, already validated)
     * @return a 60-character bcrypt hash string
     * @throws IllegalArgumentException if {@code rawPin} is {@code null} or blank
     */
    public static String hash(String rawPin) {
        if (rawPin == null || rawPin.isBlank()) {
            throw new IllegalArgumentException("PIN must not be null or blank.");
        }
        return BCrypt.withDefaults()
                     .hashToString(BCRYPT_COST, rawPin.toCharArray());
    }

    /**
     * Verifies a raw PIN against a previously stored bcrypt hash.
     *
     * <p>Used during the duplicate-NIN lookup flow if the bank ever needs
     * to re-authenticate the applicant — not required for the initial
     * account-opening submission, but provided for completeness and
     * future extensibility.</p>
     *
     * @param rawPin     the PIN to verify
     * @param storedHash the bcrypt hash retrieved from the database
     * @return {@code true} if the PIN matches the hash
     */
    public static boolean verify(String rawPin, String storedHash) {
        if (rawPin == null || storedHash == null) return false;
        BCrypt.Result result = BCrypt.verifyer()
                                     .verify(rawPin.toCharArray(), storedHash);
        return result.verified;
    }
}
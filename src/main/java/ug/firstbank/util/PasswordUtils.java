package ug.firstbank.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordUtils {

    public static final int BCRYPT_COST = 12;

    private PasswordUtils() {}

    // ── Public API ───────────────────────────────────────────────────────────

    public static String hash(String rawPin) {
        if (rawPin == null || rawPin.isBlank()) {
            throw new IllegalArgumentException("PIN must not be null or blank.");
        }
        return BCrypt.withDefaults()
                     .hashToString(BCRYPT_COST, rawPin.toCharArray());
    }

    public static boolean verify(String rawPin, String storedHash) {
        if (rawPin == null || storedHash == null) return false;
        BCrypt.Result result = BCrypt.verifyer()
                                     .verify(rawPin.toCharArray(), storedHash);
        return result.verified;
    }
}
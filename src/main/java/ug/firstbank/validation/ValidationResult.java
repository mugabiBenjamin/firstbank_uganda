package ug.firstbank.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable result object returned by every validation call.
 *
 * <p>Holds a {@code field → error message} map so the UI can place inline
 * error labels next to the exact field that failed, and also render a
 * consolidated summary dialog listing all problems at once.</p>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — owns one concern only: carrying validation outcome data.
 *       No validation logic lives here.</li>
 *   <li><b>ISP</b> — callers that only need {@link #isValid()} are not forced
 *       to interact with the error map at all.</li>
 * </ul>
 *
 * <p>Field name keys match the constants defined in {@link FieldNames} so that
 * the UI layer can look up errors by a stable, typo-safe identifier rather
 * than a raw string literal.</p>
 *
 * <p>Use {@link Builder} to construct instances fluently:</p>
 * <pre>{@code
 * ValidationResult result = new ValidationResult.Builder()
 *         .addError(FieldNames.FIRST_NAME, "Letters only, 2–30 characters.")
 *         .addError(FieldNames.EMAIL,      "Must be a valid email address.")
 *         .build();
 * }</pre>
 */
public final class ValidationResult {

    // ── Field name constants ─────────────────────────────────────────────────

    /**
     * Stable field-name identifiers used as keys in the error map.
     *
     * <p>Keeping them here (rather than scattered as string literals across
     * validator and UI classes) is a DRY measure: rename a field once here,
     * and all references update automatically.</p>
     */
    public static final class FieldNames {
        private FieldNames() {}

        public static final String FIRST_NAME       = "firstName";
        public static final String LAST_NAME        = "lastName";
        public static final String NIN              = "nin";
        public static final String SECOND_NIN       = "secondNin";
        public static final String EMAIL            = "email";
        public static final String CONFIRM_EMAIL    = "confirmEmail";
        public static final String PHONE            = "phone";
        public static final String PIN              = "pin";
        public static final String CONFIRM_PIN      = "confirmPin";
        public static final String DATE_OF_BIRTH    = "dateOfBirth";
        public static final String ACCOUNT_TYPE     = "accountType";
        public static final String BRANCH           = "branch";
        public static final String OPENING_DEPOSIT  = "openingDeposit";
    }

    // ── State ────────────────────────────────────────────────────────────────

    /**
     * Insertion-ordered map so errors are reported in form-top-to-bottom order.
     * Unmodifiable after construction.
     */
    private final Map<String, String> errors;

    // ── Constructor (private — use Builder) ──────────────────────────────────

    private ValidationResult(Map<String, String> errors) {
        this.errors = Collections.unmodifiableMap(errors);
    }

    // ── Query methods ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if no validation errors were recorded.
     *
     * @return {@code true} when the form data is valid
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Returns {@code true} if at least one error was recorded.
     *
     * @return {@code true} when the form data has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns the error message for the given field, or {@code null} if
     * that field passed validation.
     *
     * @param fieldName a constant from {@link FieldNames}
     * @return error message string, or {@code null}
     */
    public String getError(String fieldName) {
        return errors.get(fieldName);
    }

    /**
     * Returns {@code true} if the specified field has a recorded error.
     *
     * @param fieldName a constant from {@link FieldNames}
     * @return {@code true} if an error exists for this field
     */
    public boolean hasError(String fieldName) {
        return errors.containsKey(fieldName);
    }

    /**
     * Returns an unmodifiable view of all field errors, in insertion order.
     *
     * <p>The UI summary dialog iterates this map to list all problems.</p>
     *
     * @return unmodifiable {@code field → error} map
     */
    public Map<String, String> getAllErrors() {
        return errors;
    }

    /**
     * Returns the total number of validation errors.
     *
     * @return error count
     */
    public int errorCount() {
        return errors.size();
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    /**
     * Fluent builder for {@link ValidationResult}.
     *
     * <p>Errors are stored in insertion order (LinkedHashMap) so the summary
     * dialog lists them top-to-bottom matching the form layout.</p>
     */
    public static final class Builder {

        private final Map<String, String> errors = new LinkedHashMap<>();

        /**
         * Records an error for the given field.
         *
         * <p>If a field already has an error, the first message wins and
         * subsequent calls for the same field are ignored — preventing
         * duplicate messages for a single field.</p>
         *
         * @param fieldName a constant from {@link FieldNames}
         * @param message   human-readable error description shown to the user
         * @return this builder, for chaining
         */
        public Builder addError(String fieldName, String message) {
            errors.putIfAbsent(fieldName, message);
            return this;
        }

        /**
         * Conditionally records an error only when {@code condition} is true.
         *
         * <p>Reduces boilerplate {@code if} blocks in {@code FormValidator}:</p>
         * <pre>{@code
         * builder.addErrorIf(name.length() < 2, FieldNames.FIRST_NAME,
         *         "Must be at least 2 characters.");
         * }</pre>
         *
         * @param condition if {@code true}, the error is recorded
         * @param fieldName a constant from {@link FieldNames}
         * @param message   human-readable error description
         * @return this builder, for chaining
         */
        public Builder addErrorIf(boolean condition, String fieldName, String message) {
            if (condition) {
                errors.putIfAbsent(fieldName, message);
            }
            return this;
        }

        /**
         * Builds and returns an immutable {@link ValidationResult}.
         *
         * @return new {@code ValidationResult} instance
         */
        public ValidationResult build() {
            return new ValidationResult(errors);
        }
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        if (isValid()) return "ValidationResult[VALID]";
        return "ValidationResult[errors=" + errors + "]";
    }
}
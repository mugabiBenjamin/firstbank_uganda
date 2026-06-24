package ug.firstbank.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationResult {

    // ── Field name constants ─────────────────────────────────────────────────

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

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getError(String fieldName) {
        return errors.get(fieldName);
    }

    public boolean hasError(String fieldName) {
        return errors.containsKey(fieldName);
    }

    public Map<String, String> getAllErrors() {
        return errors;
    }

    public int errorCount() {
        return errors.size();
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static final class Builder {

        private final Map<String, String> errors = new LinkedHashMap<>();

        public Builder addError(String fieldName, String message) {
            errors.putIfAbsent(fieldName, message);
            return this;
        }

        public Builder addErrorIf(boolean condition, String fieldName, String message) {
            if (condition) {
                errors.putIfAbsent(fieldName, message);
            }
            return this;
        }

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
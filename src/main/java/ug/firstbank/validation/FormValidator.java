package ug.firstbank.validation;

import ug.firstbank.model.Account;
import ug.firstbank.model.JointAccount;
import ug.firstbank.model.StudentAccount;
import ug.firstbank.validation.ValidationResult.FieldNames;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public final class FormValidator {

    // ── Compiled patterns ────────────────────────────────────────────────────

    /** Generic email pattern — RFC-5322 simplified, sufficient for this context. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Ugandan phone number: {@code +256} followed by exactly 9 digits.
     * Total length: 13 characters.
     */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+256\\d{9}$");

    /** Letters only, 2–30 characters (trimmed input). */
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z]{2,30}$");

    /** PIN: 4–6 decimal digits. */
    private static final Pattern PIN_PATTERN =
            Pattern.compile("^\\d{4,6}$");

    // ── Age bounds (general) ─────────────────────────────────────────────────

    /** Minimum applicant age for all account types (inclusive). */
    public static final int MIN_AGE = 18;

    /** Maximum applicant age for all account types (inclusive). */
    public static final int MAX_AGE = 75;

    // Private constructor — utility class.
    private FormValidator() {}

    // ── Main entry point ─────────────────────────────────────────────────────

    public static ValidationResult validate(
            String firstName,
            String lastName,
            String nin,
            String secondNin,
            String email,
            String confirmEmail,
            String phone,
            String pin,
            String confirmPin,
            LocalDate dob,
            Account account,
            String branch,
            String depositText) {

        ValidationResult.Builder b = new ValidationResult.Builder();

        validateName(b, FieldNames.FIRST_NAME, "First name", firstName);
        validateName(b, FieldNames.LAST_NAME,  "Last name",  lastName);
        validateNin(b, nin);
        validateSecondNin(b, secondNin, nin, account);
        validateEmail(b, email, confirmEmail);
        validatePhone(b, phone);
        validatePin(b, pin, confirmPin);
        validateDob(b, dob, account);
        validateBranch(b, branch);
        validateDeposit(b, depositText, account);

        return b.build();
    }

    // ── Private field validators ─────────────────────────────────────────────

    private static void validateName(ValidationResult.Builder b,
                                     String fieldName,
                                     String label,
                                     String raw) {
        String value = trim(raw);
        if (value.isEmpty()) {
            b.addError(fieldName, label + " is required.");
            return;
        }
        b.addErrorIf(!NAME_PATTERN.matcher(value).matches(), fieldName,
                label + " must contain letters only, 2–30 characters.");
    }

    private static void validateNin(ValidationResult.Builder b, String raw) {
        String nin = NinValidator.normalise(raw);
        if (nin.isEmpty()) {
            b.addError(FieldNames.NIN, "National ID (NIN) is required.");
            return;
        }
        b.addErrorIf(!NinValidator.isValid(nin), FieldNames.NIN,
                NinValidator.formatDescription());
    }

    private static void validateSecondNin(ValidationResult.Builder b, 
                                          String rawSecond,
                                          String rawPrimary,
                                          Account account) {
        if (!(account instanceof JointAccount)) {
            return; // second NIN is irrelevant for all other account types
        }

        String second  = NinValidator.normalise(rawSecond);
        String primary = NinValidator.normalise(rawPrimary);

        if (second.isEmpty()) {
            b.addError(FieldNames.SECOND_NIN,
                    "Spouse National ID (NIN) is required for a Joint account.");
            return;
        }
        if (!NinValidator.isValid(second)) {
            b.addError(FieldNames.SECOND_NIN,
                    "Spouse NIN: " + NinValidator.formatDescription());
            return;
        }
        b.addErrorIf(second.equals(primary), FieldNames.SECOND_NIN,
                "Spouse NIN must differ from the primary applicant's NIN.");
    }

    private static void validateEmail(ValidationResult.Builder b,
                                      String rawEmail,
                                      String rawConfirm) {
        String email   = trim(rawEmail);
        String confirm = trim(rawConfirm);

        if (email.isEmpty()) {
            b.addError(FieldNames.EMAIL, "Email address is required.");
        } else {
            b.addErrorIf(!EMAIL_PATTERN.matcher(email).matches(),
                    FieldNames.EMAIL, "Must be a valid email address.");
        }

        if (confirm.isEmpty()) {
            b.addError(FieldNames.CONFIRM_EMAIL, "Please confirm your email address.");
        } else if (!email.isEmpty() && !email.equalsIgnoreCase(confirm)) {
            b.addError(FieldNames.CONFIRM_EMAIL, "Email addresses do not match.");
        }
    }

    private static void validatePhone(ValidationResult.Builder b, String raw) {
        String phone = trim(raw);
        if (phone.isEmpty()) {
            b.addError(FieldNames.PHONE, "Phone number is required.");
            return;
        }
        b.addErrorIf(!PHONE_PATTERN.matcher(phone).matches(),
                FieldNames.PHONE,
                "Must follow the format +256XXXXXXXXX (e.g. +256772123456).");
    }

    private static void validatePin(ValidationResult.Builder b,
                                    String rawPin,
                                    String rawConfirm) {
        String pin     = trim(rawPin);
        String confirm = trim(rawConfirm);

        if (pin.isEmpty()) {
            b.addError(FieldNames.PIN, "PIN is required.");
        } else if (!PIN_PATTERN.matcher(pin).matches()) {
            b.addError(FieldNames.PIN, "PIN must be 4–6 digits.");
        } else if (isAllIdenticalDigits(pin)) {
            b.addError(FieldNames.PIN,
                    "PIN must not be all identical digits (e.g. 0000, 1111).");
        }

        if (confirm.isEmpty()) {
            b.addError(FieldNames.CONFIRM_PIN, "Please confirm your PIN.");
        } else if (!pin.isEmpty() && !pin.equals(confirm)) {
            b.addError(FieldNames.CONFIRM_PIN, "PINs do not match.");
        }
    }

    private static void validateDob(ValidationResult.Builder b,
                                    LocalDate dob,
                                    Account account) {
        if (dob == null) {
            b.addError(FieldNames.DATE_OF_BIRTH,
                    "Date of birth is required. Select Year, Month, and Day.");
            return;
        }

        LocalDate today = LocalDate.now();

        if (!dob.isBefore(today)) {
            b.addError(FieldNames.DATE_OF_BIRTH,
                    "Date of birth must be in the past.");
            return;
        }

        int age = Period.between(dob, today).getYears();

        if (age < MIN_AGE || age > MAX_AGE) {
            b.addError(FieldNames.DATE_OF_BIRTH,
                    "Applicant age must be between " + MIN_AGE
                            + " and " + MAX_AGE + " years.");
            return;
        }

        // Student-specific age check — delegates to the subclass rule (LSP)
        if (account instanceof StudentAccount student) {
            b.addErrorIf(!student.isAgeEligible(age), FieldNames.DATE_OF_BIRTH,
                    "Student accounts require applicant age "
                            + StudentAccount.MIN_AGE + "–"
                            + StudentAccount.MAX_AGE + ". "
                            + "Your age is " + age + ".");
        }
    }

    private static void validateBranch(ValidationResult.Builder b, String branch) {
        b.addErrorIf(branch == null || trim(branch).isEmpty(),
                FieldNames.BRANCH,
                "Please select a branch.");
    }

    private static void validateDeposit(ValidationResult.Builder b,
                                        String rawDeposit,
                                        Account account) {
        String cleaned = trim(rawDeposit).replace(",", "").replace(" ", "");

        if (cleaned.isEmpty()) {
            b.addError(FieldNames.OPENING_DEPOSIT, "Opening deposit is required.");
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            b.addError(FieldNames.OPENING_DEPOSIT,
                    "Opening deposit must be a numeric value in UGX.");
            return;
        }

        if (amount <= 0) {
            b.addError(FieldNames.OPENING_DEPOSIT,
                    "Opening deposit must be a positive amount.");
            return;
        }

        b.addErrorIf(!account.isDepositSufficient(amount),
                FieldNames.OPENING_DEPOSIT,
                String.format(
                        "Minimum opening deposit for a %s account is UGX %,d. "
                                + "You entered UGX %,d.",
                        account.displayName(),
                        account.minimumDeposit(),
                        amount));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isAllIdenticalDigits(String s) {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) return false;
        }
        return true;
    }
}
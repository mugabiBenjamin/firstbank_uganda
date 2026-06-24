package ug.firstbank.validation;

import ug.firstbank.model.Account;
import ug.firstbank.model.JointAccount;
import ug.firstbank.model.StudentAccount;
import ug.firstbank.validation.ValidationResult.FieldNames;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Orchestrates all form-level validation for the account-opening application.
 *
 * <p>Accepts raw string inputs (exactly as the user typed them), runs every
 * rule defined in the specification, and returns a single
 * {@link ValidationResult} that maps each offending field to its error message.
 * A result with no errors means the form is ready to submit.</p>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — this class owns validation orchestration only.
 *       NIN format logic lives in {@link NinValidator}; account-type rules
 *       live in the {@code model} subclasses; error data lives in
 *       {@link ValidationResult}.</li>
 *   <li><b>OCP</b> — adding a new field means adding one private helper method
 *       and one call in {@link #validate}; nothing else changes.</li>
 *   <li><b>LSP</b> — deposit validation delegates to
 *       {@code account.isDepositSufficient()} and
 *       {@code account.minimumDeposit()}, so any {@link Account} subtype
 *       is substitutable without branching here.</li>
 *   <li><b>DIP</b> — depends on the abstract {@link Account}, not on any
 *       concrete subclass (except for targeted {@code instanceof} checks where
 *       the spec mandates subtype-specific rules).</li>
 * </ul>
 *
 * <p>All methods are {@code static}; the class is stateless and not
 * instantiable.</p>
 */
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

    /**
     * Validates all fields of the account-opening form.
     *
     * <p>Every field is evaluated regardless of whether an earlier field
     * failed — so the user sees all problems at once, not one at a time.</p>
     *
     * @param firstName      raw first name input
     * @param lastName       raw last name input
     * @param nin            raw NIN input (will be normalised internally)
     * @param secondNin      raw second NIN input; ignored unless
     *                       {@code account} is a {@link JointAccount}
     * @param email          raw email input
     * @param confirmEmail   raw confirm-email input
     * @param phone          raw phone number input
     * @param pin            raw PIN input (not stored — used for format check
     *                       and match against {@code confirmPin})
     * @param confirmPin     raw confirm-PIN input
     * @param dob            parsed date of birth, or {@code null} if the
     *                       user left the combo boxes unselected
     * @param account        the concrete {@link Account} subtype the user
     *                       selected; must not be {@code null}
     * @param branch         selected branch display name, or {@code null}/blank
     *                       if unselected
     * @param depositText    raw opening deposit input (may contain commas or spaces)
     * @return {@link ValidationResult} — call {@link ValidationResult#isValid()}
     *         to check overall outcome
     */
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

    /**
     * Validates a name field (first or last name).
     * Rule: required, letters only, 2–30 characters.
     */
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

    /**
     * Validates the primary NIN.
     * Rule: required, CM/CF format, exactly 14 uppercase alphanumeric chars.
     */
    private static void validateNin(ValidationResult.Builder b, String raw) {
        String nin = NinValidator.normalise(raw);
        if (nin.isEmpty()) {
            b.addError(FieldNames.NIN, "National ID (NIN) is required.");
            return;
        }
        b.addErrorIf(!NinValidator.isValid(nin), FieldNames.NIN,
                NinValidator.formatDescription());
    }

    /**
     * Validates the second (spouse) NIN for Joint accounts.
     * Rules:
     * <ul>
     *   <li>Required only when account type is {@link JointAccount}.</li>
     *   <li>Must pass the same CM/CF format as the primary NIN.</li>
     *   <li>Must differ from the primary NIN.</li>
     * </ul>
     */
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

    /**
     * Validates email and its confirmation field.
     * Rules: valid format; confirm must match.
     */
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

    /**
     * Validates the Ugandan phone number.
     * Rule: must match {@code +256XXXXXXXXX} (13 characters total).
     */
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

    /**
     * Validates the PIN and its confirmation.
     * Rules:
     * <ul>
     *   <li>Numeric only, 4–6 digits.</li>
     *   <li>Must not be all identical digits (e.g. 0000, 11111).</li>
     *   <li>Confirm PIN must match PIN.</li>
     * </ul>
     */
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

    /**
     * Validates the date of birth and derived age.
     * Rules:
     * <ul>
     *   <li>DOB must be selected (not {@code null}).</li>
     *   <li>Must not be a future date.</li>
     *   <li>General age rule: 18–75 inclusive.</li>
     *   <li>Student accounts: 18–25 inclusive (checked via
     *       {@link StudentAccount#isAgeEligible(int)}).</li>
     * </ul>
     */
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

    /**
     * Validates the branch selection.
     * Rule: exactly one branch must be selected.
     */
    private static void validateBranch(ValidationResult.Builder b, String branch) {
        b.addErrorIf(branch == null || trim(branch).isEmpty(),
                FieldNames.BRANCH,
                "Please select a branch.");
    }

    /**
     * Validates the opening deposit against the selected account type's minimum.
     *
     * <p>Delegates the minimum lookup and sufficiency check to
     * {@link Account#minimumDeposit()} and
     * {@link Account#isDepositSufficient(long)} — no hardcoded amounts here
     * (DIP / LSP).</p>
     *
     * <p>The raw deposit string may contain thousand-separator commas or spaces
     * (e.g. {@code "50,000"}); these are stripped before parsing.</p>
     */
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

    /**
     * Null-safe trim.
     *
     * @param s input string, possibly {@code null}
     * @return trimmed string, or empty string if {@code s} is {@code null}
     */
    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Returns {@code true} if every character in {@code s} is the same digit.
     *
     * <p>Used to reject trivially weak PINs like 0000, 1111, 999999.</p>
     *
     * @param s a non-null, non-empty string
     * @return {@code true} if all characters are identical
     */
    private static boolean isAllIdenticalDigits(String s) {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) return false;
        }
        return true;
    }
}
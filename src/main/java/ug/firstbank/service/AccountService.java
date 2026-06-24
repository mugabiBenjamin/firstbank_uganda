package ug.firstbank.service;

import ug.firstbank.model.Account;
import ug.firstbank.model.AccountRecord;
import ug.firstbank.persistence.AccountRepository;
import ug.firstbank.util.CurrencyFormatter;
import ug.firstbank.util.PasswordUtils;
import ug.firstbank.validation.FormValidator;
import ug.firstbank.validation.ValidationResult;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application-layer coordinator for the account-opening workflow.
 *
 * <p>{@code AccountService} is the single entry point the UI calls.
 * It orchestrates, in order:</p>
 * <ol>
 *   <li>Form validation ({@link FormValidator})</li>
 *   <li>Duplicate-NIN detection ({@link AccountRepository#existsDuplicate})</li>
 *   <li>PIN hashing ({@link PasswordUtils})</li>
 *   <li>Persistence ({@link AccountRepository#save})</li>
 *   <li>Assembly of the final {@link AccountRecord} returned to the UI</li>
 * </ol>
 *
 * <p>The UI layer never calls {@link FormValidator}, {@link PasswordUtils},
 * or {@link AccountRepository} directly — it calls this class only.
 * This boundary keeps the UI thin and the business flow testable in
 * isolation.</p>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — owns workflow coordination only. Validation rules live
 *       in {@link FormValidator}; hashing in {@link PasswordUtils}; SQL in
 *       {@link AccountRepository}. None of those concerns bleeds into this
 *       class.</li>
 *   <li><b>OCP</b> — the submission pipeline is a private method
 *       ({@link #buildAndPersist}); extending it (e.g. sending a confirmation
 *       email after save) means adding one call there, not restructuring the
 *       class.</li>
 *   <li><b>LSP</b> — the {@code account} parameter is typed as the abstract
 *       {@link Account}; any subtype substitutes without branching here.</li>
 *   <li><b>DIP</b> — depends on {@link AccountRepository} injected via
 *       constructor; never instantiates persistence classes itself.</li>
 * </ul>
 *
 * <p>All public methods that interact with the database declare
 * {@link SQLException} so that the UI can surface a meaningful error dialog
 * rather than receiving a silent failure.</p>
 */
public final class AccountService {

    private static final Logger LOG =
            Logger.getLogger(AccountService.class.getName());

    // ── Dependencies (injected) ──────────────────────────────────────────────

    private final AccountRepository repository;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Constructs an {@code AccountService} backed by the given repository.
     *
     * @param repository the account data-access object; must not be {@code null}
     */
    public AccountService(AccountRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("AccountRepository must not be null.");
        }
        this.repository = repository;
    }

    // ── Submission result carrier ─────────────────────────────────────────────

    /**
     * Sealed result type returned by {@link #submitApplication}.
     *
     * <p>The UI pattern-matches on this to decide whether to show the success
     * summary, an inline validation error overlay, or a duplicate warning
     * dialog — without needing boolean flags or checked casts.</p>
     *
     * <p>Using a sealed hierarchy here (Java 17+) keeps all outcome variants
     * in one place and makes exhaustive switch expressions possible in the
     * UI layer.</p>
     */
    public sealed interface SubmissionResult
            permits SubmissionResult.Success,
                    SubmissionResult.ValidationFailure,
                    SubmissionResult.DuplicateWarning,
                    SubmissionResult.PersistenceError {

        /**
         * The application passed all checks and the account was created.
         *
         * @param record the fully populated, persisted {@link AccountRecord}
         */
        record Success(AccountRecord record) implements SubmissionResult {}

        /**
         * One or more form fields failed validation.
         *
         * @param result the {@link ValidationResult} carrying the field→error map
         */
        record ValidationFailure(ValidationResult result) implements SubmissionResult {}

        /**
         * A record with the same NIN + account type + branch already exists.
         * The UI should show a confirmation dialog asking the user whether
         * to proceed anyway or cancel.
         *
         * @param nin         the duplicate NIN
         * @param accountType the duplicate account type
         * @param branch      the duplicate branch
         */
        record DuplicateWarning(String nin,
                                String accountType,
                                String branch) implements SubmissionResult {}

        /**
         * A database error prevented the account from being saved.
         *
         * @param cause the underlying {@link SQLException}
         */
        record PersistenceError(SQLException cause) implements SubmissionResult {}
    }

    // ── Primary workflow ─────────────────────────────────────────────────────

    /**
     * Executes the full account-opening submission pipeline.
     *
     * <p>Called by the UI's Submit button handler. The raw string inputs are
     * passed exactly as the user typed them — trimming and normalisation
     * happen inside {@link FormValidator} and {@link PasswordUtils}.</p>
     *
     * <p>The method never throws — all outcomes are encoded in the returned
     * {@link SubmissionResult}, keeping the UI handler free of try/catch
     * blocks for business logic.</p>
     *
     * @param firstName      raw first name field value
     * @param lastName       raw last name field value
     * @param nin            raw NIN field value
     * @param secondNin      raw second NIN field value (Joint accounts only)
     * @param email          raw email field value
     * @param confirmEmail   raw confirm-email field value
     * @param phone          raw phone field value
     * @param pin            raw PIN field value (never stored — hashed immediately)
     * @param confirmPin     raw confirm-PIN field value
     * @param dob            parsed date of birth from the three combo boxes,
     *                       or {@code null} if unselected
     * @param account        the concrete {@link Account} subtype selected in the UI
     * @param branch         branch display name selected in the UI
     * @param depositText    raw opening deposit field value
     * @param forceIfDuplicate {@code true} when the user has already been warned
     *                         about a duplicate and explicitly confirmed they
     *                         want to proceed; {@code false} on a first attempt
     * @return a {@link SubmissionResult} variant describing the outcome
     */
    public SubmissionResult submitApplication(
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
            String depositText,
            boolean forceIfDuplicate) {

        // ── Step 1: Validate all fields ──────────────────────────────────────
        ValidationResult validation = FormValidator.validate(
                firstName, lastName, nin, secondNin,
                email, confirmEmail, phone,
                pin, confirmPin,
                dob, account, branch, depositText);

        if (validation.hasErrors()) {
            LOG.fine("Submission rejected — validation errors: "
                    + validation.getAllErrors());
            return new SubmissionResult.ValidationFailure(validation);
        }

        // ── Step 2: Duplicate detection ──────────────────────────────────────
        String normNin = nin.trim().toUpperCase();

        if (!forceIfDuplicate) {
            try {
                if (repository.existsDuplicate(normNin, account.displayName(), branch)) {
                    LOG.info("Duplicate detected for NIN=" + normNin
                            + ", type=" + account.displayName()
                            + ", branch=" + branch);
                    return new SubmissionResult.DuplicateWarning(
                            normNin, account.displayName(), branch);
                }
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Duplicate check failed.", e);
                return new SubmissionResult.PersistenceError(e);
            }
        }

        // ── Step 3: Hash PIN (raw PIN never travels past this line) ──────────
        String pinHash;
        try {
            pinHash = PasswordUtils.hash(pin.trim());
        } catch (Exception e) {
            // Should never happen given validated input, but guard defensively.
            LOG.log(Level.SEVERE, "PIN hashing failed.", e);
            return new SubmissionResult.PersistenceError(
                    new SQLException("PIN hashing failed: " + e.getMessage(), e));
        }

        // ── Step 4: Parse deposit (already validated — safe to parse here) ───
        long depositAmount = CurrencyFormatter.parse(depositText);

        // ── Step 5: Build a pre-persist record (no account number yet) ───────
        AccountRecord preRecord = new AccountRecord(
                "PENDING",           // placeholder — replaced after save()
                firstName.trim(),
                lastName.trim(),
                normNin,
                secondNin != null && !secondNin.isBlank()
                        ? secondNin.trim().toUpperCase()
                        : null,
                email.trim(),
                phone.trim(),
                dob,
                account.displayName(),
                branch,
                depositAmount,
                pinHash
        );

        // ── Step 6: Persist and obtain the generated account number ──────────
        try {
            String accountNumber = repository.save(preRecord);

            // Rebuild with the real account number for return to the UI.
            AccountRecord finalRecord = new AccountRecord(
                    accountNumber,
                    preRecord.getFirstName(),
                    preRecord.getLastName(),
                    preRecord.getNin(),
                    preRecord.getSecondNin(),
                    preRecord.getEmail(),
                    preRecord.getPhone(),
                    preRecord.getDateOfBirth(),
                    preRecord.getAccountType(),
                    preRecord.getBranch(),
                    preRecord.getOpeningDeposit(),
                    preRecord.getPinHash()
            );

            LOG.info("Account created successfully: " + accountNumber);
            return new SubmissionResult.Success(finalRecord);

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to persist account record.", e);
            return new SubmissionResult.PersistenceError(e);
        }
    }

    // ── Lookup operations (used by LookupDialog) ─────────────────────────────

    /**
     * Finds an account by its account number.
     *
     * <p>Delegates directly to the repository — no business logic needed
     * for a simple lookup.</p>
     *
     * @param accountNumber the account number to search, e.g. {@code "KLA-2026-000142"}
     * @return {@link Optional} containing the record, or empty if not found
     * @throws SQLException if the query fails
     */
    public Optional<AccountRecord> findByAccountNumber(String accountNumber)
            throws SQLException {
        return repository.findByAccountNumber(accountNumber);
    }

    /**
     * Finds all accounts associated with a given NIN.
     *
     * @param nin the primary NIN (14-char uppercase)
     * @return unmodifiable list of matching records; empty if none found
     * @throws SQLException if the query fails
     */
    public List<AccountRecord> findByNin(String nin) throws SQLException {
        return repository.findByNin(nin.trim().toUpperCase());
    }
}
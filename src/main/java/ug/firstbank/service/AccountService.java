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

public final class AccountService {

    private static final Logger LOG =
            Logger.getLogger(AccountService.class.getName());

    // ── Dependencies (injected) ──────────────────────────────────────────────

    private final AccountRepository repository;

    // ── Constructor ──────────────────────────────────────────────────────────

    public AccountService(AccountRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("AccountRepository must not be null.");
        }
        this.repository = repository;
    }

    // ── Submission result carrier ─────────────────────────────────────────────

    public sealed interface SubmissionResult
            permits SubmissionResult.Success,
                    SubmissionResult.ValidationFailure,
                    SubmissionResult.DuplicateWarning,
                    SubmissionResult.PersistenceError {

        record Success(AccountRecord record) implements SubmissionResult {}

        record ValidationFailure(ValidationResult result) implements SubmissionResult {}

        record DuplicateWarning(String nin,
                                String accountType,
                                String branch) implements SubmissionResult {}

        record PersistenceError(SQLException cause) implements SubmissionResult {}
    }

    // ── Primary workflow ─────────────────────────────────────────────────────

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

    public Optional<AccountRecord> findByAccountNumber(String accountNumber)
            throws SQLException {
        return repository.findByAccountNumber(accountNumber);
    }

    public List<AccountRecord> findByNin(String nin) throws SQLException {
        return repository.findByNin(nin.trim().toUpperCase());
    }
}
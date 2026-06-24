package ug.firstbank.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Immutable data-transfer object (DTO) representing a fully validated,
 * ready-to-persist bank account record.
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — carries data between layers only; contains zero business
 *       logic, zero validation, zero UI code, zero persistence code.</li>
 *   <li><b>ISP</b> — each layer receives exactly this object and reads only
 *       the fields it needs; no fat interface forces callers to depend on
 *       methods they do not use.</li>
 * </ul>
 *
 * <p>Fields are populated by {@code AccountService} after all validation passes
 * and before the persistence call. The UI reads back from this object to render
 * the formatted summary line.</p>
 *
 * <p>{@code pinHash} holds the bcrypt-hashed PIN — the raw PIN never travels
 * beyond {@code AccountService}.</p>
 *
 * <p>{@code secondNin} is {@code null} for all account types except Joint.</p>
 */
public final class AccountRecord {

    // ── Identity ────────────────────────────────────────────────────────────

    /** Generated account number, e.g. {@code KLA-2026-000142}. */
    private final String accountNumber;

    // ── Personal details ────────────────────────────────────────────────────

    private final String firstName;
    private final String lastName;

    /** Primary applicant NIN, 14-char uppercase, CM/CF format. */
    private final String nin;

    /**
     * Spouse / co-owner NIN for Joint accounts; {@code null} otherwise.
     */
    private final String secondNin;

    private final String email;
    private final String phone;
    private final LocalDate dateOfBirth;

    // ── Account details ─────────────────────────────────────────────────────

    /** Display name of the selected account type, e.g. {@code "Savings"}. */
    private final String accountType;

    /** Branch display name, e.g. {@code "Kampala"}. */
    private final String branch;

    /** Opening deposit in UGX. */
    private final long openingDeposit;

    // ── Security ────────────────────────────────────────────────────────────

    /** bcrypt hash of the applicant's PIN — raw PIN is never stored. */
    private final String pinHash;

    // ── Constructor ─────────────────────────────────────────────────────────

    /**
     * Constructs a fully populated {@code AccountRecord}.
     * All parameters are required; {@code secondNin} may be {@code null}.
     *
     * @param accountNumber  generated account number
     * @param firstName      applicant first name (trimmed)
     * @param lastName       applicant last name (trimmed)
     * @param nin            primary NIN (14-char uppercase)
     * @param secondNin      spouse NIN for Joint accounts, or {@code null}
     * @param email          validated email address
     * @param phone          Ugandan phone number in {@code +256XXXXXXXXX} format
     * @param dateOfBirth    applicant date of birth
     * @param accountType    display name of the chosen account type
     * @param branch         display name of the chosen branch
     * @param openingDeposit deposit amount in UGX
     * @param pinHash        bcrypt hash of the applicant's PIN
     */
    public AccountRecord(
            String accountNumber,
            String firstName,
            String lastName,
            String nin,
            String secondNin,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String accountType,
            String branch,
            long openingDeposit,
            String pinHash) {

        this.accountNumber  = Objects.requireNonNull(accountNumber,  "accountNumber");
        this.firstName      = Objects.requireNonNull(firstName,      "firstName");
        this.lastName       = Objects.requireNonNull(lastName,       "lastName");
        this.nin            = Objects.requireNonNull(nin,            "nin");
        this.secondNin      = secondNin; // nullable — only Joint accounts supply this
        this.email          = Objects.requireNonNull(email,          "email");
        this.phone          = Objects.requireNonNull(phone,          "phone");
        this.dateOfBirth    = Objects.requireNonNull(dateOfBirth,    "dateOfBirth");
        this.accountType    = Objects.requireNonNull(accountType,    "accountType");
        this.branch         = Objects.requireNonNull(branch,         "branch");
        this.openingDeposit = openingDeposit;
        this.pinHash        = Objects.requireNonNull(pinHash,        "pinHash");
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    /** @return generated account number */
    public String getAccountNumber()  { return accountNumber;  }

    /** @return applicant first name */
    public String getFirstName()      { return firstName;      }

    /** @return applicant last name */
    public String getLastName()       { return lastName;       }

    /** @return primary NIN */
    public String getNin()            { return nin;            }

    /**
     * Returns the spouse / co-owner NIN, or {@code null} if not applicable.
     *
     * @return second NIN, or {@code null}
     */
    public String getSecondNin()      { return secondNin;      }

    /** @return validated email address */
    public String getEmail()          { return email;          }

    /** @return phone number in {@code +256XXXXXXXXX} format */
    public String getPhone()          { return phone;          }

    /** @return applicant date of birth */
    public LocalDate getDateOfBirth() { return dateOfBirth;    }

    /** @return account type display name */
    public String getAccountType()    { return accountType;    }

    /** @return branch display name */
    public String getBranch()         { return branch;         }

    /** @return opening deposit in UGX */
    public long getOpeningDeposit()   { return openingDeposit; }

    /** @return bcrypt hash of the applicant's PIN */
    public String getPinHash()        { return pinHash;        }

    // ── Formatted summary ────────────────────────────────────────────────────

    /**
     * Returns the single-line formatted summary as shown in the UI and DB.
     *
     * <p>Example output:</p>
     * <pre>
     * ACC: KLA-2026-000142 | Okello Allan | Savings | Kampala |
     * DOB 2004-02-29 | +256772123456 | Deposit 50,000 | okello.allan@firstbank.co.ug
     * </pre>
     *
     * <p>Currency formatting (thousand separators) is delegated to
     * {@code CurrencyFormatter} in the {@code util} package — but since
     * {@code AccountRecord} must not depend on utility classes to stay a
     * pure DTO, the summary here uses {@link String#format} with {@code %,d}
     * for simplicity. The UI layer may re-format as needed.</p>
     *
     * @return formatted one-line account summary
     */
    public String toSummaryLine() {
        return String.format(
                "ACC: %s | %s %s | %s | %s | DOB %s | %s | Deposit %,d | %s",
                accountNumber,
                firstName, lastName,
                accountType,
                branch,
                dateOfBirth,
                phone,
                openingDeposit,
                email
        );
    }

    @Override
    public String toString() {
        return toSummaryLine();
    }
}
package ug.firstbank.model;

import java.time.LocalDate;
import java.util.Objects;

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

    public String getAccountNumber()  { return accountNumber;  }

    public String getFirstName()      { return firstName;      }

    public String getLastName()       { return lastName;       }

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

    public LocalDate getDateOfBirth() { return dateOfBirth;    }

    public String getAccountType()    { return accountType;    }

    /** @return branch display name */
    public String getBranch()         { return branch;         }

    public long getOpeningDeposit()   { return openingDeposit; }

    /** @return bcrypt hash of the applicant's PIN */
    public String getPinHash()        { return pinHash;        }

    // ── Formatted summary ────────────────────────────────────────────────────

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
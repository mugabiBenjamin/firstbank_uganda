package ug.firstbank.model;

/**
 * Concrete account type: <b>Student</b>.
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>Minimum opening deposit: UGX 10,000</li>
 *   <li>Earns interest: yes</li>
 *   <li>Overdraft allowed: no</li>
 *   <li>Age eligibility: <b>18–25 only</b> (stricter than the general 18–75 rule)</li>
 * </ul>
 *
 * <p>The age constants {@link #MIN_AGE} and {@link #MAX_AGE} are exposed as
 * {@code public static final} so that {@code FormValidator} can reference them
 * directly without hardcoding magic numbers — keeping the business rule
 * co-located with the type that owns it (SRP).</p>
 */
public final class StudentAccount extends Account {

    /** Minimum opening deposit for a Student account, in UGX. */
    public static final long MINIMUM_DEPOSIT = 10_000L;

    /** Minimum applicant age for a Student account (inclusive). */
    public static final int MIN_AGE = 18;

    /** Maximum applicant age for a Student account (inclusive). */
    public static final int MAX_AGE = 25;

    @Override
    public long minimumDeposit() {
        return MINIMUM_DEPOSIT;
    }

    @Override
    public String displayName() {
        return "Student";
    }

    @Override
    public boolean allowsOverdraft() {
        return false;
    }

    @Override
    public boolean earnsInterest() {
        return true;
    }

    /**
     * Returns whether the given age satisfies the Student account eligibility rule.
     *
     * <p>This method exists on {@code StudentAccount} specifically — not on the
     * base {@code Account} class — because age restriction is a Student-only
     * concern (ISP / SRP).</p>
     *
     * @param age the applicant's age in whole years
     * @return {@code true} if {@code age} is between {@link #MIN_AGE} and
     *         {@link #MAX_AGE} inclusive
     */
    public boolean isAgeEligible(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }
}
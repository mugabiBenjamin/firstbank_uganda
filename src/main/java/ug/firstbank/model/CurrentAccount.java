package ug.firstbank.model;

/**
 * Concrete account type: <b>Current</b>.
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>Minimum opening deposit: UGX 200,000</li>
 *   <li>Earns interest: no</li>
 *   <li>Overdraft allowed: yes</li>
 *   <li>Age eligibility: general 18–75 (no special restriction)</li>
 * </ul>
 */
public final class CurrentAccount extends Account {

    /** Minimum opening deposit for a Current account, in UGX. */
    public static final long MINIMUM_DEPOSIT = 200_000L;

    @Override
    public long minimumDeposit() {
        return MINIMUM_DEPOSIT;
    }

    @Override
    public String displayName() {
        return "Current";
    }

    @Override
    public boolean allowsOverdraft() {
        return true;
    }

    @Override
    public boolean earnsInterest() {
        return false;
    }
}
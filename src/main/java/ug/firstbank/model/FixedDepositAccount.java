package ug.firstbank.model;

/**
 * Concrete account type: <b>Fixed Deposit</b>.
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>Minimum opening deposit: UGX 1,000,000</li>
 *   <li>Earns interest: yes (highest rate)</li>
 *   <li>Overdraft allowed: no (funds are locked for the term)</li>
 *   <li>Age eligibility: general 18–75 (no special restriction)</li>
 * </ul>
 */
public final class FixedDepositAccount extends Account {

    /** Minimum opening deposit for a Fixed Deposit account, in UGX. */
    public static final long MINIMUM_DEPOSIT = 1_000_000L;

    @Override
    public long minimumDeposit() {
        return MINIMUM_DEPOSIT;
    }

    @Override
    public String displayName() {
        return "Fixed Deposit";
    }

    @Override
    public boolean allowsOverdraft() {
        return false;
    }

    @Override
    public boolean earnsInterest() {
        return true;
    }
}
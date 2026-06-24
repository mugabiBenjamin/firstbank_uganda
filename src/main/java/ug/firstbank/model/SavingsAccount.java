package ug.firstbank.model;

/**
 * Concrete account type: <b>Savings</b>.
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>Minimum opening deposit: UGX 50,000</li>
 *   <li>Earns interest: yes</li>
 *   <li>Overdraft allowed: no</li>
 *   <li>Age eligibility: general 18–75 (no special restriction)</li>
 * </ul>
 *
 * <p><b>SOLID:</b> Single subclass per account type (SRP). Adding a rule
 * specific to Savings never requires touching any sibling subclass (OCP).</p>
 */
public final class SavingsAccount extends Account {

    /** Minimum opening deposit for a Savings account, in UGX. */
    public static final long MINIMUM_DEPOSIT = 50_000L;

    @Override
    public long minimumDeposit() {
        return MINIMUM_DEPOSIT;
    }

    @Override
    public String displayName() {
        return "Savings";
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
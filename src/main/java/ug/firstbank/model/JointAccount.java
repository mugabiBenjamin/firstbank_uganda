package ug.firstbank.model;

/**
 * Concrete account type: <b>Joint</b>.
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>Minimum opening deposit: UGX 100,000</li>
 *   <li>Earns interest: yes</li>
 *   <li>Overdraft allowed: no</li>
 *   <li>Requires a second NIN (spouse / co-owner) — the only account type
 *       for which the second NIN field is mandatory.</li>
 *   <li>Second NIN must pass the same CM/CF format validation as the primary
 *       NIN and must differ from the primary NIN.</li>
 *   <li>Age eligibility: general 18–75 (no special restriction).</li>
 * </ul>
 *
 * <p>The {@link #requiresSecondNin()} method gives the UI and validator a
 * single, authoritative place to ask "does this account type need a second
 * NIN?" — no {@code instanceof} checks or string comparisons needed anywhere
 * else (LSP / OCP).</p>
 */
public final class JointAccount extends Account {

    /** Minimum opening deposit for a Joint account, in UGX. */
    public static final long MINIMUM_DEPOSIT = 100_000L;

    @Override
    public long minimumDeposit() {
        return MINIMUM_DEPOSIT;
    }

    @Override
    public String displayName() {
        return "Joint";
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
     * Indicates that this account type mandates a second NIN.
     *
     * <p>All other {@code Account} subtypes implicitly return {@code false}
     * for this concern — they simply do not override this method, because
     * second-NIN logic is not their responsibility.</p>
     *
     * @return {@code true} always, for Joint accounts
     */
    public boolean requiresSecondNin() {
        return true;
    }
}
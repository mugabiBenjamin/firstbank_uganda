package ug.firstbank.model;

/**
 * Abstract base class representing a bank account type at First Bank Uganda.
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — this class owns exactly one concern: defining the contract
 *       that every account type must fulfil (minimum deposit, display name,
 *       overdraft eligibility, interest eligibility).</li>
 *   <li><b>OCP</b> — closed for modification; open for extension. Adding a new
 *       account type means creating a new subclass, not editing this file.</li>
 *   <li><b>LSP</b> — every concrete subclass can substitute for {@code Account}
 *       without breaking the validation logic in {@code FormValidator}.</li>
 * </ul>
 *
 * <p>No JavaFX or persistence imports belong here — this is pure domain model.</p>
 */
public abstract class Account {

    /**
     * Returns the minimum opening deposit required for this account type, in UGX.
     *
     * @return minimum deposit amount (positive integer, UGX)
     */
    public abstract long minimumDeposit();

    /**
     * Returns the human-readable display name of this account type,
     * exactly as it appears in the UI combo box.
     *
     * @return display name, e.g. {@code "Savings"}
     */
    public abstract String displayName();

    /**
     * Indicates whether this account type permits overdraft withdrawals.
     *
     * @return {@code true} if overdraft is allowed
     */
    public abstract boolean allowsOverdraft();

    /**
     * Indicates whether this account type earns interest.
     *
     * @return {@code true} if interest is earned
     */
    public abstract boolean earnsInterest();

    /**
     * Validates the proposed opening deposit against this account type's minimum.
     *
     * <p>Centralising this check here (rather than scattering it across the UI)
     * ensures every call site uses the same rule — a direct application of DRY.</p>
     *
     * @param depositAmount the amount the applicant intends to deposit, in UGX
     * @return {@code true} if the deposit satisfies the minimum requirement
     */
    public boolean isDepositSufficient(long depositAmount) {
        return depositAmount >= minimumDeposit();
    }

    /**
     * Returns a short summary string used in log/debug output.
     *
     * @return e.g. {@code "SavingsAccount[min=50000]"}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[min=" + minimumDeposit() + "]";
    }
}
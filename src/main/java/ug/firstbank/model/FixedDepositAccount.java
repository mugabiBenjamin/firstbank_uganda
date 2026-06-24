package ug.firstbank.model;

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
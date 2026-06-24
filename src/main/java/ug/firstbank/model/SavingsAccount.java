package ug.firstbank.model;

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
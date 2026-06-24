package ug.firstbank.model;

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
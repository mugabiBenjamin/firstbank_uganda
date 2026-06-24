package ug.firstbank.model;

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

    public boolean requiresSecondNin() {
        return true;
    }
}
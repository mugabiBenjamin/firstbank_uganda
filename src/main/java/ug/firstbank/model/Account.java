package ug.firstbank.model;

public abstract class Account {

    public abstract long minimumDeposit();

    public abstract String displayName();

    public abstract boolean allowsOverdraft();

    public abstract boolean earnsInterest();

    public boolean isDepositSufficient(long depositAmount) {
        return depositAmount >= minimumDeposit();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[min=" + minimumDeposit() + "]";
    }
}
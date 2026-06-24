package ug.firstbank.model;

public final class StudentAccount extends Account {

    /** Minimum opening deposit for a Student account, in UGX. */
    public static final long MINIMUM_DEPOSIT = 10_000L;

    /** Minimum applicant age for a Student account (inclusive). */
    public static final int MIN_AGE = 18;

    /** Maximum applicant age for a Student account (inclusive). */
    public static final int MAX_AGE = 25;

    @Override
    public long minimumDeposit() {
        return MINIMUM_DEPOSIT;
    }

    @Override
    public String displayName() {
        return "Student";
    }

    @Override
    public boolean allowsOverdraft() {
        return false;
    }

    @Override
    public boolean earnsInterest() {
        return true;
    }

    public boolean isAgeEligible(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }
}
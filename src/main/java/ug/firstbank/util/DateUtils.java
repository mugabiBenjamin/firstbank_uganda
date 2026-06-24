package ug.firstbank.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.Year;

/**
 * Stateless utility for date-related operations used by the account-opening form.
 *
 * <p>Covers three distinct sub-concerns, each in its own clearly labelled
 * section:</p>
 * <ol>
 *   <li>Leap-year detection</li>
 *   <li>Days-in-month calculation (drives the Day combo box auto-update)</li>
 *   <li>Age calculation from date of birth</li>
 *   <li>Year-range generation for the Year combo box</li>
 * </ol>
 *
 * <p><b>SOLID notes:</b></p>
 * <ul>
 *   <li><b>SRP</b> — date arithmetic only; no UI, no DB, no validation logic.</li>
 *   <li><b>OCP</b> — new date helpers are added as new methods without altering
 *       existing ones.</li>
 * </ul>
 *
 * <p>Delegates to {@link java.time} (JSR-310) throughout — no legacy
 * {@code java.util.Date} or {@code Calendar} usage.</p>
 *
 * <p>All methods are {@code static}; the class is not instantiable.</p>
 */
public final class DateUtils {

    // ── Age bounds mirrored from FormValidator for UI use ────────────────────

    /**
     * The earliest birth year selectable in the Year combo box.
     * Derived from the maximum allowed age (75) relative to the current year.
     */
    public static int earliestBirthYear() {
        return LocalDate.now().getYear() - 75;
    }

    /**
     * The latest birth year selectable in the Year combo box.
     * Derived from the minimum allowed age (18) relative to the current year.
     */
    public static int latestBirthYear() {
        return LocalDate.now().getYear() - 18;
    }

    private DateUtils() {}

    // ── 1. Leap-year detection ───────────────────────────────────────────────

    /**
     * Returns {@code true} if {@code year} is a leap year.
     *
     * <p>Delegates to {@link Year#isLeap(long)} which applies the full
     * Gregorian rule (divisible by 4, except centuries unless also by 400).</p>
     *
     * @param year the calendar year to test
     * @return {@code true} for leap years
     */
    public static boolean isLeapYear(int year) {
        return Year.isLeap(year);
    }

    // ── 2. Days-in-month calculation ─────────────────────────────────────────

    /**
     * Returns the number of days in {@code month} for the given {@code year},
     * correctly accounting for leap years.
     *
     * <p>Used by the UI to rebuild the Day combo box whenever the Month or
     * Year selection changes:</p>
     * <ul>
     *   <li>February in a leap year → 29</li>
     *   <li>February in a non-leap year → 28</li>
     *   <li>All other months → standard Gregorian day count</li>
     * </ul>
     *
     * @param month 1-based month number (1 = January … 12 = December)
     * @param year  the calendar year (needed to resolve February length)
     * @return number of days in the month, between 28 and 31 inclusive
     * @throws java.time.DateTimeException if {@code month} is outside 1–12
     */
    public static int daysInMonth(int month, int year) {
        return Month.of(month).length(isLeapYear(year));
    }

    /**
     * Returns the number of days in {@code month} assuming a non-leap year.
     *
     * <p>Use this overload when the year is not yet selected in the UI
     * (Year combo box still on placeholder), so the Day combo can still
     * populate with a sensible conservative count.</p>
     *
     * @param month 1-based month number
     * @return number of days assuming a non-leap year
     */
    public static int daysInMonth(int month) {
        return Month.of(month).length(false);
    }

    // ── 3. Age calculation ───────────────────────────────────────────────────

    /**
     * Calculates the applicant's age in whole years as of today.
     *
     * <p>Uses {@link Period#between(LocalDate, LocalDate)} which correctly
     * handles birthday-not-yet-reached-this-year edge cases.</p>
     *
     * @param dob the applicant's date of birth; must not be {@code null}
     * @return age in whole completed years
     */
    public static int ageInYears(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }

    /**
     * Attempts to build a {@link LocalDate} from the three combo box selections.
     *
     * <p>Returns {@code null} if any argument is {@code null} or {@code ≤ 0},
     * or if the combination is invalid (e.g. day 31 in a 30-day month — which
     * the UI prevents by rebuilding the Day combo, but this provides a
     * defensive fallback).</p>
     *
     * @param year  selected year (e.g. 2004), or {@code null} if unselected
     * @param month selected month 1-based (e.g. 2 for February), or {@code null}
     * @param day   selected day (e.g. 29), or {@code null}
     * @return a valid {@link LocalDate}, or {@code null} on invalid input
     */
    public static LocalDate toLocalDate(Integer year, Integer month, Integer day) {
        if (year == null || month == null || day == null) return null;
        if (year <= 0 || month <= 0 || day <= 0)         return null;
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null; // invalid combination — UI should have prevented this
        }
    }

    // ── 4. Year-range generation ─────────────────────────────────────────────

    /**
     * Returns an array of years to populate the Year combo box, in descending
     * order (most recent first, so the youngest eligible applicant's birth
     * year appears at the top).
     *
     * <p>Range: {@link #earliestBirthYear()} to {@link #latestBirthYear()}
     * inclusive.</p>
     *
     * @return descending array of eligible birth years
     */
    public static int[] birthYearRange() {
        int first = earliestBirthYear();
        int last  = latestBirthYear();
        int count = last - first + 1;
        int[] years = new int[count];
        for (int i = 0; i < count; i++) {
            years[i] = last - i; // descending
        }
        return years;
    }
}
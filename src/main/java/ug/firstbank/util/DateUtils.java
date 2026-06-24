package ug.firstbank.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.Year;

public final class DateUtils {

    // ── Age bounds mirrored from FormValidator for UI use ────────────────────

    public static int earliestBirthYear() {
        return LocalDate.now().getYear() - 75;
    }

    public static int latestBirthYear() {
        return LocalDate.now().getYear() - 18;
    }

    private DateUtils() {}

    // ── 1. Leap-year detection ───────────────────────────────────────────────

    public static boolean isLeapYear(int year) {
        return Year.isLeap(year);
    }

    // ── 2. Days-in-month calculation ─────────────────────────────────────────

    public static int daysInMonth(int month, int year) {
        return Month.of(month).length(isLeapYear(year));
    }

    public static int daysInMonth(int month) {
        return Month.of(month).length(false);
    }

    // ── 3. Age calculation ───────────────────────────────────────────────────

    public static int ageInYears(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }

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
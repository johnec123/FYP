package com.example.fyp_git;

import java.util.Calendar;

/**
 * A utility class that is used in both the handset and wearable apps.
 */
public class Utils {

    /**
     * Builds a simple hash for a day by concatenating year and day of year together. Note that two
     * {@link java.util.Calendar} inputs that fall on the same day will be hashed to the same
     * string.
     */
    public static String getHashedDay(Calendar day) {
        return day.get(Calendar.YEAR) + "-" + day.get(Calendar.DAY_OF_YEAR);
    }

    private Utils() {
    }
}

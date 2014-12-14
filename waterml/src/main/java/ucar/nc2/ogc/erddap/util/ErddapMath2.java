/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package ucar.nc2.ogc.erddap.util;

import java.text.MessageFormat;

/**
 * The Math2 class has several static Math-related methods.
 * <UL>
 * <LI> These are low level routines used by most other CoHort classes.
 * <LI> Many provide additional protection from run-time errors.
 * </UL>
 */
public class ErddapMath2 {
    /**
     * These are *not* final so EDStatic can replace them with translated Strings.
     * These are MessageFormat-style strings, so any single quote ' must be escaped as ''.
     */
    public static String memoryTooMuchData =
        "Your query produced too much data.  Try to request less data.";
    public static String memoryArraySize =
        "The request needs an array size ({0}) bigger than Java ever allows ({1}).";

    /**
     * Checks if the value is not NaN or +-Infinite.   
     * This works for floats.
     *
     * @param d any double value or float value
     * @return true if d is is not NaN or +/-infinity.
     */
    public static boolean isFinite(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d);
    }

    /** 
     * Even if JavaBits is 64, the limit on an array size is Integer.MAX_VALUE.
     * 
     * <p>This is almost identical to EDStatic.ensureArraySizeOkay, but lacks tallying.
     * 
     * @param tSize
     * @param attributeTo for a WARNING or ERROR message, this is the string 
     *   to which this not-enough-memory issue should be attributed.
     */
    public static void ensureArraySizeOkay(long tSize, String attributeTo) { 
        if (tSize >= Integer.MAX_VALUE) 
            throw new RuntimeException(memoryTooMuchData + "  " +
                MessageFormat.format(memoryArraySize, "" + tSize, "" + Integer.MAX_VALUE) +
                (attributeTo == null || attributeTo.length() == 0? "" : " (" + attributeTo + ")"));
    }

    /**
     * Safely rounds a double to an int.
     * (Math.round but rounds to a long and not safely.)
     * 
     * @param d any double
     * @return Integer.MAX_VALUE if d is too small, too big, or NaN;
     *   otherwise d, rounded to the nearest int.
     *   Undesirable: d.5 rounds up for positive numbers, down for negative.
     */
    public static int roundToInt(double d) {
        return d > Integer.MAX_VALUE || d <= Integer.MIN_VALUE - 0.5 || !isFinite(d)? 
            Integer.MAX_VALUE : 
            (int)Math.round(d); //safe since checked for larger values above
    }
}

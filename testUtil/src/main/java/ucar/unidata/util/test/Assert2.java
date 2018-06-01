package ucar.unidata.util.test;

import org.junit.Assert;
import org.junit.internal.ComparisonCriteria;
import ucar.nc2.util.Misc;

/**
 * Custom JUnit 4 assertions.
 *
 * @author cwardgar
 * @since 2018-05-13
 */
// TODO: Create "assertNearlyEquals(Array, Array)".
public class Assert2 {
    /**
     * Returns the result of {@link #assertNearlyEquals(float, float, float)}, with
     * {@link Misc#defaultMaxRelativeDiffFloat}.
     */
    public static void assertNearlyEquals(float expected, float actual) {
        assertNearlyEquals(expected, actual, Misc.defaultMaxRelativeDiffFloat);
    }
    
    /**
     * Asserts that two floats are {@link Misc#nearlyEquals(float, float, float) nearly equal}. If they are not,
     * an {@link AssertionError} is thrown.
     *
     * @param expected    expected value
     * @param actual      the value to check against {@code expected}
     * @param maxRelDiff  the maximum relative difference the two numbers may have and still be considered equal.
     */
    public static void assertNearlyEquals(float expected, float actual, float maxRelDiff) {
        if (!Misc.nearlyEquals(expected, actual, maxRelDiff)) {
            failNotEquals(null, Float.valueOf(expected), Float.valueOf(actual));
        }
    }
    
    /**
     * Returns the result of {@link #assertNearlyEquals(double, double, double)}, with
     * {@link Misc#defaultMaxRelativeDiffDouble}.
     */
    public static void assertNearlyEquals(double expected, double actual) {
        assertNearlyEquals(expected, actual, Misc.defaultMaxRelativeDiffDouble);
    }
    
    /** Same as {@link #assertNearlyEquals(float, float, float)}, but for doubles. */
    public static void assertNearlyEquals(double expected, double actual, double maxRelDiff) {
        if (!Misc.nearlyEquals(expected, actual, maxRelDiff)) {
            failNotEquals(null, Double.valueOf(expected), Double.valueOf(actual));
        }
    }
    
    
    /**
     * Returns the result of {@link #assertArrayNearlyEquals(float[], float[], float)}, with
     * {@link Misc#defaultMaxRelativeDiffFloat}.
     */
    public static void assertArrayNearlyEquals(float[] expecteds, float[] actuals) {
        assertArrayNearlyEquals(expecteds, actuals, Misc.defaultMaxRelativeDiffFloat);
    }
    
    /**
     * Asserts that two float arrays are nearly equal by comparing analogous elements in the two arrays with
     * {@link Misc#nearlyEquals(float, float, float)}. If they are not, an {@link AssertionError} is thrown.
     *
     * @param expecteds float array with expected values.
     * @param actuals float array with actual values
     * @param maxRelDiff  the maximum relative difference that analogous array elements may have and still be
     *                    considered equal.
     */
    public static void assertArrayNearlyEquals(float[] expecteds, float[] actuals, float maxRelDiff) {
        new NearlyEqualsComparisonCriteria(maxRelDiff).arrayEquals(null, expecteds, actuals);
    }
    
    /**
     * Returns the result of {@link #assertArrayNearlyEquals(double[], double[], double)}, with
     * {@link Misc#defaultMaxRelativeDiffDouble}.
     */
    public static void assertArrayNearlyEquals(double[] expecteds, double[] actuals) {
        assertArrayNearlyEquals(expecteds, actuals, Misc.defaultMaxRelativeDiffDouble);
    }
    
    /** Same as {@link #assertArrayNearlyEquals(float[], float[], float)}, but for doubles. */
    public static void assertArrayNearlyEquals(double[] expecteds, double[] actuals, double maxRelDiff) {
        new NearlyEqualsComparisonCriteria(maxRelDiff).arrayEquals(null, expecteds, actuals);
    }
    
    
    private static class NearlyEqualsComparisonCriteria extends ComparisonCriteria {
        public Object maxRelDiff;
        
        public NearlyEqualsComparisonCriteria(double maxRelDiff) {
            this.maxRelDiff = maxRelDiff;
        }
        
        public NearlyEqualsComparisonCriteria(float maxRelDiff) { this.maxRelDiff = maxRelDiff; }
        
        @Override
        protected void assertElementsEqual(Object expected, Object actual) {
            if (expected instanceof Double) {
                assertNearlyEquals((Double) expected, (Double) actual, (Double) maxRelDiff);
            } else {
                assertNearlyEquals((Float) expected, (Float) actual, (Float) maxRelDiff);
            }
        }
    }
    
    // The following methods come from the JUnit project, specifically the org.junit.Assert class.
    // JUnit is licensed under Eclipse Public License 1.0. A copy of the license can be found at
    // docs/src/private/licenses/third-party/junit/LICENSE-junit.txt
    
    static private void failNotEquals(String message, Object expected, Object actual) {
        Assert.fail(format(message, expected, actual));
    }
    
    static String format(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null && !message.equals("")) {
            formatted = message + " ";
        }
        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        if (expectedString.equals(actualString)) {
            return formatted + "expected: "
                    + formatClassAndValue(expected, expectedString)
                    + " but was: " + formatClassAndValue(actual, actualString);
        } else {
            return formatted + "expected:<" + expectedString + "> but was:<" + actualString + ">";
        }
    }
    
    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass().getName();
        return className + "<" + valueString + ">";
    }
    
    
    /** Private constructor to ensure non-instantiability. */
    private Assert2() { }
}

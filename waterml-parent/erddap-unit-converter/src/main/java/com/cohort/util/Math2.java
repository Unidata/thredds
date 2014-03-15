/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import java.text.MessageFormat;
import java.util.Arrays;

/**
 * The Math2 class has several static Math-related methods.
 * <UL>
 * <LI> These are low level routines used by most other CoHort classes.
 * <LI> Many provide additional protection from run-time errors.
 * </UL>
 */
public class Math2 {

    public static final double LittleDouble2 = 1e-150;

    /**
     * Eps values define small values that are suitable for quick tests
     * if the difference between two values is close to the precision of
     * of the data type.
     */
    public static final float fEps = 1e-5f; //epsilon suitable for floats
    public static final double dEps = 1e-13; //epsilon suitable for doubles
    public static final double OneRadian = 180.0 / Math.PI; //are these double precision?
    public static final double TwoPi = 2 * Math.PI; //are these double precision?
    public static final double ln10 = Math.log(10.0); //2.302585092994046;
    public static final double ln2 = Math.log(2.0);
    public static final int Binary0 = -2000; //less than -980-980
    public static final int BinaryLimit = 980; //2^980 = ~1e295
    public static final int BytesPerKB = 1024;
    public static final int BytesPerMB = BytesPerKB * BytesPerKB;
    public static final long BytesPerGB = BytesPerMB * (long)BytesPerKB;
    public static final long BytesPerTB = BytesPerGB * BytesPerKB;
    public static final long BytesPerPB = BytesPerTB * BytesPerKB;
    public static final long loAnd = ((long) Integer.MAX_VALUE * 2) + 1; //mask for low 32 bits
    public static java.util.Random random = new java.util.Random();
    public volatile static long lastUsingMemory = 0; //volatile: used by all threads
    public volatile static long maxUsingMemory = 0; //volatile: used by all threads
    public static long maxMemory = Runtime.getRuntime().maxMemory(); 
    /** This is the most memory any method should consider using
     * (and anything remotely close to this is considered dangerous
     * and should be avoided).
     */
    public static long maxSafeMemory = Math.max(
        maxMemory - 500L * BytesPerMB,  //if maxMemory>2GB, just keep 500MB aside for safety
        maxMemory / 4 * 3); //operator order avoids numeric overflow 

    /** 
     * These are *not* final so EDStatic can replace them with translated Strings. 
     * These are MessageFormat-style strings, so any single quote ' must be escaped as ''. 
     */
    public static String memoryTooMuchData = 
        "Your query produced too much data.  Try to request less data.";
    public static String memoryThanCurrentlySafe = "The request needs more memory ({0} MB) " +
        "than is currently available ({1} MB).";
    public static String memoryThanSafe = "The request needs more memory ({0} MB) " +
        "than is ever safely available in this Java setup ({1} MB).";
    public static String memoryArraySize = 
        "The request needs an array size ({0}) bigger than Java ever allows ({1}).";


    /** If memory use jumps by this amount, a call to incgc will trigger a call
     * to System.gc.
     */
    public static long gcTrigger = maxMemory / 16;


    /** This should return "?", "32", or "64". */
    public static String JavaBits = 
        System.getProperty("sun.arch.data.model") == null? "?" :
        System.getProperty("sun.arch.data.model");

    /** 
     * udunits says: 
     *  US_survey_footS (1200/3937) meter# exact
     *  US_survey_feetS US_survey_foot# alias
     *  US_survey_mileP 5280 US_survey_feet# exact
     *  US_statute_mileP US_survey_mile# alias
     *  so convert  nMiles * 5280 ft/mile * 1200/3927 m/ft * .001 km/m -> km
     */
    public final static double meterPerFoot = 1200.0 / 3927.0;
    public final static double mPerMile = 5280 * meterPerFoot;
    public final static double kmPerMile = mPerMile * 0.001;

    /**
     *<TT>two</TT> defines powers of two,
     * e.g., Two[0]=1, Two[1]=2, Two[2]=4, ... Two[31].
     */
    public static final int[] Two = {
        0x1, 0x2, 0x4, 0x8, 0x10, 0x20, 0x40, 0x80, 0x100, 0x200, 0x400, 0x800,
        0x1000, 0x2000, 0x4000, 0x8000, 0x10000, 0x20000, 0x40000, 0x80000,
        0x100000, 0x200000, 0x400000, 0x800000, 0x1000000, 0x2000000, 0x4000000,
        0x8000000, 0x10000000, 0x20000000, 0x40000000, 0x80000000
    };

    /**
     * This defines powers of ten.
     * e.g., Ten[0]=1, Ten[1]=10, Ten[2]=100...  Ten[18]
     */
    public static final double[] Ten = {
        1.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0, 10000000.0,
        100000000.0, 1000000000.0, 10000000000.0, 100000000000.0,
        1000000000000.0, 10000000000000.0, 100000000000000.0, 1000000000000000.0,
        10000000000000000.0, 100000000000000000.0, 1000000000000000000.0
    };
    /**
     * This defines inverse powers of ten.
     * e.g., InverseTen[0]=1, InverseTen[1]=.01, InverseTen[2]=.001...  InverseTen[18]
     */
    public static final double[] InverseTen = {
        1.0, 0.1, 0.01, 0.001, .0001, .00001, .000001,
        .0000001, .00000001, .000000001, .0000000001,
        .00000000001, .000000000001, .0000000000001, .00000000000001,
        .000000000000001, .0000000000000001, .00000000000000001,
        .000000000000000001};

    private static final int[] niceNumbers =  {
        -110, -100, -90, -80, -70, -60, -50, -45, -40, -35, -30, -25, -22, -20,
        -18, -16, -14, -12, -11, -10, -9, 9, 10, 11, 12, 14, 16, 18, 20, 22, 25,
        30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 110
    };



    /**
     * This returns the log base 10 of d.
     * 
     * @param d a double value
     * @return the log base 10 of d.
     *     d=0 returns Double.NEGATIVE_INFINITY. 
     *     d<0 returns NaN.
     *     d=Double.POSITIVE_INFINITY returns Double.POSITIVE_INFINITY. 
     *     d=NaN returns NaN. 
     */
    public static double log10(double d) {
        return Math.log(d) / ln10; 
    }

    /**
     * This returns the truncated part of a double.
     * 
     * @param d a double value
     * @return d truncated. The return value will be the same sign as d.
     *    !isFinite(d), this returns NaN.
     *    d=trunc(d)+frac(d);
     */
    public static double trunc(double d) {
        return (d < 0) ? Math.ceil(d) : Math.floor(d);
    }

    /**
     * This returns the truncated part of a double, stored as an int.
     *
     * @param d any double
     * @return the truncated version of d (or NaN if trouble)
     * @see Math2#trunc
     */
    public static int truncToInt(double d) {
        return roundToInt(trunc(d));
    }

    /**
     * This returns the fraction part of a double.
     * The return value will be the same sign as d.
     * d=trunc(d)+frac(d);
     * 
     * @param d a double
     * @return the fractional part of d
     */
    public static double frac(double d) {
        return d - trunc(d);
    }


    /**
     * This returns 1 for positive i's, -1 for negative i's, 
     * and 1 if i is 0 (i.e., 0 is treated as a positive number).
     *
     * @param i an int
     * @return 1 for positive i's, -1 for negative i's, 
     * and 1 if i is 0 (i.e., 0 is treated as a positive number).
     * 
     */
    public static int sign1(int i) {
        return (i < 0) ? -1 : 1;
    }

    /**
     * This returns the integer exponent of a double (-0.0175 returns -2
     * since -0.0175=-1.75*10^-2).
     * It handles 0, +, and - numbers.
     * 0 and NaN returns Integer.MAX_VALUE.
     * WARNING: round off problems cause 100 -> 10 *10^1, not 1*10^2!
     *
     * <p>See the similar String2.toRational()
     * 
     * @param d a double value
     * @return the integer exponent of the number. 
     *     If !isFinite(d), this returns Integer.MAX_VALUE
     *     d=0 returns 0.
     */
    public static int intExponent(double d) {
        if (!isFinite(d))
            return Integer.MAX_VALUE;
        if (d == 0)
            return 0;

        return (int)Math.floor(log10(Math.abs(d))); //safe since the exponent of a finite double is +/-308.
    }

    /**
     * This returns the double exponent of a double (e.g., -0.0175 returns 0.01
     * since -0.0175=-1.75*0.01).
     * It handles 0, +, and - numbers.
     * WARNING: round off problems cause 100 -> 10 *10^1, not 1*10^2!
     * 
     * @param d a double value
     * @return the exponent of the number. d=0 returns 1.
     *   If !isFinite(d), this returns NaN.
     */
    public static double exponent(double d) {
        int iExp = intExponent(d);
        return iExp == Integer.MAX_VALUE? Double.NaN: Math.pow(10, iExp); 
    }

    /**
     * This returns the mantissa of a double (-0.0175 returns -1.75
     * since -0.0175=-1.75*10^-2).
     * It handles 0, +, and - numbers.
     * WARNING: round off problems can cause (for example) 100 to be treated 
     *   10 *10^1, not 1*10^2!
     *
     * <p>See the similar String2.toRational()
     * 
     * @param d any double
     * @return d / exponent(d)   (or 0 if d=0, or NaN if !finite(d))
     */
    public static double mantissa(double d) {
        if (d == 0)
            return 0;

        if (!isFinite(d))
            return Double.NaN;

        return d / exponent(d);
    }


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
     * Checks if the value is NaN or infinite:
     *  returns Double.NaN if so; otherwise returns the original value.
     * 
     * @param d and double value
     * @return d (or NaN if !isFinite(d))
     */
    public static double NaNCheck(double d) {
        return !isFinite(d) ? Double.NaN : d;
    }



    /**
     * Asks this thread to sleep for a specified number of milliseconds.
     * When millis>=500, most callers call incgc() instead
     * (so time is used by garbage collector).
     *
     * @param millis the number of milliseconds for the thread to pause
     */
    public static void sleep(long millis) {
        try { 

            if (millis <= 0) {
                //this is a logical place to yield (if not going to sleep)
                //Yield lets other threads have time and may run incgc 
                //  whether -Xincgc is on command line or not.
                Thread.yield();
                return;
            } else {
                //String2.log("sleep millis=" + millis);
                Thread.sleep(millis); //which is like yield
            }
        } catch (InterruptedException ie) {
            //catching InterruptedException cancels interruption! so at least call interrupt()
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This returns the number of bytes of memory which the JVM
     * has allocated (currently) for this program.
     * This is like a high-water mark, since memory is allocated as needed 
     * (up to something like Xmx).
     *
     * @return the number of bytes of memory which the JVM
     * has allocated for this program.
     */
    public static long getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * This returns the number of bytes currently in use by this program.
     * This is often called getMemoryInUse(getAllocatedMemory());
     *
     * @param allocatedMemory the value from getAllocatedMemory()
     * @return the number of bytes currently in use by this program
     */
    public static long getMemoryInUse(long allocatedMemory) {
        return allocatedMemory - Runtime.getRuntime().freeMemory();
    }

    /**
     * This returns the number of bytes currently in use by this program.
     *
     * @return the number of bytes currently in use by this program
     */
    public static long getMemoryInUse() {
        return getAllocatedMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * This returns a string indicating with the values from
     * getMemoryInUse (current being used) and the high-water mark
     * for this value (noted whenever this method is called).
     * It is a useful diagnostic.
     * This does not call gc() before getMemoryInUse.
     * Call gc() if you want memory actually in use.
     *
     * @return a string indicated the amount of memory in use (now and max
     * in this session)
     */
    public static String memoryString() {
        long using = getMemoryInUse();
        maxUsingMemory = Math.max(maxUsingMemory, using); //before gc

        return "Memory: currently using " + String2.right("" + using/BytesPerMB, 7) + 
            " MB (high water mark = " + String2.right("" + maxUsingMemory/BytesPerMB, 7) + 
            " MB)";
    }

    /**
     * This returns the Xmx memory string (max amount jvm was instructed to use).
     */
    public static String xmxMemoryString() {
        return "(Xmx ~= " + (maxMemory/BytesPerMB) + " MB)";
    }

    /**
     * This checks memory usage (calls gc if memory usage is much bigger) and
     *   sleeps for a specified number of milliseconds.
     * This will always sleep or yield.
     * It will call gc() (not incgc) if memory use is creeping up.
     * 
     * @param millis the number of millis to sleep
     */
    public static void incgc(long millis) {
        //long time = System.currentTimeMillis(); //diagnostic

        //get usingMemory 
        long using = getMemoryInUse(getAllocatedMemory());
        maxUsingMemory = Math.max(maxUsingMemory, using); //before gc

        //memory usage declined?
        if (using < lastUsingMemory) {
            lastUsingMemory = using;
            sleep(millis);
        
        //big increase in memory usage -- call gc  
        //trigger number (32MB) is my choice for
        //  how tight I want it to be
        //smaller numbers will cause more gc's
        } else if ((using - lastUsingMemory) > gcTrigger) {
            gc(millis); //this will also sleep or yield
        
        //intermediate, just sleep  
        } else {
            sleep(millis);
        }

        //if (millis == 0) //diagnostic
        //    System.err.println("incgc0 " + (System.currentTimeMillis() - time));

    }

    /**
     * Asks the garbage collector to run and the current
     *   thread to sleep for a specified number of milliseconds
     *   (usually >500, but may be 0).
     * Although more than one thread might call this,
     *   I don't think it needs to be synchronized. Each thread
     *   should be free to call System.gc (I don't think it
     *   will mind, since it is static) and to sleep for
     *   a while, independently. 
     * 
     * @param millis the number of milliseconds to sleep
     * @see Math2#incgc
     */
    public static void gc(long millis) {
        long time = System.currentTimeMillis();

        //System.err.println("********** Math2.gc"); //diagnostic
        String2.log("[Math2.gc is calling System.gc]");
            //+ "\n" + MustBe.getStackTrace());   
        System.gc();

        //Second call to gc is usually very fast and more effective.
        //System.gc();

        //sleep - subtract time already used by gc
        //always call sleep, even if <0, since it yields, too.
        sleep(millis - (System.currentTimeMillis() - time));
        lastUsingMemory = getMemoryInUse(getAllocatedMemory());
    }


    /**
     * This throws an exception if the requested nBytes are unlikely to be
     * available.
     * This isn't perfect, but is better than nothing. 
     * Future: locks? synchronization? ...?
     *
     * <p>This is almost identical to EDStatic.ensureMemoryAvailable, but lacks tallying.
     *
     * @param nBytes  size of data structure that caller plans to create
     * @param attributeTo for a WARNING or ERROR message, this is the string 
     *   to which this not-enough-memory issue should be attributed.
     * @throws RuntimeException if the requested nBytes are unlikely to be available.
     */
    public static void ensureMemoryAvailable(long nBytes, String attributeTo) {

        if (nBytes < 10000000) //10MB
            return;
        String attributeToParen = 
            attributeTo == null || attributeTo.length() == 0? "" : " (" + attributeTo + ")";
       
        //is the request too big under any circumstances?
        if (nBytes > maxSafeMemory) {
            throw new RuntimeException(memoryTooMuchData + "  " +
                MessageFormat.format(memoryThanSafe, "" + (nBytes / BytesPerMB),  
                    "" + (maxSafeMemory / BytesPerMB)) +
                attributeToParen); 
        }

        //request is fine without gc?
        long memoryInUse = Math2.getMemoryInUse();
        if (memoryInUse + nBytes < maxSafeMemory / 2)  //getting close   was /4*3
            return;

        //lots of memory is in use
        //is the request is too big for right now?
        Math2.gc(500);
        memoryInUse = Math2.getMemoryInUse();
        if (memoryInUse + nBytes > Math2.maxSafeMemory) {
            //eek! not enough memory! 
            //Wait, then try gc again and hope that some other thread requiring lots of memory will finish.
            //If nothing else, this 5 second delay will delay another request by same user (e.g., programmatic re-request)
            Math2.sleep(5000);
            Math2.gc(500); 
            memoryInUse = Math2.getMemoryInUse();
        }
        if (memoryInUse > maxSafeMemory) {
            String2.log("WARNING: memoryInUse > maxSafeMemory" + attributeToParen + ".");
        }
        if (memoryInUse + nBytes > maxSafeMemory) {
            throw new RuntimeException(memoryTooMuchData + "  " +
                MessageFormat.format(memoryThanCurrentlySafe,
                    "" + (nBytes / BytesPerMB), "" + ((maxSafeMemory - memoryInUse) / BytesPerMB)) +
                attributeToParen); 
        }
    }

    /** 
     * Even if JavaBits is 64, the limit on an array size is Integer.MAX_VALUE.
     * 
     * <p>This is almost identical to EDStatic.ensureArraySizeOkay, but lacks tallying.
     * 
     * @param tSize
     * @param attributeTo for a WARNING or ERROR message, this is the string 
     *   to which this not-enough-memory issue should be attributed.
     * @throws Exception if tSize >= Integer.MAX_VALUE.  
     *  (equals is forbidden for safety since I often use if as missing value / trouble)
     */
    public static void ensureArraySizeOkay(long tSize, String attributeTo) { 
        if (tSize >= Integer.MAX_VALUE) 
            throw new RuntimeException(memoryTooMuchData + "  " +
                MessageFormat.format(memoryArraySize, "" + tSize, "" + Integer.MAX_VALUE) +
                (attributeTo == null || attributeTo.length() == 0? "" : " (" + attributeTo + ")"));
    }

    /**
     * Indicates if i is odd.
     *
     * @param i any int
     * @return true if i is an odd number
     */
    public static boolean odd(int i) {
        return (i & 1) == 1;
    }


    /**
     * This returns an integer power of ten.
     * It uses Ten[] when possible for increased speed.
     * It is protected against errors.  It returns Double.POSITIVE_INFINITY
     * if trouble.
     *
     * @param toThe the number that ten is to be raised to the power of
     * @return 10^toThe
     */
    public static double ten(int toThe) {
        if ((toThe >= 0) && (toThe <= 18))
            return Ten[toThe];

        return Math.pow(10.0, toThe);
    }

    /**
     * This returns the binary exponent of a double:
     * usually +-1023.
     *
     * @param d any double
     * @return the binary exponent of d.
     *    If d = 0, this returns Binary0 (which is a flag, not a real value).
     *    If !isFinite(d), this return BinaryLimit (which is a flag, not a real 
     *       value).
     */
    public static int binaryExponent(double d) {
        //see the description of doubles in FileInput notes
        //(from turbo Pascal 7 Language guide manual, pg 278)
        if (d == 0)
            return Binary0;

        if (!isFinite(d))
            return BinaryLimit;

        int e = (int) (Double.doubleToLongBits(d) >> 52); //safe since the binary exponent of a finite double is +-1024
        return (e & 2047) - 1023;
    }

    /**
     * This quickly tests if <TT>d</TT> is almost 0 (Math.abs(d)&lt;dEps).
     * <UL>
     * <LI> If you are working with small numbers, this test may be inappropriate.
     * <LI> This is very fast, since it only involves one comparison.
     * <LI> Use almost0(d) instead of almostEqual5 or almostEqual9.
     * </UL>
     *
     * @param d any double
     * @return true if Math.abs(d) < dEps. 
     *     NaN and Infinity correctly return false.
     */
    public static final boolean almost0(double d) {
        return (Math.abs(d) < dEps);
    }

    /**
     * This tests if d1 is less than or almostEqual d2.
     *
     * @param nSignificantDigits  0 to 18 are allowed; 5, 9, and 14 are common
     * @param d1 the first number
     * @param d2 the second number
     * @return true if d1 is less than or almostEqual d2. 
     *    If isNaN(d1) || isNaN(d2), this returns false.
     */
    public static boolean lessThanAE(int nSignificantDigits, double d1, double d2) {
        return d1 <= d2 || almostEqual(nSignificantDigits, d1, d2);
    }

    /**
     * This tests if d1 is greater than or almostEqual9 d2.
     *
     * @param nSignificantDigits  0 to 18 are allowed; 5, 9, and 14 are common
     * @param d1 the first number
     * @param d2 the second number
     * @return true if d1 is greater than or almostEqual9 d2. 
     *    If isNaN(d1) || isNaN(d2), this returns false.
     */
    public static boolean greaterThanAE(int nSignificantDigits, double d1, double d2) {
        return d1 >= d2 || almostEqual(nSignificantDigits, d1, d2);
    }

    /**
     * This tests if the numbers are equal to at least n significant digits.
     * <UL>
     * <LI> Numbers must match to 1 part in 10^n to ensure that rounding
     *   to n-1 digits is identical.
     * <LI> If d1 and d2 are almost0, this returns true.
     * <LI> This is slow compared to almost0.
     * </UL>
     * @param nSignificantDigits  0 to 18 are allowed; 5, 9, and 14 are common
     * @param d1 any double
     * @param d2 any double
     * @return true if the numbers are equal to at least n significant digits
     *    (or are both almost0).
     *    If either number is NaN or Infinity, this returns false.
     */
    public static boolean almostEqual(int nSignificantDigits, double d1, double d2) {
        //System.err.println(d1+" "+d2+" "+d2/d1);
        double eps = nSignificantDigits >= 6? dEps : fEps;
        if (Math.abs(d2) < eps) {
            //This won't overflow, since d1 can't be <eps.
            return (Math.abs(d1) < eps) ? true :
                Math.rint(d2 / d1 * Ten[nSignificantDigits]) == Ten[nSignificantDigits];
        }

        //This won't overflow, since d2 can't be <eps.
        return Math.rint(d1 / d2 * Ten[nSignificantDigits]) == Ten[nSignificantDigits];
    }

    /**
     * This tests if two floats are equal to at least n significant digits.
     * 
     * @param nSignificantDigits  0 to 18 are allowed; 5, 9, and 14 are common
     * @param f1 any float
     * @param f2 any float
     * @return true if the numbers are equal to at least n significant digits
     *    (or are both almost0).
     *    If either number is NaN or Infinity, this returns false.
     */
    public static boolean almostEqual(int nSignificantDigits, float f1, float f2) {
        //System.err.println(f1+" "+f2+" "+Math.rint(f2 / f1 * Ten[nSignificantDigits]) + "=?" + Ten[nSignificantDigits]);
        if (Math.abs(f2) < fEps) {
            //This won't overflow, since f1 can't be <eps.
            return (Math.abs(f1) < fEps) ? true :
                Math.rint(f2 / f1 * Ten[nSignificantDigits]) == Ten[nSignificantDigits];
        }

        //This won't overflow, since f2 can't be <eps.
        return Math.rint(f1 / f2 * Ten[nSignificantDigits]) == Ten[nSignificantDigits];
    }

    /**
     * A div that rounds.  Positive numbers only.
     * e.g., 1/4 goes to 0;  3/4 goes to 1;
     * den = 0 throws an exception.
     * 
     * @param num the numerator  (a positive number)
     * @param den the denominator  (a positive number)
     * @return num/den, but rounded to the next larger (abs) int
     */
    public static int roundDiv(int num, int den) {
        return (num + (den >> 1)) / den;
    }

    /**
     * A div that rounds up if den&gt;0.
     * e.g., 1/4 goes to 1;  4/4 goes to 1;
     * den = 0 throws an exception.
     *
     * @param num the numerator (a positive number)
     * @param den the denominator (a positive number)
     * @return num/den, but rounded to the next larger (abs) int
     */
    public static int hiDiv(int num, int den) {
         return (num % den == 0)? num/den : num/den + 1;
    }

    /**
     * A div that rounds up if den&gt;0.
     * e.g., 1/4 goes to 1;  4/4 goes to 1;
     * den = 0 throws an exception.
     *
     * @param num the numerator (a positive number)
     * @param den the denominator (a positive number)
     * @return num/den, but rounded to the next larger (abs) int
     */
    public static long hiDiv(long num, long den) {
         return (num % den == 0)? num/den : num/den + 1;
    }

    /**
     * A div where the implied mod is always &gt;=0.
     * This is a consistent div for positive and negative numerators.
     * For example, with regular division 1/2=0 and -1/2=0.
     * But floorDiv(-1,2)=-1.
     * den = 0 throws an exception.
     *
     * @param num the numerator (a positive or negative number)
     * @param den the denominator (a positive number)
     * @return num/den, but is consistent for positive and negative numbers
     */
    public static int floorDiv(int num, int den) {
        return (num % den < 0) ? (num / den) - 1 : num / den;
    }

    /**
     * A div where the implied mod is always &gt;=0.
     * This is a consistent div for positive and negative numerators.
     * For example, with regular division 1/2=0 and -1/2=0.
     * But floorDiv(-1,2)=-1.
     * den = 0 throws an exception.
     *
     * @param num the numerator (a positive or negative number)
     * @param den the denominator (a positive number)
     * @return num/den, but is consistent for positive and negative numbers
     */
    public static long floorDiv(long num, long den) {
        return (num % den < 0) ? (num / den) - 1 : num / den;
    }

    /**
     * This forces a int into a range defined by min..max.
     * 
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param current the current value
     * @return min if current is less than min; or
     *         max if current is greater than max; else 
     *         it returns the current value.
     */
    public static final int minMax(int min, int max, int current) {
        return (current < min) ? min : ((current > max) ? max : current);
    }

    /**
     * This forces a double into a range defined by min..max.
     *
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param current the current value
     * @return min if current is less than min; or
     *         max if current is greater than max; else 
     *         it returns the current value.
     */
    public static final double minMax(double min, double max, double current) {
        return (current < min) ? min : ((current > max) ? max : current);
    }

    /**
     * This forces an int into a range defined by min..max.
     *
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param def the default value
     * @param current the current value
     * @return def if current is less than min; or
     *         def if current is greater than max; else 
     *         it returns the current value.
     */
    public static final int minMaxDef(int min, int max, int def, int current) {
        return ((current < min) || (current > max)) ? def : current;
    }

    /**
     * This forces a double into a range defined by min..max.
     *
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param def the default value
     * @param current the current value
     * @return def if current is less than min; or
     *         def if current is greater than max; else 
     *         it returns the current value.
     */
    public static final double minMaxDef(double min, double max, double def,
        double current) {
        return ((current < min) || (current > max)) ? def : current;
    }

    /**
     * Safely rounds a double to a byte.
     * 
     * @param d any double
     * @return Byte.MAX_VALUE if d is too small, too big, or NaN;
     *   otherwise d, rounded to the nearest byte.
     *   Undesirable: d.5 rounds up for positive numbers, down for negative.
     */
    public static final byte roundToByte(double d) {
        return d > Byte.MAX_VALUE || d <= Byte.MIN_VALUE - 0.5 || !isFinite(d)? 
            Byte.MAX_VALUE : 
            (byte)Math.round(d);
    }

    /**
     * Safely rounds a double to a char (treated as unsigned short).
     * 
     * @param d any double
     * @return Character.MAX_VALUE if d is too small, too big, or NaN;
     *   otherwise d, rounded to the nearest char.
     *   Undesirable: d.5 rounds up for positive numbers, down for negative.
     */
    public static final char roundToChar(double d) {
        return d > Character.MAX_VALUE || d <= Character.MIN_VALUE - 0.5 || !isFinite(d)?
            Character.MAX_VALUE : 
            (char)Math.round(d);
    }

    /**
     * Safely rounds a double to a short.
     * 
     * @param d any double
     * @return Short.MAX_VALUE if d is too small, too big, or NaN;
     *   otherwise d, rounded to the nearest short.
     *   Undesirable: d.5 rounds up for positive numbers, down for negative.
     */
    public static final short roundToShort(double d) {
        return d > Short.MAX_VALUE || d <= Short.MIN_VALUE - 0.5 || !isFinite(d)? 
            Short.MAX_VALUE : 
            (short)Math.round(d);
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
    public static final int roundToInt(double d) {
        return d > Integer.MAX_VALUE || d <= Integer.MIN_VALUE - 0.5 || !isFinite(d)? 
            Integer.MAX_VALUE : 
            (int)Math.round(d); //safe since checked for larger values above
    }

    /**
     * Safely rounds a double to a long.
     * 
     * @param d any double
     * @return Long.MAX_VALUE if d is too small, too big, or NaN;
     *   otherwise d, rounded to the nearest int.
     *   Undesirable: d.5 rounds up for positive numbers, down for negative.
     */
    public static final long roundToLong(double d) {
        return d > Long.MAX_VALUE || d <= Long.MIN_VALUE - 0.5 || !isFinite(d)? 
            Long.MAX_VALUE : 
            Math.round(d);
    }

    /**
     * Safely rounds a double to the nearest integer (stored as a double).
     * 
     * @param d any double
     * @return Double.NaN if d is !finite;
     *   otherwise d, rounded to the nearest int.
     *   Undesirable: d.5 rounds up for positive numbers, down for negative.
     *   But this rounds d.5 in a way that is often more useful than Math.rint
     *   (which rounds to nearest even number).
     */
    public static final double roundToDouble(double d) {
        return isFinite(d)? Math.floor(d + 0.5) : Double.NaN;
    }

    /**
     * Rounds the value to the specified number of decimal places.
     *
     * @param d any double
     * @param nPlaces the desired number of digits to the right of the 
     *     decimal point
     */
    public static final double roundTo(double d, int nPlaces) {
        double factor = ten(nPlaces);
        return roundToDouble(d * factor) / factor;
    }


    /**
     * Safely narrows an int to a byte.
     * 
     * @param i any int
     * @return Byte.MAX_VALUE if i is too small or too big; otherwise i.
     */
    public static final byte narrowToByte(int i) {
        return i > Byte.MAX_VALUE || i < Byte.MIN_VALUE? 
            Byte.MAX_VALUE : (byte)i;
    }

    /**
     * Safely narrows a long to a byte.
     * 
     * @param i any long
     * @return Byte.MAX_VALUE if i is too small or too big; otherwise i.
     */
    public static final byte narrowToByte(long i) {
        return i > Byte.MAX_VALUE || i < Byte.MIN_VALUE? 
            Byte.MAX_VALUE : (byte)i;
    }

    /**
     * Safely narrows an int to a char.
     * 
     * @param i any int
     * @return Character.MAX_VALUE if i is too small or too big; otherwise i.
     */
    public static final char narrowToChar(int i) {
        return i > Character.MAX_VALUE || i < Character.MIN_VALUE? 
            Character.MAX_VALUE : (char)i;
    }

    /**
     * Safely narrows a long to a char.
     * 
     * @param i any long
     * @return Character.MAX_VALUE if i is too small or too big; otherwise i.
     */
    public static final char narrowToChar(long i) {
        return i > Character.MAX_VALUE || i < Character.MIN_VALUE? 
            Character.MAX_VALUE : (char)i;
    }

    /**
     * Safely narrows an int to a short.
     * 
     * @param i any int
     * @return Short.MAX_VALUE if i is too small or too big; otherwise i.
     */
    public static final short narrowToShort(int i) {
        return i > Short.MAX_VALUE || i < Short.MIN_VALUE? 
            Short.MAX_VALUE : (short)i;
    }

    /**
     * Safely narrows a long to a short.
     * 
     * @param i any long
     * @return Short.MAX_VALUE if i is too small or too big; otherwise i.
     */
    public static final short narrowToShort(long i) {
        return i > Short.MAX_VALUE || i < Short.MIN_VALUE? 
            Short.MAX_VALUE : (short)i;
    }

    /**
     * Safely narrows a long to an int.
     * 
     * @param i any long
     * @return Integer.MAX_VALUE if i is too small or too big; otherwise i.
     */
    public static final int narrowToInt(long i) {
        return i > Integer.MAX_VALUE || i < Integer.MIN_VALUE? 
            Integer.MAX_VALUE : (int)i;
    }

    /**
     * Safely converts a byte (-128..127) to char (0..255).
     * Note that reverse is easy: (byte)ch works (for 0..255) because
     * narrowing just saves the low order bits, so >127 becomes negative bytes.
     * 
     * @param b a byte (-128 .. 127)  (or char, short, or int where you just 
     *     want the lower 8 bits stored as 0..255)
     * @return a char (0..255)
     */
    public static final char byteToChar(int b) {
        return (char) (b & 255);
    }

    /**
     * Safely converts a signed byte (-128..127) to an unsigned byte (0..255).
     * Note that reverse is easy: (byte)ch works (for 0..255) because
     * narrowing just saves the low order bits, so >127 becomes negative bytes.
     * 
     * @param b a byte (-128 .. 127)  (or char, short, or int where you just 
     *     want the lower 8 bits stored as 0..255)
     * @return an int (0..255)
     */
    public static final int unsignedByte(int b) {
        return b & 255;
    }


    /**
     * This converts an angle (in degrees) into the range >=0 to
     *   <360.
     * <UL>
     * <LI> If isMV(angle), it returns 0.
     * </UL>
     */
    public static final double angle0360(double degrees) {
        if (!isFinite(degrees))
            return 0;

        degrees = frac(degrees / 360) * 360;

        //now it is -360..360
        if (degrees < 0)
            degrees += 360;

        return degrees;
    }

    /**
     * This converts an angle (in degrees) into the range >= 0 to <=360  
     * (note 360 is valid).
     *
     * @param degrees an angle (in degrees) 
     * @return the angle (in degrees) in the range >=0 to <=360.
     *    A non-finite degrees returns the original value.
     */
    public static final double looserAngle0360(double degrees) {
        if (!isFinite(degrees))
            return degrees;

        while (degrees < 0)   degrees += 360;
        while (degrees > 360) degrees -= 360;
        return degrees;
    }

    /**
     * This converts a compass heading (usually 0..360, where North is 0, East is 90...)
     * to Math-style degrees (East is 0, North is 90, ...).
     *
     * @param compass
     * @return degrees always >=0 and <360  (compass=NaN -> 0).
     */
    public static final double compassToMathDegrees(double compass) {
        return angle0360(90 - compass);
    }

    /**
     * This converts a Math-style degrees (East is 0, North is 90, ...) to a 
     * compass heading (where North is 0, East is 90, ...).
     *
     * @param math
     * @return degrees always >=0 and <360  (compass=NaN -> 0).
     */
    public static final double mathToCompassDegrees(double math) {
        return compassToMathDegrees(math); //they work the same! so reuse compassToMath
    }

    /**
     * This converts an angle (in degrees) into the range >=-180 to <180 
     * (180 becomes -180).
     * @param degrees an angle (in degrees) 
     * @return the angle (in degrees) in the range >=-180 to <180.
     *   If isMV(angle), it returns 0.
     */
    public static final double anglePM180(double degrees) {
        if (!isFinite(degrees))
            return 0;

        degrees = frac(degrees / 360) * 360;

        //now it is -360..360
        if (degrees < -180)
            degrees += 360;
        if (degrees >= 180)
            degrees -= 360;

        return degrees;
    }

    /**
     * This converts an angle (in degrees) into the range >=-180 to <=180  
     * (note 180 is valid).
     *
     * @param degrees an angle (in degrees) 
     * @return the angle (in degrees) in the range >=-180 to <=180.
     *    A non-finite degrees returns the original value.
     */
    public static final double looserAnglePM180(double degrees) {
        if (!isFinite(degrees))
            return degrees;

        while (degrees < -180) degrees += 360;
        while (degrees > 180)  degrees -= 360;
        return degrees;
    }

 
    /**
     * This converts an angle (in radians) into the range >=0 to
     *   <2PI.
     * <UL>
     * <LI> If !isFinite(angle), it returns 0.
     * </UL>
     */
    public static final double angle02Pi(double radians) {
        if (!isFinite(radians))
            return 0;

        radians = frac(radians / TwoPi) * TwoPi;

        //now it is -2Pi..2Pi
        if (radians < 0)
            radians += TwoPi;

        return radians;
    }

    /**
     * Finds the greatest common divisor.
     * <UL>
     * <LI> Euclidean greatest common divisor algorithm.
     * <LI> If either is 0, it returns 1.
     * <LI> For example, Math2.gcd(18,30) returns 6.
     * </UL>
     */
    public static int gcd(int n, int d) {
        //algorithm from Bentley?
        //future improvement: if Big%small==0 return small
        if ((n == 0) || (d == 0))
            return 1;

        n = Math.abs(n);
        d = Math.abs(d);

        while (n != d) {
            if (n > d) {
                n -= d;
            } else
                d -= n;
        }

        return d;
    }

    /**
     * Looks for a fraction very close to some decimal value.
     * <UL>
     * <LI> Tries denominators 1..1000.  So answer is at least accurate
     *   to within 1/1000th.  For example, .33333 -> 1/3.
     * <LI> For example: -1.75 -> whole=-1, numerator=-3 denominator=4
     * <LI> Results stored in int3[0=whole, 1=num, 2=den].
     * <LI> Slow if no good match found,
     *   but this does a good approximate job that gcd() might miss.
     * <pre>
     *     int ar[]=new int[3];
     *     double d=-1.75;
     *     int whole=Math2.guessFrac(d,ar);
     *     //results: ar[0]=-1, ar[1]=-3, ar[2]=4
     * </pre>
     * </UL>
     */
    public static void guessFrac(double r, int[] int3) {
        // faster if done with long and shifts?
        //better and faster if done initially with gcd?
        //quickly catch r= almost an integer
        //System.err.println("Math2.guessFrac "+r);//diagnostic
        if (Math.abs(r - roundToInt(r)) <= 0.0005) {
            int3[0] = roundToInt(r);
            int3[1] = 0;
            int3[2] = 1;
            return;
        }

        //find a good fraction
        double num;
        int denominator = 0;
        boolean finished = false;
        int3[0] = roundToInt(trunc(r));
        r = frac(r);

        do {
            denominator++; //looks for simple den first  /2  /3 /4 /5...
            num = r * denominator; //finds best numerator
            finished = Math.abs(num - roundToDouble(num)) <= 5e-4; //local epsilon
        } while ((denominator < 1000) && !finished);

        int3[1] = roundToInt(num);
        int3[2] = denominator;

        //System.err.println("Math2.guessFrac "+r+" "+int3[1]+" "+int3[2]);//diagnostic
        //catch 1 or -1; (should be caught above, this is insurance)
        if (Math.abs(int3[1]) == int3[2]) {
            int3[0] += roundToInt(int3[1] / (double) int3[2]);
            int3[1] = 0;
            int3[2] = 1;
            return;
        }

        //ensure it is reduced fraction (especially if denominator=1000)
        int gcd = gcd(int3[1], int3[2]);
        int3[1] /= gcd;
        int3[2] /= gcd;
    }

    /**
     * This creates a String based on the results of guessFrac()
     */
    public static String guessFracString(double d) {
        int[] ar3 = new int[3];
        guessFrac(d, ar3);

        if ((ar3[0] == 0) && (ar3[1] == 0))
            return "0";

        if (ar3[0] == 0)
            return ar3[1] + "/" + ar3[2];

        if (ar3[1] == 0)
            return ar3[0] + "";

        return ar3[0] + " " + Math.abs(ar3[1]) + "/" + ar3[2];
    }


    /**
     * Safely converts a float to a double.
     *
     * @param f a double or float
     * @return an unbruised double (not xxxx999999 or xxxx000001).
     *    If f is NaN, this returns Double.NaN.
     *    If f is +-INFINITY, this returns Double.+-INFINITY.
     */
    public static final double floatToDouble(double f) {
        return niceDouble(f, 7); //8 is not enough rounding
    }

    /**
     * Crudely (not nicely) converts a float to a double (including the non-standard
     * conversion of INFINITY values to NaN).
     * See floatToDouble.
     *
     * @param f a float or double
     * @return a double.
     *    If f is NaN or +-INFINITY, this returns Double.NaN.
     */
    public static final double floatToDoubleNaN(double f) {
        if (Double.isNaN(f) || Double.isInfinite(f))
            return Double.NaN;

        return (double)f;
    }

    /**
     * Safely converts a double to a float (including the non-standard
     * conversion of large values to Float.NaN, not Float.POSITIVE_INFINITY).
     *
     * @param d a double
     * @return a float.
     *    If f is not finite or greater than Float.MAX_VALUE,
     *    this returns Float.NaN.
     */
    public static final float doubleToFloatNaN(double d) {
        if (isFinite(d) && Math.abs(d) <= Float.MAX_VALUE)
            return (float)d;
        return Float.NaN;
    }

    /**
     * Safely tries to un-bruise a double (8.999999999 -> 9.0,
     *   or 1.000000001 -> 1.0).
     * <UL>
     * <LI> nDigits specifies how many digits.
     *   Use 7 for converting float to double.
     *   Use 11 for converting 6byte real to double.
     * <LI> nGoodDigits afterward is ~ 22-nDigits.
     * </UL>
     *
     * @param d a double (often bruised)
     * @param nDigits the desired number of significant digits
     */
    public static final double niceDouble(double d, int nDigits) {
        if (!isFinite(d))
            return d; //avoid possible overflow if nDigits>7

        if (d == 0)
            return 0; //intExponent doesn't handle it nicely

        double tTen = ten((nDigits - intExponent(d)) - 1);
        return Math.floor((d * tTen) + 0.5) / tTen; //floor( +.5) rounds
    }


    /**
     * This sets the seed for the next call to random().
     * You can use this for testing purposes if you need to get repeatable
     * results from random().
     *
     * @param seed the new seed -- be sure that it is randomly chosen
     *    from the full range of longs
     */
    public static void setSeed(long seed) {

        //this is probably thread-safe
        //http://www.velocityreviews.com/forums/t367261-java-util-random-nextint-thread-safety.html
        //but synchronize it to be extra careful
        synchronized (random) {
            random.setSeed(seed);
        }
    }

    /**
     * This returns a random integer between 0 and max-1.
     * For testing purposes, you can call Math2.setSeed(long seed)
     *    to get repeatable results.
     * 
     * @param max a int greater than 0
     * @return a "random" number in the range 0 .. max-1
     */
    public static int random(int max) {

        //this is probably thread-safe
        //http://www.velocityreviews.com/forums/t367261-java-util-random-nextint-thread-safety.html
        //but synchronize it to be extra careful
        synchronized (random) {
            return random.nextInt(max);
        }
    }

    //current time: use:  long time=System.currentTimeMillis();

    /**
     * This increases the value (nicely).
     *
     * @param d a double
     * @return a number slightly larger than d.
     *    If !isFinite(d), it returns d.
     *    If almost 0, it returns 0.01.
     */
    public static double bigger(double d) {
        if (!isFinite(d))
            return d;

        if (almost0(d))
            return 0.01;

        int m = roundToInt(mantissa(d) * 10); //-100..-10, 10..100
        int i = 0;

        while (niceNumbers[i] <= m)
            i++;

        return (niceNumbers[i] * exponent(d)) / 10;
    }

    /**
     * This decreases the value (nicely).
     *
     * @param d a double
     * @return a number slightly smaller than d.
     *    If !isFinite(d), it returns d.
     *    If almost 0, it returns -0.01.
     */
    public static double smaller(double d) {
        if (!isFinite(d))
            return d;

        if (almost0(d))
            return -0.01;

        int m = roundToInt(mantissa(d) * 10); //-100..-10, 10..100
        int i = niceNumbers.length - 1;

        while (niceNumbers[i] >= m)
            i--;

        return (niceNumbers[i] * exponent(d)) / 10;
    }

    /**
     * This increases the first digit of d 
     * (for example, .8, .9, 1, 2, 3, ..., 9, 10, 20, 30, ...).
     * It rounds to nearest single digit mantissa, then changes it.
     * If !isFinite(d), it returns def.
     * If d = 0, it returns 1.
     * 
     * @param max the maximum value which may be returned
     * @param def the default value, to be used if !isFinite
     * @param d the initial value
     * @return d, rounded to a single digit mantissa and
     *     with the initial digit increased
     */
    public static double oneDigitBigger(double max, double def, double d) {
        if (!isFinite(d))
            return def;

        double mantissa = Math.rint(mantissa(d)); //rint safer than floor
        mantissa = (mantissa == 10) ? 20 : (mantissa + 1);
        return Math.min(max, mantissa * exponent(d));
    }

    /**
     * This decreases the first digit of d 
     * (for example, 30, 20, 10, 9, ..., 3, 2, 1, .9, .8, ...).
     * It rounds to nearest single digit mantissa, then changes it.
     * If !isFinite(d), it returns def.
     * If d = 0, it returns -1.
     * 
     * @param min the minimum value which may be returned
     * @param def the default value, to be used if !isFinite
     * @param d the initial value
     * @return d, rounded to a single digit mantissa and
     *     with the initial digit decreased
     */
    public static double oneDigitSmaller(double min, double def, double d) {
        if (!isFinite(d))
            return def;

        double mantissa = Math.rint(mantissa(d)); //rint safer than floor
        mantissa = mantissa == 0? -1 :
            ((mantissa == 1) ? 0.9 : (mantissa - 1));
        return Math.max(min, mantissa * exponent(d));
    }

    /**
     * This returns a small increment roughly 1/100th the range
     *   (e.g., .1, 1, 10, ....).
     *
     * @param range 
     * @return an appropriate increment for the range
     */
    public static double getSmallIncrement(double range) {
        if (!isFinite(range) || range == 0)
            return 0.1;

        return Math2.exponent(Math.abs(range) / 2) / 10;
    }

    /**
     * This increases d to the next multiple of mult.
     *
     * @param def the default value
     * @param mult the multiple
     * @param max the maximum value
     * @param d the initial value
     * @return the next multiple of mult bigger than d.
     *     It rounds to nearest mult, then changes it.
     *     If !isFinite(d), it returns def.
     */
    public static double biggerDouble(double def, double mult, double max, double d) {
        if (!isFinite(d))
            return def;

        return Math.min(max, (Math.rint(d / mult) + 1) * mult); //rint safer than floor
    }

    /**
     * This decreases d to the previous multiple of mult.
     *
     * @param def the default value
     * @param mult the multiple
     * @param min the minimum value
     * @param d the initial value
     * @return the next multiple of mult smaller than d.
     *     It rounds to nearest mult, then changes it.
     *     If !isFinite(d), it returns def.
     */
    public static double smallerDouble(double def, double mult, double min, double d) {
        if (!isFinite(d))
            return def;

        return Math.max(min, (Math.rint(d / mult) - 1) * mult); //rint safer than floor
    }

    /**
     * This increases the double degrees value (nicely),
     *   and returns it as a string.
     *
     * @param d the initial value
     * @return the next multiple 15.
     *     It rounds to nearest mult, then changes it.
     *     If !isFinite(d), it returns d.
     */
    public static double biggerAngle(double d) {
        if (!isFinite(d))
            return d;

        //which 15degree step?
        int i = floorDiv(Math2.roundToInt(angle0360(d)), 15) + 1;

        if (i >= 24)
            i -= 24;

        return i * 15;
    }

    /**
     * This decreases the double degree value (nicely).
     *
     * @param d the initial value
     * @return the previous multiple 15.
     *     It rounds to nearest mult, then changes it.
     *     If !isFinite(d), it returns d.
     */
    public static double smallerAngle(double d) {
        if (!isFinite(d))
            return d;

        //which 15degree step?
        int i = floorDiv(Math2.roundToInt(angle0360(d)), 15) - 1;

        if (i < 0)
            i += 24;

        return i * 15;
    }

    /**
     * This increases the value (nicely) so the mantissa is 1 or 5.
     *
     * @param d the initial value
     * @return the next 1 or 5 * 10^x.
     *     It rounds to nearest 1 or 5, then changes it.
     *     If !isFinite(d), it returns d.
     *     If almost0(d), it returns 0.01.
     */
    public static double bigger15(double d) {
        if (!isFinite(d))
            return d;

        if (almost0(d))
            return 0.01;

        int m = roundToInt(mantissa(d)); //-10..-1, 1..10
        return ((m <= 4) ? 5 : ((m <= 9) ? 10 : 50)) * exponent(d);
    }

    /**
     * This gets the double value from the string, decreases it (nicely),
     *   so the mantissa is 1 or 5.
     *
     * @param d the initial value
     * @return the next 1 or 5 * 10^x.
     *     It rounds to nearest 1 or 5, then changes it.
     *     If !isFinite(d), it returns d.
     *     If almost0(d), it returns 0.01.
     */
    public static double smaller15(double d) {
        if (!isFinite(d))
            return d;

        if (almost0(d))
            return -0.01;

        int m = roundToInt(mantissa(d)); //-10..-1, 1..10
        return ((m <= 1) ? 0.5 : ((m <= 5) ? 1 : 5)) * exponent(d);
    }


    /**
     * This returns a nice bounding range (e.g., for an axis) which 
     * includes low and high.
     *
     * @param low the low end of the range
     * @param high the high end of the range
     * @return returnLowHigh an array with 2 elements.
     *    The resulting bounds are stored as [0]=low and [1]=high.
     *    If low or high is not finite, this returns 0,1.
     *    If low and high are equal, this returns an appropriate wider range.
     */
    public static double[] suggestLowHigh(double low, double high) {
        //String2.log("suggestLowHigh low=" + low + " high=" + high);
        double lowHigh[] = new double[2];
        if (!isFinite(low) || !isFinite(high)) {
            lowHigh[0] = 0;
            lowHigh[1] = 1;
            return lowHigh;
        }

        //low==high?
        if (low == high) {
            if (low == 0) {
                lowHigh[0] = -1;
                lowHigh[1] = 1;
                return lowHigh;
            } else if (low < 0) {
                low  *= 1.04;
                high *= 0.96;
            } else {
                low  *= 0.96;
                high *= 1.04;
            }
        }

        //This doesn't lead to unlabeled axis end points.
        //It has 3 levels of exp instead of 2 per decade; so more graduated.
        double exp = exponent(high - low);
        double man10100 = Math.abs(((high - low) / exp) * 10); //10..100

        if (man10100 > 50)
            exp *= 2;
        else if (man10100 <= 25)
            exp /= 2;

        //for example, 1,9 -> man10100=80 exp=1 -> exp=2
        //for example, 1,5 -> man10100=40 exp=1 -> exp=1
        //for example, 1,2 -> man10100=10 exp=1 -> exp=0.5
        //Sometimes a class <=12 would be nice, but not always.  Don't do it.
        //System.err.println("suggestLowHigh "+low+" "+high+" exp="+exp); //diagnostic
        //The ((xxx/exp)+-0.1) prevents dt or fn right at suggestLow,High
        // by making a buffer zone of 1/20 of exp
        double lowExp  = low / exp;
        if (lowExp >= 0 && lowExp <= 0.05) {  
            //special case for low end close to 0
        } else lowExp -= 0.05;
        lowHigh[0] = Math.floor(lowExp) * exp;
        lowHigh[1] = Math.ceil((high / exp) + .05) * exp;

        return lowHigh;
    }

    /**
     * This suggests the division distance along an axis so that there
     * will be about 5-7 primary divisions and 10-25 secondary.
     *
     * @param range the range of the axis, e.g., an axis spanning 30 - 50 
     *     would have a range of 20
     * @return a double[2] with [0]=suggested major division distance
     *    and [1]=suggested minor division distance.
     *    If range isn't finite, this returns 2 Double.NaN's.
     *    If range == 0, this returns 1 and .5.
     *    If range < 0, this uses Math.abs(range).
     */
    public static double[] suggestDivisions(double range) {
        double results[] = new double[2];

        //handle trouble
        if (!isFinite(range)) {
            results[0] = Double.NaN;
            results[1] = Double.NaN;
            return results;
        }
        if (range == 0) {
            results[0] = 1;
            results[1] = 0.5;
            return results;
        }

        //man10 must be in range 10..99
        range = Math.abs(range);
        int man10 =
            Math2.minMax(10, 99,
                Math2.roundToInt(Math2.trunc(10 * Math2.mantissa(range))));
        double power = Math2.exponent(range);

        if (man10 <= 12) {
            results[0] = (2 * power) / 10;
            results[1] = power / 10;
        } else if (man10 <= 30) {
            results[0] = (5 * power) / 10;
            results[1] = power / 10;
        } else if (man10 <= 69) {
            results[0] = power;
            results[1] = (2 * power) / 10;
        } else { // man10<=99
            results[0] = 2 * power;
            results[1] = power;
        }

        //System.err.println("suggestDivisions range="+range+ //division
        //  " man10="+man10+" power="+power+" div1="+results[0]+
        //  " div2="+results[1]);
        return results;
    }

    /**
     * This suggests the division distance along an axis so that there
     * will be between maxDivisions/2 and maxDivisions.
     *
     * @param range the range of the axis, e.g., an axis spanning 30 - 50 
     *     would have a range of 20
     * @param maxDivisions the maximum number of divisions (segments).
     *    If you have maxNValues, use maxNValues-1 to call this method.     
     * @return a double with the suggested division distance.
     *    If range isn't finite, this returns NaN.
     *    If range == 0, this returns 1.
     *    If range &lt; 0, this the result will be negative.
     *    If maxDivisions == 0, this returns range.
     *    If maxDivisions &lt; 0, this uses Math.abs(maxDivisions).
     */
    public static double suggestMaxDivisions(double range, int maxDivisions) {

        //handle trouble
        if (!isFinite(range))  return Double.NaN;
        if (range == 0)        return 1;
        if (maxDivisions == 0) return range;

        int factor = range < 0? -1 : 1;
        range = Math.abs(range);
        maxDivisions = Math.abs(maxDivisions);
        double dist = range / maxDivisions;

        //man1 must be in range 1..9.99999
        double man   = Math2.mantissa(dist);
        double power = Math2.exponent(dist);

        //round up to nice number
        if (man <=  1) return power * factor;
        if (man <=  2) return 2 * power * factor;
        if (man <=  5) return 5 * power * factor;
                       return 10 * power * factor;
    }

    /**
     * Find the last element which is &lt;= x in an ascending sorted array.
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param dar an ascending sorted double[] which may have duplicate values
     * @param x
     * @return the index of the last element which is &lt;= x in an ascending sorted array.
     *   If x &lt; the smallest element, this returns -1  (no element is appropriate).
     *   If x &gt; the largest element, this returns dar.length-1.
     *   If x is NaN, this is currently undefined.
     */
    public static int binaryFindLastLE(double[] dar, double x) {
        int i = Arrays.binarySearch(dar, x);

        //an exact match; look for duplicates
        if (i >= 0) {
            while (i < dar.length - 1 && dar[i + 1] <= x)
                i++;
            return i; 
        }

        int insertionPoint = -i - 1;  //0.. dar.length
        return insertionPoint - 1;
    }

    /**
     * Find the last element which is &lt;x or almostEqual(9, ) in an ascending sorted array.
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param dar an ascending sorted double[] which may have duplicate values
     * @param x
     * @return the index of the last element which is &lt;= x in an ascending sorted array.
     *   If x &lt; the smallest element, this returns -1  (no element is appropriate).
     *   If x &gt; the largest element, this returns dar.length-1.
     *   If x is NaN, this is currently undefined.
     */
    public static int binaryFindLastLAE9(double[] dar, double x) {
        int i = Arrays.binarySearch(dar, x);
        //an exact match; look for duplicates
        if (i >= 0) { 
            while (i < dar.length - 1 && 
                   (dar[i + 1] <= x || almostEqual(9, dar[i + 1], x)))
                i++;
            return i; 
        }

        //is the value at insertion point almostEqual(9)?
        int insertionPoint = -i - 1;  //0.. dar.length
        if (insertionPoint < dar.length &&
            almostEqual(9, dar[insertionPoint], x))
            return insertionPoint;

        return insertionPoint - 1;
    }

    /**
     * Find the last element which is &lt;x or almostEqual(5, x) in an ascending sorted array.
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param dar an ascending sorted double[] which may have duplicate values
     * @param x
     * @return the index of the last element which is &lt;= x in an ascending sorted array.
     *   If x &lt; the smallest element, this returns -1  (no element is appropriate).
     *   If x &gt; the largest element, this returns dar.length-1.
     *   If x is NaN, this is currently undefined.
     */
    public static int binaryFindLastLAE5(double[] dar, double x) {
        int i = Arrays.binarySearch(dar, x);

        //an exact match; look for duplicates
        if (i >= 0) { 
            while (i < dar.length - 1 && 
                   (dar[i + 1] <= x || almostEqual(5, dar[i + 1], x)))
                i++;
            return i; 
        }

        //is the value at insertion point almostEqual(5, x)?
        int insertionPoint = -i - 1;  //0.. dar.length
        if (insertionPoint < dar.length &&
            almostEqual(5, dar[insertionPoint], x))
            return insertionPoint;

        return insertionPoint - 1;
    }

    /**
     * Find the first element which is &gt;= x in an ascending sorted array.
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param dar an ascending sorted double[] which currently may not have duplicate values
     * @param x
     * @return the index of the first element which is &gt;= x in an ascending sorted array.
     *   If x &lt; the smallest element, this returns 0.
     *   If x &gt; the largest element, this returns dar.length (no element is appropriate).
     *   If x is NaN, this is currently undefined.
     */
    public static int binaryFindFirstGE(double[] dar, double x) {
        int i = Arrays.binarySearch(dar, x);

        //an exact match; look for duplicates
        if (i >= 0) { 
            while (i > 0 && dar[i - 1] >= x)
                i--;
            return i; 
        }

        return -i - 1;  //the insertion point,  0.. dar.length
    }

    /**
     * Find the first element which is &gt;x or almostEqual(9) in an ascending sorted array.
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param dar an ascending sorted double[] which currently may not have duplicate values
     * @param x
     * @return the index of the first element which is &gt;= x in an ascending sorted array.
     *   If x &lt; the smallest element, this returns 0.
     *   If x &gt; the largest element, this returns dar.length (no element is appropriate).
     *   If x is NaN, this is currently undefined.
     */
    public static int binaryFindFirstGAE9(double[] dar, double x) {
        int i = Arrays.binarySearch(dar, x);

        //an exact match; look for duplicates
        if (i >= 0) {
            while (i > 0 && 
                   (dar[i - 1] >= x || almostEqual(9, dar[i - 1], x)))
                i--;
            return i; 
        }

        //is the value at insertion point-1 almostEqual(9)?
        int insertionPoint = -i - 1;  //0.. dar.length
        if (insertionPoint > 0 &&
            almostEqual(9, dar[insertionPoint - 1], x))
            return insertionPoint - 1;

        return insertionPoint;
    }

    /**
     * Find the first element which is &gt;x or almostEqual(5, x) in an ascending sorted array.
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param dar an ascending sorted double[] which currently may not have duplicate values
     * @param x
     * @return the index of the first element which is &gt;= x in an ascending sorted array.
     *   If x &lt; the smallest element, this returns 0.
     *   If x &gt; the largest element, this returns dar.length (no element is appropriate).
     *   If x is NaN, this is currently undefined.
     */
    public static int binaryFindFirstGAE5(double[] dar, double x) {
        int i = Arrays.binarySearch(dar, x);

        //an exact match; look for duplicates
        if (i >= 0) {
            while (i > 0 && 
                   (dar[i - 1] >= x || almostEqual(5, dar[i - 1], x)))
                i--;
            return i; 
        }

        //is the value at insertion point-1 almostEqual(5, x)?
        int insertionPoint = -i - 1;  //0.. dar.length
        if (insertionPoint > 0 &&
            almostEqual(5, dar[insertionPoint - 1], x))
            return insertionPoint - 1;

        return insertionPoint;
    }

    /**
     * Find the closest element to x in an ascending sorted array.
     *
     * @param dar an ascending sorted double[].
     *   It the array has duplicates and x equals one of them,
     *   it isn't specified which duplicate's index will be returned.
     * @param x
     * @return the index of the index of the element closest to x.
     *   If x is NaN, this returns -1.
     */
    public static int binaryFindClosest(double[] dar, double x) {
        if (Double.isNaN(x))
            return -1;
        int i = Arrays.binarySearch(dar, x);
        if (i >= 0)
            return i; //success

        //insertionPoint at end point?
        int insertionPoint = -i - 1;  //0.. dar.length
        if (insertionPoint == 0) 
            return 0;
        if (insertionPoint >= dar.length)
            return dar.length - 1;

        //insertionPoint between 2 points 
        if (Math.abs(dar[insertionPoint - 1] - x) <
            Math.abs(dar[insertionPoint] - x))
             return insertionPoint - 1;
        else return insertionPoint;
    }

    /**
     * This reduces a hash code (currently to a 10 digit unsigned number --
     * no loss of information).
     * For much stronger than hashCode, use String2.md5Hex or even String2.md5Hex12.
     *
     * @param hashCode
     * @return the reduced version 
     */
    public static String reduceHashCode(int hashCode) {
        return Long.toString((long)hashCode - (long)Integer.MIN_VALUE); //-negativeNumber  is like +positiveNumber
    }


    /** 
     * This returns the lesser value.
     * If one is NaN and the other isn't, this returns the other.
     */
    public static double finiteMin(double a, double b) {
        return Double.isNaN(a)? b :
               Double.isNaN(b)? a : Math.min(a,b);
    }

    /** 
     * This returns the greater value.
     * If one is NaN and the other isn't, this returns the other.
     */
    public static double finiteMax(double a, double b) {
        return Double.isNaN(a)? b :
               Double.isNaN(b)? a : Math.max(a,b);
    }

} //End of Math2 class.

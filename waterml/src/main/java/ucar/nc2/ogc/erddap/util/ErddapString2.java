/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package ucar.nc2.ogc.erddap.util;

/**
 * A class with static String methods that add to native String methods.
 * All are static methods. 
 */
public class ErddapString2 {
    /**
     * ERROR is a constant so that it will be consistent, so that one can
     * search for it in output files.
     * This is NOT final, so EDStatic can change it.
     * This is the original definition, referenced by many other classes.
     */
    public static String ERROR = "ERROR";

    /**
     * Finds the first instance of c at or after fromIndex (0.. ) in cArray.
     *
     * @param cArray
     * @param c the char you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The first instance of c. If not found, it returns -1.
     */
    public static int indexOf(char[] cArray, char c, int fromIndex) {
        int cArrayLength = cArray.length;
        for (int index = Math.max(fromIndex, 0); index < cArrayLength; index++) {
            if (cArray[index] == c) 
                return index;
        }
        return -1;
    }

    /**
     * This includes hiASCII/ISO Latin 1/ISO 8859-1, but not extensive unicode characters.
     * Letters are A..Z, a..z, and #192..#255 (except #215 and #247).
     * For unicode characters, see Java Lang Spec pg 14.
     *
     * @param c a char
     * @return true if c is a letter
     */
    public static boolean isLetter(int c) {
        //return (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
        //|| ((c >= '\u00c0') && (c <= '\u00FF') && (c != '\u00d7')
        //&& (c != '\u00f7')));
        if (c <  'A') return false;
        if (c <= 'Z') return true;
        if (c <  'a') return false;
        if (c <= 'z') return true;
        if (c <  '\u00c0') return false;
        if (c == '\u00d7') return false;
        if (c <= '\u00FF') return true;
        return false;
    }

    /**
     * 0..9.
     * Non-Latin numeric characters are not included (see Java Lang Spec pg 14).
     *
     * @param c a char
     * @return true if c is a digit
     */
    public static boolean isDigit(int c) {
        return ((c >= '0') && (c <= '9'));
    }

    /**
     * Returns a string where all occurences of <TT>oldCh</TT> have
     *   been replaced with <TT>newCh</TT>.
     * This doesn't throw exceptions if bad values.
     */
    public static String replaceAll(String s, char oldCh, char newCh) {
        int po = s.indexOf(oldCh);
        if (po < 0)
            return s;

        StringBuilder buffer = new StringBuilder(s);
        while (po >= 0) {
            buffer.setCharAt(po, newCh);
            po = s.indexOf(oldCh, po + 1);
        }
        return buffer.toString();
    }

    /**
     * Convert a string to an int.
     * Leading or trailing spaces are automatically removed.
     * This accepts hexadecimal integers starting with "0x".
     * Leading 0's (e.g., 0012) are ignored; number is treated as decimal (not octal as Java would).
     * Floating point numbers are rounded.
     * This won't throw an exception if the number isn't formatted right.
     * To make a string from an int, use ""+i, Integer.toHexString, or Integer.toString(i,radix).
     *
     * @param s is the String representation of a number.
     * @return the int value from the String 
     *    (or Integer.MAX_VALUE if error).
     */
    public static int parseInt(String s) {
        //*** XML.decodeEntities relies on leading 0's being ignored 
        //    and number treated as decimal (not octal)

        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Integer.MAX_VALUE;
        s = s.trim();
        if (s.length() == 0)
            return Integer.MAX_VALUE;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+' && ch != '.')
            return Integer.MAX_VALUE;

        //try to parse hex or regular int        
        try {
            if (s.startsWith("0x")) 
                return Integer.parseInt(s.substring(2), 16);
            return Integer.parseInt(s);
        } catch (Exception e) {      
            //falls through
        }

        //round from double?
        try {
            //2011-02-09 Bob Simons added to avoid Java hang bug.
            //But now, latest version of Java is fixed.
            //if (isDoubleTrouble(s)) return 0;  

            return ErddapMath2.roundToInt(Double.parseDouble(s));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Convert a string to a double.
     * Leading or trailing spaces are automatically removed.
     * This accepts hexadecimal integers starting with "0x".
     * Whole number starting with '0' (e.g., 012) is treated as decimal (not octal as Java would).
     * This won't throw an exception if the number isn't formatted right.
     *
     * @param s is the String representation of a number.
     * @return the double value from the String (a finite value,
     *   Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 
     *   or Double.NaN if error).
     */
    public static double parseDouble(String s) {
        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Double.NaN;
        s = s.trim();
        if (s.length() == 0)
            return Double.NaN;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+' && ch != '.')
            return Double.NaN;

        try {
            if (s.startsWith("0x")) 
                return Integer.parseInt(s.substring(2), 16);

            //2011-02-09 Bob Simons added to avoid Java hang bug.
            //But now, latest version of Java is fixed.
            //if (isDoubleTrouble(s)) return 0;  
            
            return Double.parseDouble(s);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** 
     * This converts String representation of a long. 
     * Leading or trailing spaces are automatically removed.
     * This *doesn't* round. So floating point values lead to Long.MAX_VALUE.
     *
     * @param s a valid String representation of a long value
     * @return a long (or Long.MAX_VALUE if trouble).
     */
    public static long parseLong(String s) {
        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Long.MAX_VALUE;
        s = s.trim();
        if (s.length() == 0)
            return Long.MAX_VALUE;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+')
            return Long.MAX_VALUE;

        try {
            if (s.startsWith("0x"))
                return Long.parseLong(s.substring(2), 16);
            return Long.parseLong(s);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    /** 
     * This converts a double to a rational number (m * 10^t).
     * This is similar to Math2.mantissa and Math2.intExponent, but works via string manipulation
     * to avoid roundoff problems (e.g., with 6.6260755e-24).
     * 
     * @param d
     * @return int[2]: [0]=m, [1]=t.
     *  (or {0, 0} if d=0, or {1, Integer.MAX_VALUE} if !finite(d))
     */
    public static int[] toRational(double d) {        
        if (d == 0)
            return new int[]{0, 0};

        if (!ErddapMath2.isFinite(d))
            return new int[]{1, Integer.MAX_VALUE};

        String s = "" + d; //-12.0 or 6.6260755E-24
        //String2.log("\nd=" + d + "\ns=" + s);
        int ten = 0;

        //remove the e
        int epo = s.indexOf('E');
        if (epo > 0) {
            ten = parseInt(s.substring(epo + 1));
            s = s.substring(0, epo);
            //String2.log("remove E s=" + s + " ten=" + ten);
        }

        //remove .0; remove decimal point
        if (s.endsWith(".0"))
            s = s.substring(0, s.length() - 2);
        int dpo = s.indexOf('.');
        if (dpo > 0) {
            ten -= s.length() - dpo - 1;
            s = s.substring(0, dpo) + s.substring(dpo + 1);
            //String2.log("remove . s=" + s + " ten=" + ten);
        }

        //convert s to long
        //need to lose some precision?
        long tl = parseLong(s);    
        //String2.log("tl=" + tl + " s=" + s);
        while (Math.abs(tl) > 1000000000) {
            tl = Math.round(tl / 10.0);
            ten++;
            //String2.log("tl=" + tl + " ten=" + ten);
        }
        //remove trailing 0's
        while (tl != 0 && tl / 10 == tl / 10.0) {
            tl /= 10;
            ten++;
            //String2.log("remove 0 tl=" + tl + " ten=" + ten);
        }
        //add up to 3 0's?
        if (tl < 100000 && ten >= 1 && ten <= 3) {
            while (ten > 0) {
                tl *= 10;
                ten--;
            }
        }

        return new int[]{(int)tl, ten}; //safe since large values handled above
    }
}

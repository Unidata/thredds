/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import com.cohort.array.Attributes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;
import javax.imageio.ImageIO;

/**
 * This is a Java program to test all of the methods in com.cohort.util.
 *
 */
public class Test {

    public static String utilDir = String2.getClassPath() + "com/cohort/util/";

    /** 
     * This throws a runtime exception with the specified error message. 
     *
     * @param message
     */
    public static void error(String message) throws RuntimeException {
        throw new RuntimeException(message);
    }  

    /** 
     * If the two boolean values aren't equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param b1
     * @param b2 
     * @param message
     */
    public static void ensureEqual(boolean b1, boolean b2, String message)
        throws RuntimeException {
        if (b1 != b2) 
            error("\n" + String2.ERROR + " in Test.ensureEqual(boolean):\n" + 
                message + "\nSpecifically: " + b1 + " != " + b2);
    }  
      
    /** 
     * If the boolean values isn't true, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param b
     * @param message
     */
    public static void ensureTrue(boolean b, String message)
        throws RuntimeException {
        if (!b) 
            error("\n" + String2.ERROR + " in Test.ensureTrue:\n" + message);
    }  
      
    /** 
     * If the two char values aren't equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param c1
     * @param c2 
     * @param message
     */
    public static void ensureEqual(char c1, char c2, String message)
        throws RuntimeException {
        if (c1 != c2) 
            error("\n" + String2.ERROR + " in Test.ensureEqual(char):\n" + 
                message + "\nSpecifically: " + c1 + " != " + c2);
    }  
      
    /** 
     * If the two long values aren't equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param i1
     * @param i2 
     * @param message
     */
    public static void ensureEqual(long i1, long i2, String message)
        throws RuntimeException {
        if (i1 != i2) 
            error("\n" + String2.ERROR + " in Test.ensureEqual(int):\n" + 
                message + "\nSpecifically: " + i1 + " != " + i2);
    }  
      
    /** 
     * If the two int values are equal, 
     * this throws a RuntimeException with the specified message. 
     *
     * @param i1
     * @param i2 
     * @param message
     */
    public static void ensureNotEqual(long i1, long i2, String message)
        throws RuntimeException {
        if (i1 == i2) 
            error("\n" + String2.ERROR + " in Test.ensureNotEqual(int):\n" + 
                message + "\nSpecifically: " + i1 + " = " + i2);
    }  
      
    /** 
     * This returns true if the two float values are almost equal (or both NaN
     * or both infinite). 
     *
     * @param f1
     * @param f2 
     */
    public static boolean equal(float f1, float f2) {
        //special check if both are the same special value
        if (Float.isNaN(f1) && Float.isNaN(f2)) 
            return true;
        if (Float.isInfinite(f1) && Float.isInfinite(f2)) 
            return !(f1 > 0 ^ f2 > 0);
        return Math2.almostEqual(5, f1, f2);
    }  
      
    /** 
     * This returns true if the two double values are almost equal (or both NaN
     * or both infinite). 
     *
     * @param d1
     * @param d2 
     */
    public static boolean equal(double d1, double d2) {
        //special check if both are the same special value
        if (Double.isNaN(d1) && Double.isNaN(d2)) 
            return true;
        if (Double.isInfinite(d1) && Double.isInfinite(d2)) 
            return !(d1 > 0 ^ d2 > 0);
        return Math2.almostEqual(9, d1, d2);
    }  
      
    /** 
     * This returns true if the two String values are equal (or both null). 
     *
     * @param s1
     * @param s2 
     */
    public static boolean equal(String s1, String s2) {
        if (s1 == null && s2 == null) 
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equals(s2);
    }  
      
    /** 
     * If the two float values aren't almost equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param f1
     * @param f2 
     * @param message
     */
    public static void ensureEqual(float f1, float f2, String message)
        throws RuntimeException {
        if (!equal(f1, f2))
            error("\n" + String2.ERROR + " in Test.ensureEqual(float):\n" + 
                message + "\nSpecifically: " + f1 + " != " + f2);
    }  
      
    /** 
     * If the two double values aren't almost equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param d1
     * @param d2 
     * @param message
     */
    public static void ensureEqual(double d1, double d2, String message)
        throws RuntimeException {
        if (!equal(d1, d2))
            error("\n" + String2.ERROR + " in Test.ensureEqual(double):\n" + 
                message + "\nSpecifically: " + d1 + " != " + d2);
    }  
      
    /** 
     * If the two double values are equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param d1
     * @param d2 
     * @param message
     */
    public static void ensureNotEqual(double d1, double d2, String message)
        throws RuntimeException {
        //special check if both are the same special value
        if ((Double.isNaN(d1) && Double.isNaN(d2)) ||
            (Double.isInfinite(d1) && Double.isInfinite(d2)) ||
            (Math2.almostEqual(9, d1, d2)))
            error("\n" + String2.ERROR + " in Test.ensureNotEqual(double):\n" + 
                message + "\nSpecifically: " + d1 + " = " + d2);
    }  
      
    /** 
     * If d is less than min or greater than max, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param d
     * @param minAllowed
     * @param maxAllowed
     * @param message
     */
    public static void ensureBetween(double d, double minAllowed, 
            double maxAllowed, String message) throws RuntimeException {
        
        if (Double.isNaN(d) || d < minAllowed || d > maxAllowed) 
            error("\n" + String2.ERROR + " in Test.ensureBetween:\n" + 
                message + "\nSpecifically: " + d + " isn't between " + minAllowed + " and " + maxAllowed + ".");
    }  
      
    /** 
     * If the two String values aren't equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param s1
     * @param s2 
     * @param message
     */
    public static void ensureEqual(String s1, String s2, String message)
        throws RuntimeException {
        String result = testEqual(s1, s2, message);
        if (result.length() == 0)
            return;
        error(result);
    }  

    /** 
     * This returns "" if the Strings are equal or an error message if not. 
     * This won't throw an exception.
     *
     * @param s1
     * @param s2 
     * @param message
     */
    public static String testEqual(String s1, String s2, String message) {
        if (s1 == null && s2 == null)
            return "";
        if (s1 == null && s2 != null)
            return "\n" + String2.ERROR + " in Test.ensureEqual(Strings):\n" + 
                message + "\nSpecifically: " +                 
                "s1=[null]\n" +
                "s2=" + String2.noLongerThan(s2, 70);
        if (s1 != null && s2 == null)
            return "\n" + String2.ERROR + " in Test.ensureEqual(Strings):\n" + 
                message + "\nSpecifically:\n" +                 
                "s1=" + String2.noLongerThan(s1, 70) +"\n" +
                "s2=[null]";
        if (s1.equals(s2)) 
            return "";

        //generate the error message
        int po = 0;
        int line = 1;
        int lastNewlinePo = -1;
        if (s1 != null && s2!=null)
            while (po < s1.length() && po < s2.length() && s1.charAt(po) == s2.charAt(po)) {
                if (s1.charAt(po) == '\n') {line++; lastNewlinePo = po;}
                po++;
            }
        String c1 = po >= s1.length()? "" : String2.annotatedString("" + s1.charAt(po));
        String c2 = po >= s2.length()? "" : String2.annotatedString("" + s2.charAt(po));
        //find end of lines
        int line1End = po;
        int line2End = po;
        while (line1End < s1.length() && s1.charAt(line1End) != '\n') line1End++;
        while (line2End < s2.length() && s2.charAt(line2End) != '\n') line2End++;
        String line1Sample = String2.annotatedString(s1.substring(lastNewlinePo+1, line1End));
        String line2Sample = String2.annotatedString(s2.substring(lastNewlinePo+1, line2End));
        String annS1 = String2.annotatedString(s1);
        String annS2 = String2.annotatedString(s2);

        String lineString = "line=" + line + ": ";
        return "\n" + String2.ERROR + " in Test.ensureEqual(Strings) line=" + 
            line + " col=" + (po - lastNewlinePo) + " '" + c1 + "'!='" + c2+ "':\n" + 
            message + "\nSpecifically:\n" +                 
            "s1 " + lineString + line1Sample + "\n" +
            "s2 " + lineString + line2Sample + "\n" +
            String2.makeString(' ', (3 + lineString.length() + po - lastNewlinePo - 1)) + "^" + "\n" +
            (line > 1? "\"" + annS1 + "\" != \n\"" + annS2 + "\""  : "");
    }  

    /** 
     * If the two GregorianCalendar values aren't equal, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param g1
     * @param g2 
     * @param message
     */
    public static void ensureEqual(GregorianCalendar g1, GregorianCalendar g2, String message)
        throws RuntimeException {
        if (g1 == null && g2 == null)
            return;
        if (g1 == null && g2 != null)
            error("\n" + String2.ERROR + " in Test.ensureEqual(GregorianCalendar):\n" + 
                message + "\nSpecifically: " +                 
                "g1=[null]\n" +
                "g2=" + Calendar2.formatAsISODateTimeT(g2));
        if (g1 != null && g2 == null)
            error("\n" + String2.ERROR + " in Test.ensureEqual(GregorianCalendar):\n" + 
                message + "\nSpecifically:\n" +                 
                "g1=" + Calendar2.formatAsISODateTimeT(g1) + "\n" +
                "g2=[null]");
        if (!g1.equals(g2)) {
            error("\n" + String2.ERROR + " in Test.ensureEqual(GregorianCalendar):\n" +
                message + "\nSpecifically: " +                 
                Calendar2.formatAsISODateTimeT(g1) + " != " +
                Calendar2.formatAsISODateTimeT(g2));
        }
    }  

    /** 
     * If the object is null, this throws a RuntimeException 
     * with the specified message. 
     *
     * @param o
     * @param message
     */
    public static void ensureNotNull(Object o, String message)
        throws RuntimeException {
        if (o == null)
            error("\n" + String2.ERROR + " in Test.ensureNotNull:\n" + 
                message);
    }  

    /** 
     * If the string is null or "", this throws a RuntimeException 
     * with the specified message. 
     *
     * @param s
     * @param message
     */
    public static void ensureNotNothing(String s, String message)
        throws RuntimeException {
        if (s == null || s.length() == 0)
            error("\n" + String2.ERROR + " in Test.ensureNotNothing:\n" + 
                message);
    }  

    /** 
     * If the object is null or any character is not String2.isPrintable or newline or tab, 
     * this throws a RuntimeException with the specified message. 
     *
     * @param s a String
     * @param message the message to be included in the error message (if there is one)
     */
    public static void ensurePrintable(String s, String message)
        throws RuntimeException {
        ensureNotNull(s, message);
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (String2.isPrintable(ch) || ch == '\n' || ch == '\t') {}
            else error("\n" + String2.ERROR + " in Test.ensurePrintable:\n" + message + 
                "\nTrouble: [" + (int)ch + "] at position " + i + " in:\n" +  //safe type conversion
                String2.annotatedString(s));
        }
    }  

    /** 
     * If the object is null or any character is not ASCII (32 - 126) or newline, 
     * this throws a RuntimeException with the specified message. 
     *
     * @param s a String
     * @param message the message to be included in the error message (if there is one)
     */
    public static void ensureASCII(String s, String message)
        throws RuntimeException {
        ensureNotNull(s, message);
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if ((ch >= 32 && ch <= 126) || ch == '\n') {}
            else error("\n" + String2.ERROR + " in Test.ensureASCII:\n" + message + 
                "\nTrouble: [" + (int)ch + "] at position " + i + " in:\n" + //safe type conversion
                String2.annotatedString(s));
        }
    }  

    /** 
     * If the toString values of the arrays aren't equal, this throws a 
     * RuntimeException with the specified message. 
     *
     * @param ar1
     * @param ar2 
     * @param message
     */
    public static void ensureEqual(Object ar1[], Object ar2[], String message)
        throws RuntimeException {
        if (ar1 == null && ar2 == null)
            return;
        ensureEqual(ar1.length, ar2.length, 
            String2.ERROR + " in Test.ensureEqual(Object[].length): " + message + 
                "\n  ar1=" + String2.toNewlineString(ar1) +
                "\n  ar2=" + String2.toNewlineString(ar2));
        for (int i = 0; i < ar1.length; i++)
            ensureEqual(ar1[i].toString(), ar2[i].toString(), 
                String2.ERROR + " in Test.ensureEqual(Object[" + i + "]): " + message);
    }  

    private final static String errorInObjectEquals = 
        "\n" + String2.ERROR + " in Test.ensureEqual(object.equals):\n";

    /** 
     * If !a.equals(b), this throws a RuntimeException with the 
     * specified message. 
     *
     * @param a
     * @param b 
     * @param message
     */
    public static void ensureEqual(Object a, Object b, String message) {
        if (a == null && b == null)
            return;
        if ((a == null) && (b != null))
            error(errorInObjectEquals + message + "\nSpecifically: a=null");
        if ((a != null) && (b == null))
            error(errorInObjectEquals + message + "\nSpecifically: b=null");

        //test for some things that have no equals method
        if (a instanceof byte[] && b instanceof byte[]) {
            byte aar[] = (byte[])a;
            byte bar[] = (byte[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na byte[] length != b byte[] length");
            for (int i = 0; i < an; i++)
                if (aar[i] != bar[i])
                    Test.error(errorInObjectEquals + message + 
                        "\na byte[" + i + "]=" + aar[i] + " != b byte[" + i + "]=" + bar[i] + ".");
            return;
        }
        if (a instanceof char[] && b instanceof char[]) {
            char aar[] = (char[])a;
            char bar[] = (char[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na char[] length != b char[] length");
            for (int i = 0; i < an; i++)
                if (aar[i] != bar[i])
                    Test.error(errorInObjectEquals + message + 
                        "\na char[" + i + "]=" + (int)aar[i] + " != b char[" + i + "]=" + (int)bar[i] + ".");
            return;
        }
        if (a instanceof short[] && b instanceof short[]) {
            short aar[] = (short[])a;
            short bar[] = (short[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na short[] length != b short[] length");
            for (int i = 0; i < an; i++)
                if (aar[i] != bar[i])
                    Test.error(errorInObjectEquals + message + 
                        "\na short[" + i + "]=" + aar[i] + " != b short[" + i + "]=" + bar[i] + ".");
            return;
        }
        if (a instanceof int[] && b instanceof int[]) {
            int aar[] = (int[])a;
            int bar[] = (int[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na int[] length != b int[] length");
            for (int i = 0; i < an; i++)
                if (aar[i] != bar[i])
                    Test.error(errorInObjectEquals + message + 
                        "\na int[" + i + "]=" + aar[i] + " != b int[" + i + "]=" + bar[i] + ".");
            return;
        }
        if (a instanceof long[] && b instanceof long[]) {
            long aar[] = (long[])a;
            long bar[] = (long[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na long[] length != b long[] length");
            for (int i = 0; i < an; i++)
                if (aar[i] != bar[i])
                    Test.error(errorInObjectEquals + message + 
                        "\na long[" + i + "]=" + aar[i] + " != b long[" + i + "]=" + bar[i] + ".");
            return;
        }
        if (a instanceof float[] && b instanceof float[]) {
            float aar[] = (float[])a;
            float bar[] = (float[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na float[] length != b float[] length");
            for (int i = 0; i < an; i++)
                if (!equal(aar[i], bar[i]))
                    Test.error(errorInObjectEquals + message + 
                        "\na float[" + i + "]=" + aar[i] + " != b float[" + i + "]=" + bar[i] + ".");
            return;
        }
        if (a instanceof double[] && b instanceof double[]) {
            double aar[] = (double[])a;
            double bar[] = (double[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na double[] length != b double[] length");
            for (int i = 0; i < an; i++)
                if (!equal(aar[i], bar[i]))
                    Test.error(errorInObjectEquals + message + 
                        "\na double[" + i + "]=" + aar[i] + " != b double[" + i + "]=" + bar[i] + ".");
            return;
        }
        if (a instanceof String[] && b instanceof String[]) {
            String aar[] = (String[])a;
            String bar[] = (String[])b;
            int an = aar.length;
            int bn = bar.length;
            ensureEqual(an, bn, 
                errorInObjectEquals + message + "\na String[] length != b String[] length");
            for (int i = 0; i < an; i++)
                if (!aar[i].equals(bar[i]))
                    Test.error(errorInObjectEquals + message + 
                        "\na String[" + i + "]=\"" + aar[i] + "\" != b String[" + i + "]=\"" + bar[i] + "\".");
            return;
        }
        if (a instanceof StringBuilder && b instanceof StringBuilder) {
            ensureEqual(a.toString(), b.toString(), message);
            return;
        }

        //fall through to most general case
        if (!a.equals(b))
            error(errorInObjectEquals + message + "\nSpecifically:\n" +
                "a(" + a.getClass().getName() + ")=" + a.toString() + "\n" +
                "b(" + b.getClass().getName() + ")=" + b.toString());
    }

    /** 
     * This ensures s is something (not null or "") and is fileNameSave (see String2.isFileNameSafe(s)).
     *
     * @param s
     * @param message ending with the item's name
     */
    public static void ensureFileNameSafe(String s, String message)
        throws RuntimeException {
        if (!String2.isFileNameSafe(s)) 
            error("\n" + String2.ERROR + " in Test.ensureFileNameSafe():\n" + 
                message + "=\"" + String2.annotatedString(s) + "\" must have length>0 and must contain only safe characters " + 
                String2.fileNameSafeDescription + ".");
    }

    /** 
     * This ensures s is something (not null or "") and is valid utf-8 (see String2.findInvalidUtf8(s, "\n\t")).
     *
     * @param s
     * @param message ending with the item's name
     */
    public static void ensureSomethingUtf8(String s, String message)
        throws RuntimeException {
        if (s == null || s.trim().length() == 0)
            error("\n" + String2.ERROR + " in Test.ensureSomethingUtf8():\n" + 
                message + " wasn't set.");

        int po = String2.findInvalidUtf8(s, "\r\n\t");
        if (po >= 0) {
            int max = Math.min(po + 20, s.length());
            error("\n" + String2.ERROR + " in Test.ensureSomthingUtf8():\n" + 
                message + " has an invalid UTF-8 character (#" + (int)s.charAt(po) + ") at position=" + po + 
                (po > 80? 
                  "\npartial s=\"" + String2.annotatedString(s.substring(po - 20, max)) + "\"" :
                  "\ns=\"" + String2.annotatedString(s) + "\""));
        }
    }  

    /** 
     * This ensures atts isn't null, and the names and attributes in atts are something (not null or "") 
     * and are valid utf-8 (see String2.findInvalidUtf8(s, "\n\t")).
     * 0 names+attributes is valid.
     *
     * @param atts
     * @param message 
     */
    public static void ensureSomethingUtf8(Attributes atts, String message)
        throws RuntimeException {

        if (atts == null)
            error("\n" + String2.ERROR + " in Test.ensureSomethingUtf8():\n" + 
                message + " wasn't set.");
        String names[] = atts.getNames();
        int n = names.length;
        for (int i = 0; i < n; i++) {
            ensureSomethingUtf8(names[i], message + ": an attribute name");
            ensureSomethingUtf8(atts.get(names[i]).toString(), message + ": the attribute value for name=" + names[i]);
        }
    }  
      


}



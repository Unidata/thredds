/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import com.cohort.array.StringComparatorIgnoreCase;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class with static String methods that add to native String methods.
 * All are static methods. 
 */
public class String2 {

    /** the source code version number. (Obviously, this is not really used.) */
    public static final double version = 1.000;

    /**
     * ERROR is a constant so that it will be consistent, so that one can 
     * search for it in output files.
     * This is NOT final, so EDStatic can change it.
     * This is the original definition, referenced by many other classes.
     */
    public static String ERROR = "ERROR";

    //public static Logger log = Logger.getLogger("com.cohort.util");
    private static boolean logToSystemOut = false;
    private static boolean logToSystemErr = true;
    private static BufferedWriter logFile;
    private static String logFileName;
    /** 2=flush after every other write.  Set this to 1 to flush every time.*/
    public static int logFileFlushEveryNth = 2; 
    private static int logFileFlushCount = 0;
    private static StringBuffer logStringBuffer; //thread-safe (writing is synchronized but many threads may read)
    private static int logMaxSize;
    private static long logFileSize;

    /**
     * This returns the line separator from
     *  <code>System.getProperty("line.separator");</code>
     */
    public static String lineSeparator = System.getProperty("line.separator");

    /** Returns true if the current Operating System is Windows. */
    public static boolean OSIsWindows =
        System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
    /** Returns true if the current Operating System is Linux. */
    public static boolean OSIsLinux =
        System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
    /** Returns true if the current Operating System is Mac OS X. */
    public static boolean OSIsMacOSX =
        System.getProperty("mrj.version") != null;

    /** These are NOT thread-safe.  Always use them in synchronized blocks ("synchronized(gen....) {}").*/
    private static DecimalFormat genStdFormat6 = new DecimalFormat("0.######");
    private static DecimalFormat genEngFormat6 = new DecimalFormat("##0.#####E0");
    private static DecimalFormat genExpFormat6 = new DecimalFormat("0.######E0");
    private static DecimalFormat genStdFormat10 = new DecimalFormat("0.##########");
    private static DecimalFormat genEngFormat10 = new DecimalFormat("##0.#########E0");
    private static DecimalFormat genExpFormat10 = new DecimalFormat("0.##########E0");

    private static String classPath; //lazy creation by getClassPath

    private static Map canonicalMap = new WeakHashMap();


    /**
     * This returns the string which sorts higher.
     * null sorts low.
     *
     * @param s1
     * @param s2
     * @return the string which sorts higher.
     */
    public static String max(String s1, String s2) {
        if (s1 == null)
            return s2;
        if (s2 == null)
            return s1;
        return s1.compareTo(s2) >= 0? s1 : s2;
    }

    /**
     * This returns the string which sorts lower.
     * null sorts low.
     *
     * @param s1
     * @param s2
     * @return the string which sorts lower.
     */
    public static String min(String s1, String s2) {
        if (s1 == null)
            return s1;
        if (s2 == null)
            return s2;
        return s1.compareTo(s2) < 0? s1 : s2;
    }

    /**
     * This makes a new String of specified length, filled with ch.
     * For safety, if length>=1000000, it returns "".
     * 
     * @param ch the character to fill the string
     * @param length the length of the string
     * @return a String 'length' long, filled with ch.
     *    If length < 0 or >= 1000000, this returns "".
     */
    public static String makeString(char ch, int length) {
        if ((length < 0) || (length >= 1000000))
            return "";

        char[] car = new char[length];
        Arrays.fill(car, ch);
        return new String(car);
    }

    /**
     * Returns a String 'length' long, with 's' right-justified  
     * (using spaces as the added characters) within the resulting String.
     * If s is already longer, then there will be no change.
     * 
     * @param s is the string to be right-justified.
     * @param length is desired length of the resulting string.
     * @return 's' right-justified to make the result 'length' long.
     */
    public static String right(String s, int length) {
        int toAdd = length - s.length();

        if (toAdd <= 0)
            return s;
        else
            return makeString(' ', toAdd).concat(s);
    }

    /**
     * Returns a String 'length' long, with 's' left-justified  
     * (using spaces as the added characters) within the resulting String.  
     * If s is already longer, then there will be no change.
     * 
     * @param s is the string to be left-justified.
     * @param length is desired length of the resulting string.
     * @return 's' left-justified to make the result 'length' long.
     */
    public static String left(String s, int length) {
        int toAdd = length - s.length();

        if (toAdd <= 0)
            return s;
        else
            return s.concat(makeString(' ', toAdd));
    }

    /**
     * Returns a String 'length' long, with 's' centered  
     * (using spaces as the added characters) within the resulting String.  
     * If s is already longer, then there will be no change.
     * 
     * @param s is the string to be centered.
     * @param length is desired length of the resulting string.
     * @return 's' centered to make the result 'length' long.
     */
    public static String center(String s, int length) {
        int toAdd = length - s.length();

        if (toAdd <= 0)
            return s;
        else
            return makeString(' ', toAdd / 2) + s
            + makeString(' ', toAdd - (toAdd / 2));
    }

    /**
     * This returns a string no more than max characters long, throwing away the excess.
     * If you want to keep the whole string and just insert newlines periodically, 
     * use noLongLines() instead.
     *
     * @param s
     * @param max
     * @return s (if it is short) or the first max characters of s
     */
    public static String noLongerThan(String s, int max) {
        if (s == null)
            return "";
        if (s.length() <= max)  
            return s;
        return s.substring(0, max);
    }

    /**
     * This converts non-isPrintable characters to "[#]".
     * \\n generates both [10] and a newline character.
     *
     * @param s the string
     * @return s, but with non-32..126 characters replaced by [#].
     *    The result ends with "[end]".
     *    null returns "[null][end]".
     */
    public static String annotatedString(String s) {
        if (s == null) 
            return "[null][end]";
        int sLength = s.length();
        StringBuilder buffer = new StringBuilder(sLength / 5 * 6);

        for (int i = 0; i < sLength; i++) {
            char ch = s.charAt(i);

            if (ch >= 32 && ch <= 126) {
                buffer.append(ch);
            } else {
                buffer.append("[" + ((int) ch) + "]");  //safe char to int type conversion
                if (ch == '\n') 
                    buffer.append('\n');
            }
        }

        buffer.append("[end]");
        return buffer.toString();
    }


    /**
     * This determines the number of initial characters that match.
     * 
     * @param s1
     * @param s2
     * @return the number of characters that are the same at the start
     *   of both strings.
     */
    public static int getNMatchingCharacters(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++)
            if (s1.charAt(i) != s2.charAt(i))
                return i;
        return minLength;
    }
   
    /**
     * Finds the first instance of 'find' at or after fromIndex (0..), ignoring case.
     *
     * @param s
     * @param find
     * @param fromIndex
     * @return the first instance of 'find' at or after fromIndex (0..), ignoring case.
     */
    public static int indexOfIgnoreCase(String s, String find, int fromIndex) {
        if (s == null) 
            return -1;
        int sLength = s.length();
        if (sLength == 0)
            return -1;
        find = find.toLowerCase();
        int findLength = find.length();
        if (findLength == 0)
            return fromIndex;
        int maxPo = sLength - findLength;
        char ch0 = find.charAt(0);

        int po = fromIndex;
        while (po <= maxPo) {
            if (Character.toLowerCase(s.charAt(po)) == ch0) {
                int f2 = 1;
                while (f2 < findLength && 
                    Character.toLowerCase(s.charAt(po + f2)) == find.charAt(f2))
                    f2++;
                if (f2 == findLength)
                    return po;
            }
            po++;
        }
        return -1;
    }

    /**
     * Finds the first instance of s at or after fromIndex (0.. ) in sb.
     *
     * @param sb a StringBuilder
     * @param s the String you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The starting position of s. If s is null or not found, it returns -1.
     */
    public static int indexOf(StringBuilder sb, String s, int fromIndex) {
        if (s == null) 
            return -1;
        int sLength = s.length();
        if (sLength == 0)
            return -1;

        char ch = s.charAt(0);
        int index = Math.max(fromIndex, 0);
        int tSize = sb.length() - sLength + 1; //no point in searching last few char
        while (index < tSize) {
            if (sb.charAt(index) == ch) {
                int nCharsMatched = 1;
                while ((nCharsMatched < sLength)
                        && (sb.charAt(index + nCharsMatched) == s.charAt(nCharsMatched)))
                    nCharsMatched++;
                if (nCharsMatched == sLength)
                    return index;
            }

            index++;
        }

        return -1;
    }

    /**
     * This returns the first section of s (starting at fromIndex) 
     * which matches regex.
     *
     * @param s the source String
     * @param regex the regular expression, see java.util.regex.Pattern.
     * @param fromIndex the starting index in s
     * @return the section of s which matches regex, or null if not found
     * @throws Exception if trouble
     */
    public static String extractRegex(String s, String regex, int fromIndex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        if (m.find(fromIndex)) 
            return s.substring(m.start(), m.end());
        return null; 
    }

    /**
     * This returns all the sections of s that match regex.
     * It assumes that the extracted parts don't overlap.
     *
     * @param s the source String
     * @param regex the regular expression, see java.util.regex.Pattern.
     *    Note that you often want to use the "reluctant" qualifiers
     *    which match as few chars as possible (e.g., ??, *?, +?)
     *    not the "greedy"  qualifiers
     *    which match as many chars as possible (e.g., ?, *, +).
     * @return a String[] with all the matching sections of s (or String[0] if none)
     * @throws Exception if trouble
     */
    public static String[] extractAllRegexes(String s, String regex) {
        ArrayList al = new ArrayList();
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        int fromIndex = 0;
        while (m.find(fromIndex)) {
            al.add(s.substring(m.start(), m.end()));
            fromIndex = m.end();
        }
        return toStringArray(al.toArray());
    }

    /**
     * Finds the first instance of i at or after fromIndex (0.. ) in iArray.
     *
     * @param iArray
     * @param i the int you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The first instance of i. If not found, it returns -1.
     */
    public static int indexOf(int[] iArray, int i, int fromIndex) {
        int iArrayLength = iArray.length;
        for (int index = Math.max(fromIndex, 0); index < iArrayLength; index++) {
            if (iArray[index] == i) 
                return index;
        }
        return -1;
    }

    /**
     * Finds the first instance of i in iArray.
     *
     * @param iArray
     * @param i the int you want to find
     * @return The first instance of i. If not found, it returns -1.
     */
    public static int indexOf(int[] iArray, int i) {
        return indexOf(iArray, i, 0);
    }

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
     * Finds the first instance of c in cArray.
     *
     * @param cArray
     * @param c the char you want to find
     * @return The first instance of c. If not found, it returns -1.
     */
    public static int indexOf(char[] cArray, char c) {
        return indexOf(cArray, c, 0);
    }

    /**
     * This indexOf is a little different: it finds the first instance in s of any char in car.
     *
     * @param s a string 
     * @param car the chars you want to find any of (perhaps from charListString.toCharArray())
     * @param fromIndex the index number of the position to start the search
     * @return The first instance in s of any char in car. If not found, it returns -1.
     */
    public static int indexOf(String s, char[] car, int fromIndex) {
        int sLength = s.length();
        for (int index = Math.max(fromIndex, 0); index < sLength; index++) {
            if (indexOf(car, s.charAt(index)) >= 0)
                return index;
        }
        return -1;
    }
    


    /**
     * Finds the first instance of d at or after fromIndex (0.. ) in dArray
     * (tested with Math2.almostEqual5).
     *
     * @param dArray
     * @param d the double you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The first instance of d. If not found, it returns -1.
     */
    public static int indexOf(double[] dArray, double d, int fromIndex) {
        int dArrayLength = dArray.length;
        for (int index = Math.max(fromIndex, 0); index < dArrayLength; index++) {
            if (Math2.almostEqual(5, dArray[index], d)) 
                return index;
        }
        return -1;
    }

    /**
     * Finds the first instance of d in dArray
     * (tested with Math2.almostEqual5).
     *
     * @param dArray
     * @param d the double you want to find
     * @return The first instance of d. If not found, it returns -1.
     */
    public static int indexOf(double[] dArray, double d) {
        return indexOf(dArray, d, 0);
    }

    /**
     

    /**
     * This is a variant of readFromFile that uses the default character set 
     * and 3 tries (1 second apart) to read the file. 
     */
    public static String[] readFromFile(String fileName) {
        return readFromFile(fileName, null, 3);
    }

    /**
     * This is a variant of readFromFile that uses the specified character set
     * and 3 tries (1 second apart) to read the file.
     */
    public static String[] readFromFile(String fileName, String charset) {
        return readFromFile(fileName, charset, 3);
    }

    /**
     * This reads the text contents of the specified file.
     * This assumes the file uses the default character encoding.
     * 
     * <P>This method uses try/catch to ensure that all possible
     * exceptions are caught and returned as the error String
     * (throwable.toString()).
     * 
     * <P>This method is generally appropriate for small and medium-sized
     * files. For very large files or files that need additional processing,
     * it may be more efficient to write a custom method to
     * read the file line-by-line, processing as it goes.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
     * @param maxAttempt e.g. 3   (the tries are 1 second apart)
     * @return a String array with two strings.
     *     Using a String array gets around Java's limitation of
     *         only returning one value from a method.
     *     String #0 is an error String (or "" if no error).
     *     String #1 has the contents of the file
     *         (with any end-of-line characters converted to \n).
     *     If the error String is not "", String #1
     *         may not have all the contents of the file.
     *     ***This ensures that the last character in the file (if any) is \n.
     *     This behavior varies from other implementations of readFromFile.
     */
    public static String[] readFromFile(String fileName, String charset, int maxAttempt) {

        //declare the BufferedReader variable
        //declare the results variable: String results[] = {"", ""}; 
        //BufferedReader and results are declared outside try/catch so 
        //that they can be accessed from within either try/catch block.
        long time = System.currentTimeMillis();
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader bufferedReader = null;
        String results[] = {"", ""};
        int errorIndex = 0;
        int contentsIndex = 1;

        try {
            //open the file
            //To deal with problems in multithreaded apps 
            //(when deleting and renaming files, for an instant no file with that name exists),
            maxAttempt = Math.max(1, maxAttempt);
            for (int attempt = 1; attempt <= maxAttempt; attempt++) {
                try {
                    fis = new FileInputStream(fileName);
                    isr = new InputStreamReader(fis, 
                        charset == null || charset.length() == 0? "ISO-8859-1" : charset);
                } catch (Exception e) {
                    if (attempt == maxAttempt) {
                        log(ERROR + ": String2.readFromFile was unable to read " + fileName);
                        throw e;
                    } else {
                        log("WARNING #" + attempt + 
                            ": String2.readFromFile is having trouble. It will try again to read " + 
                            fileName);
                        if (attempt == 1) Math2.gc(1000);
                        else Math2.sleep(1000);
                    }
                }
            }
            bufferedReader = new BufferedReader(isr);
                         
            //get the text from the file
            //This uses bufferedReader.readLine() to repeatedly
            //read lines from the file and thus can handle various 
            //end-of-line characters.
            //The lines (with \n added at the end) are added to a 
            //StringBuilder.
            StringBuilder sb = new StringBuilder(8192);
            String s = bufferedReader.readLine();
            while (s != null) { //null = end-of-file
                sb.append(s);
                sb.append('\n');
                s = bufferedReader.readLine();
            }

            //save the contents as results[1]
            results[contentsIndex] = sb.toString();

        } catch (Exception e) {
            results[errorIndex] = MustBe.throwable("fileName=" + fileName, e);
        }

        //close the bufferedReader
        try {
            //close the highest level file object available
            if (bufferedReader != null) bufferedReader.close();
            else if (isr       != null) isr.close();
            else if (fis       != null) fis.close();

        } catch (Exception e) {
            if (results[errorIndex].length() == 0)
                results[errorIndex] = e.toString(); 
            //else ignore the error (the first one is more important)
        }

        //return results
        //log("  String2.readFromFile " + fileName + " time=" + 
        //    (System.currentTimeMillis() - time));
        return results;
    }

    /*
    Here is a skeleton for more direct control of reading text from a file: 
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));                      
            String s;
            while ((s = bufferedReader.readLine()) != null) { //null = end-of-file
                //do something with s
                //for example, split at whitespace: String fields[] = s.split("\\s+"); //s = whitespace regex
                }

            bufferedReader.close();
        } catch (Exception e) {
            System.err.println(error + "while reading file '" + filename + "':\n" + e);
            e.printStackTrace(System.err);
            bufferedReader.close();
        }
    */

    /**
     * This saves some text in a file named fileName.
     * This uses the default character encoding.
     * 
     * <P>This method uses try/catch to ensure that all possible
     * exceptions are caught and returned as the error String
     * (throwable.toString()).
     *
     * <P>This method is generally appropriate for small and medium-sized
     * files. For very large files or files that need additional processing,
     * it may be more efficient to write a custom method to
     * read the file line-by-line, processing as it goes.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param contents has the text that will be written to the file.
     *     contents must use \n as the end-of-line marker.
     *     Currently, this method purposely does not convert \n to the 
     *     operating-system-appropriate end-of-line characters when writing 
     *     to the file (see lineSeparator).
     * @return an error message (or "" if no error).
     */
    public static String writeToFile(String fileName, String contents) {
        return lowWriteToFile(fileName, contents, null, "\n", false);
    }

    /**
     * This is like writeToFile, but it appends the text if the file already 
     * exists. If the file doesn't exist, it makes a new file. 
     */
    public static String appendFile(String fileName, String contents) {
        return lowWriteToFile(fileName, contents, null, "\n", true);
    }

    public static String writeToFile(String fileName, String contents, String charset) {
        return lowWriteToFile(fileName, contents, charset, "\n", false);
    }

    public static String appendFile(String fileName, String contents, String charset) {
        return lowWriteToFile(fileName, contents, charset, "\n", true);
    }

    /**
     * This provides servies to writeToFile and appendFile. 
     * If there is an error and !append, the partial file is deleted.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param contents has the text that will be written to the file.
     *     contents must use \n as the end-of-line marker.
     *     Currently, this method purposely does not convert \n to the 
     *     operating-system-appropriate end-of-line characters when writing 
     *     to the file (see lineSeparator).
     * @param charset e.g., UTF-8; or null or "" for the default (ISO-8859-1 ?)
     * @param lineSeparator is the desired lineSeparator for the outgoing file.
     * @param append if you want to append any existing fileName;
     *   otherwise any existing file is deleted first.
     * @return an error message (or "" if no error).
     */
    private static String lowWriteToFile(String fileName, String contents, 
        String charset, String lineSeparator, boolean append) {
        
        //bufferedWriter and error are declared outside try/catch so 
        //that they can be accessed from within either try/catch block.
        BufferedWriter bufferedWriter = null;
        String error = "";

        try {
            //open the file
            //This uses a BufferedWriter wrapped around a FileWriter
            //to write the information to the file.
            Writer w = charset == null || charset.length() == 0?
                new FileWriter(fileName, append) :
                new OutputStreamWriter(new FileOutputStream(fileName, append), charset);
            bufferedWriter = new BufferedWriter(w);
                         
            //convert \n to operating-system-specific lineSeparator
            if (!lineSeparator.equals("\n"))
                contents = replaceAll(contents, "\n", lineSeparator);
                //since the first String is a regex, you can use "[\\n]" too

            //write the text to the file
            bufferedWriter.write(contents);

            //test speed
            //int start = 0;
            //while (start < contents.length()) {
            //    bufferedWriter.write(contents.substring(start, Math.min(start+39, contents.length())));
            //    start += 39;
            //}

        } catch (Exception e) {
            error = e.toString();
        }

        //make sure bufferedWriter is closed
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();

            }
        } catch (Exception e) {
            if (error.length() == 0)
                error = e.toString(); 
            //else ignore the error (the first one is more important)
        }

        //and delete partial file if error and not appending
        if (error.length() > 0 && !append)
            File2.delete(fileName);

        return error;
    }

    /**
     * A string of Java info (version, vendor).  32 bit.
     */
    public static String javaInfo() {
        String javaVersion = System.getProperty("java.version");
        String mrjVersion = System.getProperty("mrj.version");
        mrjVersion = (mrjVersion == null) ? "" : (" (mrj=" + mrjVersion + ")");
        return "Java " + javaVersion + mrjVersion + " (" + Math2.JavaBits + " bit, " +
            System.getProperty("java.vendor") + ") on " +
            System.getProperty("os.name") + " (" +
            System.getProperty("os.version") + ").";
    }

    /**
     * This includes hiASCII/ISO Latin 1/ISO 8859-1, but not extensive unicode characters.
     * Letters are A..Z, a..z, and #192..#255 (except #215 and #247).
     * For unicode characters, see Java Lang Spec pg 14.
     *
     * @param c a char
     * @return true if c is a letter
     */
    public static final boolean isLetter(int c) {
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
     * First letters for identifiers (e.g., variable names, method names) can be
     * all isLetter()'s plus $ and _.
     *
     * @param c a char
     * @return true if c is a valid character for the first character if a Java ID
     */
    public static final boolean isIDFirstLetter(int c) {
        if (c == '_') return true;
        if (c == '$') return true;
        return isLetter(c);
    }

    /**
     * 0..9, a..f, A..F
     * Hex numbers are 0x followed by hexDigits.
     *
     * @param c a char
     * @return true if c is a valid hex digit
     */
    public static final boolean isHexDigit(int c) {
        //return (((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'f'))
        //|| ((c >= 'A') && (c <= 'F')));
        if (c <  '0') return false;
        if (c <= '9') return true;
        if (c <  'A') return false;
        if (c <= 'F') return true;
        if (c <  'a') return false;
        if (c <= 'f') return true;
        return false;
    }

    /**
     * 0..9.
     * Non-Latin numeric characters are not included (see Java Lang Spec pg 14).
     *
     * @param c a char
     * @return true if c is a digit
     */
    public static final boolean isDigit(int c) {
        return ((c >= '0') && (c <= '9'));
    }

    /**
     * Determines if the character is a digit or a letter.
     *
     * @param c a char
     * @return true if c is a letter or a digit
     */
    public static final boolean isDigitLetter(int c) {
        return isLetter(c) || isDigit(c);
    }

    /**
     * This tries to quickly determine if the string is a correctly
     * formatted number (including decimal, hexadecimal, octal, and "NaN" (any case)).
     *
     * <p>This may not be perfect.  In the future, this may be changed to be perfect.
     * That shouldn't affect its use.
     *
     * @param s  usually already trimmed, since any space in s will return false.
     * @return true if s is *probably* a number.
     *     This returns false if s is *definitely* not a number.
     *     "NaN" (case insensitive) returns true.  (It is a numeric value of sorts.)
     *     null and "" return false.
     */
    public static final boolean isNumber(String s) {
        if (s == null)
            return false;
        int sLength = s.length();
        if (sLength == 0)
            return false;
        char ch0 = s.charAt(0);

        //hexadecimal? e.g., 0x2AFF            //octal not supported
        if (ch0 == '0' && sLength >= 3 && Character.toUpperCase(s.charAt(1)) == 'X') {
            //ensure all remaining chars are hexadecimal
            for (int po = 2; po < sLength; po++) {
                if ("0123456789abcdefABCDEF".indexOf(s.charAt(po)) < 0) 
                    return false;
            }
            return true;
        }

        //NaN?
        if (ch0 == 'n' || ch0 == 'N') 
            return s.toUpperCase().equals("NAN");

        //*** rest of method: test if floating point
        //is 1st char .+-?
        int po = 0;
        char ch = s.charAt(po);
        boolean hasPeriod = ch == '.';
        if (hasPeriod || ch == '-') {
            if (sLength == 1)  //must be another char
                return false;
            ch = s.charAt(++po);

            //is 2nd char .?
            if (ch == '.') {
                if (hasPeriod || sLength == 2)  // 2nd period or . after e are not allowed
                    return false;
                hasPeriod = true;
                ch = s.charAt(++po);
            }
        }
        
        //initial digit
        if (ch < '0' || ch > '9')  //there must be a digit 
            return false;  

        //subsequent chars
        boolean hasE = false;
        while (++po < sLength) {
            ch = s.charAt(po);
            if (ch == '.') {
                if (hasPeriod || hasE || po == sLength - 1)  // 2nd period or . after e are not allowed
                    return false;
                hasPeriod = true;
            } else if (ch == 'e' || ch == 'E') {
                if (hasE || po == sLength - 1) //e as last char is not allowed
                    return false;
                hasE = true;
                ch = s.charAt(++po);
                if (ch == '-' || ch == '+') {
                    if (po == sLength-1)
                        return false;
                    ch = s.charAt(++po);
                }
                if (ch < '0' || ch > '9') //there must be a digit after e     
                    return false;  
            } else if (ch < '0' || ch > '9') { 
                return false;  
            }
        }
        return true;  //probably a number
    }


    /**
     * Whitespace characters are u0001 .. ' '.
     * Java just considers a few of these (sp HT FF) as white space,
     *  see the Java Lang Specification.
     * u0000 is not whitespace.  Some methods count on this fact.
     *
     * @param c a char
     * @return true if c is a whitespace character
     */
    public static final boolean isWhite(int c) {
        return (c >= '\u0001') && (c <= ' ');
    }

    /**
     * This indicates if ch is printable with System.err.println() and
     *   Graphics.drawString(); hence, it is a subset of 0..255.
     * <UL>
     * <LI> This is used, for example, to limit characters entering CoText.
     * <LI> Currently, this accepts the ch if
     *   <TT>(ch>=32 && ch<127) || (ch>=161 && ch<=255)</TT>.
     * <LI> tab(#9) is not included.  It should be caught separately
     *   and dealt with (expand to spaces?).  The problem is that
     *   tabs are printed with a wide box (non-character symbol)
     *   in Windows Courier font.
     *   Thus, they mess up the positioning of characters in CoText.
     * <LI> newline is not included.  It should be caught separately
     *   and dealt with.
     * <LI> This requires further study into all standard fonts on all
     *   platforms to see if other characters can be accepted.
     * </UL>
     *
     * @param ch a char
     * @return true if ch is a printable character
     */
    public static final boolean isPrintable(int ch) {
        //return (ch>=32 && ch<127) || (ch>=161 && ch<=255);  //was 160
        if (ch <   32) return false;
        if (ch <= 126) return true;  //was 127 
        if (ch <  161) return false; //was 160
        if (ch <= 255) return true;
        return false;
    }

    /** Returns true if all of the characters in s are printable */
    public static final boolean isPrintable(String s) {
        if (s == null)
            return false;
        int sLength = s.length();
        for (int i = 0; i < sLength; i++)
            if (!isPrintable(s.charAt(i)))
                return false;
        return true;
    }

    /**
     * This returns the string with all non-isPrintable characters removed.
     *
     * @param s
     * @return s with all the non-isPrintable characters removed
     */
    public static String justPrintable(String s) {
        int n = s.length();
        StringBuilder sb = new StringBuilder(n);
        int start = 0;
        for (int i = 0; i < n; i++) {
            if (!isPrintable(s.charAt(i))) {
                sb.append(s.substring(start, i));
                start = i + 1;
            }
        }
        sb.append(s.substring(start));
        return sb.toString();
    }

    /** This crudely converts 160.. 255 to plainASCII characters which 
     * hava a similar meaning or look similar (or '?' if no good substitute). 
     */
    public final static String plainASCII = 
        " !cLoY|%:Ca<--R-" +
        "o???'uP.,'o>????" +
        "AAAAAAACEEEEIIII" +
        "DNOOOOOxOUUUUYpB" +
        "aaaaaaaceeeeiiii" +
        "onooooo/ouuuuypy";

    /**
     * This converts the string to plain ascii (0..127).
     * Diacritics are stripped off high ASCII characters.
     * Some high ASCII characters are crudely converted to similar characters
     * (the conversion is always character-for-character, 
     * so the string length will be unchanged).
     * Other characters become '?'.
     * The result will be the same length as s.
     *
     * @param s
     * @return the string converted to plain ascii (0..127).
     */
    public static String modifyToBeASCII(String s) {
        StringBuilder sb = new StringBuilder(s);
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = sb.charAt(i);
            if (ch <= 127) {}
            else if (ch >= 160 && ch <= 255) sb.setCharAt(i, plainASCII.charAt(ch - 160));
            else sb.setCharAt(i, '?');
        }
        return sb.toString();
    }

    /**
     * A description of file-name-safe characters.
     */
    public final static String fileNameSafeDescription = "(A-Z, a-z, 0-9, _, -, or .)";

    /**
     * This indicates if ch is a file-name-safe character (A-Z, a-z, 0-9, _, -, or .).
     *
     * @param ch
     * @return true if ch is a file-name-safe character (A-Z, a-z, 0-9, _, -, .).
     */
    public static boolean isFileNameSafe(char ch) {
        //return (ch >= 'A' && ch <= 'Z') ||                
        //       (ch >= 'a' && ch <= 'z') ||                
        //       (ch >= '0' && ch <= '9') ||
        //        ch == '-' || ch == '_' || ch == '.';
        if (ch == '.' || ch == '-') return true;
        if (ch <  '0') return false;
        if (ch <= '9') return true;
        if (ch <  'A') return false;
        if (ch <= 'Z') return true;
        if (ch == '_') return true;
        if (ch <  'a') return false;
        if (ch <= 'z') return true;
        return false;
    }

    /**
     * This indicates if 'email' is a valid email address.
     *
     * @param email a possible email address
     * @return true if 'email' is a valid email address.
     */
    public static boolean isEmailAddress(String email) {
        if (email == null || email.length() == 0)
            return false;

        //regex from http://www.regular-expressions.info/email.html 
        //(with a-z added instead of using case-insensitive regex)
        //(This isn't perfect, but it is probably good enough.)
        return email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
    }

    /**
     * This indicates if 'url' is probably a valid url.
     *
     * @param url a possible url
     * @return true if 'url' is probably a valid url.
     *    false if 'url' is not a valid url.
     */
    public static boolean isUrl(String url) {
        if (url == null)
            return false;
        int po = url.indexOf("://");
        if (po == -1 ||
            !isPrintable(url) ||
            url.indexOf(' ') >= 0)
            return false;

        String protocol = url.substring(0, po).toLowerCase();
        return 
            protocol.equals("file") ||
            protocol.equals("ftp") ||
            protocol.equals("http") ||
            protocol.equals("https") ||
            protocol.equals("sftp") ||
            protocol.equals("smb");
    }


    /**
     * This indicates if s has just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     *
     * @param s a string, usually a file name
     * @return true if s has just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     *    It returns false if s is null or "".
     */
    public static boolean isFileNameSafe(String s) {
        if (s == null || s.length() == 0)
            return false;
        int sLength = s.length();
        for (int i = 0; i < sLength; i++)
            if (!isFileNameSafe(s.charAt(i)))
                return false;
        return true;
    }

    /**
     * This returns the string with just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     * This is different from String2.encodeFileNameSafe --
     *   this emphasizes readability, not avoiding losing information.
     * Non-safe characters are converted to '_'.
     * Adjacent '_' are collapsed into '_'.
     * See posix fully portable file names at http://en.wikipedia.org/wiki/Filename .
     * See javadocs for java.net.URLEncoder, which describes valid characters
     *  (but deals with encoding, whereas this method alters or removes).
     * The result may be shorter than s.
     *
     * @param s  If s is null, this returns "_null".
     *    If s is "", this returns "_".
     * @return s with all of the non-fileNameSafe characters removed or changed
     */
    public static String modifyToBeFileNameSafe(String s) {
        if (s == null)
            return "_null";
        s = modifyToBeASCII(s);
        int n = s.length();
        if (n == 0)
            return "_";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            sb.append(isFileNameSafe(ch)? ch : '_');
        }
        while (sb.indexOf("__") >= 0)
            String2.replaceAll(sb, "__", "_");

        return sb.toString();
    }

    /**
     * This tests if s is a valid variableName:
     * <ul>
     * <li>first character must be (iso8859Letter|_).
     * <li>optional subsequent characters must be (iso8859Letter|_|0-9).
     * <ul>
     * Note that Java allows Unicode characters, but this does not.
     *
     * @param s a possible variable name
     * @return true if s is a valid variableName.
     */
    public static boolean isVariableNameSafe(String s) {
        if (s == null)
            return false;
        int n = s.length();
        if (n == 0)
            return false;

        //first character must be (iso8859Letter|_)
        char ch = s.charAt(0);
        if (isLetter(ch) || ch == '_') ;
        else return false;

        //subsequent characters must be (iso8859Letter|_|0-9)
        for (int i = 1; i < n; i++) {
            ch = s.charAt(i);
            if (isDigitLetter(ch) || ch == '_') ;
            else return false;    
        }
        return true;
    }

    /**
     * This tests if s is a valid jsonp function name.
     * The functionName MUST be a series of 1 or more (period-separated) words.
     * For each word:
     * <ul>
     * <li>The first character must be (iso8859Letter|_).
     * <li>The optional subsequent characters must be (iso8859Letter|_|0-9).
     * <li>s must not be longer than 255 characters.
     * <ul>
     * Note that JavaScript allows Unicode characters, but this does not.
     *
     * @param s a possible jsonp function name
     * @return true if s is a valid jsonp function name.
     */
    public static boolean isJsonpNameSafe(String s) {
        if (s == null)
            return false;
        int n = s.length();
        if (n == 0 || n > 255)
            return false;

        //last (or only) character can't be .
        if (s.charAt(n - 1) == '.')
            return false;

        ArrayList al = splitToArrayList(s, '.', false); //trim=false
        int nal = al.size();

        //test each word
        for (int part = 0; part < nal; part++) {
            String ts = (String)al.get(part);
            int tn = ts.length();
            if (tn == 0)
                return false;

            //first character must be (iso8859Letter|_)
            char ch = ts.charAt(0);
            if (isLetter(ch) || ch == '_') ;
            else return false;

            //subsequent characters must be (iso8859Letter|_|0-9)
            for (int i = 1; i < tn; i++) {
                ch = ts.charAt(i);
                if (isDigitLetter(ch) || ch == '_') ;
                else return false;    
            }
        }

        return true;
    }


    /**
     * This is like modifyToBeFileNameSafe, but restricts the name to:
     * <ul>
     * <li>first character must be (iso8859Letter|_).
     * <li>subsequent characters must be (iso8859Letter|_|0-9).
     * <ul>
     * Note that Java allows Unicode characters, but this does not.
     * See also the safer encodeVariableNameSafe(String s).
     *
     * @param s
     * @return a safe variable name (but perhaps two s's lead to the same result)
     */
    public static String modifyToBeVariableNameSafe(String s) {
        if (s == null)
            return "_null";
        s = replaceAll(s, "%20", "_");
        if (s.indexOf("%3a") >= 0) {
            s = replaceAll(s, "CF%3afeature_type", "featureType"); //CF:feature_type
            s = replaceAll(s, "CF%3a", ""); //CF:
            s = replaceAll(s, "%3a", "_");
        }
        int n = s.length();
        if (n == 0)
            return "_";

        StringBuilder sb = new StringBuilder(n);

        //first character must be (iso8859Letter|_)
        char ch = s.charAt(0);
        sb.append(isLetter(ch)? ch : '_');     //'_' will be converted to '_'

        //subsequent characters must be (iso8859Letter|_|0-9)
        for (int i = 1; i < n; i++) {
            ch = s.charAt(i);
            if (isDigitLetter(ch))
                sb.append(ch);
            else if (sb.charAt(sb.length() - 1) != '_')
                sb.append('_');    
        }

        //remove trailing _
        if (sb.length() > 1 && sb.charAt(sb.length() - 1) == '_')
            sb.setLength(sb.length() - 1);

        return sb.toString();
    }


    /**
     * This counts all occurrences of <TT>findS</TT> in sb.
     * if (sb == null || findS == null || findS.length() == 0) return 0;
     * 
     * @param sb the source StringBuilder
     * @param findS the string to be searched for
     */
    public static int countAll(StringBuilder sb, String findS) {
        if (sb == null || findS == null || findS.length() == 0) return 0;
        int n = 0;
        int sLength = findS.length();
        int po = sb.indexOf(findS, 0);
        while (po >= 0) {
            n++;
            po = sb.indexOf(findS, po + sLength);
        }
        return n;
    }

    /**
     * This counts all occurrences of <TT>findS</TT> in s.
     * if (s == null || findS == null || findS.length() == 0) return 0;
     * 
     * @param s the source string
     * @param findS the string to be searched for
     */
    public static int countAll(String s, String findS) {
        if (s == null || findS == null || findS.length() == 0) return 0;
        int n = 0;
        int sLength = findS.length();
        int po = s.indexOf(findS, 0);
        while (po >= 0) {
            n++;
            po = s.indexOf(findS, po + sLength);
        }
        return n;
    }

    /**
     * Replaces all occurences of <TT>oldS</TT> in sb with <TT>newS</TT>.
     * If <TT>oldS</TT> occurs inside <TT>newS</TT>, it won't be replaced
     *   recursively (obviously).
     * 
     * @param sb the source StringBuilder
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     */
    public static void replaceAll(StringBuilder sb, String oldS, String newS) {
        replaceAll(sb, oldS, newS, false);
    }

    /**
     * Replaces all occurences of <TT>oldS</TT> in sb with <TT>newS</TT>.
     * If <TT>oldS</TT> occurs inside <TT>newS</TT>, it won't be replaced
     *   recursively (obviously).
     * When searching sb for oldS, this ignores the case of sb and oldS.
     * 
     * @param sb the StringBuilder
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     */
    public static void replaceAllIgnoreCase(StringBuilder sb, String oldS, String newS) {
        replaceAll(sb, oldS, newS, true);
    }

    /**
     * Replaces all occurences of <TT>oldS</TT> in sb with <TT>newS</TT>.
     * If <TT>oldS</TT> occurs inside <TT>newS</TT>, it won't be replaced
     *   recursively (obviously).
     * 
     * @param sb the StringBuilder
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     * @param ignoreCase   If true, when searching sb for oldS, this ignores the case of sb and oldS.
     */
    public static void replaceAll(StringBuilder sb, String oldS, String newS, boolean ignoreCase) {
        int sbL = sb.length();
        int oldSL = oldS.length();
        if (oldSL == 0)
            return;
        int newSL = newS.length();
        StringBuilder testSB = sb;
        String testOldS = oldS;
        if (ignoreCase) {
            testSB = new StringBuilder(sbL);
            for (int i = 0; i < sbL; i++)
                testSB.append(Character.toLowerCase(sb.charAt(i)));
            testOldS = oldS.toLowerCase();
        }
        int po = testSB.indexOf(testOldS);
        //System.out.println("testSB=" + testSB.toString() + " testOldS=" + testOldS + " po=" + po); //not String2.log
        if (po < 0) return;
        StringBuilder sb2 = new StringBuilder(sbL / 5 * 6); //a little bigger
        int base = 0;
        while (po >= 0) {
            sb2.append(sb.substring(base, po));
            sb2.append(newS);
            base = po + oldSL;
            po = testSB.indexOf(testOldS, base);
            //System.out.println("testSB=" + testSB.toString() + " testOldS=" + testOldS + " po=" + po + 
            //    " sb2=" + sb2.toString()); //not String2.log
        }
        sb2.append(sb.substring(base));
        sb.setLength(0);
        sb.append(sb2);
    }

    /**
     * Returns a string where all occurences of <TT>oldS</TT> have
     *   been replaced with <TT>newS</TT>.
     * If <TT>oldS</TT> occurs inside <TT>newS</TT>, it won't be replaced
     *   recursively (obviously).
     *
     * @param s the main string
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     * @return a modified version of s, with newS in place of all the olds.
     *   Throws exception if s is null.
     */
    public static String replaceAll(String s, String oldS, String newS) {
        StringBuilder sb = new StringBuilder(s);
        replaceAll(sb, oldS, newS, false);
        return sb.toString();
    }

    /**
     * Returns a string where all occurences of <TT>oldS</TT> have
     *   been replaced with <TT>newS</TT>.
     * If <TT>oldS</TT> occurs inside <TT>newS</TT>, it won't be replaced
     *   recursively (obviously).
     * When finding oldS in s, their case is irrelevant.
     *
     * @param s the main string
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     * @return a modified version of s, with newS in place of all the olds.
     *   Throws exception if s is null.
     */
    public static String replaceAllIgnoreCase(String s, String oldS, String newS) {
        StringBuilder sb = new StringBuilder(s);
        replaceAll(sb, oldS, newS, true);
        return sb.toString();
    }

    /**
     * Returns a string where all cases of more than one space are 
     * replaced by one space.  The string is also trim'd to remove
     * leading and trailing spaces.
     *
     * @param s 
     * @return s, but with the spaces combined
     *    (or null if s is null)
     */
    public static String combineSpaces(String s) {
        if (s == null)
            return null;
        s = s.trim();
        int sLength = s.length();
        int po = s.indexOf("  ");
        if (po < 0)
            return s;
        StringBuilder sb = new StringBuilder(sLength);
        int base = 0;
        while (po >= 0) {
            //one beyond last space
            int end = po + 2;
            while (end < sLength && s.charAt(end) == ' ')
                end++;
            sb.append(s.substring(base, po+1));
            base = end;
            po = s.indexOf("  ", base);
        }
        sb.append(s.substring(base));
        return sb.toString();
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
     * This adds 0's to the left of the string until there are <TT>nDigits</TT>
     *   to the left of the decimal point (or nDigits total if there isn't
     *   a decimal point).
     * If the number is too big, nothing is added or taken away.
     *
     * @param number a positive number.  This doesn't handle negative numbers.
     * @param nDigits the desired number of digits to the left of the decimal point
     * (or total, if no decimal point)
     * @return the number, left-padded with 0's so there are nDigits to 
     * the left of the decimal point
     */
    public static String zeroPad(String number, int nDigits) {
        int decimal = number.indexOf(".");
        if (decimal < 0)
            decimal = number.length();

        int toAdd = nDigits - decimal;
        if (toAdd <= 0)
            return number;

        return makeString('0', toAdd).concat(number);
    }

    /**
     * This makes a JSON version of a string 
     * (\\, \f, \n, \r, \t and \" are escaped with a backslash character
     * and double quotes are added before and after).
     * null is returned as null.
     *
     * @param s
     * @return the JSON-encoded string surrounded by "'s.
     */
    public static String toJson(String s) {
        if (s == null)
            return "null";
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(sLength / 5 * 6);
        sb.append('\"');
        int start = 0;
        for (int i = 0; i < sLength; i++) {
            char ch = s.charAt(i);
            if (ch < 32 || ch > 255) {
                sb.append(s.substring(start, i));  
                start = i + 1;
                if      (ch == '\f') sb.append("\\f");
                else if (ch == '\n') sb.append("\\n");
                else if (ch == '\r') sb.append("\\r");
                else if (ch == '\t') sb.append("\\t");
                else sb.append("\\u" + String2.zeroPad(Integer.toHexString(ch), 4)); 
            } else if (ch == '\\') {
                sb.append(s.substring(start, i));  
                start = i + 1;
                sb.append("\\\\");
            } else if (ch == '\"') {
                sb.append(s.substring(start, i));  
                start = i + 1;
                sb.append("\\\"");
            }  // else normal character will be appended later via s.substring
        }
        sb.append(s.substring(start));  
        sb.append('\"');
        return sb.toString();
    }

   
    /**
     * This returns the unJSON version of a JSON string 
     * (surrounding "'s (if any) are removed and \\, \f, \n, \r, \t and \" are unescaped).
     * This is very liberal in what it accepts, including all common C escaped characters:
     * http://msdn.microsoft.com/en-us/library/h21280bw%28v=vs.80%29.aspx
     * null and "null" are returned as null.
     *
     * @param s  it may be enclosed by "'s, or not.
     * @return the decoded string
     */
    public static String fromJson(String s) {
        if (s == null || s.equals("null"))
            return null;
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            s = s.substring(1, s.length() - 1);
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(sLength);
        int po = 0;
        int start = 0;
        while (po < sLength) {
            char ch = s.charAt(po);
            if (ch == '\\') {
                sb.append(s.substring(start, po)); 
                if (po == sLength - 1) 
                    po--;  //so reread \ and treat as \\
                po++; 
                start = po + 1;
                ch = s.charAt(po);
                if      (ch == 'f') sb.append('\f');
                else if (ch == 'n') sb.append('\n');
                else if (ch == 'r') sb.append('\r');
                else if (ch == 't') sb.append('\t');
                else if (ch == '?') sb.append('?');
                else if (ch == '\\')sb.append('\\');
                else if (ch == '"') sb.append('\"');
                else if (ch == '\'') sb.append('\'');
                else if (ch == 'a' || ch == 'b' || ch == 'v') {
                    //delete a=bell, b=backspace, v=vertTab
                } else if (isDigit(ch) && po + 2 < sLength) { 
                    //  \\ooo octal
                    String os = s.substring(po, po + 3);
                    try {
                        po += 2;
                        start = po + 1;

                        sb.append((char)Integer.parseInt(os, 8));
                    } catch (Exception e) {      
                        log("ERROR in fromJson: invalid escape sequence \\" + os);
                        //falls through
                    }
                    
                } else if (ch == 'x' && po + 2 < sLength) { 
                    //  \\xhh hex
                    String os = s.substring(po + 1, po + 3);
                    try {
                        po += 2;
                        start = po + 1;

                        sb.append((char)Integer.parseInt(os, 16));
                    } catch (Exception e) {      
                        log("ERROR in fromJson: invalid escape sequence \\x" + os);
                        //falls through
                    }
                    
                } else if (ch == 'u' && po + 4 < sLength) { 
                    //  \\uhhhh unicode 
                    String os = s.substring(po + 1, po + 5);
                    try {
                        po += 4;
                        start = po + 1;

                        sb.append((char)Integer.parseInt(os, 16));
                    } catch (Exception e) {      
                        log("ERROR in fromJson: invalid escape sequence \\u" + os);
                        //falls through
                    }                    
                   
                } else { 
                    //this shouldn't happen, but be forgiving
                    //before 2009-02-27, this tossed the \    is this the best solution?
                    log("ERROR in fromJson: invalid escape sequence \\" + ch);
                    sb.append('\\'); 
                    sb.append(ch); 
                } 
            } //else normal char will be appended by s.substring later
            po++;
        }
        sb.append(s.substring(start));
        return sb.toString();
    }


    
    /**
     * This takes a multi-line string (with \\r, \\n, \\r\\n line separators)
     *   and converts it into an ArrayList strings.
     * <ul>
     * <li> Only isPrintable and tab characters are saved.
     * <li> Each line separator generates another line.  So if last char
     *   is a line separator, it generates a blank line at the end.
     * <li> If s is "", this still generates 1 string ("").
     * </ul>
     *
     * @param s the string with internal line separators
     * @return an arrayList of Strings (separate lines of text)
     */
    public static ArrayList multiLineStringToArrayList(String s) {
        char endOfLineChar = s.indexOf('\n') >= 0? '\n' : '\r';
        int sLength = s.length();
        ArrayList arrayList = new ArrayList(); //this is local, so okay if not threadsafe
        StringBuilder oneLine = new StringBuilder(512);
        char ch;
        int start = 0;
        for (int po = 0; po < sLength; po++) {
            ch = s.charAt(po);
            if (!(isPrintable(ch) || ch == '\t')) {
                //unprintable    
                //copy the accumulated printable chars
                oneLine.append(s.substring(start, po));
                start = po + 1;
                if (ch == endOfLineChar) {
                    arrayList.add(oneLine.toString());                
                    oneLine.setLength(0);
                } //else:  other characters are ignored
            }
        }
        arrayList.add(oneLine.toString());
        return arrayList;
    }

    /**
     * This creates an ArrayList with the objects from the enumeration.
     * WARNING: This does not have a sychronized block: if your enumeration
     *    needs thread-safety, wrap this call in somthing like 
     *    <tt>synchronized (enum) {String2.toArrayList(enum); }</tt>.
     *
     * @param e an enumeration
     * @return arrayList with the objects from the enumeration
     */
    public static ArrayList toArrayList(Enumeration e) {
        ArrayList al = new ArrayList();
        while (e.hasMoreElements()) 
            al.add(e.nextElement());
        return al;
    }

    /**
     * This creates an ArrayList from an Object[].
     *
     * @param objectArray an Object[]
     * @return arrayList with the objects
     */
    public static ArrayList toArrayList(Object objectArray[]) {
        int n = objectArray.length;
        ArrayList al = new ArrayList(n);
        for (int i = 0; i < n; i++)
            al.add(objectArray[i]);
        return al;
    }

    /**
     * This returns the standard Help : About message.
     *
     * @return the standard Help : About message
     */
    public static String standardHelpAboutMessage() {
        return
            //"This program includes open source com.cohort classes (version " + 
            //    version + "),\n" +
            //"Copyright(c) 2004 - 2007, CoHort Software.\n" +
            //"For more information, visit www.cohort.com.\n" +
            //"\n" + 
            "This program is using\n" + 
            javaInfo();
    }


    /**
     * This replaces "{0}", "{1}", and "{2}" in msg with s0, s1, s2.
     *
     * @param msg a string which may contain "{0}", "{1}", and/or "{2}".
     * @param s0 the first substitution string. If null, that substitution
     *     won't be attempted.
     * @param s1 the second substitution string. If null, that substitution
     *     won't be attempted.
     * @param s2 the third substitution string. If null, that substitution
     *     won't be attempted.
     * @return the modified msg
     */
    public static String substitute(String msg, String s0, String s1, String s2) {
        StringBuilder msgSB = new StringBuilder(msg);
        if (s0 != null) 
            replaceAll(msgSB, "{0}", s0); 
        if (s1 != null) 
            replaceAll(msgSB, "{1}", s1); 
        if (s2 != null) 
            replaceAll(msgSB, "{2}", s2); 
        return msgSB.toString();
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>WARNING: This does not have a sychronized block: if your enumeration
     *   needs thread-safety, wrap this call in somthing like 
     *   <tt>synchronized (enum) {String2.toArrayList(enum); }</tt>.
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param en an enumeration of objects
     * @return a CSSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(Enumeration en) {
        return toSVString(toArrayList(en).toArray(), ", ", false);
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param al an arrayList of objects
     * @return a CSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(ArrayList al) {
        return toSVString(al.toArray(), ", ", false);
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     *
     * @param v a vector of objects
     * @return a CSSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(Vector v) {
        return toSVString(v.toArray(), ", ", false);
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of objects
     * @return a CSSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(Object ar[]) {
        return toSVString(ar, ", ", false);
    }

    /**
     * Generates a space-separated-value string.  
     * <p>WARNING: This is simplistic. It doesn't do anything special for 
     *   strings with internal spaces.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @return a SSV String with the values with " " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toSSVString(Object ar[]) {
        return toSVString(ar, " ", false);
    }

    /**
     * Generates a tab-separated-value string.  
     * <p>WARNING: This is simplistic. It doesn't do anything special for 
     *   strings with internal tabs.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @return a TSV String with the values with "\t" after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toTSVString(Object ar[]) {
        return toSVString(ar, "\t", false);
    }

    /**
     * Generates a newline-separated string.
     * <p>WARNING: This is simplistic. It doesn't do anything special for 
     *   strings with internal newlines.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @return a String with the values, 
     *    with a '\n' after each value, even the last.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toNewlineString(Object ar[]) {
        return toSVString(ar, "\n", true);
    }

    /**
     * This is used at a low level to generate a 
     * 'separator'-separated-value string (without newlines) 
     * with the element.toString()'s from the array.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @param separator the separator string
     * @param finalSeparator if true, a separator will be added to the 
     *    end of the resulting string (if it isn't "").
     * @return a separator-separated-value String.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toSVString(Object ar[], String separator, boolean finalSeparator) {
        if (ar == null) 
            return null;
        int n = ar.length;
        boolean csv = separator.charAt(0) == ',';
        //8 bytes is lame estimate of bytes/element
        StringBuilder sb = new StringBuilder(8 * Math.min(n, (Integer.MAX_VALUE-8192) / 8));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(separator);
            Object o = ar[i];
            if (o == null) 
                sb.append("[null]");
            else {
                String s = o.toString();
                if (csv && (s.indexOf(',') >= 0 || s.indexOf('"') >= 0))
                    s = toJson(s);
                sb.append(s);
            }
        }
        if (finalSeparator && n > 0)
            sb.append(separator);
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of boolean
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(boolean ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 7 bytes/element
        StringBuilder sb = new StringBuilder(7 * Math.min(n, (Integer.MAX_VALUE-8192) / 7));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(ar[i]);
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of bytes
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(byte ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 5 bytes/element
        StringBuilder sb = new StringBuilder(5 * Math.min(n, (Integer.MAX_VALUE-8192) / 5));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(ar[i]);
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * (chars are treated as unsigned shorts).
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of char
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(char ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 6 bytes/element
        StringBuilder sb = new StringBuilder(6 * Math.min(n, (Integer.MAX_VALUE-8192) / 6));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append((int)ar[i]);  //safe char to int type conversion
        }
        return sb.toString();
    }

    /**
     * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array.
     * Negative numbers are twos compliment, e.g., -4 -> 0xfc.
     * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
     *
     * @param ar an array of bytes
     * @return a CSSV String (or null if ar is null)
     */
    public static String toHexCSSVString(byte ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 6 bytes/element
        StringBuilder sb = new StringBuilder(6 * Math.min(n, (Integer.MAX_VALUE-8192) / 6));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            String s = Integer.toHexString(ar[i]);
            if (s.length() == 8 && s.startsWith("ffffff"))
                s = s.substring(6);
            sb.append("0x" + s);
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of shorts
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(short ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 7 bytes/element
        StringBuilder sb = new StringBuilder(7 * Math.min(n, (Integer.MAX_VALUE-8192) / 7));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array.
     * Negative numbers are twos compliment, e.g., -4 -> 0xfffc.
     * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
     *
     * @param ar an array of short
     * @return a CSSV String (or null if ar is null)
     */
    public static String toHexCSSVString(short ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 8 bytes/element
        StringBuilder sb = new StringBuilder(8 * Math.min(n, (Integer.MAX_VALUE-8192) / 8));
        for (int i = 0; i < n; i++) {
            String s = Integer.toHexString(ar[i]);
            if (s.length() == 8 && s.startsWith("ffff"))
                s = s.substring(4);
            sb.append("0x" + s);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of ints
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(int ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 8 bytes/element
        StringBuilder sb = new StringBuilder(8 * Math.min(n, (Integer.MAX_VALUE-8192) / 8));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array.
     * Negative numbers are twos compliment, e.g., -4 -> 0xfffffffc.
     * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
     *
     * @param ar an array of ints
     * @return a CSSV String (or null if ar is null)
     */
    public static String toHexCSSVString(int ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append("0x" + Integer.toHexString(ar[i]));
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of longs
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(long ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of float
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(float ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of double
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(double ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a newline-separated (always '\n') String from the array.
     *
     * @param ar an array of ints
     * @return a newline-separated String (or null if ar is null)
     */
    public static String toNewlineString(int ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * This generates a newline-separated (always '\n') String from the array.
     *
     * @param ar an array of double
     * @return a newline-separated String (or null if ar is null)
     */
    public static String toNewlineString(double ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * This converts an ArrayList of Strings into a String[].
     * If you have an ArrayList or a Vector, use arrayList.toArray().
     *
     * @param aa
     * @return the corresponding String[] by calling toString() for each object
     */
    public static String[] toStringArray(Object aa[]) {
        if (aa == null)
            return null;
        int n = aa.length;
        Math2.ensureMemoryAvailable(8L * n, "String2.toStringArray"); //8L is lame estimate of bytes/element
        String sa[] = new String[n];
        for (int i = 0; i < n; i++) {
            Object o = aa[i];
            sa[i] = o == null? (String)o : o.toString();
        }
        return sa;
    }

    /**
     * Add the items in the array (if any) to the arrayList.
     *
     * @param arrayList
     * @param ar the items to be added
     */
    public static void add(ArrayList arrayList, Object ar[]) {
        if (arrayList == null || ar == null) 
            return;
        int n = ar.length;
        for (int i = 0; i < n; i++)
            arrayList.add(ar[i]);
    }

    /**
     * This displays the contents of a bitSet as a String.
     *
     * @param bitSet
     * @return the corresponding String (the 'true' bits, comma separated)
     */
    public static String toString(BitSet bitSet) {
        if (bitSet == null)
            return null;
        StringBuilder sb = new StringBuilder(1024);

        String separator = "";
        int i = bitSet.nextSetBit(0);
        while (i >= 0) {
            sb.append(separator + i);
            separator = ", ";
            i = bitSet.nextSetBit(i + 1);
        }
        return sb.toString();
    }

    /**
     * This displays the contents of a map as a String.
     * See also StringArray(Map)
     *
     * @param map  if it needs to be thread-safe, use ConcurrentHashMap
     * @return the corresponding String, with one entry on each line 
     *    (<key> = <value>) sorted (case insensitive) by key
     */
    public static String toString(Map map) {
        if (map == null)
            return null;
        StringBuilder sb = new StringBuilder(1024);

        Set entrySet = map.entrySet();
        Iterator it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry)it.next();
            sb.append(me.getKey().toString() + " = " + me.getValue().toString() + "\n");
        }
        return sb.toString();
    }

    /**
     * From an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this generates a String with 
     * "    <name>=<value>" on each line.
     * If arrayList == null, this returns "    [null]\n".
     *
     * @param arrayList 
     * @return the desired string representation
     */
    public static String alternateToString(ArrayList arrayList) {
        if (arrayList == null)
            return "    [null]\n";
        int n = arrayList.size();
        //estimate 32 bytes/element
        StringBuilder sb = new StringBuilder(32 * Math.min(n, (Integer.MAX_VALUE-8192) / 32));
        for (int i = 0; i < n; i += 2) {
            sb.append("    ");
            sb.append(arrayList.get(i).toString());
            sb.append('=');
            sb.append(arrayToCSSVString(arrayList.get(i+1)));
            sb.append('\n');
        }
        return sb.toString();
    }


    /**
     * From an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this an array of attributeNames.
     * If arrayList == null, this returns "    [null]\n".
     *
     * @param arrayList 
     * @return the attributeNames in the arrayList
     */
    public static String[] alternateGetNames(ArrayList arrayList) {
        if (arrayList == null)
            return null;
        int n = arrayList.size();
        String[] sar = new String[n / 2];
        int i2 = 0;
        for (int i = 0; i < n / 2; i++) {
            sar[i] = arrayList.get(i2).toString();
            i2 += 2;
        }
        return sar;
    }
    /**
     * From an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this returns the attributeValue
     * associated with the supplied attributeName.
     * If array == null or there is no matching value, this returns null.
     *
     * @param arrayList 
     * @param attributeName
     * @return the associated value
     */
    public static Object alternateGetValue(ArrayList arrayList, String attributeName) {
        if (arrayList == null)
            return null;
        int n = arrayList.size();
        for (int i = 0; i < n; i += 2) {
            if (arrayList.get(i).toString().equals(attributeName))
                return arrayList.get(i + 1);
        }
        return null;
    }

    /**
     * Given an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this either removes the attribute
     * (if value == null), adds the attribute and value (if it isn't in the list),
     * or changes the value (if the attriubte is in the list).
     *
     * @param arrayList 
     * @param attributeName
     * @param value the value associated with the attributeName
     * @return the previous value for the attribute (or null)
     * @throws Exception of trouble (e.g., if arrayList is null)
     */
    public static Object alternateSetValue(ArrayList arrayList, 
            String attributeName, Object value) {
        if (arrayList == null)
            throw new SimpleException(ERROR + " in String2.alternateSetValue: arrayList is null.");
        int n = arrayList.size();
        for (int i = 0; i < n; i += 2) {
            if (arrayList.get(i).toString().equals(attributeName)) {
                Object oldValue = arrayList.get(i + 1);
                if (value == null) {
                    arrayList.remove(i + 1); //order of removal is important
                    arrayList.remove(i);
                }
                else arrayList.set(i+1, value);
                return oldValue;
            }
        }

        //attributeName not found? 
        if (value == null)
            return null;
        else {
            //add it
            arrayList.add(attributeName);
            arrayList.add(value);
            return null;
        }
    }

    /**
     * This returns a nice String representation of the attribute value
     * (which should be a String or an array of primitives).
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param value
     * @return a nice String representation
     */
    public static String arrayToCSSVString(Object value) {
        if (value instanceof byte[])   return toCSSVString((byte[])value);
        if (value instanceof char[])   return toCSSVString((char[])value);
        if (value instanceof short[])  return toCSSVString((short[])value);
        if (value instanceof int[])    return toCSSVString((int[])value);
        if (value instanceof long[])   return toCSSVString((long[])value);
        if (value instanceof float[])  return toCSSVString((float[])value);
        if (value instanceof double[]) return toCSSVString((double[])value);
        return value.toString();
    }

    /**
     * This extracts the lower 8 bits of each char to form a
     * byte array.
     *
     * @param s a String
     * @return the corresponding byte[] (or null if s is null)
     */
    public static byte[] toByteArray(String s) {
        if (s == null)
            return null;
        int sLength = s.length();
        byte[] ba = new byte[sLength];
        for (int i = 0; i < sLength; i++)
            ba[i] = (byte)s.charAt(i);
        return ba;
    }

    /**
     * This extracts the lower 8 bits of each char to form a
     * byte array.
     *
     * @param sb a StringBuilder
     * @return the corresponding byte[] (or null if s is null)
     */
    public static byte[] toByteArray(StringBuilder sb) {
        if (sb == null)
            return null;
        int sbLength = sb.length();
        byte[] ba = new byte[sbLength];
        for (int i = 0; i < sbLength; i++)
            ba[i] = (byte)sb.charAt(i);
        return ba;
    }

    /**
     * This creates a String which displays the bytes in hex, 16 per line.
     *
     * @param byteArray   perhaps from toByteArray(s)
     * @return the hex dump of the bytes (or null if byteArray is null).
     *    Each line will be 71 chars long (char#71 will be newline).
     */
    public static String hexDump(byte[] byteArray) {
        int bal = byteArray.length;
        StringBuilder printable = new StringBuilder(32);
        //~5 bytes/element
        StringBuilder sb = new StringBuilder(5 * Math.min(bal, (Integer.MAX_VALUE-8192) / 5));
        int i;
        for (i = 0; i < bal; i++) {
            int data = byteArray[i] & 255;
            sb.append(zeroPad(Integer.toHexString(data), 2) + " ");
            printable.append(data >= 32 && data <= 126? (char)data: ' '); 
            if (i % 8 == 7) 
                sb.append("  ");
            if (i % 16 == 15) {
                sb.append(printable + " |\n");
                printable.setLength(0);
            }

        }
        if (byteArray.length % 16 != 0) {
            sb.append(printable);
            sb.append(makeString(' ', 69 - sb.length() % 71));
            sb.append("|\n");
        }

        return sb.toString();
    }

    /**
     * This finds the first element in Object[] 
     * where the ar[i].toString value equals to s.
     *
     * @param ar the array of Objects
     * @param s the String to be found
     * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not found)
     */
    public static int indexOf(Object[] ar, String s) {
        return indexOf(ar, s, 0);
    }

    /**
     * This finds the first element in Object[]  (starting at element startAt)
     * where the ar[i].toString value equals s.
     *
     * @param ar the array of Objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt < 0, this starts with startAt = 0.
     *    If startAt >= ar.length, this returns -1.
     * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not found)
     */
    public static int indexOf(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && s.equals(ar[i].toString()))  
                return i;
        return -1;
    }

    /**
     * This finds the first element in Object[] 
     * where ar[i].toString().toLowerCase() equals to s.toLowerCase().
     *
     * @param ar the array of Objects
     * @param s the String to be found
     * @return the element number of ar which is equal to s (or -1 if s is null or not found)
     */
    public static int caseInsensitiveIndexOf(Object[] ar, String s) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        s = s.toLowerCase();
        for (int i = 0; i < n; i++)
            if (ar[i] != null && s.equals(ar[i].toString().toLowerCase()))  
                return i;
        return -1;
    }

    /**
     * This finds the first element in Object[] 
     * where the ar[i].toString value contains the substring s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @return the element number of ar which is equal to s (or -1 if not found)
     */
    public static int lineContaining(Object[] ar, String s) {
        return lineContaining(ar, s, 0);
    }

    /**
     * This finds the first element in Object[] (starting at element startAt)
     * where the ar[i].toString value contains the substring s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt < 0, this starts with startAt = 0.
     * @return the element number of ar which is equal to s (or -1 if not found)
     */
    public static int lineContaining(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i].toString().indexOf(s) >= 0) 
                return i;
        return -1;
    }

    /**
     * This returns the first element in Object[] (starting at element 0)
     * where the ar[i].toString value starts with s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @return the first element ar (as a String) which starts with s (or null if not found)
     */
    public static String stringStartsWith(Object[] ar, String s) {
        int i = lineStartsWith(ar, s, 0);
        return i < 0? null : ar[i].toString();
    }

    /**
     * This finds the first element in Object[] (starting at element 0)
     * where the ar[i].toString value starts with s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @return the element number of ar which starts with s (or -1 if not found)
     */
    public static int lineStartsWith(Object[] ar, String s) {
        return lineStartsWith(ar, s, 0);
    }

    /**
     * This finds the first element in Object[] (starting at element startAt)
     * where the ar[i].toString value starts with s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt < 0, this starts with startAt = 0.
     * @return the element number of ar which starts with s (or -1 if not found)
     */
    public static int lineStartsWith(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i].toString().startsWith(s)) 
                return i;
        return -1;
    }

    /**
     * This is like lineStartsWith, but ignores case.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt < 0, this starts with startAt = 0.
     * @return the element number of ar which starts with s (or -1 if not found)
     */
    public static int lineStartsWithIgnoreCase(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        s = s.toLowerCase();
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i].toString().toLowerCase().startsWith(s)) 
                return i;
        return -1;
    }

    /* *  NOT ACTIVE.     
     * This replaces the current log.
     * The default log prints to System.err.
     * Use the logger by calling, e.g., String2.log.fine(msg) or
     *   String2.log.log(level, msg);
     *
     * @param mainClassName e.g., "gov.noaa.pfel.coastwatch.CWDataBrowser", 
     *   sets up a separate logger related to the current program
     * @param fullFileName the name for the log file (or null or "" for System.err).
     *   Append .%g to the end of the file name to create and rotate through a series of 
     *   up to 10 log files, each with up to 1MB of messages.
     * @param append determines whether log info should be appended
     *   or overwrite previous log files.
     * @param defaultLevel e.g., java.util.logging.Level.FINER.
     *    Note that this can be changed any time with String2.log.setLevel(level).
     */
    /*public static void setUpLog(String mainClassName, String fullFileName,
            boolean append, Level initialLevel) throws Exception {
        log = Logger.getLogger(mainClassName);
        if (fullFileName.length() > 0) {
            Handler har[] = log.getHandlers();
            for (int i = 0; i < har.length; i++)
                log.removeHandler(har[i]);
            FileHandler fh = fullFileName.endsWith("%g")? 
                new FileHandler(fullFileName, 1000000, 10, append) :
                new FileHandler(fullFileName, 1000000, 1,  append);
            fh.setFormatter(new RawFormatter()); //replace the default XMLFormatter
            log.addHandler(fh);
        }
        log.setLevel(initialLevel);
    }*/

    /**
     * This tells Commons Logging to use com.cohort.util.String2Log.
     * I use this at the beginning of my programs 
     * (TestAll, NetCheck, Browser, ConvertTable, DoubleCenterGrids)
     * to route Commons Logging requests through String2Log.
     * !!!Don't use this in lower level methods as it will hijack the 
     * parent program's (e.g., Armstrong's) logger setup.
     *
     * param level a String2Log.XXX_LEVEL constant (or -1 to leave unchanged,
     *     default=String2Log.WARN_LEVEL)
     */
    public static void setupCommonsLogging(int level) {
        //By setting this property, I specify that String2LogFactory
        //  will be used to generate logFactories.
        //  (It makes one String2Log, which sends all messages to String2.log.)
        System.setProperty("org.apache.commons.logging.LogFactory", 
            "com.cohort.util.String2LogFactory");
        if (level >= 0) {
            System.setProperty("com.cohort.util.String2Log.level", "" + level);
        } else {
            if (System.getProperty("com.cohort.util.String2Log.level") == null)
                System.setProperty("com.cohort.util.String2Log.level", 
                    "" + String2Log.WARN_LEVEL);
        }

        //this dummy variable ensures String2LogFactory gets compiled 
        String2LogFactory string2LogFactory;

    }

    /**
     * This changes the log system set up.
     * The default log prints to System.err.
     * Use the logger by calling String2.log(msg); 
     *
     * @param tLogToSystemOut indicates if info should be printed to System.out (default = false).
     * @param tLogToSystemErr indicates if info should be printed to System.err (default = true).
     * @param fullFileName the name for the log file (or "" for none).
     * @param logToStringBuffer specifies if the logged info should also
     *   be saved in a StringBuffer (see getlogStringBuffer)
     * @param append If a previous log file of the same name exists,
     *   and/or if a logStringBuffer exists,
     *   this determines whether a new log file should be created
     *   or whether info should be appended to the old file.
     * @param maxSize determines the approximate max size of the log file
     *   and/or logStringBuffer.
     *   When maxSize is reached, the current log file is copied to 
     *   fullFileName.previous, and a new fullFileName is created.
     *   When maxSize is reached, the first half of the
     *   logStringBuffer is deleted.
     *   Specify 0 for no limit to the size.    
     */
    public static synchronized void setupLog(
            boolean tLogToSystemOut, boolean tLogToSystemErr,
            String fullFileName, boolean logToStringBuffer, 
            boolean append, int maxSize) 
            throws Exception {

        if (logFileName != null && logFileName.equals(fullFileName))
            return;

        logToSystemOut = tLogToSystemOut;
        logToSystemErr = tLogToSystemErr;

        if (!append)
            logFileSize = 0;
        logMaxSize = maxSize;

        //close the old file
        closeLogFile();

        //StringBuilderToo?
        if (logToStringBuffer) {
            if (append && logStringBuffer != null) {
                //use existing logStringBuffer
            } else logStringBuffer = new StringBuffer();
        } else logStringBuffer = null;

        //if no file name, return
        if (fullFileName.length() == 0)
            return;

        //open the file
        //This uses a BufferedWriter wrapped around a FileWriter
        //to write the information to the file.
        logFileName = fullFileName;
        logFile = new BufferedWriter(new FileWriter(fullFileName, append));
        logFileSize = (new File(fullFileName)).length();
    }

    /**
     * This closes the log file (if it exists and is open).
     * It is best if a crashing program calls this.
     * It seems like Java should handle this if program crashes,
     *   but it doesn't seem to (at least in Windows XP Pro).
     */
    public static void closeLogFile() {
        if (logFile != null) {
            try {
                logFile.close();
                logFile = null;
                logFileName = null;
            } catch (Exception e) {
                //do nothing
            }
        }
    }

    /**
     * This writes the specified message (with \n as line separator) to the log file,
     * appending \n at the end.
     * This will not throw an exception.
     *
     * @param message the message
     */
    public static void log(String message) {
        lowLog(message, true);
    }

    /**
     * This writes the specified message (with \n as line separator) to the log file
     * without appending \n at the end.
     * This will not throw an exception.
     *
     * @param message the message
     */
    public static void logNoNewline(String message) {
        lowLog(message, false);
    }

    /**
     * This writes the specified message (with \n as line separator) to the log file
     * without appending \n at the end.
     * This will not throw an exception.
     *
     * @param message the message
     */
    public static synchronized void lowLog(String message, boolean addNewline) {
        try {
            //print message with \n's to logStringBuffer
            if (logStringBuffer != null) {
                if (logStringBuffer.length() > logMaxSize && logMaxSize > 0)
                    logStringBuffer.delete(0, logMaxSize / 2);
                logStringBuffer.append(message);
                if (addNewline) 
                    logStringBuffer.append('\n');
            }

            //write to system.out or logFile 
            if (!lineSeparator.equals("\n"))
                message = replaceAll(message, "\n", lineSeparator);

            if (logToSystemOut) {
                if (addNewline)
                     System.out.println(message);
                else System.out.print(message);
            }

            if (logToSystemErr) {
                if (addNewline)
                     System.err.println(message);
                else System.err.print(message);
            }

            if (logFile != null) {
                //is file too big?
                if (logFileSize > logMaxSize && logMaxSize > 0) {
                    logFile.close();
                    logFile = null; //otherwise, infinite loop if File2.rename calls String2.log
                    File2.rename(logFileName, logFileName + ".previous");
                    logFile = new BufferedWriter(new FileWriter(logFileName));
                    logFileSize = 0;
                }

                //write the message to the file
                logFile.write(message);
                if (addNewline)
                    logFile.write(lineSeparator);
                //flush so file is always up-to-date if trouble?
                //  nice, but rarely used and big time penalty (2.5X slower!)
                if (++logFileFlushCount >= logFileFlushEveryNth) {
                    logFile.flush(); 
                    logFileFlushCount = 0;
                }
                logFileSize += message.length(); 

            }
        } catch (Exception e) {
            //eek! what should I do?
        }
    }

    /**
     * This returns the logFileName (or null if none).
     *
     * @return the logFileName (or null if none)
     */
    public static String logFileName() {
        return logFileName;
    }

    /**
     * This returns the logStringBuffer object.
     * Then you can get call sb.toString() to get the contents,
     * setLength(0) to clear it, etc.
     *
     * @return the logStringBuffer (or null if none)
     */
    public static synchronized StringBuffer getLogStringBuffer() {
        return logStringBuffer;
    }

    /**
     * This splits the string at the specified character.
     * Leading and trailing whitespace is removed.
     * A missing final strings is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @return an ArrayList of strings.
     *   s=null returns null.
     *   s="" returns ArrayList with one value: "".
     */
    public static ArrayList splitToArrayList(String s, char separator) {
        return splitToArrayList(s, separator, true);
    }

    /**
     * This splits the string at the specified character.
     * A missing final string is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @param trim  trim the substrings, or don't
     * @return an ArrayList of strings.
     *   s=null returns null.
     *   s="" returns ArrayList with one value: "".
     */
    public static ArrayList splitToArrayList(String s, char separator, boolean trim) {
        if (s == null) 
            return null;

        //go through the string looking for separators
        ArrayList al = new ArrayList();
        int sLength = s.length();
        int start = 0;
        //log("split line=" + annotatedString(s));
        for (int index = 0; index < sLength; index++) {
            if (s.charAt(index) == separator) {
                String ts = s.substring(start, index);
                if (trim) ts = ts.trim();
                al.add(ts);
                start = index + 1;
            }
        }

        //add the final substring
        String ts = s.substring(start, sLength); //start == sLength? "" : s.substring(start, sLength);
        if (trim) ts = ts.trim();
        al.add(ts);
        //log("al.size=" + al.size() + "\n");
        return al;
    }

    /**
     * This splits the string at the specified character.
     * The substrings are trim'd.
     * A missing final string is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @return a String[] with the strings.
     *   s=null returns null.
     *   s="" returns String[1]{""}.
     */
    public static String[] split(String s, char separator) {
        ArrayList al = splitToArrayList(s, separator, true);
        if (al == null)
            return null;
        return toStringArray(al.toArray());
    }

    /**
     * This splits the string at the specified character.
     * A missing final string is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @return a String[] with the strings.
     *   s=null returns null.
     *   s="" returns String[1]{""}.
     */
    public static String[] splitNoTrim(String s, char separator) {
        ArrayList al = splitToArrayList(s, separator, false);
        if (al == null)
            return null;
        return toStringArray(al.toArray());
    }

    /**
     * This converts an Object[] (for example, where objects are Strings or 
     *  Integers) into an int[].
     *
     * @param oar an Object[]
     * @return the corresponding int[]  (invalid values are converted to Integer.MAX_VALUE).
     *   oar=null returns null.
     */
    public static int[] toIntArray(Object oar[]) {
        if (oar == null)
            return null;
        int n = oar.length;
        Math2.ensureMemoryAvailable(4L * n, "String2.toIntArray"); 
        int ia[] = new int[n];
        for (int i = 0; i < n; i++)
            ia[i] = parseInt(oar[i].toString());
        return ia;
    }

    /**
     * This converts an Object[] (for example, where objects are Strings or 
     *  Floats) into a float[].
     *
     * @param oar an Object[]
     * @return the corresponding float[] (invalid values are converted to Float.NaN).
     *   oar=null returns null.
     */
    public static float[] toFloatArray(Object oar[]) {
        if (oar == null)
            return null;
        int n = oar.length;
        Math2.ensureMemoryAvailable(4L * n, "String2.toFloatArray"); 
        float fa[] = new float[n];
        for (int i = 0; i < n; i++)
            fa[i] = parseFloat(oar[i].toString());
        return fa;
    }

    /**
     * This converts an Object[] (for example, where objects are Strings or 
     *  Doubles) into a double[].
     *
     * @param oar an Object[]
     * @return the corresponding double[] (invalid values are converted to Double.NaN).
     *   oar=null returns null.
     */
    public static double[] toDoubleArray(Object oar[]) {
        if (oar == null)
            return null;
        int n = oar.length;
        Math2.ensureMemoryAvailable(8L * n, "String2.toDoubleArray"); 
        double da[] = new double[n];
        for (int i = 0; i < n; i++)
            da[i] = parseDouble(oar[i].toString());
        return da;
    }

    /**
     * This converts an ArrayList with Integers into an int[].
     *
     * @param al an Object[]
     * @return the corresponding int[]  (invalid values are converted to Integer.MAX_VALUE).
     *   al=null returns null.
     */
    public static int[] toIntArray(ArrayList al) {
        if (al == null)
            return null;
        int n = al.size();
        Math2.ensureMemoryAvailable(4L * n, "String2.toIntArray"); 
        int ia[] = new int[n];
        for (int i = 0; i < n; i++)
            ia[i] = ((Integer)al.get(i)).intValue();
        return ia;
    }

    /**
     * This converts an ArrayList with Floats into a float[].
     *
     * @param al an Object[]
     * @return the corresponding float[] (invalid values are converted to Float.NaN).
     *   al=null returns null.
     */
    public static float[] toFloatArray(ArrayList al) {
        if (al == null)
            return null;
        int n = al.size();
        Math2.ensureMemoryAvailable(4L * n, "String2.toFloatArray"); 
        float fa[] = new float[n];
        for (int i = 0; i < n; i++)
            fa[i] = ((Float)al.get(i)).floatValue();
        return fa;
    }

    /**
     * This converts an ArrayList with Doubles into a double[].
     *
     * @param al an Object[]
     * @return the corresponding double[] (invalid values are converted to Double.NaN).
     *   al=null returns null.
     */
    public static double[] toDoubleArray(ArrayList al) {
        if (al == null)
            return null;
        int n = al.size();
        Math2.ensureMemoryAvailable(4L * n, "String2.toDoubleArray"); 
        double da[] = new double[n];
        for (int i = 0; i < n; i++)
            da[i] = ((Double)al.get(i)).doubleValue();
        return da;
    }

    /**
     * This returns an int[] with just the non-Integer.MAX_VALUE values from the original 
     * array.
     *
     * @param iar is an int[]
     * @return a new int[] with just the non-Integer.MAX_VALUE values.
     *   iar=null returns null.
     */
    public static int[] justFiniteValues(int iar[]) {
        if (iar == null)
            return null;
        int n = iar.length;
        int nFinite = 0;
        int ia[] = new int[n];
        for (int i = 0; i < n; i++)
            if (iar[i] < Integer.MAX_VALUE)
                ia[nFinite++] = iar[i];

        //copy to a new array
        int iaf[] = new int[nFinite];
        System.arraycopy(ia, 0, iaf, 0, nFinite);
        return iaf;
    }

    /**
     * This returns a double[] with just the finite values from the original 
     * array.
     *
     * @param dar is a double[]
     * @return a new double[] with just finite values.
     *   dar=null returns null.
     */
    public static double[] justFiniteValues(double dar[]) {
        if (dar == null)
            return null;
        int n = dar.length;
        int nFinite = 0;
        double da[] = new double[n];
        for (int i = 0; i < n; i++)
            if (Math2.isFinite(dar[i]))
                da[nFinite++] = dar[i];

        //copy to a new array
        double daf[] = new double[nFinite];
        System.arraycopy(da, 0, daf, 0, nFinite);
        return daf;
    }

    /** 
     * This returns a String[] with just non-null strings
     * from the original array.
     *
     * @param sar is a String[]
     * @return a new String[] with just non-null strings.
     *   sar=null returns null.
     */
    public static String[] removeNull(String sar[]) {
        if (sar == null)
            return null;
        int n = sar.length;
        int nValid = 0;
        String sa[] = new String[n];
        for (int i = 0; i < n; i++)
            if (sar[i] != null)
                sa[nValid++] = sar[i];

        //copy to a new array
        String sa2[] = new String[nValid];
        System.arraycopy(sa, 0, sa2, 0, nValid);
        return sa2;
    }

    /** 
     * This returns a String[] with just non-null and non-"" strings
     * from the original array.
     *
     * @param sar is a String[]
     * @return a new String[] with just non-null and non-"" strings.
     *   sar=null returns null.
     */
    public static String[] removeNullOrEmpty(String sar[]) {
        if (sar == null)
            return null;
        int n = sar.length;
        int nValid = 0;
        String sa[] = new String[n];
        for (int i = 0; i < n; i++)
            if (sar[i] != null && sar[i].length() > 0)
                sa[nValid++] = sar[i];

        //copy to a new array
        String sa2[] = new String[nValid];
        System.arraycopy(sa, 0, sa2, 0, nValid);
        return sa2;
    }

    /**
     * This converts a comma-separated-value String into an int[].
     * Invalid values are converted to Integer.MAX_VALUE.
     *
     * @param csv the comma-separated-value String.
     * @return the corresponding int[].
     *    csv=null returns null.
     *    csv="" is converted to int[1]{Integer.MAX_VALUE}.
     */
    public static int[] csvToIntArray(String csv) {
        return toIntArray(split(csv, ','));      
    }

    /**
     * This converts a comma-separated-value String into a double[].
     * Invalid values are converted to Double.NaN.
     *
     * @param csv the comma-separated-value String
     * @return the corresponding double[].
     *    csv=null returns null.
     *    csv="" is converted to double[1]{Double.NAN}.
     */
    public static double[] csvToDoubleArray(String csv) {
        return toDoubleArray(split(csv, ','));      
    }

    /**
     * This converts a string to a boolean.
     * 
     * @param s the string
     * @return false if s is "false", "f", or "0". Case and leading/trailing
     *   spaces don't matter.  All other values (and null) are treated as true.
     */
    public static boolean parseBoolean(String s) {
        if (s == null)
            return true;
        s = s.toLowerCase().trim();
        return !(s.equals("false") || s.equals("f") || s.equals("0"));
    }

    /** This removes leading ch's.
     * @param s
     * @param ch
     * @return s or a new string without leading ch's.
     *   null returns null.
     */
    public static String removeLeading(String s, char ch) {
        if (s == null)
            return s;
        int sLength = s.length();
        int start = 0;
        while (start < sLength && s.charAt(start) == ch)
            start++;
        return start == 0? s : s.substring(start);
    }


    /** Like parseInt(s), but returns def if error). */
    public static int parseInt(String s, int def) {
        int i = parseInt(s);
        return i == Integer.MAX_VALUE? def : i;
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

            return Math2.roundToInt(Double.parseDouble(s));
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
     * DON'T USE THIS; RELY ON THE FIXES AVAILABLE FOR JAVA: 
     * EITHER THE LATEST VERSION OF JAVA OR THE 
     * JAVA UPDATER TO FIX THE BUG ON EXISTING OLDER JAVA INSTALLATIONS
     * http://www.oracle.com/technetwork/java/javase/fpupdater-tool-readme-305936.html
     *
     * <p>This returns true if s is a value that causes Java to hang. 
     * Avoid java hang.     2011-02-09 
     * http://www.exploringbinary.com/java-hangs-when-converting-2-2250738585072012e-308
     * This was Bob's work-around to avoid the Java bug.
     *
     * @param s a string representing a double value
     * @return true if the value is the troublesome value.
     *    If true, the value can be interpreted as either +/-Double.MIN_VALUE (not sure which) 
     *    or (crudely) 0.
     */
    public static boolean isDoubleTrouble(String s) {
        if (s == null || s.length() < 22)  //this is a good quick reject
            return false;

        //all variants are relevant, so look for the mantissa
        return replaceAll(s, ".", "").indexOf("2225073858507201") >= 0;
    }

    /**
     * Convert a string to an int, with rounding.
     * Leading or trailing spaces are automatically removed.
     * This won't throw an exception if the number isn't formatted right.
     *
     * @param s is the String representation of a number.
     * @return the int value from the String (or Double.NaN if error).
     */
    public static double roundingParseInt(String s) {
        return Math2.roundToInt(parseDouble(s));
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
     * Parse as a float with either "." or "," as the decimal point.
     * Leading or trailing spaces are automatically removed.
     *
     * @param s a String representing a float value (e.g., 1234.5 or 1234,5 or 
     *     1.234e3 1,234e3)
     * @return the corresponding float (or Float.NaN if not properly formatted)
     */
    public static float parseFloat(String s) {
        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Float.NaN;
        s = s.trim();
        if (s.length() == 0)
            return Float.NaN;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+' && ch != '.')
            return Float.NaN;

        try {
            s = s.replace(',', '.');   //!!! this is inconsistent with parseDouble
            return Float.parseFloat(s);
        } catch (Exception e) {
            //String2.log("parseFloat exception: " + s);
            return Float.NaN;
        }
    }

    /**
     * This converts a multiple-space-separated string into a String[] of separate tokens.
     * Double quoted tokens may have internal spaces.
     *
     * @param s the space-separated string
     * @return String[] of tokens (or null if s is null)
     */
    public static String[] tokenize(String s) {
        if (s == null)
            return null;

        ArrayList arrayList = new ArrayList();
        int sLength = s.length();
        int index = 0; //next char to be read
        //eat spaces
        while (index < sLength && s.charAt(index) == ' ') 
            index++;
        //repeatedly get tokens
        while (index < sLength) {
            //grab a token
            int start = index;
            int stop;
            //does it start with quotes?
            if (s.charAt(index) == '"') {
                index++; //skip the quotes
                start++;
                while (index < sLength && s.charAt(index) != '"') 
                    index++;
                stop = index; //if end of string and no closing quotes, it's a silent error
                index++; //skip the quotes
            } else {
                while (index < sLength && s.charAt(index) != ' ') 
                    index++;
                stop = index;
            }
            arrayList.add(s.substring(start, stop));

            //eat spaces
            while (index < sLength && s.charAt(index) == ' ') 
                index++;
        }

        return toStringArray(arrayList.toArray());
    }

    /** The size of the int[] needed for distribute() and getDistributionStatistics(). */
    public static int DistributionSize = 22;

    private static int BinMax[] = new int[]{0, 1, 2, 5, 10, 20, 50, 100, 200, 500,
        1000, 2000, 5000, 10000, 20000, //1,2,5,10,20 seconds
        60000, 120000, 300000, 600000, 1200000, 3600000, //1,2,5,10,20,60 minutes
        Integer.MAX_VALUE};

    /**
     * Put aTime into one of the distribution bins.
     * @param aTime 
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     */
    public static void distribute(long aTime, int[] distribution) {
        //catch really long times (greater than Integer.MAX_VALUE)
        if (aTime < 0)
            aTime = 0;
        if (aTime > 3600000) { distribution[21]++; return; }   //1hr   
        
        int iTime = (int)aTime; //safe since extreme values caught above
        for (int bin = 0; bin < DistributionSize; bin++) {
            if (iTime <= BinMax[bin]) {
                distribution[bin]++;
                return;
            }
        }
    }

    /**
     * Get the number of values in the distribution.
     *
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @return the number of values in the distribution.
     */
    public static int getDistributionN(int[] distribution) {
        //calculate n
        int n = 0;
        for (int bin = 0; bin < DistributionSize; bin++)
            n += distribution[bin];
        return n;
    }

    /**
     * Get the approximate median of the distribution.
     * See Sokal and Rohlf, Biometry, Box 4.1, pg 45.
     *
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @param n from getDistributionN
     * @return the approximate median of the distribution.
     *    If trouble or n<=0, this returns -1.
     */
    public static int getDistributionMedian(int[] distribution, int n) {
        double n2 = n / 2.0;

        if (n > 0) {
            //handle bin 0
            int cum = distribution[0];
            if (cum >= n2)
                return 0;

            for (int bin = 1; bin < DistributionSize; bin++) {  //bin 0 handled above
                if (distribution[bin] > 0) {
                    int tCum = cum + distribution[bin];
                    if (cum <= n2 && tCum >= n2) {
                        int tBinMax = bin == DistributionSize - 1? BinMax[bin-1] * 3 : BinMax[bin];
                        return Math2.roundToInt(
                            BinMax[bin-1] + ((n2-cum+0.0)/distribution[bin]) * (tBinMax - BinMax[bin-1]));
                    }
                    cum = tCum;
                }
            }
        }
        return -1; //trouble
    }

    /**
     * Generate brief statistics for a distribution.
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @return the statistics
     */
    public static String getBriefDistributionStatistics(int[] distribution) {
        int n = getDistributionN(distribution);
        String s = "n =" + right("" + n, 9);
        if (n == 0) 
            return s;
        int median = getDistributionMedian(distribution, n);
        return s + ",  median ~=" + right("" + median, 9) + " ms";
    }

    /**
     * Generate statistics for a distribution.
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @return the statistics
     */
    public static String getDistributionStatistics(int[] distribution) {
        int n = getDistributionN(distribution);
        String s = 
            "    " + getBriefDistributionStatistics(distribution) + "\n";

        if (n == 0)
            return s;

        return 
            s + 
            "    0 ms:      " + right("" + distribution[0], 10) + "\n" +
            "    1 ms:      " + right("" + distribution[1], 10) + "\n" +
            "    2 ms:      " + right("" + distribution[2], 10) + "\n" +
            "    <= 5 ms:   " + right("" + distribution[3], 10) + "\n" +
            "    <= 10 ms:  " + right("" + distribution[4], 10) + "\n" +
            "    <= 20 ms:  " + right("" + distribution[5], 10) + "\n" +
            "    <= 50 ms:  " + right("" + distribution[6], 10) + "\n" +
            "    <= 100 ms: " + right("" + distribution[7], 10) + "\n" +
            "    <= 200 ms: " + right("" + distribution[8], 10) + "\n" +
            "    <= 500 ms: " + right("" + distribution[9], 10) + "\n" +
            "    <= 1 s:    " + right("" + distribution[10], 10) + "\n" +
            "    <= 2 s:    " + right("" + distribution[11], 10) + "\n" +
            "    <= 5 s:    " + right("" + distribution[12], 10) + "\n" +
            "    <= 10 s:   " + right("" + distribution[13], 10) + "\n" +
            "    <= 20 s:   " + right("" + distribution[14], 10) + "\n" +
            "    <= 1 min:  " + right("" + distribution[15], 10) + "\n" +
            "    <= 2 min:  " + right("" + distribution[16], 10) + "\n" +
            "    <= 5 min:  " + right("" + distribution[17], 10) + "\n" +
            "    <= 10 min: " + right("" + distribution[18], 10) + "\n" +
            "    <= 20 min: " + right("" + distribution[19], 10) + "\n" +
            "    <= 1 hr:   " + right("" + distribution[20], 10) + "\n" +
            "    >  1 hr:   " + right("" + distribution[21], 10) + "\n";
    }


    /**
     * If lines in s are &gt;=maxLength characters, this inserts "\n"+spaces at the
     * previous non-DigitLetter + DigitLetter; or if none, this inserts "\n"+spaces at maxLength.
     * Useful keywords for searching for this method: longer, longest, noLongerThan.
     *
     * @param s a String with multiple lines, separated by \n's
     * @param maxLength the maximum line length allowed
     * @param spaces the string to be inserted after the inserted newline, e.g., "&lt;br&gt;    " 
     * @return s (perhaps the same, perhaps different), but with no long lines
     */
    public static String noLongLines(String s, int maxLength, String spaces) {
        int maxLength2 = maxLength / 2;
        int spacesLength = spaces.length();
        int start = 0, count = 0;
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(sLength / 5 * 6);
        for (int i = 0; i < sLength; i++) { 
           if (s.charAt(i) == '\n') {
               count = 0;
           } else {
               count++; 
               if (count >= maxLength) {
                   int oi = i;
                   char ch, ch1 = s.charAt(i);
                   while (count > maxLength2) {
                       ch = ch1;
                       ch1 = s.charAt(i - 1);
                       //work backwards from maxLength to maxLength/2 to find a break point  (i)
                       if (!isDigitLetter(ch1) && ch1 != '(' && isDigitLetter(ch)) {
                           count = 0; //signal success
                           break;
                       }
                       count--;
                       i--;
                   }
                   if (count > 0) { 
                       //newline not inserted above; insert it at oi                    
                       i = oi;
                   }
                   sb.append(s.substring(start, i));
                   sb.append("\n" + spaces);
                   start = i;
                   count = spacesLength;
               }
           }
        }
        if (start == 0)
            return s;

        sb.append(s.substring(start));
        return sb.toString();
    }


    /**
     * This is like noLongLines, but will only break (add newlines) at spaces.
     * If there is no reasonable break before maxLength, it will break after maxLength.
     *
     * @param sb a StringBuilder with multiple lines, separated by \n's
     * @param maxLength the maximum line length allowed
     * @param spaces the string to be inserted after the inserted newline, e.g., "&lt;br&gt;    " 
     * @return the same or a different StringBuilder, but with no long lines
     */
    public static StringBuilder noLongLinesAtSpace(StringBuilder sb, int maxLength, String spaces) {
        int sbLength = sb.length();
        if (sbLength <= maxLength)
            return sb;
        StringBuilder newSB = new StringBuilder(sbLength / 5 * 6);         
        int minCount = maxLength / 2; //try hard

        int startAt = 0;  //start for next copy chunk
        int count = 0; //don't jump ahead because there may be an internal \n
        int lastSpaceAt = -1;
        
        for (int sbi = 0; sbi < sbLength; sbi++) { 
            char ch = sb.charAt(sbi);
            if (ch == '\n') {
                newSB.append(sb, startAt, sbi + 1);
                startAt = sbi + 1;
                count = 0;
                lastSpaceAt = -1;
            } else {
                if (ch == ' ' && count >= minCount)
                    lastSpaceAt = sbi;
                count++; 
                if (count >= maxLength && lastSpaceAt >= 0) {

                    //use lastSpaceAt
                    newSB.append(sb, startAt, lastSpaceAt);
                    newSB.append('\n');
                    newSB.append(spaces);
                    sbi = lastSpaceAt;
                    lastSpaceAt = -1;
                    count = spaces.length();

                    //maybe next char is a space, too; skip to last space in a series
                    while (sbi < sbLength - 1 && sb.charAt(sbi + 1) == ' ')
                        sbi++;
                    startAt = sbi + 1;
                }
            }
        }
        //copy remainder of sb
        if (startAt < sbLength)
            newSB.append(sb, startAt, sbLength);
        return newSB;
    }

    /**
     * This is like noLongLines, but will only break at spaces.
     *
     * @param s a String with multiple lines, separated by \n's
     * @param maxLength the maximum line length allowed
     * @param spaces the string to be inserted after the inserted newline, e.g., "    " 
     * @return the content of s, but with no long lines
     */
    public static String noLongLinesAtSpace(String s, int maxLength, String spaces) {
        if (s.length() <= maxLength)
            return s;
        return noLongLinesAtSpace(new StringBuilder(s), maxLength, spaces).toString();
    }

    /**
     * This reads an ASCII file line by line (with any common end-of-line characters), 
     * does a simple (not regex) search and replace on each line, 
     * and saves the lines in another file (with String2.lineSeparator's).
     *
     * @param fullInFileName the full name of the input file
     * @param fullOutFileName the full name of the output file 
        (if same as fullInFileName, fullInFileName will be renamed +.original)
     * @param search  a plain text string to search for
     * @param replace  a plain text string to replace any instances of <search>
     * @throws Exception if any trouble
     */
    public static void simpleSearchAndReplace(String fullInFileName,
        String fullOutFileName, String search, String replace) 
        throws Exception {       
             
        String2.log("simpleSearchAndReplace in=" + fullInFileName +
            " out=" + fullOutFileName + " search=" + search + " replace=" + replace);
        String tOutFileName = fullOutFileName + Math2.random(Integer.MAX_VALUE);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fullInFileName));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tOutFileName));
        try {
                             
            //convert the text, line by line
            //This uses bufferedReader.readLine() to repeatedly
            //read lines from the file and thus can handle various 
            //end-of-line characters.
            String s = bufferedReader.readLine();
            while (s != null) { //null = end-of-file
                bufferedWriter.write(replaceAll(s, search, replace));
                bufferedWriter.write(lineSeparator);
                s = bufferedReader.readLine();
            }

            bufferedReader.close();
            bufferedWriter.close();

            if (fullInFileName.equals(fullOutFileName))
                File2.rename(fullInFileName, fullInFileName + ".original");
            File2.rename(tOutFileName, fullOutFileName);
            if (fullInFileName.equals(fullOutFileName))
                File2.delete(fullInFileName + ".original");

        } catch (Exception e) {
            try {
                bufferedReader.close();
                bufferedWriter.close();
            } catch (Exception e2) {
            }
            File2.delete(tOutFileName);
            throw e;
        }

    }

    /**
     * This reads an ASCII file line by line (with any common end-of-line characters), 
     * does a regex search and replace on each line, 
     * and saves the lines in another file (with String2.lineSeparator's).
     *
     * @param fullInFileName the full name of the input file
     * @param fullOutFileName the full name of the output file
     * @param search  a regex to search for
     * @param replace  a plain text string to replace any instances of <search>
     * @throws Exception if any trouble
     */
    public static void regexSearchAndReplace(String fullInFileName,
        String fullOutFileName, String search, String replace) 
        throws Exception {
         
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fullInFileName));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fullOutFileName));
                         
        //get the text from the file
        //This uses bufferedReader.readLine() to repeatedly
        //read lines from the file and thus can handle various 
        //end-of-line characters.
        String s = bufferedReader.readLine();
        while (s != null) { //null = end-of-file
            bufferedWriter.write(s.replaceAll(search, replace));
            bufferedWriter.write(String2.lineSeparator);
            s = bufferedReader.readLine();
        }

        bufferedReader.close();
        bufferedWriter.close();

    }


    /**
     * This returns a string with the keys and values of the Map (sorted by the keys, ignoreCase).
     *
     * @param map (keys and values are objects with good toString methods).
     *   If it needs to be thead-safe, use ConcurrentHashMap.
     * @return a string with the sorted (ignoreCase) keys and their values ("key1: value1\nkey2: value2\n")
     */
    public static String getKeysAndValuesString(Map map) {
        ArrayList al = new ArrayList();

        //synchronize so protected from changes in other threads
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            al.add(key.toString() + ": " + map.get(key).toString());
        }
        Collections.sort(al, new StringComparatorIgnoreCase());
        return toNewlineString(al.toArray());
    }

    /**
     * This returns the number formatted with up to 6 digits to the left and right of
     * the decimal and trailing decimal 0's removed.  
     * If abs(d) &lt; 0.0999995 or abs(d) &gt;= 999999.9999995, the number is displayed
     * in scientific notation (e.g., 8.954321E-5).
     * Thus the maximum length should be 14 characters (-123456.123456).
     * 0 returns "0"
     * NaN returns "NaN".
     * Double.POSITIVE_INFINITY returns "Infinity".
     * Double.NEGATIVE_INFINITY returns "-Infinity".
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genEFormat6(double d) {

        //!finite
        if (!Math2.isFinite(d))
            return "" + d;

        //almost 0
        if (Math2.almost0(d))
            return "0";

        //close to 0 
        //String2.log("genEFormat test " + (d*1000) + " " + Math.rint(d*1000));
        if (Math.abs(d) < 0.0999995 &&  
            !Math2.almostEqual(6, d * 10000, Math.rint(d * 10000))) {     //leave .0021 as .0021, but display .00023 as 2.3e-4
            synchronized(genExpFormat6) {
                return genExpFormat6.format(d);
            }
        }

        //large int
        if (Math.abs(d) < 1e13 && d == Math.rint(d))
            return "" + Math2.roundToLong(d);

        //>10e6
        if (Math.abs(d) >= 999999.9999995) {
            synchronized(genExpFormat6) {
                return genExpFormat6.format(d);
            }
        }

        synchronized(genStdFormat6) {
            return genStdFormat6.format(d);
        }
    }

    /**
     * This returns the number formatted with up to 10 digits to the left and right of
     * the decimal and trailing decimal 0's removed.  
     * If abs(d) &lt; 0.09999999995 or abs(d) &gt;= 999999.99999999995, the number is displayed
     * in scientific notation (e.g., 8.9544680321E-5).
     * Thus the maximum length should be 18 characters (-123456.1234567898).
     * 0 returns "0"
     * NaN returns "NaN".
     * Double.POSITIVE_INFINITY returns "Infinity".
     * Double.NEGATIVE_INFINITY returns "-Infinity".
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genEFormat10(double d) {

        //!finite
        if (!Math2.isFinite(d))
            return "" + d;

        //almost 0
        if (Math2.almost0(d))
            return "0";

        //close to 0 and many sig digits
        //String2.log("genEFormat test " + (d*1000) + " " + Math.rint(d*1000));
        if (Math.abs(d) < 0.09999999995 &&     
            !Math2.almostEqual(9, d * 1000000, Math.rint(d * 1000000))) {     //leave .0021 as .0021, but display .00023 as 2.3e-4
            synchronized(genExpFormat10) {
                return genExpFormat10.format(d);
            }
        }

        //large int
        if (Math.abs(d) < 1e13 && d == Math.rint(d)) //rint only catches 9 digits(?)
            return "" + Math2.roundToLong(d);

        //>10e6
        if (Math.abs(d) >= 999999.99999999995) {
            synchronized(genExpFormat10) {
                return genExpFormat10.format(d);
            }
        }

        synchronized(genStdFormat10) {
            return genStdFormat10.format(d);
        }
    }

    /**
     * This is like genEFormat6, but the scientific notation format
     * is, e.g., 8.954321x10^-5.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genX10Format6(double d) {
        return replaceAll(genEFormat6(d), "E", "x10^");
    }

    /**
     * This is like genEFormat10, but the scientific notation format
     * is, e.g., 8.9509484321x10^-5.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genX10Format10(double d) {
        return replaceAll(genEFormat10(d), "E", "x10^");
    }

    /**
     * This is like genEFormat6, but the scientific notation format
     * is, e.g., 8.954321x10<sup>-5</sup>.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genHTMLFormat6(double d) {
        String s = genEFormat6(d);
        int po = s.indexOf('E');
        if (po >= 0) 
            s = replaceAll(genEFormat6(d), "E", "x10<sup>") + "</sup>";
        return s;
    }

    /**
     * This is like genEFormat10, but the scientific notation format
     * is, e.g., 8.9509244321x10<sup>-5</sup>.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genHTMLFormat10(double d) {
        String s = genEFormat10(d);
        int po = s.indexOf('E');
        if (po >= 0) 
            s = replaceAll(genEFormat10(d), "E", "x10<sup>") + "</sup>";
        return s;
    }

    /**
     * This removes white space characters at the beginning and end of a StringBuilder.
     *
     * @param sb a StringBuilder
     * @return the same pointer to the StringBuilder
     */
    public static StringBuilder trim(StringBuilder sb) {
        int po = 0;
        while (po < sb.length() && isWhite(sb.charAt(po))) po++;
        sb.delete(0, po);

        po = sb.length();
        while (po > 0 && isWhite(sb.charAt(po - 1))) po--;
        sb.delete(po, sb.length());
        return sb;
    }

    /**
     * This trims just the start of the string.
     * 
     * @param s
     * @return s with just the start of the string trim'd.
     *    If s == null, this returns null.
     */
    public static String trimStart(String s) {
        if (s == null)
            return s;
        int sLength = s.length();
        int po = 0;
        while (po < sLength && String2.isWhite(s.charAt(po))) 
            po++;
        return po > 0? s.substring(po) : s;
    }

    /**
     * This trims just the end of the string.
     * 
     * @param s
     * @return s with just the end of the string trim'd.
     *    If s == null, this returns null.
     */
    public static String trimEnd(String s) {
        if (s == null)
            return s;
        int sLength = s.length();
        int po = sLength;
        while (po > 0 && String2.isWhite(s.charAt(po - 1))) 
            po--;
        return po < sLength? s.substring(0, po) : s;
    }

    /**
     * This returns the directory that is the classpath for the source
     * code files (with forward slashes and a trailing slash, 
     * e.g., c:/programs/tomcat/webapps/cwexperimental/WEB-INF/classes/.
     *
     * @return directory that is the classpath for the source
     *     code files 
     * @throws Exception if trouble
     */
    public static String getClassPath() {
        if (classPath == null) {
            String find = "/com/cohort/util/String2.class";
            //use this.getClass(), not ClassLoader.getSystemResource (which fails in Tomcat)
            classPath = String2.class.getResource(find).getFile();
            classPath = replaceAll(classPath, '\\', '/');
            int po = classPath.indexOf(find);
            classPath = classPath.substring(0, po + 1);

            //on windows, remove the troublesome leading "/"
            if (OSIsWindows && classPath.length() > 2 && 
                classPath.charAt(0) == '/' && classPath.charAt(2) == ':')
                classPath = classPath.substring(1);

            //classPath is a URL! so spaces are encoded as %20 on Windows!
            //UTF-8: see http://en.wikipedia.org/wiki/Percent-encoding#Current_standard
            try {
                classPath = URLDecoder.decode(classPath, "UTF-8");  
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }

        return classPath;
    }

    /**
     * On the command line, this prompts the user a String.
     *
     * @param prompt
     * @return the String the user entered
     * @throws Exception if trouble
     */
    public static String getStringFromSystemIn(String prompt) throws Exception {
        System.out.print(prompt);
        BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
        return inReader.readLine();
    }

    /**
     * On the command line, this prompts the user a String (which is
     * not echoed to the screen, so is suitable for passwords).
     * This is slighly modified from 
     * http://java.sun.com/developer/technicalArticles/Security/pwordmask/ .
     *
     * @param prompt
     * @return the String the user entered
     * @throws Exception if trouble
     */
/*    public static String getPasswordFromSystemIn(String prompt) throws Exception {
        System.out.print(prompt);
        StringBuilder sb = new StringBuilder();
        while (true) {
            while (System.in.available() == 0) {
                Math2.sleep(1);
                System.out.print("\b*");
            }                    
            int ch = System.in.read();
            if (ch <= 0) 
                continue;
            if (ch == '\n') 
                return sb.toString();
            sb.append((char)ch);
        }
    }
*/
    public static final String getPasswordFromSystemIn(String prompt) throws Exception {
        InputStream in = System.in; //bob added, instead of parameter

        MaskingThread maskingthread = new MaskingThread(prompt);
        Thread thread = new Thread(maskingthread);
        thread.start();
        

        char[] lineBuffer;
        char[] buf;
        int i;

        buf = lineBuffer = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;
        
        try { //bob added
            loop:   while (true) {
                c = in.read();
                if (c == -1 || c == '\n')
                    break loop;
                if (c == '\r') {
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream)in).unread(c2);
                    } else {
                        break loop;
                    }
                }

                //if not caught and 'break loop' above...
                if (--room < 0) {
                    buf = new char[offset + 128];
                    room = buf.length - offset - 1;
                    System.arraycopy(lineBuffer, 0, buf, 0, offset);
                    Arrays.fill(lineBuffer, ' ');
                    lineBuffer = buf;
                }
                buf[offset++] = (char) c;
            }
        } catch (Exception e) {
        }
        maskingthread.stopMasking();
        if (offset == 0) {
           return ""; //bob changed from null
        }
        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');
        return new String(ret); //bob added; originally it returned char[]
    }

    /**
     * Find the last element which is <= s in an ascending sorted array.
     *
     * @param sar an ascending sorted String[] which may have duplicate values
     * @param s
     * @return the index of the last element which is <= s in an ascending sorted array.
     *   If s is null or s < the smallest element, this returns -1  (no element is appropriate).
     *   If s > the largest element, this returns sar.length-1.
     */
    public static int binaryFindLastLE(String[] sar, String s) {
        if (s == null) 
            return -1;
        int i = Arrays.binarySearch(sar, s);

        //an exact match; look for duplicates
        if (i >= 0) {
            while (i < sar.length - 1 && sar[i + 1].compareTo(s) <= 0)
                i++;
            return i; 
        }

        int insertionPoint = -i - 1;  //0.. sar.length
        return insertionPoint - 1;
    }

    /**
     * Find the first element which is >= s in an ascending sorted array.
     *
     * @param sar an ascending sorted String[] which currently may not have duplicate values
     * @param s
     * @return the index of the first element which is >= s in an ascending sorted array.
     *   If s < the smallest element, this returns 0.
     *   If s is null or s > the largest element, this returns sar.length (no element is appropriate).
     */
    public static int binaryFindFirstGE(String[] sar, String s) {
        if (s == null) 
            return sar.length;
        int i = Arrays.binarySearch(sar, s);

        //an exact match; look for duplicates
        if (i >= 0) {
            while (i > 0 && sar[i - 1].compareTo(s) >= 0)
                i--;
            return i; 
        }

        return -i - 1;  //the insertion point,  0.. sar.length
    }

    /**
     * Find the closest element to s in an ascending sorted array.
     *
     * @param sar an ascending sorted String[].
     *   It the array has duplicates and s equals one of them,
     *   it isn't specified which duplicate's index will be returned.
     * @param s
     * @return the index of the element closest to s.
     *   If s is null, this returns -1.
     */
    public static int binaryFindClosest(String[] sar, String s) {
        if (s == null)
            return -1;
        int i = Arrays.binarySearch(sar, s);
        if (i >= 0)
            return i; //success

        //insertionPoint at end point?
        int insertionPoint = -i - 1;  //0.. sar.length
        if (insertionPoint == 0) 
            return 0;
        if (insertionPoint >= sar.length)
            return sar.length - 1;

        //insertionPoint between 2 points 
        //do they differ at a different position?
        //make all the same length
        int preIndex = insertionPoint - 1;
        int postIndex = insertionPoint;
        String pre  = sar[preIndex];
        String post = sar[postIndex];
        int longest = Math.max(s.length(), Math.max(pre.length(), post.length()));
        String ts = s + makeString(' ', longest - s.length());
        pre  += makeString(' ', longest - pre.length());
        post += makeString(' ', longest - post.length());
        for (i = 0; i < longest; i++) {
            char ch = ts.charAt(i);
            char preCh = pre.charAt(i);
            char postCh = post.charAt(i);
            if (preCh == ch && postCh != ch) return preIndex;
            if (preCh != ch && postCh == ch) return postIndex;
            if (preCh != ch && postCh != ch) {
                //which one is closer
                return Math.abs(preCh - ch) < Math.abs(postCh - ch)?
                    preIndex : postIndex;
            }
        }
        //shouldn't all be equal
        return preIndex;
    }

    /**
     * This returns the index of the first non-utf-8 character.
     * Currently, valid characters are #32 - #126, #160+.
     *
     * @param s
     * @param alsoOK a string with characters (e.g., \n, \t) which are also valid
     * @return the index of the first non-utf-8 character, or -1 if all valid.
     */
    public static int findInvalidUtf8(String s, String alsoOK) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (alsoOK.indexOf(ch) >= 0)
                continue;
            if (ch < 32) 
                return i;
            if (ch <= 126)
                continue;
            if (ch <= 159)
                return i;
            //160+ is valid
        }
        return -1;
    }

    /**
     * This returns the UTF-8 encoding of the string (or null if trouble).
     * The inverse of this is utf8ToString.
     */
    public static byte[] getUTF8Bytes(String s) {
        try {
            return s.getBytes("UTF-8");             
        } catch (Exception e) {
            String2.log(ERROR + " in String2.getUTF8Bytes(" + s + "): " + e.toString());
            return null;
        }
    }

    /**
     * This returns a string from the UTF-8 encoded byte[] (or null if trouble).
     * The inverse of this is getUTF8Bytes.
     */
    public static String utf8ToString(byte[] bar) {
        try {
            return new String(bar, "UTF-8");             
        } catch (Exception e) {
            String2.log(ERROR + " in String2.utf8ToString: " + e.toString());
            return null;
        }
    }

    /**
     * This creates the jump table (int[256]) for a given 'find' stringUtf8
     * of use by indexOf(byte[], byte[], jumpTable[]) below.
     * Each entry in the result is: how far for indexOf to jump endPo forward for any given s[endPo] byte.
     *
     * @param find the byte array to be found
     * @return jump table (int[256]) for a given 'find' stringUtf8
     */
    public static int[] makeJumpTable(byte[] find) {
        //work forwards so last found instance of a letter is most important
        //   s = Two times nine.
        //find = nine
        //First test will compare find's 'e' and s's ' '
        //Not a match, so jump jump[' '] positions forward.
        int findLength = find.length;
        int jump[] = new int[256];
        Arrays.fill(jump, findLength);
        for (int po = 0; po < findLength; po++)
            jump[find[po] & 0xFF] = findLength - 1 - po;   //make b 0..255
        return jump;
    }


    /**
     * Return the first index of 'find' in s (or -1 if not found).
     * Idea: since a full text search entails looking for a few 'find' strings
     *   inside any of nDatasets long searchStrings (that don't change often),
     *   and since we don't really care about exact index, just relative index,
     *   it would be nice to store searchStrings as byte[]
     *   (1/2 the memory use and simpler search).
     *   So encode 'find' and searchString as UTF-8 via byte[] find.getBytes(utf8Charset).
     *   Then we can do Boyer-Moore-like search for first indexOf.
     *   This can speed up the searches ~3.5X in good conditions (assuming setup is amortized).
     *
     * @param s the long string to be search, stored utf8.
     * @param find the short string to be found, stored utf 8.
     * @param jumpTable from makeJumpTable
     * @return the first index of 'find' in s (or -1 if not found).
     */
    public static int indexOf(byte[] s, byte[] find, int jumpTable[]) {
        //future: is jump table for second character jumpTable[s[endPo]]-1 IFF that value isn't <=0?

        //see algorithm in makeJumpTable
        int findLength = find.length;
        int sLength = s.length;
        if (findLength == 0)  return 0;
        if (sLength == 0)     return -1;
        int findLength1 = findLength - 1;
        int endPo = findLength1;
        byte lastFindByte = find[findLength1]; 

        //if findLength is 1, do simple search
        if (findLength == 1) {
            int po = -1;
            while (++po < sLength) {
                if (s[po] == lastFindByte)
                    return po;  
            }
            return -1;
        }

        //Boyer-Moore-like search
        whileBlock:
        while (endPo < sLength) {
            byte b = s[endPo];

            //last bytes don't match? jump
            if (b != lastFindByte) {
                endPo += jumpTable[b & 0xFF]; //make b 0..255
                continue;
            }

            //last bytes do match: try to match all of 'find'
            int countBack = 1;
            do {  //we know find is at least 2 long
                if (s[endPo - countBack] == find[findLength1 - countBack]) {
                   countBack++;
                } else {
                    endPo += 1;
                    continue whileBlock;
                }
            } while (countBack < findLength); 

            //found it!
            return endPo - findLength1;
        }
        return -1;
    } 

    /**
     * This returns the MD5 hash digest of getUTF8Bytes(password) as a String of 32 lowercase hex digits.
     * Lowercase because the digest authentication standard uses lower case; so mimic them.
     * And lowercase is easier to type.
     * 
     * @param password  the text to be digested
     * @return the MD5 hash digest of the password (32 lowercase hex digits, as a String),
     *   or null if password is null or there is trouble.
     */
    public static String md5Hex(String password) {
        try {
            if (password == null) return null;
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(getUTF8Bytes(password));
            byte bytes[] = md.digest();
            int nBytes = bytes.length;
            StringBuilder sb = new StringBuilder(nBytes * 2);
            for (int i = 0; i < nBytes; i++)
                sb.append(zeroPad(Integer.toHexString(
                    (int)bytes[i] & 0xFF), 2));   //safe, (int) and 0xFF make it unsigned byte
            return sb.toString();
            //return password == null? null : DigestUtils.md5Hex(password).toLowerCase();     
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return null;
        }
    }

    /** 
     * This returns the last 12 hex digits from md5Hex (or null if md5 is null),
     * broken into 3 blocks of 4 digits, separated by '_'.
     * I use this as a short, easy to type, repeatable, representation of 
     * long strings (e.g., an ERDDAP query URL), sort of like the idea of tinyURL.
     * It performs much better than hashcode or CRC32 when a large number of passwords
     * (or filenames) are encoded and you don't want any collisions.
     * See Projects.testHashFunctions.
     */
    public static String md5Hex12(String password) {
        String s = md5Hex(password);
        return s == null? null : 
            s.substring(20, 24) + "_" + s.substring(24, 28) + "_" + s.substring(28, 32);
    }

    /**
     * Given two strings with internal newlines, oldS and newS, this a message
     * indicating where they differ.
     * 
     * @param oldS  
     * @param newS
     * @return a message indicating where they differ, or "" if there is no difference.
     */
    public static String differentLine(String oldS, String newS) {
        if (oldS == null) return "(There is no old version.)";
        if (newS == null) return "(There is no new version.)";
        int oldLength = oldS.length();
        int newLength = newS.length();
        int newlinePo = -1;
        int line = 1;
        int n = Math.min(oldLength, newLength);
        int po = 0;
        while (po < n && oldS.charAt(po) == newS.charAt(po)) {
            if (oldS.charAt(po) == '\n') {newlinePo = po; line++;}
            po++;
        }
        if (po == oldLength && po == newLength)
            return "";
        int oldEnd = newlinePo + 1;
        int newEnd = newlinePo + 1;
        while (oldEnd < oldLength && oldS.charAt(oldEnd) != '\n') oldEnd++;
        while (newEnd < newLength && newS.charAt(newEnd) != '\n') newEnd++;
        return 
            "  old line #" + line + "=" + toJson(oldS.substring(newlinePo + 1, oldEnd)) + ",\n" +
            "  new line #" + line + "=" + toJson(newS.substring(newlinePo + 1, newEnd)) + ".";
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

        if (!Math2.isFinite(d))
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

    /**
     * This is different from String2.modifyToBeFileNameSafe --
     *   this encodes non-fileNameSafe characters so little or no information is lost.
     * <br>This returns the string with just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     * <br>'x' and non-safe characters are CONVERTED to 'x' plus their 
     *   2 lowercase hexadecimalDigit number or "xx" + their 4 hexadecimalDigit number.
     * <br>See posix fully portable file names at http://en.wikipedia.org/wiki/Filename .
     * <br>When the encoding is more than 25 characters, this stops encoding and 
     *   adds "xh" and the hash code for the entire original string,
     *   so the result will always be less than ~41 characters.
     *
     * <p>THIS WON'T BE CHANGED. FILE NAMES CREATED FOR EDDGridCopy and EDDTableCopy 
     *  DEPEND ON SAME ENCODING OVER TIME.
     *
     * @param s  
     * @return s with all of the non-fileNameSafe characters changed.
     *    <br>If s is null, this returns "x-1".
     *    <br>If s is "", this returns "x-0".
     */
    public static String encodeFileNameSafe(String s) {
        if (s == null)
            return "x-1";
        int n = s.length();
        if (n == 0)
            return "x-0";
        StringBuilder sb = new StringBuilder(n / 3 * 4);
        for (int i = 0; i < n; i++) {
            if (sb.length() >= 25) {
                sb.append("xh" + md5Hex12(s)); //was Math2.reduceHashCode(s.hashCode()));
                break;
            }
            char ch = s.charAt(i);

            if (ch != 'x' && String2.isFileNameSafe(ch)) {
                sb.append(ch);
            } else if (ch <= 255) {
                sb.append("x" + String2.zeroPad(Integer.toHexString(ch), 2));
            } else {
                sb.append("xx" + String2.zeroPad(Integer.toHexString(ch), 4));
            }
        }

        return sb.toString();
    }

    /**
     * This is like encodeFileNameSafe, but further restricts the name to
     * <ul>
     * <li>first character must be A-Z, a-z, _.
     * <li>subsequent characters must be A-Z, a-z, _, 0-9.
     * <ul>
     * <br>'x' and non-safe characters are CONVERTED to 'x' plus their 
     *   2 lowercase hexadecimalDigit number or "xx" + their 4 hexadecimalDigit number.
     * <br>See posix fully portable file names at http://en.wikipedia.org/wiki/Filename .
     * <br>When the encoding is more than 25 characters, this stops encoding and 
     *   adds "xh" and the hash code for the entire original string,
     *   so the result will always be less than ~41 characters.
     *
     * <p>THIS WON'T BE CHANGED. FILE NAMES CREATED FOR EDDGridFromFile and EDDTableFromFile 
     *  DEPEND ON SAME ENCODING OVER TIME.
     *
     * @param s  
     * @return s with all of the non-variableNameSafe characters changed.
     *    <br>If s is null, this returns "x_1".
     *    <br>If s is "", this returns "x_0".
     */
    public static String encodeVariableNameSafe(String s) {
        if (s == null)
            return "x_1";
        int n = s.length();
        if (n == 0)
            return "x_0";
        StringBuilder sb = new StringBuilder(n / 3 * 4);
        for (int i = 0; i < n; i++) {
            if (sb.length() >= 25) {
                sb.append("xh" + md5Hex12(s)); //was Math2.reduceHashCode(s.hashCode()));
                break;
            }
            char ch = s.charAt(i);

            if (ch != 'x' && String2.isFileNameSafe(ch) && ch != '-' && ch != '.' &&
                (i > 0 || ((ch >= 'A' && ch <= 'Z') || (ch >='a' && ch <='z') || (ch == '_')))) {
                sb.append(ch);
            } else if (ch <= 255) {
                sb.append("x" + String2.zeroPad(Integer.toHexString(ch), 2));
            } else {
                sb.append("xx" + String2.zeroPad(Integer.toHexString(ch), 4));
            }
        }

        return sb.toString();
    }


    /** 
     * Get gets the String from the system clipboard
     * (or null if none).
     * This works in a standalone Java program, not an applet.
     * From Java Developers Almanac.
     * This won't throw an exception.
     */
    public static String getClipboardString() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = clipboard.getContents(null);    
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) 
                return (String)t.getTransferData(DataFlavor.stringFlavor);
        } catch (Throwable th) {
            String2.log(ERROR + " while getting the string from the clipboard:\n" +
                MustBe.throwableToString(th));
        }
        return null;
    }
    
    /** This method writes a string to the system clipboard.
     * This works in a standalone Java program, not an applet.
     * From Java Developers Almanac.
     * This won't throw an exception.
     */
    public static void setClipboardString(String s) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(s), null);
        } catch (Throwable t) {
            String2.log(ERROR + " while putting the string on the clipboard:\n" +
                MustBe.throwableToString(t));
        }
    }



    /** 
     * This is like String.intern(), but uses a WeakHashMap so the canonical strings 
     * can be garbage collected.
     * <br>This is thread safe.
     * <br>It is fast: ~0.002ms per call.
     * <br>See TestUtil.testString2canonical().
     *
     * <p>Using this increases memory use by ~6 bytes per canonical string
     * (4 for pointer * ~.5 hashMap load factor).
     * <br>So it only saves memory if many strings would otherwise be duplicated.
     * <br>But if lots of strings are originally duplicates, it saves *lots* of memory.
     *
     * @param s  the string   (may be null)  (may be from s2.substring(start, stop))
     * @return a canonical string with the same characters as s.
     */
    public static String canonical(String s) {
        if (s == null)
            return null;
        //it usually slows things down to compare to lastCanonical String.

        //faster and logically better to synchronized(canonicalMap) once 
        //  (and use a few times in consistent state)
        //than to synchronize canonicalMap and lock/unlock twice
        synchronized (canonicalMap) {
            WeakReference wr = (WeakReference)canonicalMap.get(s);
            //wr won't be garbage collected, but referent might (making wr.get() return null)
            String canonical = wr == null? null : (String)(wr.get());
            if (canonical == null) {
                //For proof that new String(s.substring(,)) is just storing relevant chars,
                //not a reference to the parent string, see TestUtil.testString2canonical2()
                canonical = new String(s); //in case s is from s2.substring, copy to be just the characters
                canonicalMap.put(canonical, new WeakReference(canonical));
                //String2.log("new canonical string: " + canonical);
            }
            return canonical;
        }
    }

    /** This is only used to test canonical. */
    public static int canonicalSize() {
        return canonicalMap.size();
    }

    /** If quoted=true, this puts double quotes around a string, if needed.
     * In any case, carriageReturn/newline characters/combos are replaced by 
     * char #166 (pipe with gap).
     *
     * @param quoted if true, if a String value starts or ends with a space 
     *    or has a double quote or comma, 
     *    the value will be surrounded in double quotes
     *    and internal double quotes become two double quotes.
     * @return the revised string
     */
    public static String quoteIfNeeded(boolean quoted, String s) {
        //this is Bob's unprecedented solution to dealing with newlines
        //� is (char)166 (#166), so distinct from pipe, (char)124
        int po = s.indexOf('\n');
        if (po >= 0) {
            s = replaceAll(s, '\n', (char)166); //'�'  (#166)
            s = replaceAll(s, "\r", "");
        } else {
            s = replaceAll(s, '\r', (char)166); //'�'  (#166)
            s = replaceAll(s, "\n", ""); 
        }
        if (quoted) {
            if (s.indexOf('"') >= 0 || s.indexOf(',') >= 0 ||
                (s.length() > 0 && (s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' '))) 
                s = "\"" + replaceAll(s, "\"", "\"\"") + "\"";
        }
        return s;
    }

    /* *
     * This makes a medium-deep clone of an ArrayList by calling clone() of
     * each element of the ArrayList.
     *
     * @param oldArrayList
     * @param newArrayList  If oldArrayList is null, this returns null.
     *    Elements of oldArrayList can be null.
     */
    /* I couldn't make this compile. clone throws an odd exception.
    public ArrayList clone(ArrayList oldArrayList) {
        if (oldArrayList == null)
            return (ArrayList)null;

        ArrayList newArrayList = new ArrayList();
        int n = oldArrayList.size();
        for (int i = 0; i < n; i++) {
            Object o = oldArrayList.get(i);
            try {
                if (o != null) o = o.clone();
            } catch (Exception e) {
            }
            newArrayList.add(o);
        }
        return newArrayList;
    } */

    /** This changes the characters case to title case (only letters after non-letters are
     * capitalized.  This is simplistic.
     */
    public static String toTitleCase(String s) {
        if (s == null)
            return null;
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(s);
        char c = ' ', oc = ' ';
        for (int i = 0; i < sLength; i++) {
            oc = c;
            c = sb.charAt(i);
            if (isLetter(c)) 
                sb.setCharAt(i, isLetter(oc)? Character.toLowerCase(c): Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /** This changes the character's case to sentence case 
     * (first letter and first letter after each period capitalized).  This is simplistic.
     */
    public static String toSentenceCase(String s) {
        if (s == null)
            return null;
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(s);
        boolean capNext = true;
        for (int i = 0; i < sLength; i++) {
            char c = sb.charAt(i);
            if (isLetter(c)) {
                if (capNext) {
                    sb.setCharAt(i, Character.toUpperCase(c));
                    capNext = false;
                } else {
                    sb.setCharAt(i, Character.toLowerCase(c));
                }
            } else if (c == '.') {
                capNext = true;
            }
        }
        return sb.toString();
    }

    /** This suggests a camel-case variable name.
     * 
     * @param s the starting string for the variable name.
     * @return a valid variable name asciiLowerCaseLetter+asciiDigitLetter*, using camel case.
     *   This is a simplistic suggestion. Different strings may return the same variable name.
     *   null returns "null".
     *   "" returns "a".
     */
    public static String toVariableName(String s) {
        if (s == null)
            return "null";
        int sLength = s.length();
        if (sLength == 0)
            return "a";
        s = modifyToBeASCII(s);
        s = toTitleCase(s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sLength; i++) {
            char c = s.charAt(i);
            if (isDigitLetter(c))
                sb.append(c);
        }
        if (sb.length() == 0)
            return "a";
        char c = sb.charAt(0);
        sb.setCharAt(0, Character.toLowerCase(c));
        if (c >= '0' && c <= '9')
            sb.insert(0, 'a');
        return sb.toString();
    }

    /**
     * This returns true if the string contains only ISO 8859-1 characters (i.e., 0 - 255).
     */
    public static boolean isIso8859(String s) {
        int sLength = s.length();
        for (int i = 0; i < sLength; i++) 
            if (s.charAt(i) > 255) return false;
        return true;
    }

} //End of String2 class.

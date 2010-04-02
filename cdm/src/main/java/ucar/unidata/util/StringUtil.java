/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.unidata.util;


import java.text.ParsePosition;


import java.text.SimpleDateFormat;


import java.util.*;
import java.util.regex.*;




/**
 * String utilities
 */

public class StringUtil {

    /** debug flag */
    public static boolean debug = false;

    /** Ordinal names for images */
    public static final String[] ordinalNames = {
        "Latest", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh",
        "Eighth", "Ninth", "Tenth"
    };




    /**
     * Check if the string is not empty
     * @param s  String to check
     * @return true if it's not the empty string (len > 0)
     */
    public static boolean notEmpty(String s) {
        return (s != null) && (s.trim().length() > 0);
    }

    /**
     * Collapse continuous whitespace into one single " ".
     *
     * @param s operate on this string
     * @return result with collapsed whitespace
     */
    static public String collapseWhitespace(String s) {
        int          len = s.length();
        StringBuffer b   = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if ( !Character.isWhitespace(c)) {
                b.append(c);
            } else {
                b.append(' ');
                while ((i + 1 < len)
                        && Character.isWhitespace(s.charAt(i + 1))) {
                    i++;  /// skip further whitespace
                }
            }
        }
        return b.toString();
    }



    /**
     * Remove all occurrences of the substring sub in the string s.
     *
     * @param s operate on this string
     * @param sub remove all occurrences of this substring.
     *
     * @return result with substrings removed
     */
    static public String remove(String s, String sub) {
        int len = sub.length();
        int pos;
        while (0 <= (pos = s.indexOf(sub))) {
            s = s.substring(0, pos) + s.substring(pos + len);
        }
        return s;
    }

    /**
     * Remove all occurrences of the character c in the string s.
     *
     * @param s operate on this string
     * @param c remove all occurrences of this character.
     *
     * @return result with any character c removed
     */
    static public String remove(String s, int c) {
        if (0 > s.indexOf(c)) {  // none
            return s;
        }

        StringBuffer buff = new StringBuffer(s);
        int          i    = 0;
        while (i < buff.length()) {
            if (buff.charAt(i) == c) {
                buff.deleteCharAt(i);
            } else {
                i++;
            }
        }
        return buff.toString();
    }

    /**
     * Return the format string in the given text for the given macro.
     * text may contain a macro of the form 'macroDelimiter macroName:format string macroDelimiter'
     * e.g.: %count:some format%
     * This returns the format string
     *
     * @param macroName The name of the macro
     * @param macroDelimiter The delimiter used. e.g. '%'
     * @param text the text
     * @return the format string or null
     */
    public static String findFormatString(String macroName,
                                          String macroDelimiter,
                                          String text) {
        String prefix = macroDelimiter + macroName + ":";
        int    idx1   = text.indexOf(prefix);
        if (idx1 >= 0) {
            int idx2 = text.indexOf(macroDelimiter, idx1 + 1);
            if (idx2 > idx1) {
                return text.substring(idx1 + prefix.length(), idx2);
            }
        }
        return null;
    }


    /**
     * Find all occurences of the "match" in original, and substitute the "subst" string.
     * @param original starting string
     * @param match string to match
     * @param subst string to substitute
     * @return a new string with substitutions
     */
    static public String substitute(String original, String match,
                                    String subst) {
        String s = original;
        int    pos;
        while (0 <= (pos = s.indexOf(match))) {
            StringBuffer sb = new StringBuffer(s);
            s = sb.replace(pos, pos + match.length(), subst).toString();
        }
        return s;
    }

    /**
     * Concatentate the given string cnt times
     *
     * @param s base string
     * @param cnt
     *
     * @return repeated string
     */
    public static String repeat(String s, int cnt) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < cnt; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Find all occurences of match strings in original, and substitute the corresponding
     *   subst string.
     * @param original starting string
     * @param match array of strings to match
     * @param subst array of strings to substitute
     * @return a new string with substitutions
     */
    static public String substitute(String original, String[] match,
                                    String[] subst) {

        boolean ok = true;
        for (int i = 0; i < match.length; i++) {
            if (0 <= original.indexOf(match[i])) {
                ok = false;
                break;
            }
        }
        if (ok) {
            return original;
        }

        // gotta do it;
        StringBuffer sb = new StringBuffer(original);
        for (int i = 0; i < match.length; i++) {
            substitute(sb, match[i], subst[i]);
        }
        return sb.toString();
    }

    /**
     * Find all occurences of the "match" in original, and substitute the "subst" string,
     *  directly into the original.
     * @param sbuff starting string buffer
     * @param match string to match
     * @param subst string to substitute
     */
    static public void substitute(StringBuffer sbuff, String match,
                                  String subst) {
        int pos,
            fromIndex = 0;
        int substLen  = subst.length();
        int matchLen  = match.length();
        while (0 <= (pos = sbuff.indexOf(match, fromIndex))) {
            sbuff.replace(pos, pos + matchLen, subst);
            fromIndex = pos + substLen;  // make sure dont get into an infinite loop
        }
    }

    /**
     * Return true if all characters are numeric.
     *
     * @param s operate on this String
     *
     * @return true if all characters are numeric
     */
    static public boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if ( !Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replace special characters with entities for HTML content.
     * special: '&', '"', '\'', '<', '>', '\n'
     * @param x string to quote
     * @return equivilent string using entities for any special chars
     */
    static public String quoteHtmlContent(String x) {
        return replace(x, htmlIn, htmlOut);
    }

    /** these chars must get replaced */
    private static char[] htmlIn = {
        '&', '"', '\'', '<', '>', '\n'
    };

    /** replacement strings */
    private static String[] htmlOut = {
        "&amp;", "&quot;", "&#39;", "&lt;", "&gt;", "\n<p>"
    };



    /**
     * Replace special characters with entities for XML attributes.
     * special: '&', '<', '>', '\'', '"', '\r', '\n'
     * @param x string to quote
     * @return equivilent string using entities for any special chars
     */
    static public String quoteXmlContent(String x) {
        return replace(x, xmlInC, xmlOutC);
    }

    /**
     * Reverse XML quoting to recover the original string.
     * @param x string to quote
     * @return equivilent string
     */
    static public String unquoteXmlContent(String x) {
        return unreplace(x, xmlOutC, xmlInC);
    }

    /** these chars must get replaced in XML */
    private static char[] xmlInC = { '&', '<', '>' };

    /** replacement strings */
    private static String[] xmlOutC = { "&amp;", "&lt;", "&gt;" };



    /**
     * Replace special characters with entities for XML attributes.
     * special: '&', '<', '>', '\'', '"', '\r', '\n'
     * @param x string to quote
     * @return equivilent string using entities for any special chars
     */
    static public String quoteXmlAttribute(String x) {
        return replace(x, xmlIn, xmlOut);
    }

    /**
     * Reverse XML quoting to recover the original string.
     * @param x string to quote
     * @return equivilent string
     */
    static public String unquoteXmlAttribute(String x) {
        return unreplace(x, xmlOut, xmlIn);
    }

    /** these chars must get replaced */
    private static char[] xmlIn = {
        '&', '"', '\'', '<', '>', '\r', '\n'
    };

    /** replacement strings */
    private static String[] xmlOut = {
        "&amp;", "&quot;", "&apos;", "&lt;", "&gt;", "&#13;", "&#10;"
    };

    /**
     * Replace all occurences of replaceChar with replaceWith
     *
     * @param x operate on this string
     * @param replaceChar get rid of these
     * @param replaceWith replace with these
     *
     * @return resulting string
     */
    static public String replace(String x, char[] replaceChar,
                                 String[] replaceWith) {
        // common case no replacement
        boolean ok = true;
        for (int i = 0; i < replaceChar.length; i++) {
            int pos = x.indexOf(replaceChar[i]);
            ok = (pos < 0);
            if ( !ok) {
                break;
            }
        }
        if (ok) {
            return x;
        }

        // gotta do it
        StringBuffer sb = new StringBuffer(x);
        for (int i = 0; i < replaceChar.length; i++) {
            int pos = x.indexOf(replaceChar[i]);
            if (pos >= 0) {
                replace(sb, replaceChar[i], replaceWith[i]);
            }
        }

        return sb.toString();
    }


    /**
     * Replace all occurences of orgReplace with orgChar; inverse of replace().
     *
     * @param x operate on this string
     * @param orgReplace get rid of these
     * @param orgChar replace with these
     *
     * @return resulting string
     */
    static public String unreplace(String x, String[] orgReplace,
                                   char[] orgChar) {
        // common case no replacement
        boolean ok = true;
        for (int i = 0; i < orgReplace.length; i++) {
            int pos = x.indexOf(orgReplace[i]);
            ok = (pos < 0);
            if ( !ok) {
                break;
            }
        }
        if (ok) {
            return x;
        }

        // gotta do it
        StringBuffer result = new StringBuffer(x);
        for (int i = 0; i < orgReplace.length; i++) {
            int pos = result.indexOf(orgReplace[i]);
            if (pos >= 0) {
                unreplace(result, orgReplace[i], orgChar[i]);
            }
        }

        return result.toString();
    }

    /**
     * Count number of chars that match in two strings, starting from front.
     * @param s1 compare this string
     * @param s2 compare this string
     * @return number of matching chars, starting from first char
     */
    static public int match(String s1, String s2) {
        int i = 0;
        while ((i < s1.length()) && (i < s2.length())) {
            if (s1.charAt(i) != s2.charAt(i)) {
                break;
            }
            i++;
        }
        return i;
    }

    /**
     * Replace any char "out" in sb with "in".
     * @param sb StringBuffer to replace
     * @param out repalce this character
     * @param in with this string
     */
    static public void replace(StringBuffer sb, char out, String in) {
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == out) {
                sb.replace(i, i + 1, in);
                i += in.length() - 1;
            }
        }
    }

    /**
     * Replace any String "out" in sb with char "in".
     * @param sb StringBuffer to replace
     * @param out repalce this String
     * @param in with this char
     */
    static public void unreplace(StringBuffer sb, String out, char in) {
        int pos;
        while (0 <= (pos = sb.indexOf(out))) {
            sb.setCharAt(pos, in);
            sb.delete(pos + 1, pos + out.length());
        }
    }

    /**
     * Replace any char "out" in s with "in".
     * @param s string to replace
     * @param out repalce this character
     * @param in with this string
     * @return modified string if needed
     */
    static public String replace(String s, char out, String in) {
        if (s.indexOf(out) < 0) {
            return s;
        }

        // gotta do it
        StringBuffer sb = new StringBuffer(s);
        replace(sb, out, in);
        return sb.toString();
    }





    /**
     * Escape any char not alphanumeric or in okChars.
     * Escape by replacing char with %xx (hex).
     * LOOK: need to check for %, replace with %%
     * @param x escape this string
     * @param okChars these are ok.
     * @return equivilent escaped string.
     */
    static public String escape(String x, String okChars) {
        boolean ok = true;
        for (int pos = 0; pos < x.length(); pos++) {
            char c = x.charAt(pos);
            if ( !(Character.isLetterOrDigit(c)
                    || (0 <= okChars.indexOf(c)))) {
                ok = false;
                break;
            }
        }
        if (ok) {
            return x;
        }

        // gotta do it
        StringBuffer sb = new StringBuffer(x);
        for (int pos = 0; pos < sb.length(); pos++) {
            char c = sb.charAt(pos);
            if (Character.isLetterOrDigit(c) || (0 <= okChars.indexOf(c))) {
                continue;
            }

            sb.setCharAt(pos, '%');
            int value = (int) c;
            pos++;
            sb.insert(pos, Integer.toHexString(value));
            pos++;
        }

        return sb.toString();
    }


    /**
     * Escape any char in reservedChars.
     * Escape by replacing char with %xx (hex).
     * LOOK: need to check for %, replace with %%
     *
     * @param x             escape this string
     * @param reservedChars these must be replaced
     * @return equivilent escaped string.
     */
    static public String escape2(String x, String reservedChars) {
        boolean ok = true;
        for (int pos = 0; pos < x.length(); pos++) {
            char c = x.charAt(pos);
            if (reservedChars.indexOf(c) >= 0) {
                ok = false;
                break;
            }
        }
        if (ok) {
            return x;
        }

        // gotta do it
        StringBuffer sb = new StringBuffer(x);
        for (int pos = 0; pos < sb.length(); pos++) {
            char c = sb.charAt(pos);
            if (reservedChars.indexOf(c) < 0) {
                continue;
            }

            sb.setCharAt(pos, '%');
            int value = (int) c;
            pos++;
            sb.insert(pos, Integer.toHexString(value));
            pos++;
        }

        return sb.toString();
    }



    /**
     * Convert the given color to is string hex representation
     *
     * @param c color
     *
     * @return hex represenation
     */
    public static String toHexString(java.awt.Color c) {
        return "#" + padRight(Integer.toHexString(c.getRed()), 2, "0")
               + padRight(Integer.toHexString(c.getGreen()), 2, "0")
               + padRight(Integer.toHexString(c.getBlue()), 2, "0");
    }





    /**
     *  Remove all but printable 7bit ascii
     *  @param s filter this string
     *  @return filtered string.
     */
    static public String filter7bits(String s) {
        byte[] b     = s.getBytes();
        byte[] bo    = new byte[b.length];
        int    count = 0;
        for (int i = 0; i < s.length(); i++) {
            if ((b[i] < 128) && (b[i] > 31)
                    || ((b[i] == '\n') || (b[i] == '\t'))) {
                bo[count++] = b[i];
            }
        }

        return new String(bo, 0, count);
    }


    /**
     * Remove any char not alphanumeric or in okChars.
     * @param x filter this string
     * @param okChars these are ok.
     * @return filtered string.
     */
    static public String filter(String x, String okChars) {
        boolean ok = true;
        for (int pos = 0; pos < x.length(); pos++) {
            char c = x.charAt(pos);
            if ( !(Character.isLetterOrDigit(c)
                    || (0 <= okChars.indexOf(c)))) {
                ok = false;
                break;
            }
        }
        if (ok) {
            return x;
        }

        // gotta do it
        StringBuffer sb = new StringBuffer(x.length());
        for (int pos = 0; pos < x.length(); pos++) {
            char c = x.charAt(pos);
            if (Character.isLetterOrDigit(c) || (0 <= okChars.indexOf(c))) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Replace any char not alphanumeric or in allowChars by replaceChar.
     * @param x operate on this string
     * @param allowChars these are ok.
     * @param replaceChar thar char to replace
     * @return resulting string.
     */
    static public String allow(String x, String allowChars,
                               char replaceChar) {
        boolean ok = true;
        for (int pos = 0; pos < x.length(); pos++) {
            char c = x.charAt(pos);
            if ( !(Character.isLetterOrDigit(c)
                    || (0 <= allowChars.indexOf(c)))) {
                ok = false;
                break;
            }
        }
        if (ok) {
            return x;
        }

        // gotta do it
        StringBuffer sb = new StringBuffer(x);
        for (int pos = 0; pos < sb.length(); pos++) {
            char c = sb.charAt(pos);
            if (Character.isLetterOrDigit(c)
                    || (0 <= allowChars.indexOf(c))) {
                continue;
            }

            sb.setCharAt(pos, replaceChar);
        }

        return sb.toString();
    }


    /**
     * This finds any '%xx' and converts to the equivilent char. Inverse of escape().
     *
     * @param x
     * @return original String.
     */
    static public String unescape(String x) {
        if (x.indexOf('%') < 0) {
            return x;
        }

        // gotta do it
        char[]       b  = new char[2];
        StringBuffer sb = new StringBuffer(x);
        for (int pos = 0; pos < sb.length(); pos++) {
            char c = sb.charAt(pos);
            if (c != '%') {
                continue;
            }
            b[0] = sb.charAt(pos + 1);
            b[1] = sb.charAt(pos + 2);
            int value;
            try {
              value = Integer.parseInt(new String(b), 16);
            } catch (NumberFormatException e) {
              continue;   // not a hex number
            }
            c = (char) value;
            sb.setCharAt(pos, c);
            sb.delete(pos + 1, pos + 3);
        }

        return sb.toString();
    }


    /////////////////////////////////////////////////////////////

    /**
     * Run through the List of Objects  and return the Object whose
     * toString matches the source string.  If none found then return
     * the dflt value.
     *
     * @param source      Source String to match on.
     * @param patternList List of objects whose toString is the pattern.
     * @param dflt        The default if nothing matches.
     * @return The Object whose toString matches the source or the
     *                    dflt if no matches found.
     */
    public static Object findMatch(String source, List patternList,
                                   Object dflt) {
        if (debug) {
            System.err.println("findMatch:" + source + ":");
        }
        //        debug = true;
        for (int i = 0; i < patternList.size(); i++) {
            Object object = patternList.get(i);
            if (object != null) {
                if (debug) {
                    System.err.println("\t:" + object.toString() + ":");
                }
                if (stringMatch(source, object.toString())) {
                    return object;
                }
            }
        }
        return dflt;
    }

    /**
     * Run through the List of patterns (pattern, result)  and return
     * corresponding result whose  pattern  matches the source string.
     * If none found then return the dflt.
     *
     * @param source        Source String to match on.
     * @param patternList   List of objects whose toString is the pattern.
     * @param results       The list of return objects.
     * @param dflt          The default if nothing matches.
     * @return The return Object whose toString matches the source or the dflt if no matches found.
     */
    public static Object findMatch(String source, List patternList,
                                   List results, Object dflt) {
        for (int i = 0; i < patternList.size(); i++) {
            if (stringMatch(source, patternList.get(i).toString())) {
                return results.get(i);
            }
        }
        return dflt;
    }


    /** locking object */
    private static final Object MATCH_MUTEX = new Object();

    /** a cache of patterns */
    private static Hashtable patternCache = new Hashtable();



    /**
     * See if a pattern string contains regular expression characters
     * (^,*,$,+).
     *
     * @param patternString   pattern string to check
     * @return  true if it contains (^,*,$,+).
     */
    public static boolean containsRegExp(String patternString) {
        return ((patternString.indexOf('^') >= 0)
                || (patternString.indexOf('*') >= 0)
                || (patternString.indexOf('|') >= 0)
                || (patternString.indexOf('(') >= 0)
                || (patternString.indexOf('$') >= 0)
                || (patternString.indexOf('?') >= 0)
                || (patternString.indexOf('.') >= 0)
                || ((patternString.indexOf('[') >= 0)
                    && (patternString.indexOf(']')
                        >= 0)) || (patternString.indexOf('+') >= 0));
    }



    /**
     * Check if the given input String  matches the given pattern String.
     * First see if input.equals (patternString). If true then return true.
     * Next if there are no regular expression characters
     * (look for ^, $, *, and +) in the patternString then return false.
     * Else treat the patternString as a regexp and return if it matches
     * the input.
     *
     * @param input           The input source string.
     * @param patternString   The regular expression pattern.
     * @return                true if the pattern match the input.
     */
    public static boolean stringMatch(String input, String patternString) {
        return stringMatch(input, patternString, false, true);

    }


    /**
     * Check if the given input String  matches the given pattern String.
     * First see if input.equals (patternString). If true then return true.
     * Next, if the pattern string is a simple "*" or begins with a "*"
     * or starts with the prefix "glob:" then is is a glob style pattern
     * and we convert it to a regexp.
     *
     * Next if there are no regular expression characters
     * (look for ^, $, *, and +) in the patternString then return false.
     * Else treat the patternString as a regexp and return if it matches
     * the input.
     *
     * @param input           The input source string.
     * @param patternString   The regular expression pattern.
     * @param substring Search for substrings
     * @param caseSensitive Is case sensitive
     * @return                true if the pattern match the input.
     */
    public static boolean stringMatch(String input, String patternString,
                                      boolean substring,
                                      boolean caseSensitive) {

        synchronized (MATCH_MUTEX) {
            try {
                //First try a straight String.equals
                if ( !caseSensitive) {
                    if (input.equalsIgnoreCase(patternString)) {
                        return true;
                    }
                } else {
                    if (input.equals(patternString)) {
                        return true;
                    }
                }

                if (substring) {
                    if ( !caseSensitive) {
                        if (input.toLowerCase().indexOf(
                                patternString.toLowerCase()) >= 0) {
                            return true;
                        }
                    } else {
                        if (input.indexOf(patternString) >= 0) {
                            return true;
                        }
                    }
                }
                //Next see if there are any regexp chars
                if (patternString.toLowerCase().indexOf("t_") >= 0) {
                    //                    System.err.println ("pattern:" + patternString + " " +StringUtil.containsRegExp(patternString));
                }
                //Simple check for  glob style
                if (patternString.startsWith("*")
                        || patternString.equals("*")) {
                    patternString = "." + patternString;
                } else if (patternString.startsWith("glob:")) {
                    patternString = patternString.substring("glob:".length());
                    patternString = wildcardToRegexp(patternString);
                    //                    System.err.println("   xxx:" + patternString+ " " +input);
                }

                if ( !StringUtil.containsRegExp(patternString)) {
                    return false;
                }
                Pattern pattern = (Pattern) patternCache.get(patternString);
                if (pattern == null) {
                    pattern = Pattern.compile(patternString);
                    patternCache.put(patternString, pattern);
                }
                return pattern.matcher(input).find();
            } catch (Exception exc) {
                System.err.println("Error regexpMatch:" + exc);
                exc.printStackTrace();
            }
            return true;
        }
    }

    /**
     * Match a regular expression
     *
     * @param input  string to match
     * @param patternString  reg ex pattern string
     *
     * @return  true if a match
     */
    public static boolean regexpMatch(String input, String patternString) {
        synchronized (MATCH_MUTEX) {
            Pattern pattern = (Pattern) patternCache.get(patternString);
            if (pattern == null) {
                pattern = Pattern.compile(patternString);
                patternCache.put(patternString, pattern);
            }
            return pattern.matcher(input).find();
        }
    }


    /**
     * Change a wildcard expression to a proper regular expression
     *
     * @param wildcard  wildcard string (*, ?);
     *
     * @return  the corresponding regular expression
     */
    public static String wildcardToRegexp(String wildcard) {
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {

              case '*' :
                  s.append(".*");
                  break;

              case '?' :
                  s.append(".");
                  break;

              // escape special regexp-characters
              case '(' :
              case ')' :
              case '[' :
              case ']' :
              case '$' :
              case '^' :
              case '.' :
              case '{' :
              case '}' :
              case '|' :
              case '\\' :
                  s.append("\\");
                  s.append(c);
                  break;

              default :
                  s.append(c);
                  break;
            }
        }
        s.append('$');
        return (s.toString());
    }



    /**
     * Check to see if the string starts with a vowel.
     *
     * @param  value  String to check
     * @return true if value starts with a, e, i, o, or u (but not sometimes y).
     *         Check is case insensitive.
     */
    public static boolean startsWithVowel(String value) {
        if ((value == null) || value.equals("")) {
            return false;
        }
        char lower = Character.toLowerCase(value.charAt(0));
        return (lower == 'a') || (lower == 'e') || (lower == 'i')
               || (lower == 'o') || (lower == 'u');
    }


    /**
     * Convert the given text to html by adding &lt;br&gt;.
     * If there are new lines then we replace them with a space.
     * Then we break the lines into 50
     * character (or so) chunks, adding br tags.
     *
     * @param text The text to convert
     * @param insert string to insert
     * @param lineSize line size to insert at
     *
     * @return The text with added br tags.
     */
    public static String breakText(String text, String insert, int lineSize) {
        text = StringUtil.replace(text, "\n", " ");
        StringBuffer buff = new StringBuffer();
        while (text.length() > 0) {
            int len = text.length();
            if (len < lineSize) {
                buff.append(text);
                break;
            }
            int idx = lineSize;
            while ((idx < len) && (text.charAt(idx) != ' ')) {
                idx++;
            }
            if (idx == len) {
                buff.append(text);
                break;
            }
            buff.append(text.substring(0, idx));
            buff.append(insert);
            text = text.substring(idx);
        }
        return buff.toString();
    }

    /**
     * Break the given text into lines, respecting word boundaries (blank space).
     *
     * @param text The text to convert
     * @param insert break to insert
     * @param lineSize line size to insert at
     *
     * @return The text with added br tags.
     */
    public static String breakTextAtWords(String text, String insert,
                                          int lineSize) {
        StringBuffer    buff      = new StringBuffer();
        StringTokenizer stoker    = new StringTokenizer(text);
        int             lineCount = 0;
        while (stoker.hasMoreTokens()) {
            String tok = stoker.nextToken();
            if (tok.length() + lineCount >= lineSize) {
                buff.append(insert);
                lineCount = 0;
            }
            buff.append(tok);
            buff.append(" ");
            lineCount += tok.length() + 1;
        }

        return buff.toString();
    }


    /**
     * Remove any beginning or ending &lt;html&gt; tags
     *
     * @param html the html
     * @return the stripped html
     */
    public static String stripHtmlTag(String html) {
        html = html.trim();
        if (html.startsWith("<html>")) {
            html = html.substring(6);
        }
        if (html.endsWith("</html>")) {
            html = html.substring(0, html.length() - 7);
        }
        return html;
    }


    /**
     *  Remove all text contained within "&lt; &gt;" tags.
     *
     *  @param html The source html string.
     *  @return The raw text.
     */
    public static String stripTags(String html) {
        StringBuffer stripped = new StringBuffer();
        while (html.length() > 0) {
            int idx = html.indexOf("<");
            if (idx < 0) {
                stripped.append(html.trim());
                break;
            }
            String text = html.substring(0, idx);
            text = text.trim();
            if (text.length() > 0) {
                stripped.append(text + " \n");
            }
            html = html.substring(idx);
            int idx2 = html.indexOf(">");
            if (idx2 < 0) {
                break;
            }
            html = html.substring(idx2 + 1);
        }

        stripped = new StringBuffer(replace(stripped.toString(), "&nbsp;",
                                            ""));
        return stripped.toString();
    }


    /**
     * Replaces all occurrences of the strings delimited by patter1/pattern2 with replace, e.g.: "pattern1 ... pattern2"
     *
     * @param s initial string
     * @param pattern1 delimiter 1
     * @param pattern2 delimiter 2
     * @param replace replace with
     *
     * @return replaced string
     */
    public static String stripAndReplace(String s, String pattern1,
                                         String pattern2, String replace) {
        StringBuffer stripped = new StringBuffer();
        while (s.length() > 0) {
            int idx = s.indexOf(pattern1);
            if (idx < 0) {
                stripped.append(s);
                break;
            }
            String text = s.substring(0, idx);
            if (text.length() > 0) {
                stripped.append(text);
            }
            s = s.substring(idx + 1);

            int idx2 = s.indexOf(pattern2);
            if (idx2 < 0) {
                break;
            }
            stripped.append(replace);
            s = s.substring(idx2 + 1);
        }
        stripped.append(s);
        return stripped.toString();
    }


    /**
     *  Remove any whitespace (ie., Character.isWhitespace) from the input string.
     *
     *  @param inputString The string to remove the whitespace.
     *  @return The whitespaceless result.
     */
    public static String removeWhitespace(String inputString) {
        StringBuffer sb    = new StringBuffer();
        char[]       chars = inputString.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isWhitespace(c)) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }



    /**
     * If the given value is less than 10 than pad the String return
     * with a leading "0".
     *
     * @param value The value.
     * @return The String  represenation of the value, padded with a
     *         leading "0" if value &lt; 10
     */
    public static String zeroString(int value) {
        return padZero(value, 2);
    }


    /**
     * Left pad the given value with zeros up to the number of digits
     *
     * @param value The value.
     * @param numDigits number of digits
     * @return The String  represenation of the value, padded with
     *         leading "0"-s if value &lt; 10E(numDigits-1)
     */
    public static String padZero(int value, int numDigits) {
        return padLeft(String.valueOf(value), numDigits, "0");
    }


    /**
     * Pad the given string with spaces on the left up to the given length.
     *
     * @param s               String to pad
     * @param desiredLength   ending length
     * @return  padded String
     */
    public static String padLeft(String s, int desiredLength) {
        return padLeft(s, desiredLength, " ");
    }


    /**
     * Pad the given string with padString on the left up to the given length.
     *
     * @param s               String to pad
     * @param desiredLength   ending length
     * @param padString       String to pad with (e.g, " ")
     * @return  padded String
     */
    public static String padLeft(String s, int desiredLength,
                                 String padString) {
        while (s.length() < desiredLength) {
            s = padString + s;
        }
        return s;
    }



    /**
     * Pad the given string with spaces on the right up to the given length.
     *
     * @param s               String to pad
     * @param desiredLength   ending length
     * @return  padded String
     */
    public static String padRight(String s, int desiredLength) {
        return padRight(s, desiredLength, " ");
    }


    /**
     * Pad the given string with padString on the right up to the given length.
     *
     * @param s               String to pad
     * @param desiredLength   ending length
     * @param padString       String to pad with (e.g, " ")
     * @return  padded String
     */
    public static String padRight(String s, int desiredLength,
                                  String padString) {
        while (s.length() < desiredLength) {
            s = s + padString;
        }
        return s;
    }


    /**
     *  Merge the given strings, using a space between each.
     *
     *  @param args An array of Strings to merge.
     *  @return The given strings concatenated together with a
     *          space between each.
     */
    public static String join(String[] args) {
        return join(" ", args);
    }

    /**
     *  Merge the given strings, using  the given delimiter between each.
     *
     *  @param delimiter The delimiter.
     *  @param args An array of Strings to merge.
     *  @return The given strings concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, Object[] args) {
        return join(delimiter, args, false);
    }


    /**
     *  Merge the given strings, using  the given delimiter between each.
     *
     *  @param delimiter The delimiter.
     *  @param args An array of Strings to merge.
     *  @param  ignoreEmptyStrings Don't join empty strings
     *  @return The given strings concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, Object[] args,
                              boolean ignoreEmptyStrings) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            if (ignoreEmptyStrings
                    && ((args[i] == null)
                        || (args[i].toString().length() == 0))) {
                continue;
            }
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(args[i].toString());
        }
        return sb.toString();
    }


    /**
     *  Merge the given strings, using  the given delimiter between each.
     *
     *  @param delimiter The delimiter.
     *  @param args A List of objects whose toString value are merged.
     *  @return The given object.toString values concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, List args) {
        return join(delimiter, listToStringArray(args));
    }


    /**
     *  Merge the given strings, using  the given delimiter between each.
     *
     *  @param delimiter The delimiter.
     *  @param args A List of objects whose toString value are merged.
     *  @param ignoreEmptyStrings Should ignore empty strings
     *  @return The given object.toString values concatenated together with the delimiter between each.
     */
    public static String join(String delimiter, List args,
                              boolean ignoreEmptyStrings) {
        return join(delimiter, listToStringArray(args), ignoreEmptyStrings);
    }





    /**
     * Tokenize the  toString value of the given source object,
     * splitting on ",".
     *
     * @param source The source object string.
     *
     * @return List of String tokens.
     */
    public static List<String> split(Object source) {
        return split(source, ",");
    }


    /**
     * Parse the date. Can be of a number of forms:
     * <pre>
     *  yyyy-MM-dd HH:mm:ss z yyyy-MM-dd HH:mm:ss   yyyy-MM-dd HH:mm
     *   yyyy-MM-dd yyyyMMddHHmmss  yyyyMMddHHmm
     *   yyyyMMddHH yyyyMMdd
     * </pre>
     *
     * @param dttm The date string
     *
     * @return The Date or null if not able to parse it
     */
    public static Date parseDate(String dttm) {
        String[] formats = {
            "yyyy-MM-dd HH:mm:ss z", "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "yyyyMMddHHmmss",
            "yyyyMMddHHmm", "yyyyMMddHH", "yyyyMMdd"
        };
        for (int i = 0; i < formats.length; i++) {
            SimpleDateFormat dateFormat =
                new java.text.SimpleDateFormat(formats[i]);
            dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
            Date date = dateFormat.parse(dttm, new ParsePosition(0));
            if (date != null) {
                return date;
            }
        }
        return null;
    }



    /**
     * Parse the given string and return a list of Integer values.
     * The String s is a comma separated list of integer values or
     * integer ranges of the form start:end:stride
     * where stride is optional.
     *
     * So s can be:
     * <pre>
     * 1,2,3   - the numbers 1 2 and 3
     * 0:10,15:20,30  - 0-10, 15-20 and 30
     * 0:10:2   - 0,2,4,6,8,10
     * </pre>
     *
     * @param s The string to parse
     * @return List of Integers
     */
    public static List parseIntegerListString(String s) {
        List items  = new ArrayList();
        List tokens = split(s, ",");
        for (int tokIdx = 0; tokIdx < tokens.size(); tokIdx++) {
            String token = (String) tokens.get(tokIdx);
            if (token.indexOf(":") >= 0) {
                int  stride    = 1;
                List subTokens = split(token, ":");
                if (subTokens.size() > 2) {
                    stride =
                        new Integer(subTokens.get(2).toString()).intValue();
                }
                int start =
                    new Integer(subTokens.get(0).toString()).intValue();
                int end = new Integer(subTokens.get(1).toString()).intValue();
                for (int i = start; i <= end; i += stride) {
                    items.add(new Integer(i));
                }
            } else {
                items.add(new Integer(token));
            }
        }
        return items;
    }



    /**
     *  This splits the given content String into a set of lines
     * (delimited by the given lineDelimiter).
     * If a line begins with the given commentString it is ignored.
     * If the length of the trim value of the line is 0 it is ignored.
     * If a line is not to be ignored then the substrings defined by the
     * given fromIndices/toIndices are extracted, placed into a String
     * array and added to the result List.
     *
     * @param content The String to  parse
     * @param lengths the length of each word.
     * @param lineDelimiter What to split  the line content string on
     *                      (usually "\n").
     * @param commentString If non-null defines the comment String in
     *                      the content.
     * @param trimWords Do we trim each word.
     *
     * @return A list of String arrays  that holds the words.
     */
    public static List<String[]> parseLineWords(String content,
            int[] lengths, String lineDelimiter, String commentString,
            boolean trimWords) {
        int[] indices = new int[lengths.length];
        int   length  = 0;
        for (int i = 0; i < indices.length; i++) {
            indices[i] = length;
            length     += lengths[i];
        }
        return parseLineWords(content, indices, lengths, lineDelimiter,
                              commentString, trimWords);
    }



    /**
     *  This splits the given content String into a set of lines
     * (delimited by the given lineDelimiter).
     * If a line begins with the given commentString it is ignored.
     * If the length of the trim value of the line is 0 it is ignored.
     * If a line is not to be ignored then the substrings defined by the
     * given fromIndices/toIndices are extracted, placed into a String
     * array and added to the result List.
     *
     * @param content The String to  parse
     * @param indices the index in the line which defines the word start.
     * @param lengths the length of each word.
     * @param lineDelimiter What to split  the line content string on (usually "\n").
     *  @param commentString If non-null defines the comment String in the content.
     *  @param trimWords Do we trim each word.
     *
     *  @return A list of String arrays  that holds the words.
     */
    public static List<String[]> parseLineWords(String content,
            int[] indices, int[] lengths, String lineDelimiter,
            String commentString, boolean trimWords) {
        List lines  = StringUtil.split(content, lineDelimiter, false);
        List result = new ArrayList();
        for (int i = 0; i < lines.size(); i++) {
            String line  = (String) lines.get(i);
            String tline = line.trim();
            if (tline.length() == 0) {
                continue;
            }
            if ((commentString != null) && tline.startsWith(commentString)) {
                continue;
            }
            String[] words = new String[indices.length];
            for (int idx = 0; idx < indices.length; idx++) {
                int endIndex = indices[idx] + lengths[idx];
                if (endIndex > line.length()) {
                    endIndex = line.length();
                }
                words[idx] = line.substring(indices[idx], endIndex);
                if (trimWords) {
                    words[idx] = words[idx].trim();
                }
            }
            result.add(words);
        }
        return result;
    }

  /**
   * Split a string on one or more whitespace.
   * Cover for STring.split, because who can remember regexp?
   * @param source split this string
   * @return space-seperated tokens
   */
    public static String[] split(String source) {
      return source.trim().split("\\s+"); // Separated by "whitespace"
    }


    /**
     * Tokenize the  toString value of the given source object,
     * splitting on the given delimiter.
     *
     * @param source     The source object string.
     * @param delimiter  The delimiter to break up the sourceString on.
     * @return List of String tokens.
     */
    public static List<String> split(Object source, String delimiter) {
        return split(source, delimiter, true);
    }

    /**
     * Tokenize the  toString value of the given source object, splitting on
     * the given delimiter. If trim is true the string trim each token.
     *
     * @param source     The source object string.
     * @param delimiter  The delimiter to break up the sourceString on.
     * @param trim       Do we string trim the tokens.
     *
     * @return List of String tokens.
     */
    public static List<String> split(Object source, String delimiter,
                                     boolean trim) {
        return split(source, delimiter, trim, false);
    }

    /**
     * tokenize the given string on spaces. Respect double quotes
     *
     * @param s The string to tokenize
     * @return the list of tokens
     */
    public static List<String> splitWithQuotes(String s) {
        ArrayList<String> list = new ArrayList();
        if (s == null) {
            return list;
        }
        //        System.err.println ("S:" + s);
        while (true) {
            s = s.trim();
            int qidx1 = s.indexOf("\"");
            int qidx2 = s.indexOf("\"", qidx1 + 1);
            int sidx1 = 0;
            int sidx2 = s.indexOf(" ", sidx1 + 1);
            if ((qidx1 < 0) && (sidx2 < 0)) {
                if (s.length() > 0) {
                    list.add(s);
                }
                break;
            }
            if ((qidx1 >= 0) && ((sidx2 == -1) || (qidx1 < sidx2))) {
                if (qidx1 >= qidx2) {
                    //Malformed string. Add the rest of the line and break
                    if (qidx1 == 0) {
                        s = s.substring(qidx1 + 1);
                    } else if (qidx1 > 0) {
                        s = s.substring(0, qidx1);
                    }
                    if (s.length() > 0) {
                        list.add(s);
                    }
                    break;
                }
                if (qidx2 < 0) {
                    //Malformed string. Add the rest of the line and break
                    s = s.substring(1);
                    list.add(s);
                    break;
                }
                String tok = s.substring(qidx1 + 1, qidx2);
                if (tok.length() > 0) {
                    list.add(tok);
                }
                s = s.substring(qidx2 + 1);
                //                System.err.println ("qtok:" + tok);
            } else {
                if (sidx2 < 0) {
                    list.add(s);
                    break;
                }
                String tok = s.substring(sidx1, sidx2);
                if (tok.length() > 0) {
                    list.add(tok);
                }
                s = s.substring(sidx2);
                //                System.err.println ("stok:" + tok);
            }
        }
        return list;
    }



    /**
     * Tokenize the toString value of the given source object, splitting
     * on the given delimiter. If trim is true the string trim each token.
     *
     * @param source             The source object string.
     * @param delimiter          The delimiter to break up the sourceString on.
     * @param trim               Do we string trim the tokens.
     * @param excludeZeroLength  If true then don't add in zero length strings.
     *
     * @return List of String tokens.
     */
    public static List<String> split(Object source, String delimiter,
                                     boolean trim,
                                     boolean excludeZeroLength) {
        ArrayList<String> list = new ArrayList();
        if (source == null) {
            return list;
        }
        String sourceString = source.toString();
        int    length       = delimiter.length();
        while (true) {
            int    idx = sourceString.indexOf(delimiter);
            String theString;
            if (idx < 0) {
                theString = sourceString;
            } else {
                theString    = sourceString.substring(0, idx);
                sourceString = sourceString.substring(idx + length);
            }
            if (trim) {
                theString = theString.trim();
            }
            if (excludeZeroLength && (theString.length() == 0)) {
                if (idx < 0) {
                    break;
                }
                continue;
            }
            list.add(theString);
            if (idx < 0) {
                break;
            }

        }
        return list;
    }


    /**
     * Split the given string into the first cnt number of substrings
     * as delimited by the given delimiter.
     *
     * @param s             String to split
     * @param delimiter     token delimeter
     * @param cnt           max number of tokens
     * @return array of strings or <code>null</code> if unable to split
     *         the string.
     */
    public static String[] split(String s, String delimiter, int cnt) {
        String[] a = new String[cnt];
        for (int i = 0; i < cnt - 1; i++) {
            int idx = s.indexOf(delimiter);
            if (idx < 0) {
                return null;
            }
            a[i] = s.substring(0, idx);
            s    = s.substring(idx + 1);
        }
        a[cnt - 1] = s;
        return a;
    }

    /**
     * Split up to a certain number of characters
     *
     * @param s   the string to split
     * @param delimiter  the delimiter
     * @param cnt the max number
     *
     * @return the list of split strings
     */
    public static List<String> splitUpTo(String s, String delimiter,
                                         int cnt) {
        List<String> toks = new ArrayList<String>();
        for (int i = 0; i < cnt - 1; i++) {
            int idx = s.indexOf(delimiter);
            if (idx < 0) {
                break;
            }
            toks.add(s.substring(0, idx));
            s = s.substring(idx + 1).trim();
        }
        if (s.length() > 0) {
            toks.add(s);
        }
        return toks;
    }


    /**
     * Replace the macro within s with the formatted date.
     * s can contain macros of the form ${macroName:some date format}
     *
     * @param s source string
     * @param macroName macro name_
     * @param date date to use
     *
     * @return formatted string
     */
    public static String replaceDate(String s, String macroName, Date date) {
        return replaceDate(s, macroName, date, "${", "}");
    }


    /**
     * Replace the macro within s with the formatted date.
     * s can contain macros of the form <macroPrefix>macroName:some date format<macroSuffix>
     *
     * @param s source string
     * @param macroName macro name_
     * @param date date to use
     * @param macroPrefix  the macro prefix
     * @param macroSuffix  the macro suffix
     *
     * @return formatted string
     */
    public static String replaceDate(String s, String macroName, Date date,
                                     String macroPrefix, String macroSuffix) {

        while(true) {
            int idx1 = s.indexOf(macroPrefix + macroName);
            if (idx1 < 0) {
                return s;
            }

            int idx2 = s.indexOf(macroSuffix, idx1);
            if (idx2 < 0) {
                return s;
            }

            String   fullMacro = s.substring(idx1 + macroPrefix.length(), idx2);
            String[] toks      = StringUtil.split(fullMacro, ":", 2);

            if ((toks == null) || (toks.length != 2)) {
                throw new IllegalArgumentException("Could not find date format:"
                                                   + s);
            }
            SimpleDateFormat sdf = new SimpleDateFormat(toks[1]);
            sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
            String formattedDate = sdf.format(date);
            s = s.replace(macroPrefix + fullMacro + macroSuffix, formattedDate);
            //        System.err.println(s);
        }
        //        return s;
    }



    /**
     *  Take the List of objects and return a String array
     *  of the toString values of each object in the list.
     *
     *  @param  objectList The list of objects.
     *  @return The array of the object string values.
     */
    public static String[] listToStringArray(List objectList) {
        String[] sa = new String[objectList.size()];
        for (int i = 0; i < objectList.size(); i++) {
            Object o = objectList.get(i);
            if (o != null) {
                sa[i] = o.toString();
            }
        }
        return sa;
    }


    /**
     * Take the List of objects and return a String of all the
     * list's elements toString values appended to each other,
     * separated by semicolons
     *
     * @param l   list of objects
     * @return  semicolon separated String of Strings.
     */
    public static String listToString(List l) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                sb.append(";");
            }
            sb.append(l.get(i).toString());
        }

        return sb.toString();
    }

    /**
     * Convert the list of objects to a list of strings.
     *
     * @param l List of objects
     * @return List of strings.
     */
    public static List toString(List l) {
        List stringList = new ArrayList();
        for (int i = 0; i < l.size(); i++) {
            stringList.add(l.get(i).toString());
        }
        return stringList;
    }


    /**
     *  Replaces all occurrences of "pattern" in "string" with "value"
     *
     * @param string   string to munge
     * @param pattern  pattern to replace
     * @param value    replacement value
     * @return  munged string
     */
    public static String replace(String string, String pattern,
                                 String value) {
        if (pattern.length() == 0) {
            return string;
        }
        StringBuffer returnValue   = new StringBuffer();
        int          patternLength = pattern.length();
        while (true) {
            int idx = string.indexOf(pattern);
            if (idx < 0) {
                break;
            }
            returnValue.append(string.substring(0, idx));
            if (value != null) {
                returnValue.append(value);
            }
            string = string.substring(idx + patternLength);
        }
        returnValue.append(string);
        return returnValue.toString();
    }

    /**
     * Replaces all occurrences of "patterns" in "v" with "values"
     *
     * @param v            original String
     * @param patterns     patterns to match
     * @param values       replacement values
     * @return  munged String
     */
    public static String replaceList(String v, String[] patterns,
                                     String[] values) {
        for (int i = 0; i < patterns.length; i++) {
            v = replace(v, patterns[i], values[i]);
        }
        return v;
    }


    /**
     * Replaces all occurrences of "patterns" in "v" with "values"
     *
     * @param v            original String
     * @param patterns     patterns to match
     * @param values       replacement values
     * @return  munged String
     */
    public static String replaceList(String v, List patterns, List values) {
        if (patterns.size() != values.size()) {
            throw new IllegalArgumentException(
                "Patterns list not the same size as values list");
        }
        for (int i = 0; i < patterns.size(); i++) {
            v = replace(v, (String) patterns.get(i), (String) values.get(i));
        }
        return v;
    }


    /**
     * Construct and return a list of Strings where each string is the result
     * of replacing all of the patterns with the corresponding values for
     * each String in the given sourceList .
     *
     * @param sourceList   original list of Strings
     * @param patterns     patterns to replace
     * @param values       replacement values
     * @return  new list with replaced values
     */
    public static List replaceList(List sourceList, String[] patterns,
                                   String[] values) {
        List result = new ArrayList();
        for (int i = 0; i < sourceList.size(); i++) {
            String str = (String) sourceList.get(i);
            for (int patternIdx = 0; patternIdx < patterns.length;
                    patternIdx++) {
                if (patterns[patternIdx] != null) {
                    str = StringUtil.replace(str, patterns[patternIdx],
                                             values[patternIdx]);
                }
            }
            result.add(str);
        }
        return result;
    }


    /**
     * A utility method to an append to a StringBuffer.
     * If the given object is null the string "null" will be appended. If
     * non-null the we append to the StringBuffer the results of s1.toString ();
     *
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  object to append
     * @return  StringBuffer with appended object
     */
    public static StringBuffer append(StringBuffer sb, Object s1) {
        if (sb == null) {
            sb = new StringBuffer();
        }
        sb.append((s1 == null)
                  ? "null"
                  : s1.toString());
        return sb;
    }


    /**
     * A utility method to do multiple appends to a StringBuffer.
     * If the given object is null the string "null" will be appended. If
     * non-null then we append to the StringBuffer the results of
     * sn.toString ();
     *
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @return  StringBuffer with appended objects
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2) {
        sb = append(sb, s1);
        sb.append((s2 == null)
                  ? "null"
                  : s2.toString());
        return sb;
    }

    /**
     * A utility method to do multiple appends to a StringBuffer.
     * If the given object is null the string "null" will be appended. If
     * non-null then we append to the StringBuffer the results of
     * sn.toString ();
     *
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @param s3  third object to append
     * @return  StringBuffer with appended objects
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2,
                                      Object s3) {
        sb = append(sb, s1, s2);
        sb.append((s3 == null)
                  ? "null"
                  : s3.toString());
        return sb;
    }

    /**
     * A utility method to do multiple appends to a StringBuffer.
     * If the given object is null the string "null" will be appended. If
     * non-null then we append to the StringBuffer the results of
     * sn.toString ();
     *
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @param s3  third object to append
     * @param s4  fourth object to append
     * @return  StringBuffer with appended objects
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2,
                                      Object s3, Object s4) {
        sb = append(sb, s1, s2, s3);
        sb.append((s4 == null)
                  ? "null"
                  : s4.toString());
        return sb;
    }

    /**
     * A utility method to do multiple appends to a StringBuffer.
     * If the given object is null the string "null" will be appended. If
     * non-null then we append to the StringBuffer the results of
     * sn.toString ();
     *
     * @param sb  StringBuffer to append to (may be <code>null</code>)
     * @param s1  first object to append
     * @param s2  second object to append
     * @param s3  third object to append
     * @param s4  fourth object to append
     * @param s5  fifth object to append
     * @return  StringBuffer with appended objects
     */
    public static StringBuffer append(StringBuffer sb, Object s1, Object s2,
                                      Object s3, Object s4, Object s5) {
        sb = append(sb, s1, s2, s3, s4);
        sb.append((s5 == null)
                  ? "null"
                  : s5.toString());
        return sb;
    }


    /**
     * Parse a comma separated value (CVS) String
     *
     * @param s             String to parse
     * @param skipFirst     true to skip the first value
     * @return   list of parsed Strings
     */
    public static List parseCsv(String s, boolean skipFirst) {
        //Normalize the string
        s = s.trim();
        s = s + "\n";
        s = replaceList(s, new String[] { "\"\"\",", ",\"\"\"", "\"\"" },
                        new String[] { "_QUOTE_\",",
                                       ",\"_QUOTE_", "_QUOTE_" });

        int       cnt     = s.length();


        ArrayList lines   = new ArrayList();
        ArrayList line    = new ArrayList();
        String    word    = "";
        final int INWORD  = 0;
        final int INQUOTE = 1;
        final int LOOKING = 2;
        int       state   = INWORD;
        for (int i = 0; i < cnt; i++) {
            char c = s.charAt(i);
            switch (state) {

              case LOOKING :
                  if (c == ',') {
                      state = INWORD;
                  } else if (c == '\n') {
                      if (line.size() > 0) {
                          if ( !skipFirst) {
                              if ( !line.get(0).toString().startsWith("#")) {
                                  lines.add(line);
                              }
                          }
                          line = new ArrayList();
                          word = "";
                      }
                      state = INWORD;
                  }

                  break;

              case INWORD :
                  if (c == '\"') {
                      state = INQUOTE;
                  } else if (c == ',') {
                      line.add(replace(word, "_QUOTE_", "\""));
                      word = "";
                  } else if (c == '\n') {
                      line.add(replace(word, "_QUOTE_", "\""));
                      word = "";
                      if ( !skipFirst || (lines.size() > 1)) {
                          if ( !line.get(0).toString().startsWith("#")) {
                              lines.add(line);
                          }
                      }
                      skipFirst = false;
                      line      = new ArrayList();
                  } else {
                      word = word + c;
                  }
                  break;

              case INQUOTE :
                  if (c == '\"') {
                      line.add(replace(word, "_QUOTE_", "\""));
                      word  = "";
                      state = LOOKING;
                  } else {
                      word = word + c;
                  }
                  break;
            }
        }

        if ((line.size() > 0) || (word.length() > 0)) {
            if (word.length() > 0) {
                line.add(replace(word, "_QUOTE_", "\""));
            }
            if ( !skipFirst || (lines.size() > 1)) {
                if ( !line.get(0).toString().startsWith("#")) {
                    lines.add(line);
                }
                //                System.err.println ("adding last:" + line);
            }
        }
        return lines;
    }



    /**
     * Shorten a string using elipses (...)
     *
     * @param s      String to shorten
     * @param length shortened length where elipses will start
     *
     * @return shortened string.
     */
    public static final String shorten(String s, int length) {
        if (s.length() > length) {
            s = s.substring(0, length - 1) + "...";
        }
        return s;
    }




    /**
     * Create a string representation of the given array
     *
     * @param array  array to print
     *
     * @return  array as a String
     */
    public static String toString(Object[] array) {
        StringBuffer buf = new StringBuffer();
        buf.append(": ");
        for (int i = 0; i < array.length; i++) {
            buf.append("[");
            buf.append(i);
            buf.append("]: ");
            buf.append((array[i] == null)
                       ? "null"
                       : array[i]);
            buf.append(" ");
        }
        return buf.toString();
    }



    /**
     * A first attempt at parsing a dttm range string
     *
     * @param time The time
     *
     * @return List iof times
     */
    public static List expandIso8601(String time) {
        List times  = new ArrayList();
        List tokens = StringUtil.split(time, ",", true, true);
        for (int i = 0; i < tokens.size(); i++) {
            String tok = (String) tokens.get(i);
            //Look for range
            if (tok.indexOf("/") < 0) {
                times.add(parseIso8601(tok));
                continue;
            }
            List ranges = split(tok, "/", true, true);
            if (ranges.size() != 3) {
                throw new IllegalArgumentException("Invalid date format:"
                        + tok);
            }
            Date min  = parseIso8601((String) ranges.get(0));
            Date max  = parseIso8601((String) ranges.get(1));
            long step = parseTimePeriod((String) ranges.get(2));
        }
        return times;
    }



    /**
     * If s1 is null return dflt. Else return s1
     *
     * @param s1 string
     * @param dflt default
     *
     * @return s1 or dflt
     */
    private static String str(String s1, String dflt) {
        return ((s1 != null)
                ? s1
                : dflt);
    }


    /**
     * Find the pattern in a string
     *
     * @param source  String to search
     * @param patternString  pattern
     *
     * @return the String that matches
     */
    public static String findPattern(String source, String patternString) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(source);
        boolean ok      = matcher.find();
        if ( !ok) {
            return null;
        }
        if (matcher.groupCount() > 0) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Is the string all upper case?
     *
     * @param s  string to check
     *
     * @return true if all uppercase
     */
    public static boolean isUpperCase(String s) {
        return s.toUpperCase().equals(s);
    }

    /**
     * Is the string all lower case?
     *
     * @param s  string to check
     *
     * @return true if all characters are lowercase
     */
    public static boolean isLowerCase(String s) {
        return s.toLowerCase().equals(s);
    }


    /**
     * Camel case a string (eg howard -&gt; Howard)
     *
     * @param s  string to camel case
     *
     * @return the camel cased string
     */
    public static String camelCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }


    /**
     * Not working yet but this is supposed to parse an iso8601 date format
     *
     * @param time date
     *
     * @return date
     */
    public static Date parseIso8601(String time) {
        String tmp =
            "((\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d))?(T(\\d\\d):(\\d\\d)(:(\\d\\d)?)?(.*)?)?";
        Pattern pattern = Pattern.compile(tmp);
        Matcher matcher = pattern.matcher(time);
        boolean ok      = matcher.find();
        if ( !ok) {
            System.err.println("No match:" + time);
            return null;
        }
        System.err.println("Time:" + time);
        for (int i = 1; i <= matcher.groupCount(); i++) {
            //      System.err.println("\t"+matcher.group(i));
        }
        int gidx = 1;
        gidx++;
        String year  = str(matcher.group(gidx++), "0000");
        String month = str(matcher.group(gidx++), "01");
        String day   = str(matcher.group(gidx++), "01");
        gidx++;
        String hh = str(matcher.group(gidx++), "00");
        String mm = str(matcher.group(gidx++), "00");
        gidx++;
        String ss  = str(matcher.group(gidx++), "00");
        String tzd = str(matcher.group(gidx++), "GMT");
        if (tzd.equals("Z") || (tzd.length() == 0)) {
            tzd = "GMT";
        }

        //      System.err.println(year+"-"+month+"-"+day+"T"+hh+":"+mm+":"+ss+tzd);
        String           format = "yyyy-MM-dd-HH-mm-ss-Z";
        SimpleDateFormat sdf    = new SimpleDateFormat();
        sdf.applyPattern(format);
        String dateString = year + "-" + month + "-" + day + "-" + hh + "-"
                            + mm + "-" + ss + "-" + tzd;
        try {
            Date dttm = sdf.parse(dateString);
            return dttm;
        } catch (Exception exc) {
            System.err.println("exc:" + exc);
            //      System.err.println(dateString);
            return null;
        }
    }

    /**
     * Note, right now this just parses the hour/minute/second periods
     *
     *
     * @param s The date string
     *
     * @return The time
     */
    public static long parseTimePeriod(String s) {
        if ( !s.startsWith("P")) {
            throw new IllegalArgumentException("Unknown time period:" + s);
        }
        s = s.substring(1);
        String tmp =
            "((\\d+)Y)?((\\d+)M)?((\\d+)D)?(T((\\d+)H)?((\\d+)M)?((\\d+)S)?)";
        Pattern pattern = Pattern.compile(tmp);
        Matcher matcher = pattern.matcher(s);
        boolean ok      = matcher.find();
        if ( !ok) {
            System.err.println("No match:" + s);
            return 0;
        }
        System.err.println("Duration:" + s);
        for (int i = 1; i <= matcher.groupCount(); i++) {
            System.err.println("\t" + matcher.group(i));
        }

        int gidx = 1;
        gidx++;
        long y = convert(str(matcher.group(gidx++), "0"));
        gidx++;
        long m = convert(str(matcher.group(gidx++), "0"));
        gidx++;
        long d = convert(str(matcher.group(gidx++), "0"));
        gidx++;
        gidx++;
        long h = convert(str(matcher.group(gidx++), "0"));
        gidx++;
        long min = convert(str(matcher.group(gidx++), "0"));
        gidx++;
        long sec = convert(str(matcher.group(gidx++), "0"));
        //      System.err.println ("y:" + y + "/" + m+"/"+d+"/" +h+"/"+min+"/"+sec);
        return h * 3600 + m * 60 + sec;
    }

    /**
     * Convert to a long
     *
     * @param s string
     *
     * @return long
     */
    private static long convert(String s) {
        return new Long(s).longValue();
    }



    /**
     * test
     *
     * @param args args
     */
    public static void main2(String args[]) {
        for (int i = 0; i < args.length; i++) {
            System.err.println(parseIntegerListString(args[i]));
        }


        /*
        System.err.println("dttm:" +    parseIso8601("2003-06-20T02:44MST"));
        System.err.println("dttm:" +    parseIso8601("2003-06-20T02:44"));
        System.err.println("dttm:" +parseIso8601("2003-06-20T02:44:33MST"));
        System.err.println("dttm:" +parseIso8601("2003-06-20"));
        System.err.println("dttm:" +parseIso8601("T02:44:33MST"));

        System.err.println(
            expandIso8601("2003-06-20T02:44Z,2003-06-22T00:56Z"));
        System.err.println(
            expandIso8601("2003-06-20T02:44Z/2003-06-22T00:56Z/PT1H39M"));

         * testEscape("abcdef", "()");
         * testEscape("abc(*)ef", "()");
         * testEscape("abc(*)ef", "*");
         * testEscape("(abc)", "*");
         * testEscape("\\n", "");
         * testEscape("ok with me", "");
         * testEscape("_dods-\"Name\" (not so good)", "_!~*'-\"");
         */
    }


    /**
     * Parse the semi-colon delimited string of name=value properties.
     *
     * @param s Semi-colon delimited name=value string
     *
     * @return properties
     */
    public static Hashtable parsePropertiesString(String s) {
        Hashtable properties = new Hashtable();
        if (s != null) {
            StringTokenizer tok = new StringTokenizer(s, ";");
            while (tok.hasMoreTokens()) {
                String nameValue = tok.nextToken();
                int    idx       = nameValue.indexOf("=");
                if (idx < 0) {
                    continue;
                }
                properties.put(nameValue.substring(0, idx).trim(),
                               nameValue.substring(idx + 1));
            }
        }
        return properties;
    }


    /**
     * Parse HTML Properties
     *
     * @param s  the string
     *
     * @return a hashtable of properties
     */
    public static Hashtable parseHtmlProperties(String s) {
        //        boolean debug = true;
        boolean   debug      = false;
        Hashtable properties = new Hashtable();
        //        debug = true;
        if (debug) {
            System.err.println("Source:" + s);
        }

        while (true) {
            if (debug) {
                System.err.println("S:" + s);
            }
            int idx = s.indexOf("=");
            if (idx < 0) {
                s = s.trim();
                if (s.length() > 0) {
                    if (debug) {
                        System.err.println("\tsingle name:" + s + ":");
                    }
                    properties.put(s, "");
                }
                break;
            }
            String name = s.substring(0, idx).trim();
            s = s.substring(idx + 1).trim();
            if (s.length() == 0) {
                if (debug) {
                    System.err.println("\tsingle name=" + name);
                }
                properties.put(name, "");
                break;
            }
            if (s.charAt(0) == '\"') {
                s   = s.substring(1);
                idx = s.indexOf("\"");
                if (idx < 0) {
                    //no closing "="
                    properties.put(name, s);
                    break;
                }
                String value = s.substring(0, idx);
                if (debug) {
                    System.err.println("\tname=" + name);
                }
                if (debug) {
                    System.err.println("\tvalue=" + value);
                }
                properties.put(name, value);
                s = s.substring(idx + 1);
            } else {
                idx = s.indexOf(" ");
                if (idx < 0) {
                    if (debug) {
                        System.err.println("\tname=" + name);
                    }
                    if (debug) {
                        System.err.println("\tvalue=" + s);
                    }
                    properties.put(name, s);
                    break;
                }
                String value = s.substring(0, idx);
                properties.put(name, value);
                if (debug) {
                    System.err.println("\tname=" + name);
                }
                if (debug) {
                    System.err.println("\tvalue=" + value);
                }
                s = s.substring(idx + 1);

            }

        }
        if (debug) {
            System.err.println("props:" + properties);
        }



        return properties;
    }



    /**
     * usage for test code
     */
    private static void showUsage() {
        System.out.println(" StringUtil escape <string> [okChars]");
        System.out.println(" StringUtil unescape <string>");
    }

    /**
     * Method for debugging.
     *
     * @param args  arguments
     *
     * @throws Exception some problem
     */
    public static void main(String[] args) throws Exception {
        System.err.println(
            splitWithQuotes(
                " single  again \"hello there\" another couple of toks  \"how are you\" I am fine \"and you"));

        System.err.println(splitWithQuotes("text1 text2"));
        System.err.println(splitWithQuotes("hello"));
        System.err.println(splitWithQuotes("\"hello"));
        System.err.println(splitWithQuotes("\"hello\""));
        System.err.println(splitWithQuotes("hello\""));
        if (true) {
            return;
        }


        args = new String[] { "*", "glob:fo*o", "glob:*fo*o*", "x.*" };
        for (int i = 0; i < args.length; i++) {
            System.err.println("pattern:" + args[i]);
            System.err.println("   "
                               + stringMatch("foobar", args[i], false, true));
        }
    }



    /**
     * test
     *
     * @param args args
     */
    public static void main3(String args[]) {
        if (args.length < 2) {
            showUsage();
            return;
        }

        if (args[0].equalsIgnoreCase("escape")) {
            String ok = (args.length > 2)
                        ? args[2]
                        : "";
            System.out.println(" escape(" + args[1] + "," + ok + ")= "
                               + escape(args[1], ok));
        } else if (args[0].equalsIgnoreCase("unescape")) {
            System.out.println(" unescape(" + args[1] + ")= "
                               + unescape(args[1]));
        } else {
            showUsage();
        }
    }


    /**
     * This parses the given string with the following  form.
     * <pre>some text ${macro1} more text ... ${macro2} ... ${macroN} end text</pre>
     *  It returns a list that flip-flops between the text and the macro:<pre>
     *  [some text, macro1, more text, ..., macro2, ..., macroN, end text]
     * </pre>
     * @param s String to parse
     *
     * @return List of tokens
     */
    public static List<String> splitMacros(String s) {
        List<String> tokens = new ArrayList<String>();
        int          idx1   = s.indexOf("${");
        while (idx1 >= 0) {
            int idx2 = s.indexOf("}", idx1);
            if (idx2 < 0) {
                break;
            }
            tokens.add(s.substring(0, idx1));
            tokens.add(s.substring(idx1 + 2, idx2));
            s    = s.substring(idx2 + 1);
            idx1 = s.indexOf("${");
        }
        if (s.length() > 0) {
            tokens.add(s);
        }
        return tokens;
    }

    /**
     * This takes a string of the following form.
     * <pre>some text ${macro1} more text ... ${macro2} ... ${macroN} end text</pre>
     * And replaces the macros with values from the given properties table
     * If throwError is true then an IllegalArgumentException is thrown
     * if the properties does not contain one of the macros
     *
     * @param s String to process
     * @param props Contains the macro values
     * @param throwError Throw exception when macro is missing
     *
     * @return Processed string
     */
    public static String applyMacros(String s, Hashtable props,
                                     boolean throwError) {
        List         toks      = StringUtil.splitMacros(s);
        StringBuffer sb        = new StringBuffer("");
        boolean      nextMacro = false;
        for (int i = 0; i < toks.size(); i++) {
            String tok = (String) toks.get(i);
            if (nextMacro) {
                Object obj   = props.get(tok);
                String value = ((obj != null)
                                ? obj.toString()
                                : null);
                if (value == null) {
                    if (throwError) {
                        throw new IllegalArgumentException(
                            "Undefined macro: ${" + tok + "} in:" + s);
                    }
                    sb.append("${" + tok + "}");
                } else {
                    sb.append(value);
                }
            } else {
                sb.append(tok);
            }
            nextMacro = !nextMacro;
        }
        return sb.toString();
    }




    /**
     * Parse the lat/lon/alt coordinate string
     *
     * @param coords comma and space separated coord string
     *
     * @return coords
     */
    public static double[][] parseCoordinates(String coords) {
        coords = StringUtil.replace(coords, "\n", " ");
        while (true) {
            String newCoords = StringUtil.replace(coords, " ,", ",");
            if (newCoords.equals(coords)) {
                break;
            }
            coords = newCoords;
        }
        while (true) {
            String newCoords = StringUtil.replace(coords, ", ", ",");
            if (newCoords.equals(coords)) {
                break;
            }
            coords = newCoords;
        }


        List       tokens = StringUtil.split(coords, " ", true, true);
        double[][] result = null;
        for (int pointIdx = 0; pointIdx < tokens.size(); pointIdx++) {
            String tok     = (String) tokens.get(pointIdx);
            List   numbers = StringUtil.split(tok, ",");
            if ((numbers.size() != 2) && (numbers.size() != 3)) {
                //Maybe its just comma separated
                if ((numbers.size() > 3) && (tokens.size() == 1)
                        && ((int) numbers.size() / 3) * 3 == numbers.size()) {
                    result = new double[3][numbers.size() / 3];
                    int cnt = 0;
                    for (int i = 0; i < numbers.size(); i += 3) {
                        result[0][cnt] = new Double(
                            numbers.get(i).toString()).doubleValue();
                        result[1][cnt] = new Double(numbers.get(i
                                + 1).toString()).doubleValue();
                        result[2][cnt] = new Double(numbers.get(i
                                + 2).toString()).doubleValue();
                        cnt++;
                    }
                    return result;
                }
                throw new IllegalStateException(
                    "Bad number of coordinate values:" + numbers);
            }
            if (result == null) {
                result = new double[numbers.size()][tokens.size()];
            }
            for (int coordIdx = 0;
                    (coordIdx < numbers.size()) && (coordIdx < 3);
                    coordIdx++) {
                result[coordIdx][pointIdx] = new Double(
                    numbers.get(coordIdx).toString()).doubleValue();
            }
        }
        return result;
    }


    /**
     * Get "a" or "an" for prefixing to a string based on the first
     * character
     *
     * @param subject subject to prefix
     *
     * @return "an" for vowels, "a" for consonants
     */
    public static String getAnOrA(String subject) {
        String s = subject.toLowerCase();
        if (s.startsWith("a") || s.startsWith("e") || s.startsWith("o")
                || s.startsWith("i") || s.startsWith("u")) {
            return "an";
        }
        return "a";
    }









}


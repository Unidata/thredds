/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////



package opendap.util;

/**
 * User: ndp
 * Date: Jul 7, 2006
 * Time: 10:23:19 AM
 */
public class EscapeStrings {


    /**
     * This method is used to normalize strings prior
     * to their inclusion in XML documents. XML has certain parsing requirements
     * around reserved characters. These reserved characters must be replaced with
     * symbols recognized by the XML parser as place holder for the actual symbol.
     * <p/>
     * The rule for this normalization is as follows:
     * <p/>
     * <ul>
     * <li> The &lt; (less than) character is replaced with &amp;lt;
     * <li> The &gt; (greater than) character is replaced with &amp;gt;
     * <li> The &amp; (ampersand) character is replaced with &amp;amp;
     * <li> The ' (apostrophe) character is replaced with &amp;apos;
     * <li> The &quot; (double quote) character is replaced with &amp;quot;
     * </ul>
     *
     * @param s The String to be normalized.
     * @return The normalized String.
     */
    public static String normalizeToXML(String s) {

        // Some handy definitons.
        String xmlGT = "&gt;";
        String xmlLT = "&lt;";
        String xmlAmp = "&amp;";
        String xmlApos = "&apos;";
        String xmlQuote = "&quot;";

        StringBuffer sb = new StringBuffer(s);

        for (int offset = 0; offset < sb.length(); offset++) {

            char c = sb.charAt(offset);

            switch (c) {

                case '>': // GreaterThan
                    sb.replace(offset, offset + 1, xmlGT);
                    break;

                case '<': // Less Than
                    sb.replace(offset, offset + 1, xmlLT);
                    break;

                case '&': // Ampersand
                    sb.replace(offset, offset + 1, xmlAmp);
                    break;

                case '\'': // Single Quote
                    sb.replace(offset, offset + 1, xmlApos);
                    break;

                case '\"': // Double Quote
                    sb.replace(offset, offset + 1, xmlQuote);
                    break;

                default:
                    break;
            }

        }

        return (sb.toString());

    }


    // May need to include/exclude the escape character!
    private static String _allowableInURI = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+_/.\\*";
    private static String _allowableInURI_CE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+_/.\\,";
    private static char _URIEscape = '%';

    /**
     * Replace characters that are not allowed in WWW URLs using rules specific
     * to Constraint Expressions. This has canged over time and now the only
     * differences are:
     * <ui>
     * <li>'*' is escaped by this function while it is not
     * escaped by id2www().</li>
     * <li> ',' is not escaped by this function and it is by id2www</li>
     * </ui>
     * The set of characters that are allowed in a CE are:
     * "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+_/.\";
     * All other characters will replaced with their hexidecimal value preceeded by
     * the "%" character. Thus a space, " ", character will be represented in the
     * returned string as "%20".
     *
     * @param in The string in which to replace characters.
     * @return The modified identifier.
     */
    public static String id2www_ce(String in) {
        String s;

        try {
            s = escapeString(in, _allowableInURI_CE, _URIEscape);
        }
        catch (Exception e) {
            s = null;

        }
        return s;
    }


    /**
     * Replace all characters in the String <code>in</code> not present in the String <code>allowable</code> with
     * their hexidecimal values (encoded as ASCII) and preceeded by the String <code>esc</code>
     * <p/>
     * The <cods>esc</code> character may not appear on the allowable list, as if it did it would break the 1:1
     * and onto mapping between the unescaped character space and the escaped characater space.
     *
     * @param in        The string in which to replace characters.
     * @param allowable The set of allowable characters.
     * @param esc       The escape String (typically "%" for a URI or "\" for a regular expression).
     * @return The modified identifier.
     */
    public static String escapeString(String in, String allowable, char esc) throws Exception {
        String out = "";

        if (in == null) return null;

        if (allowable.indexOf(esc) >= 0) {//isEscAllowed(allowable, esc)) 
            throw new Exception("Escape character MAY NOT be in the list of allowed characters!");
        }

        char[] inca = in.toCharArray();
        String c;

        boolean isAllowed;
        for (char candidate : inca) {
            isAllowed = allowable.indexOf(candidate) >= 0;
            if (isAllowed) {
                out += candidate;
            } else {
                c = Integer.toHexString(candidate);
                if (c.length() < 2)
                    c = "0" + c;
                out += esc + c;
            }

        }

        return out;

    }

    /**
     * Replace characters that are not allowed in DAP2 identifiers.
     * The set of characters that are allowed in a URI are:
     * "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+_/.\*";
     * All other characters will replaced with their hexidecimal value preceeded by
     * the "%" character. Thus a space, " ", character will be represented in the
     * returned string as "%20".
     *
     * @param in The string in which to replace characters.
     * @return The modified identifier.
     */
    public static String id2www(String in) {
        String s;

        try {
            s = escapeString(in, _allowableInURI, _URIEscape);
        }
        catch (Exception e) {
            s = null;

        }
        return s;
    }


    /**
     * Given a string that contains WWW escape sequences, translate those escape
     * sequences back into ASCII characters. Return the modified string.
     *
     * @param in     The string to modify.
     * @param escape The character used to signal the begining of an escape sequence.
     * @param except If there is some escape code that should not be removed by
     *               this call (e.g., you might not want to remove spaces, %20) use this
     *               parameter to specify that code. The function will then transform all
     *               escapes except that one.
     * @return The modified string.
     */
    public static String unescapeString(String in, char escape, String except) {
        if (in == null) return null;

        String esc = String.valueOf(escape);
        String out = in, replacement;
        int i = 0;
        while ((i = out.indexOf(esc, i)) != -1) {

            String candidate = out.substring(i, i + 3);

            if (candidate.equals(except)) {
                i += 3;

            } else {
                //out = out.substring(0,i) + " + [esc]" + out.substring(i+1,i+3) + " + " + out.substring(i+3,out.length());

                replacement = Character.toString((char) Integer.valueOf(out.substring(i + 1, i + 3), 16).intValue());

                out = out.substring(0, i) +
                        replacement +
                        out.substring(i + 3, out.length());

                if (replacement.equals(esc))
                    i++;

            }
        }

        return out;

    }


    /**
     * Given a string that contains WWW escape sequences, translate those escape
     * sequences back into ASCII characters. Escape sequences are indicted by a
     * leading "%" character followed by 2 characters indicating the hexidecimal
     * value of the character that was escaped.
     *
     * @param in The string to modify.
     * @return The modified string.
     */
    public static String www2id(String in) {

        return unescapeString(in, _URIEscape, "");

    }


    /**
     * Given a string that contains WWW escape sequences, translate those escape
     * sequences back into ASCII characters, with the exception of the escaped
     * space (0x20) character which appears as "%20". THe Constraint Expression
     * Parser will break if there are spaces in the CE. Escape sequences are
     * indicted by a leading "%" character followed by 2 characters indicating
     * the hexidecimal value of the character that was escaped.
     *
     * @param in The string to modify.
     * @return The modified string.
     */
    public static String www2ce(String in) {

        return unescapeString(in, _URIEscape, "%20");

    }


    public static void main(String[] args) throws Exception {

        if (args.length > 0) {
            for (String s : args) {
                System.out.println("id2www - Input: \"" + s + "\"   Output: \"" + id2www(s) + "\"   recaptured: " + www2id(id2www(s)));
            }
            for (String s : args) {
                String out = id2www(s);
                System.out.println("www2id - Input: \"" + out + "\"   Output: \"" + www2id(out) + "\" recaptured: " + id2www(www2id(out)));
            }

        } else {
            char[] allBytes = new char[256];

            for (int b = 0; b < 256; b++)
                allBytes[b] = (char) b;
            String allChars = new String(allBytes);
            System.out.println("id2www All Characters");
            System.out.println("Input String:      \"" + allChars + "\"");
            System.out.println("Output String:     \"" + id2www(allChars) + "\"");
            System.out.println("Recaptured String: \"" + www2id(id2www(allChars)) + "\" ");

        }
    }


}


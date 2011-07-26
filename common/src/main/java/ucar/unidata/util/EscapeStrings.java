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


package ucar.unidata.util;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

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

    // Set of all ascii printable alphanumeric characters
    public static String asciiAlphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    // Set of all ascii printable non-alphanumeric characters
    public static String asciiNonAlphaNumeric =
            " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~" ;

    // Non-alphanumeric (nam) allowed lists
    private static String _namAllowedInURL = "!#$&'()*+,-./:;=?@_~" ;
    private static String _namAllowedInURLQuery = "!#$&'()*+,-./:;=?@_~" ;

    // Non-alphanumeric disallowed lists
    private static String _disallowedInUrlQuery = " \"<>[\\]^`{|}%";   //Determined by experiment
    private static String _disallowedInUrl      = " \"<>[\\]^`{|}%";   //Determined by experiment

    // This is set of legal non-alphanumerics that can appear unescaped in a url query.
    public static String _allowableInUrlQuery =   asciiAlphaNumeric +_namAllowedInURLQuery;
                                                //+ "!#$&'()*+,-./:;=?@_~" ; // asciiNonAlphaNumerics - _disallowedInUrlQuery

    // This is set of legal characters that can appear unescaped in a url query.
    public static String _allowableInUrl =   asciiAlphaNumeric + _namAllowedInURL;
                                            //+ "!#$&'()*+,-./:;=?@_~" ; // asciiNonAlphaNumerics - _disallowedInUrl

    // This is set of legal characters that can appear unescaped in an OGC query.
    private static String _disallowedInOGC = stringUnion(" ?&=,+", //OGC Web Services Common 2.0.0 section 11.3
                                                         stringDiff(asciiNonAlphaNumeric,"-_.!~*'()"));
    private static String _namAllowedInOGC = "-_.!~*'()";

    public static String _allowableInOGC =   asciiAlphaNumeric + _namAllowedInOGC;

    private static char _URIEscape = '%';

    //<obsolete>
    // This appears to be incorrect wrt dap spec: private static String _allowableInURI = asciiAlphaNumeric + "-+_/.\\*";
    private static String _allowableInURI =
		asciiAlphaNumeric + "-+_\\*!~";  // plus: '"?
    // This appears to be incorrect wrt dap spec: private static String _allowableInURI_CE = asciiAlphaNumeric + "-+_/.\\,";
    private static String _allowableInURI_CE = asciiAlphaNumeric + "-+_\\,="; // plus "?
    //</obsolete>

    // These are the DEFINITIVE set of non-alphanumeric characters that are legal
    // in opendap identifiers (according to DAP2 protocol spec).
    public static String opendap_identifier_special_characters = "_!~*-\"";

    // The complete set of legal opendap identifier characters
    public static String opendap_identifier_characters =
		          asciiAlphaNumeric
                + opendap_identifier_special_characters;


    /*
     * s1 union s2
     */
    static private String stringUnion(String s1, String s2)
    {
        String union = s1;
        for(char c: s2.toCharArray()) {
            if(union.indexOf(c) < 0) union += c;
        }
        return union;
    }

    /*
     * s1 - s2
     */
    static private String stringDiff(String s1, String s2)
    {
        String diff = "";
        for(char c: s1.toCharArray()) {
            if(s2.indexOf(c) < 0) diff += c;
        }
        return diff;
    }


    /**
     * Replace all characters in the String <code>in</code> not present in the String <code>allowable</code> with
     * their hexidecimal values (encoded as UTF8) and preceeded by the String <code>esc</code>
     * <p/>
     * The <cods>esc</code> character may not appear on the allowable list, as if it did it would break the 1:1
     * and onto mapping between the unescaped character space and the escaped characater space.
     *
     * @param in        The string in which to replace characters.
     * @param allowable The set of allowable characters.
     * @param esc       The escape String (typically "%" for a URI or "\" for a regular expression).
     * @param spaceplus True if spaces should be replaced by '+'.
     * @return The modified identifier.
     */

    // Useful constants
    static byte blank = ((byte)' ');
    static byte plus = ((byte)'+');

    private static String escapeString(String in, String allowable, char esc, boolean spaceplus) throws Exception {
        StringBuffer out = new StringBuffer();
        int i;
/*
final int length = s.length();
for (int offset = 0; offset < length; ) {
   final int codepoint = s.codePointAt(offset);

   // do something with the codepoint

   offset += Character.charCount(codepoint);
}

 */
        if (in == null) return null;

        byte[] utf8 = in.getBytes("UTF-8");
        byte[] allow8 = allowable.getBytes("UTF-8");

        for(byte b: utf8) {
            if(b == blank && spaceplus) {
               out.append('+');
            } else {
                // search allow8
                boolean found = false;
                for(byte a: allow8) {
                    if(a == b) {found = true; break;}
                }
                if(found) {out.append((char)b);}
                else {
                    String c = Integer.toHexString(b);
                    out.append(esc);
                    if (c.length() < 2) out.append('0');
                    out.append(c);
                }
            }
        }
        /*
        StringBuilder buf = new StringBuilder(in);
        if(spaceplus) {
            for(i=0;(i=in.indexOf(' ',i)) >= 0; i++) {
                buf.setCharAt(i,'+');
                i++;
            }
            in = buf.toString();
        }

        if (allowable.indexOf(esc) >= 0) {//isEscAllowed(allowable, esc)) 
            throw new Exception("Escape character MAY NOT be in the list of allowed characters!");
        }

        char[] inca = in.toCharArray();
        String c;

        boolean isAllowed;
        for (char candidate : inca) {
            isAllowed = allowable.indexOf(candidate) >= 0 || (candidate == '+' && spaceplus);
            if (isAllowed) {
                out += candidate;
            } else {
                c = Integer.toHexString(candidate);
                if (c.length() < 2)
                    c = "0" + c;
                out += esc + c;
            }

        }
        */
        return out.toString();

    }

    /**
     * Given a string that contains WWW escape sequences, translate those escape
     * sequences back into ASCII characters. Return the modified string.
     *
     * @param in     The string to modify.
     * @param escape The character used to signal the begining of an escape sequence.
     * param except If there is some escape code that should not be removed by
     *               this call (e.g., you might not want to remove spaces, %20) use this
     *               parameter to specify that code. The function will then transform all
     *               escapes except that one.
     * @param spaceplus True if spaces should be replaced by '+'.
     * @return The modified string.
     */
    private static String unescapeString(String in, char escape, boolean spaceplus) throws Exception
    {
        if (in == null) return null;

        byte[] utf8 = in.getBytes("UTF-8");
        byte escape8 = (byte)escape;
        byte[] out = new byte[utf8.length]; // Should be max we need

        int index8 = 0;
        for(int i=0;i<utf8.length;) {
            byte b = utf8[i++];
            if(b == plus && spaceplus) {
               out[index8++] = blank;
            } else if(b == escape8) {
                // check to see if there are enough characters left
                if(i+2 <= utf8.length) {
                    b = (byte)(fromHex(utf8[i])<<4 | fromHex(utf8[i + 1]));
                    i += 2;
                }
            }
            out[index8++] = b;
        }
        /*
        String esc = String.valueOf(escape);
        String replacement;
        int i;

        if(spaceplus) {
            StringBuilder escaped = new StringBuilder();
            for(i=0;(i=in.indexOf('+',i)) >= 0; i++) {
                escaped.setCharAt(i,' ');
            }
            in = escaped.toString();
        }

        String out = in;
        i = 0;
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
        */
        return new String(out,0,index8,"UTF-8");

    }

    static final byte hexa = (byte)'a';
    static final byte hexf = (byte)'f';
    static final byte hexA = (byte)'A';
    static final byte hexF = (byte)'F';
    static final byte hex0 = (byte)'0';
    static final byte hex9 = (byte)'9';
    static final byte ten = (byte)10;


    private static byte fromHex(byte b) throws NumberFormatException
    {
        if(b >= hex0 && b <= hex9) return (byte)(b - hex0);
        if(b >= hexa && b <= hexf) return (byte)(ten + (b - hexa));
        if(b >= hexA && b <= hexF) return (byte)(ten + (b - hexA));
        throw new NumberFormatException("Illegal hex character: "+b);
    }

    /**
     * Split a url into the base plus the query
     *
     * @param url The expression to unescape.
     * @return The base and url as a 2 element string array.
     */
     public static String[] splitURL(String url)
     {
         String[] pair = new String[2];
         int index = url.indexOf('?');
         if(index >= 0) {
             pair[0] = url.substring(0,index);
             pair[1] = url.substring(index+1,url.length());
         } else {
             pair[0] = url;
             pair[1] = null;
         }
         return pair;
     }

    /**
     * Define the DEFINITIVE opendap identifier escape function.
     * @param id The identifier to modify.
     * @return The escaped identifier.
     */
    public static String escapeDAPIdentifier(String id)
    {
       String s;
       try {
           s = escapeString(id, opendap_identifier_characters, _URIEscape, false);
       } catch (Exception e) {
            s = null;
       }
       return s;
    }

    /**
    * Define the DEFINITIVE opendap identifier unescape function.
    * @param id The identifier to unescape.
    * @return The unescaped identifier.
    */
    public static String unEscapeDAPIdentifier(String id)
    {
        String s;
        try {
            s = unescapeString(id, _URIEscape, false);
        } catch (Exception e) {
            s = null;
        }
        return s;
    }

    /**
     * Define the DEFINITIVE URL constraint expression escape function.
     *
     * @param ce The expression to modify.
     * @return The escaped expression.
     */
     public static String escapeURLQuery(String ce)
     {
	try {
	    ce = escapeString(ce, _allowableInUrlQuery, _URIEscape, false);
	} catch(Exception e) {ce = null;}
        return ce;
     }

    /**
     * Define the DEFINITIVE URL constraint expression unescape function.
     *
     * @param ce The expression to unescape.
     * @return The unescaped expression.
     */
     public static String unescapeURLQuery(String ce)
     {
        try {
            ce = unescapeString(ce, _URIEscape, false);
        } catch(Exception e) {ce = null;}
        return ce;
     }

    /**
     * Define the DEFINITIVE URL escape function.
     * Note that the whole string is escaped, so
     * be careful what you pass into this procedure.
     *
     * @param s The string to modify.
     * @return The escaped expression.
     */
     public static String urlEncode(String s)
     {
        try {
            //s = escapeString(s, _allowableInUrl, _URIEscape, false);
            s = URLEncoder.encode(s,"UTF-8");
        } catch(Exception e) {s = null;}
        return s;
     }

    /**
     * Define the DEFINITIVE URL unescape function.
     *
     * @param s The string to unescape.
     * @return The unescaped expression.
     */
     public static String urlDecode(String s)
     {
        try {
            //s = unescapeString(s, _URIEscape, "", false);
            s = URLDecoder.decode(s,"UTF-8");
        } catch(Exception e) {s = null;}
        return s;
     }

    /**
     *  Decompose a url and piecemeal encode all of its parts, including query and fragment
     * @param url  the url to encode
     */
     public static String escapeURL(String url)
     {
        String newurl = null;
        try {
            URI u = new URI(url);
            u = new URI(u.getScheme(),u.getUserInfo(),u.getHost(),u.getPort(),
                        escapeString(u.getPath(),_allowableInUrl,_URIEscape,true),
                        escapeURLQuery(u.getQuery()),
                        u.getFragment());
            newurl = u.toASCIIString();
        } catch (Exception e) {newurl = url;}
        return newurl;
     }

    /**
     *  Decode all of the parts of the url including query and fragment
     * @param url  the url to encode
     */
     public static String unescapeURL(String url)
     {
        String newurl = null;
        newurl = urlDecode(url);
        return newurl;
     }


    /**
     * Define the OGC Web Services escape function.
     *
     * @param s The string to encode.
     * @return The escaped string.
     */
     public static String escapeOGC(String s)
     {
        return urlEncode(s);
     }

    /**
     * Define the OGC unescape function.
     *
     * @param s The string to unescape.                                                                 b
     * @return The unescaped string.
     */
     public static String unescapeOGC(String s)
     {
        return urlDecode(s);
     }


  ///////////////////////////////////////////////////////////////

  /**
   * backslash escape a string
   * @param x escape this
   * @param reservedChars these chars get a backslash in front of them
   * @return escaped string
   */
  static public String backslashEscape(String x, String reservedChars) {
    if (reservedChars == null) return x;
    boolean ok = true;
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (reservedChars.indexOf(c) >= 0) {
        ok = false;
        break;
      }
    }
    if (ok) return x;

    // gotta do it
    StringBuilder sb = new StringBuilder(x);
    for (int pos = 0; pos < sb.length(); pos++) {
      char c = sb.charAt(pos);
      if (reservedChars.indexOf(c) < 0) {
        continue;
      }

      sb.setCharAt(pos, '\\');
      pos++;
      sb.insert(pos, c);
      pos++;
    }

    return sb.toString();
  }

  /**
   * backslash unescape a string
   * @param x unescape this
   * @return string with \c -> c
   */
  static public String backslashUnescape(String x) {
    if (!x.contains("\\")) return x;

    // gotta do it
    StringBuilder sb = new StringBuilder(x.length());
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (c == '\\') {
        c = x.charAt(++pos); // skip backslash, get next char
      }
      sb.append(c);
    }

    return sb.toString();
  }


  /**
   * Tokenize an escaped name using "." as delimiter, skipping "\."
   *
   * @param escapedName an escaped name
   * @return list of tokens
   */
  public static List<String> tokenizeEscapedName(String escapedName) {
    List<String> result = new ArrayList<String>();
    int pos = 0;
    int start = 0;
    while (true) {
      pos = escapedName.indexOf(sep, pos+1);
      if (pos <= 0) break;
      if ((pos > 0) && escapedName.charAt(pos-1) != '\\') {
        result.add(escapedName.substring(start, pos));
        start = pos+1;
      }
    }
    result.add(escapedName.substring(start, escapedName.length())); // remaining
    return result;
  }
  static private final int sep = '.';

  /**
   * Find first occurence of char c in escapedName, excluding escaped c.
   * @param escapedName search in this string
   * @param c for this char but not \\char
   * @return  pos in string, or -1
   */
  public static int indexOf(String escapedName, char c) {
    int pos = 0;
    while (true) {
      pos = escapedName.indexOf(c, pos+1);
      if (pos <= 0) return pos;
      if ((pos > 0) && escapedName.charAt(pos-1) != '\\') return pos;
    }
  }

  public static void main(String[] args) {
    String s = "http://motherlode.ucar.edu:8081/thredds/dodsC/fmrc/NCEP/GFS/Global_0p5deg/runs/NCEP-GFS-Global_0p5deg_RUN_2011-07-15T00:00:00Z.html.asc?Total_cloud_cover_low_cloud%5B1:1:1%5D%5B0:1:360%5D%5B0:1:719%5D";
    System.out.printf("%s%n", s);
    System.out.printf("%s%n", unescapeURL(s));
  }

    public static void mainOld(String[] args) throws Exception
    {
        /* Ignore

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
        */
    }


}


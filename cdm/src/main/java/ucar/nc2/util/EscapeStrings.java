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
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, O
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package ucar.nc2.util;

import ucar.nc2.constants.CDM;
import ucar.httpservices.HTTPSession;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Jul 7, 2006
 * Time: 10:23:19 AM
 */
public class EscapeStrings
{

    //////////////////////////////////////////////////////////////////////////
    static public org.slf4j.Logger log
                = org.slf4j.LoggerFactory.getLogger(HTTPSession.class);
    //////////////////////////////////////////////////////////////////////////

/*
    // Sets of ascii characters
    static final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static final String numeric = "0123456789";
    static final String alphaNumeric = alpha + numeric;

    // Experimentally determined url and query
    // legal and illegal chars as defined by apache httpclient3
    // Sets are larger than strictly necessary
    static final String httpclient_urllegal   = "!#$&'()*+,-./:;=?@_~";
    static final String httpclient_querylegal = "!#$&'()*+,-./:;=?@_~%"; // % is difference
    static final String httpclient_urlillegal   = " \"<>[\\]^`{|}%";
    static final String httpclient_queryillegal = " \"<>[\\]^`{|}";  // % is difference


    // Set of all ascii printable non-alphanumeric (aka nan) characters
    static private final String nonAlphaNumeric = " !\"#$%&'()*+,-./:;<=>?@[]\\^_`|{}~" ;

    static private final String queryReserved
        = httpclient_queryillegal; // " ?&=,+;#"; // special parsing meaning in queries
    static private final String urlReserved
        = httpclient_urlillegal; // ":/#?"; // special parsing meaning in url

    // We assume that whoever constructs a url (minus the query)
    // has properly percent encoded whatever characters need to be encoded.
    // Sets of characters absolutely DIS-allowed in url.
    static private final String urlDisallowed
                                = stringUnion(urlReserved); // what else?
    // Complement of urlDisallowed
    static private final String urlAllowed = stringDiff(nonAlphaNumeric,urlDisallowed);

    // This is set of legal characters that can appear unescaped in a url
    static private final String _allowableInUrl= alphaNumeric + urlAllowed;

    // Sets of characters absolutely DIS-allowed in query string identifiers.
    // Basically, this set is determined by what kind of query parsing needs to occur.
    static private final String queryIdentDisallowed
                                = stringUnion(queryReserved+"\"\\^`|<>[]{}");

    // Complement of queryIdentDisallowed
    static private final String queryIdentAllowed = stringDiff(nonAlphaNumeric,queryIdentDisallowed);

    // This is set of legal characters that can appear unescaped in a url query.
    static private final String _allowableInUrlQuery = alphaNumeric + queryIdentAllowed;

    // Define the set of characters allowable in DAP Identifiers
    static private final String dapSpecAllowed = "_!~*'-\"" ; //as specified in dap2 spec
    static private final String _namAllowedInDAP = dapSpecAllowed
                                                   + "./"; // for groups and structure names
    static private final String _allowableInDAP = alphaNumeric + _namAllowedInDAP;


    // This is set of legal characters that can appear unescaped in an OGC query.
    // See OGC Web Services Common 2.0.0 section 11.3
    static private final String _namAllowedInOGC = "-_.!~*'()";
    static private final String _disallowedInOGC
			        = stringUnion(queryReserved+stringDiff(nonAlphaNumeric,_namAllowedInOGC));
    static private final String _allowableInOGC = alphaNumeric + _namAllowedInOGC;

    static private final char _URIEscape = '%';

    // These are the DEFINITIVE set of non-alphanumeric characters that are legal
    // in opendap identifiers (according to DAP2 protocol spec).
    // Add '/' to support group names

    static private String opendap_identifier_special_characters = "_!~*'-\"" // see dap spec.
                                                                  + "./" ; // less strict

    // The complete set of legal opendap identifier characters
    public static String opendap_identifier_characters
	 	         = alphaNumeric + opendap_identifier_special_characters;
*/

    static protected final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static protected final String numeric = "0123456789";
    static protected final String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    static protected final String httpclient_urllegal   = "!#$&'()*+,-./:;=?@_~";
    static protected final String httpclient_querylegal = "!#$&'()*+,-./:;=?@_~%";
    static protected final String httpclient_urlillegal   = " \"<>[\\]^`{|}%";
    static protected final String httpclient_queryillegal = " \"<>[\\]^`{|}";
    static protected final String nonAlphaNumeric = " !\"#$%&'()*+,-./:;<=>?@[]\\^_`|{}~";
    static protected final String queryReserved = " \"<>[\\]^`{|}";
    static protected final String urlReserved = " \"<>[\\]^`{|}%";
    static protected final String urlDisallowed = " \"<>[\\]^`{|}%";
    static protected final String urlAllowed = "!#$&'()*+,-./:;=?@_~";
    static protected final String _allowableInUrl = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$&'()*+,-./:;=?@_~";
    static protected final String queryIdentDisallowed = " \"\\^`|<>[]{}";
    static protected final String queryIdentAllowed = "!#$%&'()*+,-./:;=?@_~";
    static protected final String _allowableInUrlQuery = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&'()*+,-./:;=?@_~";
    static protected final String dapSpecAllowed = "_!~*'-\"";
    static protected final String _namAllowedInDAP = "_!~*'-\"./";
    static protected final String _allowableInDAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_!~*'-\"./";
    static protected final String _namAllowedInOGC = "-_.!~*'()";
    static protected final String _disallowedInOGC = " \"#$%&+,/:;<=>?@[]\\^`|{}";
    static protected final String _allowableInOGC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()";
    static protected final String opendap_identifier_special_characters = "_!~*'-\"./";
    static protected final String opendap_identifier_characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_!~*'-\"./";
    static protected final char _URIEscape = '%';

    /*
     * s1 union s2
     */
    static private String stringUnion(String s)
    {
        StringBuilder union = new StringBuilder();
        for(int i=0;i<s.length();i++) {
            char c = s.charAt(i);
            if(s.indexOf(c,i+1) >= 0) continue; // later occurrence
            union.append(c);
        }
        return union.toString();
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
    static final byte blank = ((byte)' ');
    static final byte plus = ((byte)'+');

    private static String xescapeString(String in, String allowable, char esc, boolean spaceplus)
    {
        try {
            StringBuffer out = new StringBuffer();
            if (in == null) return null;
            byte[] utf8 = in.getBytes(CDM.utf8Charset);
            byte[] allow8 = allowable.getBytes(CDM.utf8Charset);
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

            return out.toString();

        } catch (Exception e) {
            return in;
        }
    }

    private static String escapeString(String in, String allowable)
   {return xescapeString(in,allowable,_URIEscape,false);}

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

    private static String xunescapeString(String in, char escape, boolean spaceplus)
    {
        try {
            if (in == null) return null;

            byte[] utf8 = in.getBytes(CDM.utf8Charset);
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
            return new String(out,0,index8, CDM.utf8Charset);
        } catch(Exception e) {
            return in;
        }

    }

    private static String unescapeString(String in)
    {return xunescapeString(in, _URIEscape,false);}

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
    * Define the DEFINITIVE opendap identifier unescape function.
    * @param id The identifier to unescape.
    * @return The unescaped identifier.
    */
    public static String unescapeDAPIdentifier(String id)
    {
        String s;
        try {
            s = unescapeString(id);
        } catch (Exception e) {
            s = null;
        }
        return s;
    }

    /**
     * Define the DEFINITIVE URL escape function.
     * Beware, this is a rather complex operation
     *
     * @param url The url string
     * @return The escaped expression.
     */
     static private final Pattern p
            = Pattern.compile("([\\w]+)://([.\\w]+(:[\\d]+)?)([/][^?#])?([?][^#]*)?([#].*)?");

     public static String escapeURL(String url)
     {
        String protocol;
        String authority;
        String path;
        String query;
        String fragment;
        if(false) {
            // We split the url ourselves to minimize character dependencies
            Matcher m = p.matcher(url);
            boolean match = m.matches();
            if(!match) return null;
            protocol = m.group(1);
            authority = m.group(2);
            path = m.group(3);
            query = m.group(4);
            fragment = m.group(5);
        } else {// faster, but may not work quite right
            URL u;
            try {u = new URL(url);} catch (MalformedURLException e) {
                return null;
            }
            protocol = u.getProtocol();
            authority = u.getAuthority();
            path = u.getPath();
            query = u.getQuery();
            fragment = u.getRef();
        }
        // Reassemble
        StringBuilder ret = new StringBuilder(protocol);
        ret.append("://");
        ret.append(authority);
        if(path != null && path.length() > 0) {
            // Encode pieces between '/'
            String pieces[] = path.split("[/]",-1);
            for(int i=0;i<pieces.length;i++)  {
                String p = pieces[i];
                if(p == null) p = "";
                if(i > 0) ret.append("/");
                ret.append(urlEncode(p));
            }
        }
        if(query != null && query.length() > 0) {
            ret.append("?");
            ret.append(escapeURLQuery(query));
        }
        if(fragment != null && fragment.length() > 0) {
            ret.append("#");
            ret.append(urlEncode(fragment));
        }
        return ret.toString();
     }

     static int nextpiece(String s, int index, String sep)
     {
         index = s.indexOf(sep,index);
         if(index < 0)
             index = s.length();
         return index;
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
            ce = escapeString(ce, _allowableInUrlQuery);
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
            ce = unescapeString(ce);
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
     private static String urlEncode(String s)
     {
        //try {s = URLEncoder.encode(s,"UTF-8");} catch(Exception e) {s = null;}
        s = escapeString(s, _allowableInUrl);
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
     *  Decode all of the parts of the url including query and fragment
     * @param url  the url to encode
     */
     public static String unescapeURL(String url)
     {
        String newurl;
        newurl = urlDecode(url);
        return newurl;
     }


    /**
     * Define the DAP escape identifier function.
     *
     * @param s The string to encode.
     * @return The escaped string.
     */
     public static String escapeDAPIdentifier(String s)
     {
        return escapeString(s, _allowableInDAP);
     }

    /**
     * Define the OGC Web Services escape function.
     *
     * @param s The string to encode.
     * @return The escaped string.
     */
     public static String escapeOGC(String s)
     {
        return escapeString(s, _allowableInOGC);
     }

    static public void testOGC()
    {
        for (char c : (alphaNumeric + " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~").toCharArray()) {
            String encoded = EscapeStrings.escapeOGC("" + c);
            System.err.printf("|%c|=|%s|%n", c, encoded);
        }
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
   * @param x escape this; may be null
   * @param reservedChars these chars get a backslash in front of them
   * @return escaped string
   */
  static public String backslashEscape(String x, String reservedChars) {
    if (x == null) {
      return null;
    } else if (reservedChars == null) {
      return x;
    }

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
        c = x.charAt(++pos); // skip backslash, get next cha
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
    List<String> result = new ArrayList<>();
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
   * @param c for this char but not \\cha
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

  public static void main2(String[] args) {
    String s = "http://thredds.ucar.edu/thredds/dodsC/fmrc/NCEP/GFS/Global_0p5deg/runs/NCEP-GFS-Global_0p5deg_RUN_2011-07-15T00:00:00Z.html.asc?Total_cloud_cover_low_cloud%5B1:1:1%5D%5B0:1:360%5D%5B0:1:719%5D";
    log.debug("%s%n", s);
    log.debug("%s%n", unescapeURL(s));
  }

  public static void main(String[] args) {
    String s = "https://localhost:8443/thredds/admin/log/access/";
    System.out.printf("%s%n", s);
    System.out.printf("%s%n", escapeURL(s));
  }

    public static void mainOld(String[] args) throws Exception
    {
        /* Ignore

        if (args.length > 0) {
            for (String s : args) {
                LogStream.out.println("id2www - Input: \"" + s + "\"   Output: \"" + id2www(s) + "\"   recaptured: " + www2id(id2www(s)));
            }
            for (String s : args) {
                String out = id2www(s);
                LogStream.out.println("www2id - Input: \"" + out + "\"   Output: \"" + www2id(out) + "\" recaptured: " + id2www(www2id(out)));
            }

        } else {
            char[] allBytes = new char[256];

            for (int b = 0; b < 256; b++)
                allBytes[b] = (char) b;
            String allChars = new String(allBytes);
            LogStream.out.println("id2www All Characters");
            LogStream.out.println("Input String:      \"" + allChars + "\"");
            LogStream.out.println("Output String:     \"" + id2www(allChars) + "\"");
            LogStream.out.println("Recaptured String: \"" + www2id(id2www(allChars)) + "\" ");
        }
        */
    }



/**
 * This method is used to normalize strings prio
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

    // Some handy definitons.
    static final String xmlGT = "&gt;";
    static final String xmlLT = "&lt;";
    static final String xmlAmp = "&amp;";
    static final String xmlApos = "&apos;";
    static final String xmlQuote = "&quot;";

public static String normalizeToXML(String s)
{
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
            default: break;
        }
    }
    return (sb.toString());

}

    /**
     * Given a backslash escaped name,
     * convert to a DAP escaped name
     * 
     * @param bs the string to DAP encode; may have backslash escapes
     * @return escaped string
    */

    public static String backslashToDAP(String bs)
    {
	StringBuilder buf = new StringBuilder();
	int len = bs.length();	
	for(int i=0;i<len;i++) {
	    char c = bs.charAt(i);
	    if(i< (len-1) && c == '\\') {c = bs.charAt(++i);}
	    if(_allowableInDAP.indexOf(c) < 0) {
		buf.append(_URIEscape);
		// convert the char to hex
		String ashex = Integer.toHexString((int)c);
		if(ashex.length() < 2) buf.append('0');
		buf.append(ashex); 
	    } else
		buf.append(c);
	}
	return buf.toString();
    }


/**
 *  Given a DAP (attribute) string, insert backslashes
 *  before '"' and '/' characters. This code also escapes
 *  control characters, although the spec does not call for it;
 *  make that code conditional.
 */
static public String
backslashEscapeDapString(String s)
{
    StringBuilder buf = new StringBuilder();
    for(int i=0;i<s.length();i++) {
	int c = s.charAt(i);
        if(true) {
            if(c < ' ') {
                switch (c) {
                case '\n': case '\r': case '\t': case '\f':
                    buf.append((char)c);
                    break;
                default:
                    buf.append(String.format("\\x%02x",(c&0xff)));
                    break;
                }
                continue;
            }
        }
        if(c == '"') {
            buf.append("\\\"");
        } else if(c == '\\') {
            buf.append("\\\\");
        } else
            buf.append((char)c);
    }
    return buf.toString();
}




}

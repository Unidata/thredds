/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Provide various methods for (un)escaping text
 */

public class Escape
{

    // define constants for consistency
    static final String ENTITY_AMP = "amp";
    static final String ENTITY_LT = "lt";
    static final String ENTITY_GT = "gt";
    static final String ENTITY_QUOT = "quot";
    static final String ENTITY_APOS = "apos";

    static public final String[][] DEFAULTTRANSTABLE = {
        {ENTITY_AMP, "&"},
        {ENTITY_LT, "<"},
        {ENTITY_GT, ">"},
        {ENTITY_QUOT, "\""},
        {ENTITY_APOS, "'"},
    };

    // For reference: set of all ascii printable non-alphanumeric characters
    static private final String nonAlphaNumeric = " !\"#$%&'()*+,-./:;<=>?@[]\\^_`|{}~";

    // define the printable backslash characters to escape (control chars not included)
    static public final String BACKSLASHESCAPE = "./\\\"'";

    // Define the alphan characters

    /* Defind loose set of characters that can appear in an xml entity name */
    static public boolean entitychar(char c)
    {
        return ("#_0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(c) >= 0);
    }

    /**
     * Escape selected characters in a string using XML entities
     */
    static public String
    entityEscape(String s)
    {
        StringBuilder escaped = new StringBuilder();
        for(int i = 0;i < s.length();i++) {
            char c = s.charAt(i);
            switch (c) {
            case '&':
                escaped.append('&' + ENTITY_AMP + ';');
                break;
            case '<':
                escaped.append('&' + ENTITY_LT + ';');
                break;
            case '>':
                escaped.append('&' + ENTITY_GT + ';');
                break;
            case '"':
                escaped.append('&' + ENTITY_QUOT + ';');
                break;
            case '\'':
                escaped.append('&' + ENTITY_APOS + ';');
                break;
            default:
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    static public String
    entityUnescape(String s)
    {
        return entityUnescape(s, null);
    }

    static public String
    entityUnescape(String s, String[][] translations)
    {
        int count, len;
        boolean found;
        StringBuilder u; // returned string with entities unescaped
        int p; // insertion point into u
        int q; // next char from s
        int stop;
        StringBuilder entity;

        if(translations == null)
            translations = DEFAULTTRANSTABLE;

        if(s == null) len = 0;
        else len = s.length();
        u = new StringBuilder();
        p = 0;
        q = 0;
        stop = (len);
        entity = new StringBuilder();

        while(q < stop) {
            char c = s.charAt(q++);
            switch (c) {
            case '&': // see if this is a legitimate entity
                entity.setLength(0);
                // move forward looking for a semicolon;
                for(found = true, count = 0;;count++) {
                    if(q + count >= len) break;
                    c = s.charAt(q + count);
                    if(c == ';')
                        break;
                    if(!entitychar(c)) {
                        found = false; // not a legitimate entity
                        break;
                    }
                    entity.append(c);
                }
                if(q + count >= len || count == 0 || !found) {
                    // was not in correct form for entity
                    u.append('&');
                } else { // looks legitimate
                    String test = entity.toString();
                    String replacement = null;
                    for(String[] trans : translations) {
                        if(trans[0].equals(test)) {
                            replacement = trans[1];
                            break;
                        }
                    }
                    if(replacement == null) { // no translation, ignore
                        u.append('&');
                    } else { // found it
                        q += (count + 1); // skip input entity, including trailing semicolon
                        u.append(replacement);
                    }
                }
                break;
            default:
                u.append(c);
                break;
            }
        }
        return u.toString();
    }

    /**
     * Escape control chars plus
     * selected other characters in a string using backslash
     */
    static public String
    backslashEscape(String s, String wrt)
    {
        if(wrt == null)
            wrt = BACKSLASHESCAPE;
        StringBuilder escaped = new StringBuilder();
        for(int i = 0;i < s.length();i++) {
            char c = s.charAt(i);
            if(c < ' ' || c == 127) {
                escaped.append('\\');
                switch (c) {
                case '\r':
                    c = 'r';
                    break;
                case '\n':
                    c = 'n';
                    break;
                case '\t':
                    c = 't';
                    break;
                case '\f':
                    c = 'f';
                    break;
                default:
                    escaped.append('x');
                    escaped.append(Escape.toHex((int) c));
                    continue; /* since this is a string */
                }
            } else if(wrt.indexOf(c) >= 0)
                escaped.append('\\');
            escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * Remove backslashed characters in a string
     */
    static public String
    backslashUnescape(String s)
    {
        StringBuilder clear = new StringBuilder();
        for(int i = 0;i < s.length();) {
            char c = s.charAt(i++);
            if(c == '\\') {
                c = s.charAt(i++);
                switch (c) {
                case 'r':
                    c = '\r';
                    break;
                case 'n':
                    c = '\n';
                    break;
                case 't':
                    c = '\t';
                    break;
                case 'f':
                    c = '\f';
                    break;
                default:
                    break;
                }
                clear.append(c);
            } else
                clear.append(c);
        }
        return clear.toString();
    }

/*    static public String backslashEscape(String s, String wrt)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0;i < s.length();i++) {
            char c = s.charAt(i);
            if(wrt.indexOf(c) >= 0)
                buf.append('\\');
            buf.append(c);
        }
        return buf.toString();
    }
    */

    static final public String hexchars = "0123456789abcdef";
    static final String allhexchars = "0123456789abcdefABCDEF";

    static public String toHex(int i)
    {
        char digit1 = hexchars.charAt((i >>> 4) & 0xf);
        char digit2 = hexchars.charAt((i & 0xf));
        return Character.toString(digit1) + digit2;
    }

    static public int fromHex(char c)
    {
        c = Character.toLowerCase(c);
        int index = hexchars.indexOf(c);
        if(index < 0)
            return -1;
        return index;
    }

    static public boolean isHexDigit(char c)
    {
        return allhexchars.indexOf(c) >= 0;
    }


    static public String
    bytes2hex(byte[] bytes)
    {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(2 + (len * 2));
        buf.append("0x");
        for(int i = 0;i < len;i++) {
            byte b = bytes[i];
            buf.append(hexchars.charAt((b >>> 4) & 0xF));
            buf.append(hexchars.charAt((i & 0xF)));
        }
        return buf.toString();
    }

    static public String urlDecode(String s)
    {
        try {
            s = URLDecoder.decode(s, "UTF8");
        } catch (UnsupportedEncodingException uee) {
        }
        return s;
    }

    // (Minimal?) Set of non-alphanum characters that need to be escaped in a query
    // before sending it to server
    static final String URLESCAPECHARS = " %";

    static public String urlEncodeQuery(String s)
    {
        if(s == null || s.length() == 0) return s;
        if(false) try {// Note that URLEncoder over encodes. For practical purposes,
            // only a limited set of characters needs to be encoded
            s = URLEncoder.encode(s, "UTF8");
        } catch (UnsupportedEncodingException uee) {
        }
        else {
            StringBuilder buf = new StringBuilder();
            for(int i = 0;i < s.length();i++) {
                char c = s.charAt(i);
                if(URLESCAPECHARS.indexOf(c) >= 0) {
                    buf.append("%");
                    String encode = Integer.toHexString((int) c);
                    if(encode.length() == 1) buf.append("0");
                    buf.append(encode);
                } else
                    buf.append(c);
            }
        }
        return s;
    }


}//Escape

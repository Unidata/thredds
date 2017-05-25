/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Misc. utilities; avoid duplicating dap4.core.dmr.{Escape,Util}
 */

public class ParseUtil
{
    static public final int MAXTEXT = 12;

    /* Common Flag Set */
    static public final int FLAG_NONE = 0;
    static public final int FLAG_ESCAPE = 1; //convert \n,\r, etc to \\ form
    static public final int FLAG_NOCR = 2; // elide \r
    static public final int FLAG_ELIDETEXT = 4; // only print the first 12 characters of text
    static public final int FLAG_TRIMTEXT = 8; //remove leading and trailing whitespace;
    // if result is empty, then ignore
    static public final int FLAG_TRACE = 16;   // Trace the DomLexer tokens

    static public final int DEFAULTFLAGS = (FLAG_ELIDETEXT | FLAG_ESCAPE | FLAG_NOCR | FLAG_TRIMTEXT);

    /* Characters legal as first char of an element or attribute name */
    static public boolean namechar1(char c)
    {
        return (":_?".indexOf(c) >= 0
            || "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(c) >= 0
            || ((int) c) > 127);
    }

    /* Characters legal as greater than first char of an element or attribute name */
    static public boolean namecharn(char c)
    {
        return (namechar1(c)
            || "-.".indexOf(c) >= 0
            || "0123456789".indexOf(c) >= 0);
    }

    /**
     * Split a <Value>...</Value> into its component strings.
     * Generally, type checking is not performed. String quotes
     * are obeyed and backslash escapes are removed.
     */
    static public List<String>
    collectValues(String text)
        throws ParseException
    {
        List<String> values = new ArrayList<String>();
        StringBuffer buf = new StringBuffer();
        text = text.trim() + '\0';
        int i = 0;
        for(; ; ) {
            char c = text.charAt(i++);
            if(c == '\0') break; // eos
            if(c <= ' ' || c == 127) // whitespace
                continue;
            if(c == '\'') {// collect char constant
                c = text.charAt(i++);
                if(c == '\0')
                    throw new ParseException("Malformed char constant: no final '''");
                else if(i >= 128)
                    throw new ParseException("Illegal char constant: " + (int) c);
                buf.append(c);
                values.add(buf.toString());
                buf.setLength(0);
            } else if(c == '"') { // collect quoted string
                for(; ; ) {
                    c = text.charAt(i++);
                    if(c == '\0') {
                        i--;
                        break;
                    }
                    if(c == '\\') {
                        c = text.charAt(i++);
                    } else if(c == '"') break;
                    buf.append(c);
                }
                if(c == '\0')
                    throw new ParseException("Malformed string: no final '\"'");
                values.add(buf.toString());
                buf.setLength(0);
            } else {// collect upto next whitespace or eos
                do {
                    if(c == '\\') {
                        c = text.charAt(i++);
                    }
                    buf.append(c);
                    c = text.charAt(i++);
                } while(c > ' ' && c != 127);
                values.add(buf.toString());
                buf.setLength(0);
                if(c == 0) i--; // So we never move past the trailing eol
            }
        }
        return values;
    }

    static public boolean
    isLegalEnumConstName(String name)
    {
        // Name must consist of non-blank non-control characters
        for(int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if(c <= ' ' || c == 127) return false;
        }
        return true;
    }


} // class Util


/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import dap4.core.util.Escape;

import java.util.Map;

import static dap4.core.dmr.parser.ParseUtil.DEFAULTFLAGS;
import static dap4.core.dmr.parser.ParseUtil.MAXTEXT;

public abstract class Debug
{

    static void
    addtext(StringBuilder dst, String txt, int flags)
    {
        int len;
        int pos;
        boolean shortened = false;

        if(txt == null) {
            dst.append("null");
            return;
        }
        if((flags & ParseUtil.FLAG_TRIMTEXT) != 0) {
            txt = txt.trim();
        }
        len = txt.length();
        if((flags & ParseUtil.FLAG_ELIDETEXT) != 0 && len > MAXTEXT) {
            len = MAXTEXT;
            shortened = true;
        }
        dst.append('|');
        for(int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            if(len-- <= 0) continue;
            if((flags & ParseUtil.FLAG_ESCAPE) != 0 && c < ' ') {
                dst.append('\\');
                switch (c) {
                case '\n':
                    dst.append('n');
                    break;
                case '\r':
                    dst.append('r');
                    break;
                case '\f':
                    dst.append('f');
                    break;
                case '\t':
                    dst.append('t');
                    break;
                default: {// convert to octal
                    int uc = c;
                    int oct;
                    oct = ((uc >> 6) & 077);
                    dst.append((char) ('0' + oct));
                    oct = ((uc >> 3) & 077);
                    dst.append((char) ('0' + oct));
                    oct = ((uc) & 077);
                    dst.append((char) ('0' + oct));
                }
                break;
                }
            } else if((flags & ParseUtil.FLAG_NOCR) != 0 && c == '\r') {
                continue;
            } else {
                dst.append((char) c);
            }
        }
        if(shortened) {
            dst.append("...");
        }
        dst.append('|');
    }

    // Trace a SAX Token
    static public String
    trace(SaxEvent token)
    {
        return trace(token, DEFAULTFLAGS);
    }

    static public String
    trace(SaxEvent token, int flags)
    {
        StringBuilder result = new StringBuilder();
        String name = "UNDEFINED";
        String value = "";
        String text = "";
        SaxEventType event = null;

        name = token.name;
        value = token.value;
        text = token.text;
        event = token.eventtype;

        result.append("[" + event.name() + "] ");

        switch (event) {
        case STARTELEMENT:
        case ENDELEMENT:
            result.append(": element=|");
            result.append(name);
            result.append("|");
            break;
        case CHARACTERS:
            result.append(" text=");
            addtext(result, text, flags);
            String trans = Escape.entityUnescape(text);
            result.append(" translation=");
            addtext(result, trans, flags);
            break;
        case ATTRIBUTE:
            result.append(": name=");
            addtext(result, name, flags);
            result.append(" value=");
            addtext(result, value, flags);
            break;
        case STARTDOCUMENT:
            break;
        case ENDDOCUMENT:
            break;
        default:
            assert (false) : "Unexpected tokentype";
        }
        result.append(" eventtype=" + event.name());
        return result.toString();
    }

    static String
    traceList(Dap4Actions.XMLAttributeMap map)
    {
        StringBuilder result = new StringBuilder();
        for(Map.Entry<String,SaxEvent> entry : map.entrySet()) {
            SaxEvent event = entry.getValue();
            String trace = trace(event);
            if(result.length() != 0)
                result.append("\n");
            result.append(entry.getKey());
            result.append(": ");
            result.append(trace);
        }
        return result.toString();
    }


}//Debug

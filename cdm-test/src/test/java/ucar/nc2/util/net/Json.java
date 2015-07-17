/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package ucar.nc2.util.net;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.*;

import static java.io.StreamTokenizer.*;


// Singleton class

abstract public class Json
{

    //////////////////////////////////////////////////
    // Constants

    static final char LBRACKET = '[';
    static final char RBRACKET = ']';
    static final char LBRACE = '{';
    static final char RBRACE = '}';
    static final char COLON = ':';
    static final char COMMA = ',';
    static final char QUOTE = '"';
    static final String TRUE = "true";
    static final String FALSE = "false";

    //////////////////////////////////////////////////

    static public Object parse(String text)
            throws IOException
    {
        Parser parser = new Parser();
        return parser.parse(text);
    }

    //////////////////////////////////////////////////

    static protected class Parser
    {

        public Parser()
        {
        }

        // SImple recursive descent

        public Object parse(String text)
                throws IOException
        {
            StringReader rdr = new StringReader(text);
            StreamTokenizer tokens = new StreamTokenizer(rdr);
            tokens.eolIsSignificant(false);
            tokens.quoteChar(QUOTE);
            tokens.wordChars('a', 'z');
            tokens.wordChars('A', 'Z');
            tokens.wordChars('0', '9');
            tokens.wordChars('_', '_');
            tokens.wordChars('-', '-');
            tokens.wordChars('$', '$');
            Object result = parseR(tokens);
            return result;
        }

        protected Object parseR(StreamTokenizer tokens)
                throws IOException
        {
            int token = tokens.nextToken();
            switch (token) {
            case TT_EOF:
                return null;
            case TT_WORD:
                return parseAtomic(tokens);
            case LBRACE:
                return parseMap(tokens);
            case LBRACKET:
                return parseArray(tokens);
            case QUOTE:
                return parseAtomic(tokens);
            default:
                throw new IOException("Unexpected token:" + (char) token);
            }
        }

        protected Object parseAtomic(StreamTokenizer tokens)
                throws IOException
        {
            assert (tokens.ttype == TT_WORD || tokens.ttype == QUOTE);
            String word = tokens.sval;
            if(tokens.ttype == QUOTE)
                return word;
            try {
                Long l = Long.decode(word);
                return l;
            } catch (NumberFormatException nfe) {/*ignore*/}
            ;
            if(word.equalsIgnoreCase(TRUE))
                return Boolean.TRUE;
            if(word.equalsIgnoreCase(FALSE))
                return Boolean.FALSE;
            return word;
        }

        protected Object parseArray(StreamTokenizer tokens)
                throws IOException
        {
            assert (tokens.ttype == LBRACKET);
            List<Object> array = new ArrayList<>();
            loop:
            for(; ; ) {
                int token = tokens.nextToken();
                switch (token) {
                case TT_EOL:
                    break; // ignore
                case TT_EOF:
                    throw new IOException("Unexpected eof");
                case RBRACKET:
                    break loop;
                default:
                    tokens.pushBack();
                    Object o = parseR(tokens);
                    tokens.nextToken();
                    if(tokens.ttype == TT_EOF) break;
                    else if(tokens.ttype == RBRACKET) tokens.pushBack();
                    else if(tokens.ttype != COMMA)
                        throw new IOException("Missing comma in list");
                    array.add(o);
                }
            }
            return array;
        }

        protected Object parseMap(StreamTokenizer tokens)
                throws IOException
        {
            assert (tokens.ttype == LBRACE);
            Map<String, Object> map = new LinkedHashMap<>();  // Keep insertion order
            loop:
            for(; ; ) {
                int token = tokens.nextToken();
                switch (token) {
                case TT_EOL:
                    break; // ignore
                case TT_EOF:
                    throw new IOException("Unexpected eof");
                case RBRACE:
                    break loop;
                default:
                    tokens.pushBack();
                    Object name = parseR(tokens);
                    if(tokens.ttype == TT_EOF) break;
                    if(name instanceof String
                            || name instanceof Long
                            || name instanceof Boolean) {
                          /*ok*/
                    } else
                        throw new IOException("Unexpected map name type: " + name);
                    if(tokens.nextToken() != COLON)
                        throw new IOException("Expected ':'; found: " + tokens.ttype);
                    Object o = parseR(tokens);
                    tokens.nextToken();
                    if(tokens.ttype == TT_EOF) break;
                    else if(tokens.ttype == RBRACE) tokens.pushBack();
                    else if(tokens.ttype != COMMA)
                        throw new IOException("Missing comma in list");
                    map.put(name.toString(), o);
                }
            }
            return map;
        }
    }

    static public String toString(Object o) {return toString(o,"");}

    static public String toString(Object o, String demark)
    {
        StringBuilder buf = new StringBuilder();
        toStringR(o, buf, demark, 0);
        return buf.toString();
    }

    static protected void toStringR(Object o, StringBuilder buf, String demark, int indent)
    {
        boolean first = true;
        if(o instanceof List) {
            List<Object> list = (List<Object>) o;
            if(list.size()== 0) {
                buf.append(LBRACKET);
                buf.append(RBRACKET);
            } else {
                buf.append(LBRACKET);
                buf.append('\n');
                for(int i=0;i<list.size();i++) {
                    Object e = list.get(i);
                    buf.append(indent(indent));
                    toStringR(e, buf, demark, indent + 2);
                    if(i < list.size()-1) buf.append(",");
                    buf.append("\n");
                }
                buf.append(indent(indent));
                buf.append(RBRACKET);
            }
        } else if(o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            if(map.size() == 0) {
                buf.append(LBRACE);
                buf.append(RBRACE);
            } else {
                buf.append(LBRACE);
                buf.append('\n');
                int i = 0;
                for(Map.Entry<String, Object> e : map.entrySet()) {
                    buf.append(indent(indent + 2));
                    buf.append(QUOTE);
                    buf.append(e.getKey().replace("\"","\\\""));
                    buf.append(QUOTE);
                    buf.append(' ');
                    buf.append(COLON);
                    buf.append(' ');
                    toStringR(e.getValue(), buf, demark, indent + 2);
                    if(i < map.size() - 1) buf.append(",");
                    buf.append("\n");
                    i++;
                }
                buf.append(indent(indent));
                buf.append(RBRACE);
            }
        } else if((o instanceof Long) || (o instanceof Boolean)) {
            buf.append(demark);
            buf.append(o.toString());
            buf.append(demark);
        } else {
            buf.append(QUOTE);
            buf.append(o.toString().replace("\"","\\\""));
            buf.append(QUOTE);
        }
    }

    static String blanks = "                                                  ";

    static protected String indent(int n)
    {
        while(n > blanks.length()) {
            blanks = blanks + blanks;
        }
        return blanks.substring(0, n);
    }

}

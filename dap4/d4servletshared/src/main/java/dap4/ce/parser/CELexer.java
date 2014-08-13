/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

/*****************************************/

package dap4.ce.parser;

import dap4.core.dmr.parser.ParseException;

class CELexer implements CEParser.Lexer
{

    //////////////////////////////////////////////////
    // Constants

    /**
     * Single character delimiters. Note that '/' is not
     * included; it will be part of the leading name.
     */
    static final String DELIMS = "[].:,;|=<>!";

    /**
     * Escape Character
     */
    static final char ESCAPE = '\\';

    /**
     * End-of-string marker
     */
    static final char EOS = '\0';

    static final int CONTEXTLEN = 20; // yyerror shows last CONTEXTLEN characters of input

    /**
     * Hex digits
     */
    static final String hexdigits = "0123456789abcdefABCDEF";


    /**
     * Key words: none currently
     */
    static final String[] keywords = new String[]{
    };

    /**
     * Key word tokens: none currently
     */
    static final int[] keytokens = new int[]{
    };


    //////////////////////////////////////////////////

    /**
     * Equivalent of StringReader that allows for better
     * access to position info
     */

    static class TextStream
    {

        // Don't bother with getters
        String text = null; // source of text to lex
        int mark = 0;
        int next = 0; // next unread character
        int textlen = 0;

        public TextStream()
        {
        }

        public String around()
        {
            String prefix = text.substring(mark - 10, mark);
            String suffix = text.substring(mark, mark + 10);
            return prefix + "|" + suffix;
        }

        public void setText(String text)
        {
            this.textlen = text.length();
            this.text = text + '\0'; // Null terminate
        }

        public String getText()
        {
            return text.substring(0, this.textlen);
        }

        public String toString()
        {
            return getText();
        }

        int peek()
        {
            return text.charAt(next);
        }

        void
        backup()
        {
            if(next <= 0) next = 0;
            else
                next--;
        }

        int
        read()
        {
            char c = text.charAt(next);
            if(c != EOS) next++;
            return c;
        }

        void
        mark()
        {
            this.mark = this.next;
        }

    }


    //////////////////////////////////////////////////
    //

    /**
     * Per-lexer state
     */

    CEParser parsestate = null; // our parent parser

    Object lval = null;

    TextStream text = null;
    int charno = 0;
    //int lineno = 0; // not currently used
    StringBuilder yytext = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public CELexer(CEParser state)
    {
        this();
        setParser(state);
    }

    public CELexer()
    {
        this.text = new TextStream();
        yytext = new StringBuilder();
        lval = null;
    }

    /* Get/Set */

    void setParser(CEParser state)
    {
        this.parsestate = state;
    }

    void setText(String text)
    {
        this.text.setText(text);
    }


    public String getInput()
    {
        return text.getText();
    }

    /**
     * The Bison Lexer interface methods: yylex, getLval, yyerror.
     */

    /**
     * Entry point for the scanner.
     * Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * of the token.
     *
     * @return the token identifier corresponding to the next token.
     */

    public int
    yylex()
        throws ParseException
    {
        int token;
        int c = 0;
        token = 0;
        yytext.setLength(0);
        text.mark();

        token = -1;
        while(token < 0 && (c = text.read()) != EOS) {
            if(c <= ' ' || c == '\177') {
                /* whitespace: ignore */
            } else if(c == '"' || c == '\'') {
                int delim = c;
                boolean more = true;
                /* We have a string token; will be reported as STRINGCONST */
                while(more && (c = text.read()) > 0) {
                    switch (c) {
                    case EOS:
                        throw new ParseException("Unterminated character or string constant");
                    case '"':
                        more = (delim != c);
                        break;
                    case '\'':
                        more = (delim != c);
                        break;
                    case ESCAPE:
/* Suppress for now
            if(c >= '0' && c <= '9') {
			int decimal = 0;
		        do {
                            decimal = decimal * 10 + (c - '0');
                            c = text.read();
                        } while(c >= '0' && c <= '9');
                        c = decimal;
			text.backup();
                        break;
		    } else
*/
                        switch (c) {
                        case 'r':
                            c = '\r';
                            break;
                        case 'n':
                            c = '\n';
                            break;
                        case 'f':
                            c = '\f';
                            break;
                        case 't':
                            c = '\t';
                            break;
                        default:
                            break;
                        }
                        break;
                    default:
                        break;
                    }
                    if(more) yytext.append((char) c);
                }
                token = CEParser.Lexer.STRING;
            } else if(DELIMS.indexOf(c) >= 0) {
                // Single char delimiter
                yytext.append((char) c);
                token = c;
            } else { // Assume we have a word or integer
                yytext.append((char) c);
                while((c = text.read()) > 0) {
                    if(c <= ' ' || c == '\177') break; // whitespace
                    if(DELIMS.indexOf(c) >= 0) break;
                    if(c == ESCAPE) {
                        c = text.read();
                        if(c == EOS)
                            throw new ParseException("Unterminated backslash escape");
                    }
                    yytext.append((char) c);
                }
                // pushback the delimiter
                if(c != EOS) text.backup();
                try {// See if this looks like an integer
                    long num = Long.parseLong(yytext.toString());
                    token = CEParser.Lexer.LONG;
                } catch (NumberFormatException nfe) {
                    token = CEParser.Lexer.NAME;
                }
            }
        }
        if(c == EOS && token < 0) {
            token = 0;
            lval = null;
        } else {
            lval = (yytext.length() == 0 ? (String) null : yytext.toString());
        }
        if(parsestate.getDebugLevel() > 0)
            dumptoken(token, (String) lval);
        return token; // Return the type of the token
    }

    void
    dumptoken(int token, String lval)
        throws ParseException
    {
        String stoken;
        if(token < '\177')
            stoken = Character.toString((char) token);
        else
            switch (token) {
            case CEParser.Lexer.STRING:
                stoken = '"' + lval + '"';
                break;
            case CEParser.Lexer.LONG:
                stoken = lval;
                break;
            case CEParser.Lexer.DOUBLE:
                stoken = lval;
                break;
            case CEParser.Lexer.BOOLEAN:
                stoken = lval;
                break;
            case CEParser.Lexer.NAME:
                stoken = lval;
                break;
            default:
                stoken = "X" + Integer.toString(token);
            }
        System.err.println("TOKEN = |" + stoken + "|");

    }

    static int
    tohex(int c)
        throws ParseException
    {
        if(c >= 'a' && c <= 'f') return (c - 'a') + 0xa;
        if(c >= 'A' && c <= 'F') return (c - 'A') + 0xa;
        if(c >= '0' && c <= '9') return (c - '0');
        return -1;
    }

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * Part of Lexer interface.
     *
     * @return the semantic value of the last scanned token.
     */
    public Object getLVal()
    {
        return this.lval;
    }

    /**
     * Entry point for error reporting.  Emits an error
     * in a user-defined way.
     * Part of Lexer interface.
     *
     * @param s The string for the error message.
     */
    public void yyerror(String s)
    {
        System.err.println("CEParser.yyerror: " + s + "; parse failed at char: " + charno + "; near: ");
        String context = getInput();
        int show = (context.length() < CONTEXTLEN ? context.length() : CONTEXTLEN);
        System.err.println(context.substring(context.length() - show) + "^");
        new Exception().printStackTrace(System.err);
    }

}

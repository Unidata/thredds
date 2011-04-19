/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/*****************************************/
/* Note: Much of the naming is here to keep the oc parser
   parallel to this one
*/

package opendap.dap.parsers;

import static opendap.dap.parsers.DapParser.*;

import java.io.*;

class Daplex implements DapParser.Lexer
{

    static final boolean URLCVT = false;

    static final boolean NONSTDCVT = false;

    /* First character in DDS and DAS TOKEN_IDENTifier or number */
    String wordchars1 =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+_/%.\\*";
    String worddelims =
            "{}[]:;=,";
    String ddswordcharsn =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+_/%.\\*#";
    static String daswordcharsn =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+_/%.\\*:#";

    String wordcharsn = ddswordcharsn;

    /**
     * **********************************************
     */
    /* Hex digits */
    static String hexdigits = "0123456789abcdefABCDEF";

    static String[] keywords = new String[]{
            "alias",
            "array",
            "attributes",
            "byte",
            "code",
            "dataset",
            "error",
            "float32",
            "float64",
            "grid",
            "int16",
            "int32",
            "maps",
            "message",
            "program",
            "program_type",
            "sequence",
            "string",
            "structure",
            "uint16",
            "uint32",
            "url",
            null
    };

    static int[] keytokens = new int[]{
            SCAN_ALIAS,
            SCAN_ARRAY,
            SCAN_ATTR,
            SCAN_BYTE,
            SCAN_CODE,
            SCAN_DATASET,
            SCAN_ERROR,
            SCAN_FLOAT32,
            SCAN_FLOAT64,
            SCAN_GRID,
            SCAN_INT16,
            SCAN_INT32,
            SCAN_MAPS,
            SCAN_MESSAGE,
	    SCAN_PROG,
	    SCAN_PTYPE,
            SCAN_SEQUENCE,
            SCAN_STRING,
            SCAN_STRUCTURE,
            SCAN_UINT16,
            SCAN_UINT32,
            SCAN_URL
    };

    /**
     * **********************************************
     */
    /* Per-lexer state */

    Dapparse parsestate = null;
    InputStream stream = null;
    StringBuilder input = null; /*Accumulated InputStream */
    StringBuilder yytext = null;
    int lineno = 0;
    Object lval = null;
    StringBuilder lookahead = null;

    /**
     * *********************************************
     */

    /* Constructor(s) */
    public Daplex(Dapparse state)
    {
        reset(state);
    }

    /* Reset the lexer */

    public void reset(Dapparse state)
    {
        this.parsestate = state;
        this.stream = null;
        input = new StringBuilder();
        yytext = new StringBuilder();
        lineno = 0;
        lval = null;
        lookahead = new StringBuilder();
    }

    /* Get/Set */

    void setStream(InputStream stream) {this.stream = stream;}

    public String getInput()
    {
        return input.toString();
    }

    void
    dassetup()
            throws ParseException
    {
        wordcharsn = daswordcharsn;
    }

    /* Support lookahead */

    int
    peek() throws IOException
    {
        int c = read();
        pushback(c);
        return c;
    }

    void
    pushback(int c)
    {
        lookahead.insert(0, (char) c);
    }

    int
    read() throws IOException
    {
        int c;
        if (lookahead.length() == 0) {
            c = stream.read();
            if (c < 0) c = 0;
        } else {
            c = lookahead.charAt(0);
            lookahead.deleteCharAt(0);
        }
        return c;
    }

    /* This is part of the Lexer interface */

    public int
    yylex()
            throws ParseException
    {
        int token;
        int c;
        token = 0;
        yytext.setLength(0);

        try {

            token = -1;
            while (token < 0 && (c = read()) > 0) {
                if (c == '\n') {
                    lineno++;
                } else if (c <= ' ' || c == '\177') {
                    /* whitespace: ignore */
                } else if (c == '#') {
                    /* single line comment */
                    for (; ;) {
                        c = read();
                        if (c == '\n' || c == '\0') break;
                    }
                } else if (worddelims.indexOf(c) >= 0) {
                    token = c;
                } else if (c == '"') {
                    boolean more = true;
                    /* We have a string token; will be reported as SCAN_WORD */
                    while (more && (c = read()) > 0) {
                        if (NONSTDCVT) {
                            switch (c) {
                            case '"':
                                more = false;
                                break;
                            case '\\':
                                c = read();
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
                                case 'x': {
                                    int d1, d2;
                                    c = read();
                                    d1 = tohex(c);
                                    if (d1 < 0) {
                                        throw new ParseException("Illegal \\xDD in TOKEN_STRING");
                                    } else {
                                        c = read();
                                        d2 = tohex(c);
                                        if (d2 < 0) {
                                            throw new ParseException("Illegal \\xDD in TOKEN_STRING");
                                        } else {
                                            c = ((d1) << 4) | d2;
                                        }
                                    }
                                }
                                break;
                                default:
                                    break;
                                }
                                break;
                            default:
                                break;
                            }
                        } else {  /* Implement DAP2 standard */

                            if (c == '"')
                                more = false;
                            else if (c == '\\') {
                                c = read();
                                if (c < 0) more = false;
                            }
                        }
                        if (more) yytext.append((char) c);
                    }
                    token = SCAN_WORD;
                } else if (wordchars1.indexOf(c) >= 0) {
                    yytext.append((char) c);
                    /* we have a SCAN_WORD */
                    while ((c = read()) > 0) {
                        if (URLCVT) {
                            int c1 = read();
                            int c2 = read();
                            ;
                            if (c == '%' && c1 != '\0'
                                    && c2 != '\0'
                                    && hexdigits.indexOf(c1) >= 0
                                    && hexdigits.indexOf(c2) >= 0) {
                                int d1, d2;
                                d1 = tohex(c1);
                                d2 = tohex(c2);
                                if (d1 >= 0 || d2 >= 0) {
                                    c = ((d1) << 4) | d2;
                                }
                            } else {
                                pushback(c2);
                                pushback(c1);
                                if (wordcharsn.indexOf(c) < 0) {
                                    pushback(c);
                                    break;
                                }
                            }
                            yytext.append((char) c);
                        } else {
                            if (wordcharsn.indexOf(c) < 0) {
                                pushback(c);
                                break;
                            }
                            yytext.append((char) c);
                        } /*URLCVT*/
                    }
                    /* Special check for Data: */
                    String tmp = yytext.toString();
                    if ("Data".equals(tmp) && peek() == ':') {
                        yytext.append((char) read());
                        // also absorb any [\r\n]
                        c = read();
                        if(c == '\r') c = read();
                        if(c != '\n')
                            throw new ParseException("Malformed 'Data:' trailer");
                        token = SCAN_DATA;
                    } else {
                        token = SCAN_WORD; /* assume */
                        /* check for keyword */
                        for (int i = 0; ; i++) {
                            if (keywords[i] == null) break;
                            if (keywords[i].equalsIgnoreCase(tmp)) {
                                token = keytokens[i];
                                break;
                            }
                        }
                    }
                } else { /* illegal */
                    String msg = String.format("Illegal Character: '%c'", c);
                    yytext.append((char)c);
                    lexerror(msg);
                    throw new ParseException(msg);
                }
            }

            // do eof check
            if (token <= 0) {
                token = 0;
                lval = null;
            } else {
                lval = (yytext.length() == 0 ? (String) null : yytext.toString());
            }
            if (parsestate.getDebugLevel() > 0) dumptoken(token, (String) lval);

            return token;       /* Return the type of the token.  */

        } catch (IOException ioe) {
            throw new ParseException(ioe);
        }

    }

    void
    dumptoken(int token, String lval)
            throws ParseException
    {
        String stoken;
        if (token == SCAN_WORD)
            stoken = lval;
        else if (token < '\177')
            stoken = "" + (char) token;
        else switch (token) {
            case SCAN_ALIAS:
                stoken = "alias";
                break;
            case SCAN_ARRAY:
                stoken = "array";
                break;
            case SCAN_ATTR:
                stoken = "attributes";
                break;
            case SCAN_BYTE:
                stoken = "byte";
                break;
            case SCAN_DATASET:
                stoken = "dataset";
                break;
            case SCAN_FLOAT32:
                stoken = "float32";
                break;
            case SCAN_FLOAT64:
                stoken = "float64";
                break;
            case SCAN_GRID:
                stoken = "grid";
                break;
            case SCAN_INT16:
                stoken = "int16";
                break;
            case SCAN_INT32:
                stoken = "int32";
                break;
            case SCAN_MAPS:
                stoken = "maps";
                break;
            case SCAN_SEQUENCE:
                stoken = "sequence";
                break;
            case SCAN_STRING:
                stoken = "string";
                break;
            case SCAN_STRUCTURE:
                stoken = "structure";
                break;
            case SCAN_UINT16:
                stoken = "uint16";
                break;
            case SCAN_UINT32:
                stoken = "uint32";
                break;
            case SCAN_URL:
                stoken = "url";
                break;
            default:
                stoken = "X" + Integer.toString(token);
            }
        System.err.println("TOKEN = |" + stoken + "|");
        if (stoken.length() == 1) System.err.println("TOKEN = " + ((int) stoken.charAt(0)));

    }

    static int
    tohex(int c)
            throws ParseException
    {
        if (c >= 'a' && c <= 'f') return (c - 'a') + 0xa;
        if (c >= 'A' && c <= 'F') return (c - 'A') + 0xa;
        if (c >= '0' && c <= '9') return (c - '0');
        return -1;
    }

    /**************************************************/
    /* Lexer Interface */

    /**
     * Method to retrieve the semantic value of the last scanned token.
     *
     * @return the semantic value of the last scanned token.
     */
    public Object getLVal()
    {
        return this.lval;
    }

    /**
     * Entry point for the scanner.	 Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * of the token.
     * @return the token identifier corresponding to the next token. */
    // int yylex() throws ParseException
    // Defined above

    /**
     * Entry point for error reporting.  Emits an error
     * in a user-defined way.
     *
     * @param s The string for the error message.
     */
    public void yyerror(String s)
    {
        System.err.println("yyerror: line "
                + lineno
                + " near |"
                + yytext
                + "|; " + s);
    }

    public void lexerror(String msg)
    {
        StringBuilder nextline = new StringBuilder();
        int c = 0;
        try {
            for(int i=0;i<1024;i++) { // limit amount read
                if((c = read()) == -1) break;
                if (c == '\n') break;
                nextline.append((char) c);
            }
            if(c != -1) nextline.append("...");
        } catch (IOException ioe) {
        }
        ;
        System.out.printf("Lex error: %s; line: %d: %s^%s\n", msg, lineno, yytext, nextline);
    }


    String readallinput(InputStream is)
    {
        StringBuilder b = new StringBuilder();
        int c;
        try {
            while ((c = is.read()) > 0) b.append((char) c);
        } catch (IOException ioe) {
        }
        ;
        return b.toString();
    }
}

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
/* Note: Much of the naming is here to keep the
   netcdf/libncdap3 ce parser parallel to this one
*/

package opendap.dap.parser;

import opendap.dap.parser.CeParser.*;
import static opendap.dap.parser.CeParser.*;
import opendap.dap.parser.ParseException;

import java.io.*;


class Celex implements CeParser.Lexer, ExprParserConstants
{

    /* Define 1 and > 1st legal characters */
    static final String wordchars1 =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+_/%\\";
    static final String wordcharsn =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+_/%\\";
    static String worddelims = "{}[]:;=,&";

    /* Number characters */
    static final String numchars1 = "+-0123456789";
    static final String numcharsn = "Ee.+-0123456789";

    /**
     * **********************************************
     */
    /* Per-lexer state */

    Ceparse parsestate = null;
    Reader stream = null;
    StringBuilder input = null;
    StringBuilder yytext = null;
    int charno = 0;
    Object lval = null;
    StringBuilder lookahead = null;

    /**
     * *********************************************
     */

    /* Constructor(s) */
    public Celex(Ceparse state)
    {
        reset(state);
    }

    public void reset(Ceparse state)
    {
        this.parsestate = state;
        input = new StringBuilder(); /* InputStream so far */
        yytext = new StringBuilder();
        lookahead = new StringBuilder();
        lval = null;
        charno = 0;
	this.stream = null;
    }

    /* Get/Set */

    void setStream(StringReader stream) {this.stream = stream;}


    public String getInput()
    {
        return input.toString();
    }

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
        charno--;
    }

    int
    read() throws IOException
    {
        int c;
        if (lookahead.length() == 0) {
            c = stream.read();
            if (c < 0) c = 0;
            charno++;
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
        /* invariant: p always points to current char */

        try {
            token = -1;
            while (token < 0 && (c = read()) > 0) {
                if (c == '\n') {
                } else if (c <= ' ' || c == '\177') {
                    /* whitespace: ignore */
                } else if (worddelims.indexOf(c) >= 0) {
                    /* don't put in yytext to avoid memory leak */
                    token = c;
                } else if (c == '"') {
                    boolean more = true;
                    /* We have a string token; will be reported as SCAN_STRINGCONST */
                    while (more && (c = read()) > 0) {
                        if (c == '"')
                            more = false;
                        else if (c == '\\') {
                            c = read();
                            if (c < 0) more = false;
                        }
                        if(more) yytext.append((char) c);
                    }
                    token = SCAN_STRINGCONST;
                } else if (numchars1.indexOf(c) >= 0) {
                    /* we might have a SCAN_NUMBERCONST */
                    boolean isnumber = false;
                    yytext.append((char) c);
                    while ((c = read()) > 0) {
                        if (numcharsn.indexOf(c) < 0) {
                            pushback(c);
                            break;
                        }
                        yytext.append((char) c);
                    }
                    /* See if this is a number */
                    try {
                        Double number = new Double(yytext.toString());
                        isnumber = true;
                    } catch (NumberFormatException nfe) {
                        isnumber = false;
                    }
                    /* A number followed by an id char is assumed to just be a funny id */
                    if (isnumber) {
                        c = read();
                        if(wordcharsn.indexOf(c) >= 0) { // this is apparently just a funny id
                                token = SCAN_WORD;
                        } else {  // its really a number
                            token = SCAN_NUMBERCONST;
                            if(c != '\0') pushback(c);
                        }
                    } else {// !isNumber
                        /* Now, if the funny word has a "." in it,
                           we have to back up to that dot */
                        int dotpoint = yytext.toString().indexOf('.');
                        if (dotpoint >= 0) {
                            for (int i = 0; i < dotpoint; i++) pushback(yytext.charAt(i));
                            yytext.setLength(dotpoint);
                        }
                        token = SCAN_WORD;
                    }
                } else if (wordchars1.indexOf(c) >= 0) {
                    /* we have a WORD */
                    yytext.append((char) c);
                    while ((c = read()) > 0) {
                        if (wordcharsn.indexOf(c) < 0) {
                            pushback(c);
                            break;
                        }
                        yytext.append((char) c);
                    }
                    token = SCAN_WORD;
                } else {
                    /* we have a single char token */
                    token = c;
                }
            }
            if (token < 0) {
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
        switch (token) {
        case SCAN_STRINGCONST:
            System.err.printf("TOKEN = |\"%s\"|\n", lval);
            break;
        case SCAN_WORD:
        case SCAN_NUMBERCONST:
            System.err.printf("TOKEN = |%s|\n", lval);
            break;
        default:
            System.err.printf("TOKEN = |%c|\n", (char)token);
            break;
        }
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
            System.err.print("yyerror: " + s + "; char "+ charno);
            if(yytext.length() > 0)
                System.err.print(" near |"+ yytext + "|");
            System.err.println();
    }

    public void lexerror(String msg)
    {
        StringBuilder nextline = new StringBuilder();
        int c;
        try {
            while ((c = read()) != -1) {
                if (c == '\n') break;
                nextline.append((char) c);
            }
        } catch (IOException ioe) {
        }
        ;
        System.out.printf("Lex error: %s; charno: %d: %s^%s\n", msg, charno, yytext, nextline);
    }


}

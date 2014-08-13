/* A Bison parser, made by GNU Bison 3.0.  */

/* Skeleton implementation for Bison LALR(1) parsers in Java

   Copyright (C) 2007-2013 Free Software Foundation, Inc.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.

   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */

package dap4.core.dmr.parser;
/* First part of user declarations.  */

/* "Dap4ParserBody.java":37  */ /* lalr1.java:91  */

/* "Dap4ParserBody.java":39  */ /* lalr1.java:92  */
/* "%code imports" blocks.  */
/* "dap4.y":15  */ /* lalr1.java:93  */

import dap4.core.util.DapException;

/* "Dap4ParserBody.java":45  */ /* lalr1.java:93  */

/**
 * A Bison parser, automatically generated from <tt>dap4.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
abstract class Dap4ParserBody extends Dap4Actions
{
    /** Version number for the Bison executable that generated this parser.  */
  public static final String bisonVersion = "3.0";

  /** Name of the skeleton that generated this parser.  */
  public static final String bisonSkeleton = "lalr1.java";


  /**
   * True if verbose error messages are enabled.
   */
  private boolean yyErrorVerbose = true;

  /**
   * Return whether verbose error messages are enabled.
   */
  public final boolean getErrorVerbose() { return yyErrorVerbose; }

  /**
   * Set the verbosity of error messages.
   * @param verbose True to request verbose error messages.
   */
  public final void setErrorVerbose(boolean verbose)
  { yyErrorVerbose = verbose; }



  /**
   * A class defining a pair of positions.  Positions, defined by the
   * <code>Bison.Position</code> class, denote a point in the input.
   * Locations represent a part of the input through the beginning
   * and ending positions.
   */
  public class Location {
    /**
     * The first, inclusive, position in the range.
     */
    public Bison.Position begin;

    /**
     * The first position beyond the range.
     */
    public Bison.Position end;

    /**
     * Create a <code>Location</code> denoting an empty range located at
     * a given point.
     * @param loc The position at which the range is anchored.
     */
    public Location (Bison.Position loc) {
      this.begin = this.end = loc;
    }

    /**
     * Create a <code>Location</code> from the endpoints of the range.
     * @param begin The first position included in the range.
     * @param end   The first position beyond the range.
     */
    public Location (Bison.Position begin, Bison.Position end) {
      this.begin = begin;
      this.end = end;
    }

    /**
     * Print a representation of the location.  For this to be correct,
     * <code>Bison.Position</code> should override the <code>equals</code>
     * method.
     */
    public String toString () {
      if (begin.equals (end))
        return begin.toString ();
      else
        return begin.toString () + "-" + end.toString ();
    }
  }



  
  private Location yylloc (YYStack rhs, int n)
  {
    if (n > 0)
      return new Location (rhs.locationAt (n-1).begin, rhs.locationAt (0).end);
    else
      return new Location (rhs.locationAt (0).end);
  }

  /**
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>Dap4ParserBody</tt>.
   */
  public interface Lexer {
    /** Token returned by the scanner to signal the end of its input.  */
    public static final int EOF = 0;

/* Tokens.  */
    /** Token number,to be returned by the scanner.  */
    static final int DATASET_ = 258;
    /** Token number,to be returned by the scanner.  */
    static final int _DATASET = 259;
    /** Token number,to be returned by the scanner.  */
    static final int GROUP_ = 260;
    /** Token number,to be returned by the scanner.  */
    static final int _GROUP = 261;
    /** Token number,to be returned by the scanner.  */
    static final int ENUMERATION_ = 262;
    /** Token number,to be returned by the scanner.  */
    static final int _ENUMERATION = 263;
    /** Token number,to be returned by the scanner.  */
    static final int ENUMCONST_ = 264;
    /** Token number,to be returned by the scanner.  */
    static final int _ENUMCONST = 265;
    /** Token number,to be returned by the scanner.  */
    static final int NAMESPACE_ = 266;
    /** Token number,to be returned by the scanner.  */
    static final int _NAMESPACE = 267;
    /** Token number,to be returned by the scanner.  */
    static final int DIMENSION_ = 268;
    /** Token number,to be returned by the scanner.  */
    static final int _DIMENSION = 269;
    /** Token number,to be returned by the scanner.  */
    static final int DIM_ = 270;
    /** Token number,to be returned by the scanner.  */
    static final int _DIM = 271;
    /** Token number,to be returned by the scanner.  */
    static final int ENUM_ = 272;
    /** Token number,to be returned by the scanner.  */
    static final int _ENUM = 273;
    /** Token number,to be returned by the scanner.  */
    static final int MAP_ = 274;
    /** Token number,to be returned by the scanner.  */
    static final int _MAP = 275;
    /** Token number,to be returned by the scanner.  */
    static final int STRUCTURE_ = 276;
    /** Token number,to be returned by the scanner.  */
    static final int _STRUCTURE = 277;
    /** Token number,to be returned by the scanner.  */
    static final int SEQUENCE_ = 278;
    /** Token number,to be returned by the scanner.  */
    static final int _SEQUENCE = 279;
    /** Token number,to be returned by the scanner.  */
    static final int VALUE_ = 280;
    /** Token number,to be returned by the scanner.  */
    static final int _VALUE = 281;
    /** Token number,to be returned by the scanner.  */
    static final int ATTRIBUTE_ = 282;
    /** Token number,to be returned by the scanner.  */
    static final int _ATTRIBUTE = 283;
    /** Token number,to be returned by the scanner.  */
    static final int OTHERXML_ = 284;
    /** Token number,to be returned by the scanner.  */
    static final int _OTHERXML = 285;
    /** Token number,to be returned by the scanner.  */
    static final int ERROR_ = 286;
    /** Token number,to be returned by the scanner.  */
    static final int _ERROR = 287;
    /** Token number,to be returned by the scanner.  */
    static final int MESSAGE_ = 288;
    /** Token number,to be returned by the scanner.  */
    static final int _MESSAGE = 289;
    /** Token number,to be returned by the scanner.  */
    static final int CONTEXT_ = 290;
    /** Token number,to be returned by the scanner.  */
    static final int _CONTEXT = 291;
    /** Token number,to be returned by the scanner.  */
    static final int OTHERINFO_ = 292;
    /** Token number,to be returned by the scanner.  */
    static final int _OTHERINFO = 293;
    /** Token number,to be returned by the scanner.  */
    static final int CHAR_ = 294;
    /** Token number,to be returned by the scanner.  */
    static final int _CHAR = 295;
    /** Token number,to be returned by the scanner.  */
    static final int BYTE_ = 296;
    /** Token number,to be returned by the scanner.  */
    static final int _BYTE = 297;
    /** Token number,to be returned by the scanner.  */
    static final int INT8_ = 298;
    /** Token number,to be returned by the scanner.  */
    static final int _INT8 = 299;
    /** Token number,to be returned by the scanner.  */
    static final int UINT8_ = 300;
    /** Token number,to be returned by the scanner.  */
    static final int _UINT8 = 301;
    /** Token number,to be returned by the scanner.  */
    static final int INT16_ = 302;
    /** Token number,to be returned by the scanner.  */
    static final int _INT16 = 303;
    /** Token number,to be returned by the scanner.  */
    static final int UINT16_ = 304;
    /** Token number,to be returned by the scanner.  */
    static final int _UINT16 = 305;
    /** Token number,to be returned by the scanner.  */
    static final int INT32_ = 306;
    /** Token number,to be returned by the scanner.  */
    static final int _INT32 = 307;
    /** Token number,to be returned by the scanner.  */
    static final int UINT32_ = 308;
    /** Token number,to be returned by the scanner.  */
    static final int _UINT32 = 309;
    /** Token number,to be returned by the scanner.  */
    static final int INT64_ = 310;
    /** Token number,to be returned by the scanner.  */
    static final int _INT64 = 311;
    /** Token number,to be returned by the scanner.  */
    static final int UINT64_ = 312;
    /** Token number,to be returned by the scanner.  */
    static final int _UINT64 = 313;
    /** Token number,to be returned by the scanner.  */
    static final int FLOAT32_ = 314;
    /** Token number,to be returned by the scanner.  */
    static final int _FLOAT32 = 315;
    /** Token number,to be returned by the scanner.  */
    static final int FLOAT64_ = 316;
    /** Token number,to be returned by the scanner.  */
    static final int _FLOAT64 = 317;
    /** Token number,to be returned by the scanner.  */
    static final int STRING_ = 318;
    /** Token number,to be returned by the scanner.  */
    static final int _STRING = 319;
    /** Token number,to be returned by the scanner.  */
    static final int URL_ = 320;
    /** Token number,to be returned by the scanner.  */
    static final int _URL = 321;
    /** Token number,to be returned by the scanner.  */
    static final int OPAQUE_ = 322;
    /** Token number,to be returned by the scanner.  */
    static final int _OPAQUE = 323;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_BASE = 324;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_BASETYPE = 325;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_DAPVERSION = 326;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_DMRVERSION = 327;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_ENUM = 328;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_HREF = 329;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_NAME = 330;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_NAMESPACE = 331;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_NS = 332;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_SIZE = 333;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_TYPE = 334;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_VALUE = 335;
    /** Token number,to be returned by the scanner.  */
    static final int ATTR_HTTPCODE = 336;
    /** Token number,to be returned by the scanner.  */
    static final int TEXT = 337;
    /** Token number,to be returned by the scanner.  */
    static final int UNKNOWN_ATTR = 338;
    /** Token number,to be returned by the scanner.  */
    static final int UNKNOWN_ELEMENT_ = 339;
    /** Token number,to be returned by the scanner.  */
    static final int _UNKNOWN_ELEMENT = 340;


    /**
     * Method to retrieve the beginning position of the last scanned token.
     * @return the position at which the last scanned token starts.
     */
    Bison.Position getStartPos ();

    /**
     * Method to retrieve the ending position of the last scanned token.
     * @return the first position beyond the last scanned token.
     */
    Bison.Position getEndPos ();

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * @return the semantic value of the last scanned token.
     */
    Object getLVal ();

    /**
     * Entry point for the scanner.  Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * and beginning/ending positions of the token.
     * @return the token identifier corresponding to the next token.
     */
    int yylex () throws DapException;

    /**
     * Entry point for error reporting.  Emits an error
     * referring to the given location in a user-defined way.
     *
     * @param loc The location of the element to which the
     *                error message is related
     * @param msg The string for the error message.
     */
     void yyerror (Location loc, String msg);
  }

  private class YYLexer implements Lexer {
/* "%code lexer" blocks.  */
/* "dap4.y":19  */ /* lalr1.java:236  */

public Object getLVal() {return null;}
public int yylex() {return 0;}
public Bison.Position getStartPos() {return null;}
public Bison.Position getEndPos() {return null;}
public void yyerror(Location loc, String s)
{
System.err.println(s);
System.err.printf("near %s%n",getLocator());
}


/* "Dap4ParserBody.java":369  */ /* lalr1.java:236  */

  }

  /**
   * The object doing lexical analysis for us.
   */
  private Lexer yylexer;
  
  


  /**
   * Instantiates the Bison-generated parser.
   */
  public Dap4ParserBody () 
  {
    
    this.yylexer = new YYLexer();
    
  }


  /**
   * Instantiates the Bison-generated parser.
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  protected Dap4ParserBody (Lexer yylexer) 
  {
    
    this.yylexer = yylexer;
    
  }

  private java.io.PrintStream yyDebugStream = System.err;

  /**
   * Return the <tt>PrintStream</tt> on which the debugging output is
   * printed.
   */
  public final java.io.PrintStream getDebugStream () { return yyDebugStream; }

  /**
   * Set the <tt>PrintStream</tt> on which the debug output is printed.
   * @param s The stream that is used for debugging output.
   */
  public final void setDebugStream(java.io.PrintStream s) { yyDebugStream = s; }

  private int yydebug = 0;

  /**
   * Answer the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   */
  public final int getDebugLevel() { return yydebug; }

  /**
   * Set the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   * @param level The verbosity level for debugging output.
   */
  public final void setDebugLevel(int level) { yydebug = level; }

  /**
   * Print an error message via the lexer.
   * Use a <code>null</code> location.
   * @param msg The error message.
   */
  public final void yyerror (String msg)
  {
    yylexer.yyerror ((Location)null, msg);
  }

  /**
   * Print an error message via the lexer.
   * @param loc The location associated with the message.
   * @param msg The error message.
   */
  public final void yyerror (Location loc, String msg)
  {
    yylexer.yyerror (loc, msg);
  }

  /**
   * Print an error message via the lexer.
   * @param pos The position associated with the message.
   * @param msg The error message.
   */
  public final void yyerror (Bison.Position pos, String msg)
  {
    yylexer.yyerror (new Location (pos), msg);
  }

  protected final void yycdebug (String s) {
    if (yydebug > 0)
      yyDebugStream.println (s);
  }

  private final class YYStack {
    private int[] stateStack = new int[16];
    private Location[] locStack = new Location[16];
    private Object[] valueStack = new Object[16];

    public int size = 16;
    public int height = -1;

    public final void push (int state, Object value                            , Location loc) {
      height++;
      if (size == height)
        {
          int[] newStateStack = new int[size * 2];
          System.arraycopy (stateStack, 0, newStateStack, 0, height);
          stateStack = newStateStack;
          
          Location[] newLocStack = new Location[size * 2];
          System.arraycopy (locStack, 0, newLocStack, 0, height);
          locStack = newLocStack;

          Object[] newValueStack = new Object[size * 2];
          System.arraycopy (valueStack, 0, newValueStack, 0, height);
          valueStack = newValueStack;

          size *= 2;
        }

      stateStack[height] = state;
      locStack[height] = loc;
      valueStack[height] = value;
    }

    public final void pop () {
      pop (1);
    }

    public final void pop (int num) {
      // Avoid memory leaks... garbage collection is a white lie!
      if (num > 0) {
        java.util.Arrays.fill (valueStack, height - num + 1, height + 1, null);
        java.util.Arrays.fill (locStack, height - num + 1, height + 1, null);
      }
      height -= num;
    }

    public final int stateAt (int i) {
      return stateStack[height - i];
    }

    public final Location locationAt (int i) {
      return locStack[height - i];
    }

    public final Object valueAt (int i) {
      return valueStack[height - i];
    }

    // Print the state stack on the debug stream.
    public void print (java.io.PrintStream out)
    {
      out.print ("Stack now");

      for (int i = 0; i <= height; i++)
        {
          out.print (' ');
          out.print (stateStack[i]);
        }
      out.println ();
    }
  }

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return success (<tt>true</tt>).
   */
  public static final int YYACCEPT = 0;

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return failure (<tt>false</tt>).
   */
  public static final int YYABORT = 1;


  /**
   * Returned by a Bison action in order to request a new token.
   */
  public static final int YYPUSH_MORE = 4;

  /**
   * Returned by a Bison action in order to start error recovery without
   * printing an error message.
   */
  public static final int YYERROR = 2;

  /**
   * Internal return codes that are not supported for user semantic
   * actions.
   */
  private static final int YYERRLAB = 3;
  private static final int YYNEWSTATE = 4;
  private static final int YYDEFAULT = 5;
  private static final int YYREDUCE = 6;
  private static final int YYERRLAB1 = 7;
  private static final int YYRETURN = 8;
  private static final int YYGETTOKEN = 9; /* Signify that a new token is expected when doing push-parsing.  */

  private int yyerrstatus_ = 0;


    /* Lookahead and lookahead in internal form.  */
    int yychar = yyempty_;
    int yytoken = 0;

    /* State.  */
    int yyn = 0;
    int yylen = 0;
    int yystate = 0;
    YYStack yystack = new YYStack ();
    int label = YYNEWSTATE;

    /* Error handling.  */
    int yynerrs_ = 0;
    /* The location where the error started.  */
    Location yyerrloc = null;

    /* Location. */
    Location yylloc = new Location (null, null);

    /* Semantic value of the lookahead.  */
    Object yylval = null;

  /**
   * Return whether error recovery is being done.  In this state, the parser
   * reads token until it reaches a known state, and then restarts normal
   * operation.
   */
  public final boolean recovering ()
  {
    return yyerrstatus_ == 0;
  }

  private int yyaction (int yyn, YYStack yystack, int yylen) throws DapException
  {
    Object yyval;
    Location yyloc = yylloc (yystack, yylen);

    /* If YYLEN is nonzero, implement the default value of the action:
       '$$ = $1'.  Otherwise, use the top of the stack.

       Otherwise, the following line sets YYVAL to garbage.
       This behavior is undocumented and Bison
       users should not rely upon it.  */
    if (yylen > 0)
      yyval = yystack.valueAt (yylen - 1);
    else
      yyval = yystack.valueAt (0);

    yy_reduce_print (yyn, yystack);

    switch (yyn)
      {
          case 4:
  if (yyn == 4)
    /* "dap4.y":101  */ /* lalr1.java:476  */
    {enterdataset(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 5:
  if (yyn == 5)
    /* "dap4.y":104  */ /* lalr1.java:476  */
    {leavedataset();};
  break;
    

  case 6:
  if (yyn == 6)
    /* "dap4.y":110  */ /* lalr1.java:476  */
    {entergroup(((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 7:
  if (yyn == 7)
    /* "dap4.y":113  */ /* lalr1.java:476  */
    {leavegroup();};
  break;
    

  case 14:
  if (yyn == 14)
    /* "dap4.y":134  */ /* lalr1.java:476  */
    {enterenumdef(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 15:
  if (yyn == 15)
    /* "dap4.y":137  */ /* lalr1.java:476  */
    {leaveenumdef();};
  break;
    

  case 18:
  if (yyn == 18)
    /* "dap4.y":147  */ /* lalr1.java:476  */
    {enumconst(((SaxEvent)(yystack.valueAt (4-(2)))),((SaxEvent)(yystack.valueAt (4-(3)))));};
  break;
    

  case 19:
  if (yyn == 19)
    /* "dap4.y":149  */ /* lalr1.java:476  */
    {enumconst(((SaxEvent)(yystack.valueAt (4-(3)))),((SaxEvent)(yystack.valueAt (4-(2)))));};
  break;
    

  case 20:
  if (yyn == 20)
    /* "dap4.y":155  */ /* lalr1.java:476  */
    {enterdimdef(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 21:
  if (yyn == 21)
    /* "dap4.y":158  */ /* lalr1.java:476  */
    {leavedimdef();};
  break;
    

  case 22:
  if (yyn == 22)
    /* "dap4.y":163  */ /* lalr1.java:476  */
    {dimref(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 23:
  if (yyn == 23)
    /* "dap4.y":165  */ /* lalr1.java:476  */
    {dimref(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 28:
  if (yyn == 28)
    /* "dap4.y":179  */ /* lalr1.java:476  */
    {enteratomicvariable(((SaxEvent)(yystack.valueAt (2-(1)))),((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 29:
  if (yyn == 29)
    /* "dap4.y":182  */ /* lalr1.java:476  */
    {leaveatomicvariable(((SaxEvent)(yystack.valueAt (5-(5)))));};
  break;
    

  case 30:
  if (yyn == 30)
    /* "dap4.y":189  */ /* lalr1.java:476  */
    {enterenumvariable(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 31:
  if (yyn == 31)
    /* "dap4.y":192  */ /* lalr1.java:476  */
    {leaveenumvariable(((SaxEvent)(yystack.valueAt (5-(5)))));};
  break;
    

  case 32:
  if (yyn == 32)
    /* "dap4.y":198  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 33:
  if (yyn == 33)
    /* "dap4.y":199  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 34:
  if (yyn == 34)
    /* "dap4.y":200  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 35:
  if (yyn == 35)
    /* "dap4.y":201  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 36:
  if (yyn == 36)
    /* "dap4.y":202  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 37:
  if (yyn == 37)
    /* "dap4.y":203  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 38:
  if (yyn == 38)
    /* "dap4.y":204  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 39:
  if (yyn == 39)
    /* "dap4.y":205  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 40:
  if (yyn == 40)
    /* "dap4.y":206  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 41:
  if (yyn == 41)
    /* "dap4.y":207  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 42:
  if (yyn == 42)
    /* "dap4.y":208  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 43:
  if (yyn == 43)
    /* "dap4.y":209  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 44:
  if (yyn == 44)
    /* "dap4.y":210  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 45:
  if (yyn == 45)
    /* "dap4.y":211  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 46:
  if (yyn == 46)
    /* "dap4.y":212  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 47:
  if (yyn == 47)
    /* "dap4.y":216  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 48:
  if (yyn == 48)
    /* "dap4.y":217  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 49:
  if (yyn == 49)
    /* "dap4.y":218  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 50:
  if (yyn == 50)
    /* "dap4.y":219  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 51:
  if (yyn == 51)
    /* "dap4.y":220  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 52:
  if (yyn == 52)
    /* "dap4.y":221  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 53:
  if (yyn == 53)
    /* "dap4.y":222  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 54:
  if (yyn == 54)
    /* "dap4.y":223  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 55:
  if (yyn == 55)
    /* "dap4.y":224  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 56:
  if (yyn == 56)
    /* "dap4.y":225  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 57:
  if (yyn == 57)
    /* "dap4.y":226  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 58:
  if (yyn == 58)
    /* "dap4.y":227  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 59:
  if (yyn == 59)
    /* "dap4.y":228  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 60:
  if (yyn == 60)
    /* "dap4.y":229  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 61:
  if (yyn == 61)
    /* "dap4.y":230  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 62:
  if (yyn == 62)
    /* "dap4.y":231  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 67:
  if (yyn == 67)
    /* "dap4.y":244  */ /* lalr1.java:476  */
    {entermap(((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 68:
  if (yyn == 68)
    /* "dap4.y":247  */ /* lalr1.java:476  */
    {leavemap();};
  break;
    

  case 69:
  if (yyn == 69)
    /* "dap4.y":253  */ /* lalr1.java:476  */
    {enterstructurevariable(((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 70:
  if (yyn == 70)
    /* "dap4.y":256  */ /* lalr1.java:476  */
    {leavestructurevariable(((SaxEvent)(yystack.valueAt (5-(5)))));};
  break;
    

  case 76:
  if (yyn == 76)
    /* "dap4.y":270  */ /* lalr1.java:476  */
    {entersequencevariable(((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 77:
  if (yyn == 77)
    /* "dap4.y":273  */ /* lalr1.java:476  */
    {leavesequencevariable(((SaxEvent)(yystack.valueAt (5-(5)))));};
  break;
    

  case 89:
  if (yyn == 89)
    /* "dap4.y":305  */ /* lalr1.java:476  */
    {enteratomicattribute(((XMLAttributeMap)(yystack.valueAt (3-(2)))),((NamespaceList)(yystack.valueAt (3-(3)))));};
  break;
    

  case 90:
  if (yyn == 90)
    /* "dap4.y":308  */ /* lalr1.java:476  */
    {leaveatomicattribute();};
  break;
    

  case 91:
  if (yyn == 91)
    /* "dap4.y":313  */ /* lalr1.java:476  */
    {enteratomicattribute(((XMLAttributeMap)(yystack.valueAt (3-(2)))),((NamespaceList)(yystack.valueAt (3-(3)))));};
  break;
    

  case 92:
  if (yyn == 92)
    /* "dap4.y":315  */ /* lalr1.java:476  */
    {leaveatomicattribute();};
  break;
    

  case 93:
  if (yyn == 93)
    /* "dap4.y":320  */ /* lalr1.java:476  */
    {yyval=namespace_list();};
  break;
    

  case 94:
  if (yyn == 94)
    /* "dap4.y":322  */ /* lalr1.java:476  */
    {yyval=namespace_list(((NamespaceList)(yystack.valueAt (2-(1)))),((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 95:
  if (yyn == 95)
    /* "dap4.y":329  */ /* lalr1.java:476  */
    {yyval=(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 96:
  if (yyn == 96)
    /* "dap4.y":336  */ /* lalr1.java:476  */
    {entercontainerattribute(((XMLAttributeMap)(yystack.valueAt (3-(2)))),((NamespaceList)(yystack.valueAt (3-(3)))));};
  break;
    

  case 97:
  if (yyn == 97)
    /* "dap4.y":339  */ /* lalr1.java:476  */
    {leavecontainerattribute();};
  break;
    

  case 102:
  if (yyn == 102)
    /* "dap4.y":356  */ /* lalr1.java:476  */
    {value(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 103:
  if (yyn == 103)
    /* "dap4.y":358  */ /* lalr1.java:476  */
    {value(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 104:
  if (yyn == 104)
    /* "dap4.y":364  */ /* lalr1.java:476  */
    {enterotherxml(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 105:
  if (yyn == 105)
    /* "dap4.y":367  */ /* lalr1.java:476  */
    {leaveotherxml();};
  break;
    

  case 108:
  if (yyn == 108)
    /* "dap4.y":378  */ /* lalr1.java:476  */
    {enterxmlelement(((SaxEvent)(yystack.valueAt (2-(1)))),((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 109:
  if (yyn == 109)
    /* "dap4.y":381  */ /* lalr1.java:476  */
    {leavexmlelement(((SaxEvent)(yystack.valueAt (5-(5)))));};
  break;
    

  case 110:
  if (yyn == 110)
    /* "dap4.y":383  */ /* lalr1.java:476  */
    {xmltext(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 111:
  if (yyn == 111)
    /* "dap4.y":391  */ /* lalr1.java:476  */
    {yyval=xml_attribute_map();};
  break;
    

  case 112:
  if (yyn == 112)
    /* "dap4.y":393  */ /* lalr1.java:476  */
    {yyval=xml_attribute_map(((XMLAttributeMap)(yystack.valueAt (2-(1)))),((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 186:
  if (yyn == 186)
    /* "dap4.y":486  */ /* lalr1.java:476  */
    {entererror(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 187:
  if (yyn == 187)
    /* "dap4.y":490  */ /* lalr1.java:476  */
    {leaveerror();};
  break;
    

  case 190:
  if (yyn == 190)
    /* "dap4.y":500  */ /* lalr1.java:476  */
    {errormessage(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 191:
  if (yyn == 191)
    /* "dap4.y":502  */ /* lalr1.java:476  */
    {errorcontext(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 192:
  if (yyn == 192)
    /* "dap4.y":504  */ /* lalr1.java:476  */
    {errorotherinfo(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    


/* "Dap4ParserBody.java":1162  */ /* lalr1.java:476  */
        default: break;
      }

    yy_symbol_print ("-> $$ =", yyr1_[yyn], yyval, yyloc);

    yystack.pop (yylen);
    yylen = 0;

    /* Shift the result of the reduction.  */
    yyn = yyr1_[yyn];
    int yystate = yypgoto_[yyn - yyntokens_] + yystack.stateAt (0);
    if (0 <= yystate && yystate <= yylast_
        && yycheck_[yystate] == yystack.stateAt (0))
      yystate = yytable_[yystate];
    else
      yystate = yydefgoto_[yyn - yyntokens_];

    yystack.push (yystate, yyval, yyloc);
    return YYNEWSTATE;
  }


  /* Return YYSTR after stripping away unnecessary quotes and
     backslashes, so that it's suitable for yyerror.  The heuristic is
     that double-quoting is unnecessary unless the string contains an
     apostrophe, a comma, or backslash (other than backslash-backslash).
     YYSTR is taken from yytname.  */
  private final String yytnamerr_ (String yystr)
  {
    if (yystr.charAt (0) == '"')
      {
        StringBuffer yyr = new StringBuffer ();
        strip_quotes: for (int i = 1; i < yystr.length (); i++)
          switch (yystr.charAt (i))
            {
            case '\'':
            case ',':
              break strip_quotes;

            case '\\':
              if (yystr.charAt(++i) != '\\')
                break strip_quotes;
              /* Fall through.  */
            default:
              yyr.append (yystr.charAt (i));
              break;

            case '"':
              return yyr.toString ();
            }
      }
    else if (yystr.equals ("$end"))
      return "end of input";

    return yystr;
  }


  /*--------------------------------.
  | Print this symbol on YYOUTPUT.  |
  `--------------------------------*/

  private void yy_symbol_print (String s, int yytype,
                                 Object yyvaluep                                 , Object yylocationp)
  {
    if (yydebug > 0)
    yycdebug (s + (yytype < yyntokens_ ? " token " : " nterm ")
              + yytname_[yytype] + " ("
              + yylocationp + ": "
              + (yyvaluep == null ? "(null)" : yyvaluep.toString ()) + ")");
  }



  /**
   * Push Parse input from external lexer
   *
   * @param yylextoken current token
   * @param yylexval current lval
   * @param yylexloc current position
   *
   * @return <tt>YYACCEPT, YYABORT, YYPUSH_MORE</tt>
   */
  public int push_parse (int yylextoken, Object yylexval, Location yylexloc)
      throws DapException, DapException
  {
    /* @$.  */
    Location yyloc;


    if (!this.push_parse_initialized)
      {
        push_parse_initialize ();

        yycdebug ("Starting parse\n");
        yyerrstatus_ = 0;
      } else
        label = YYGETTOKEN;

    boolean push_token_consumed = true;

    for (;;)
      switch (label)
      {
        /* New state.  Unlike in the C/C++ skeletons, the state is already
           pushed when we come here.  */
      case YYNEWSTATE:
        yycdebug ("Entering state " + yystate + "\n");
        if (yydebug > 0)
          yystack.print (yyDebugStream);

        /* Accept?  */
        if (yystate == yyfinal_)
          {label = YYACCEPT; break;}

        /* Take a decision.  First try without lookahead.  */
        yyn = yypact_[yystate];
        if (yy_pact_value_is_default_ (yyn))
          {
            label = YYDEFAULT;
            break;
          }
        /* Fall Through */

      case YYGETTOKEN:
        /* Read a lookahead token.  */
        if (yychar == yyempty_)
          {

            if (!push_token_consumed)
              return YYPUSH_MORE;
            yycdebug ("Reading a token: ");
            yychar = yylextoken;
            yylval = yylexval;
            yylloc = yylexloc;
            push_token_consumed = false;

          }

        /* Convert token to internal form.  */
        if (yychar <= Lexer.EOF)
          {
            yychar = yytoken = Lexer.EOF;
            yycdebug ("Now at end of input.\n");
          }
        else
          {
            yytoken = yytranslate_ (yychar);
            yy_symbol_print ("Next token is", yytoken,
                             yylval, yylloc);
          }

        /* If the proper action on seeing token YYTOKEN is to reduce or to
           detect an error, take that action.  */
        yyn += yytoken;
        if (yyn < 0 || yylast_ < yyn || yycheck_[yyn] != yytoken)
          label = YYDEFAULT;

        /* <= 0 means reduce or error.  */
        else if ((yyn = yytable_[yyn]) <= 0)
          {
            if (yy_table_value_is_error_ (yyn))
              label = YYERRLAB;
            else
              {
                yyn = -yyn;
                label = YYREDUCE;
              }
          }

        else
          {
            /* Shift the lookahead token.  */
            yy_symbol_print ("Shifting", yytoken,
                             yylval, yylloc);

            /* Discard the token being shifted.  */
            yychar = yyempty_;

            /* Count tokens shifted since error; after three, turn off error
               status.  */
            if (yyerrstatus_ > 0)
              --yyerrstatus_;

            yystate = yyn;
            yystack.push (yystate, yylval, yylloc);
            label = YYNEWSTATE;
          }
        break;

      /*-----------------------------------------------------------.
      | yydefault -- do the default action for the current state.  |
      `-----------------------------------------------------------*/
      case YYDEFAULT:
        yyn = yydefact_[yystate];
        if (yyn == 0)
          label = YYERRLAB;
        else
          label = YYREDUCE;
        break;

      /*-----------------------------.
      | yyreduce -- Do a reduction.  |
      `-----------------------------*/
      case YYREDUCE:
        yylen = yyr2_[yyn];
        label = yyaction (yyn, yystack, yylen);
        yystate = yystack.stateAt (0);
        break;

      /*------------------------------------.
      | yyerrlab -- here on detecting error |
      `------------------------------------*/
      case YYERRLAB:
        /* If not already recovering from an error, report this error.  */
        if (yyerrstatus_ == 0)
          {
            ++yynerrs_;
            if (yychar == yyempty_)
              yytoken = yyempty_;
            yyerror (yylloc, yysyntax_error (yystate, yytoken));
          }

        yyerrloc = yylloc;
        if (yyerrstatus_ == 3)
          {
        /* If just tried and failed to reuse lookahead token after an
         error, discard it.  */

        if (yychar <= Lexer.EOF)
          {
          /* Return failure if at end of input.  */
          if (yychar == Lexer.EOF)
            {label = YYABORT; break;}
          }
        else
            yychar = yyempty_;
          }

        /* Else will try to reuse lookahead token after shifting the error
           token.  */
        label = YYERRLAB1;
        break;

      /*-------------------------------------------------.
      | errorlab -- error raised explicitly by YYERROR.  |
      `-------------------------------------------------*/
      case YYERROR:

        yyerrloc = yystack.locationAt (yylen - 1);
        /* Do not reclaim the symbols of the rule which action triggered
           this YYERROR.  */
        yystack.pop (yylen);
        yylen = 0;
        yystate = yystack.stateAt (0);
        label = YYERRLAB1;
        break;

      /*-------------------------------------------------------------.
      | yyerrlab1 -- common code for both syntax error and YYERROR.  |
      `-------------------------------------------------------------*/
      case YYERRLAB1:
        yyerrstatus_ = 3;       /* Each real token shifted decrements this.  */

        for (;;)
          {
            yyn = yypact_[yystate];
            if (!yy_pact_value_is_default_ (yyn))
              {
                yyn += yyterror_;
                if (0 <= yyn && yyn <= yylast_ && yycheck_[yyn] == yyterror_)
                  {
                    yyn = yytable_[yyn];
                    if (0 < yyn)
                      break;
                  }
              }

            /* Pop the current state because it cannot handle the
             * error token.  */
            if (yystack.height == 0)
              {label = YYABORT; break;}

            yyerrloc = yystack.locationAt (0);
            yystack.pop ();
            yystate = yystack.stateAt (0);
            if (yydebug > 0)
              yystack.print (yyDebugStream);
          }

        if (label == YYABORT)
            /* Leave the switch.  */
            break;


        /* Muck with the stack to setup for yylloc.  */
        yystack.push (0, null, yylloc);
        yystack.push (0, null, yyerrloc);
        yyloc = yylloc (yystack, 2);
        yystack.pop (2);

        /* Shift the error token.  */
        yy_symbol_print ("Shifting", yystos_[yyn],
                         yylval, yyloc);

        yystate = yyn;
        yystack.push (yyn, yylval, yyloc);
        label = YYNEWSTATE;
        break;

        /* Accept.  */
      case YYACCEPT:
        this.push_parse_initialized = false; return YYACCEPT;

        /* Abort.  */
      case YYABORT:
        this.push_parse_initialized = false; return YYABORT;
      }
}

  boolean push_parse_initialized = false;

    /**
     * (Re-)Initialize the state of the push parser.
     */
  public void push_parse_initialize()
  {
    /* Lookahead and lookahead in internal form.  */
    this.yychar = yyempty_;
    this.yytoken = 0;

    /* State.  */
    this.yyn = 0;
    this.yylen = 0;
    this.yystate = 0;
    this.yystack = new YYStack ();
    this.label = YYNEWSTATE;

    /* Error handling.  */
    this.yynerrs_ = 0;
    /* The location where the error started.  */
    this.yyerrloc = null;
    this.yylloc = new Location (null, null);

    /* Semantic value of the lookahead.  */
    this.yylval = null;

    yystack.push (this.yystate, this.yylval, this.yylloc);

    this.push_parse_initialized = true;

  }

  /**
   * Push parse given input from an external lexer.
   *
   * @param yylextoken current token
   * @param yylexval current lval
   * @param yylexpos current position
   *
   * @return <tt>YYACCEPT, YYABORT, YYPUSH_MORE</tt>
   */
  public int push_parse (int yylextoken, Object yylexval, Bison.Position yylexpos)
      throws DapException, DapException
  {
    return push_parse (yylextoken, yylexval, new Location (yylexpos));
  }




  // Generate an error message.
  private String yysyntax_error (int yystate, int tok)
  {
    if (yyErrorVerbose)
      {
        /* There are many possibilities here to consider:
           - If this state is a consistent state with a default action,
             then the only way this function was invoked is if the
             default action is an error action.  In that case, don't
             check for expected tokens because there are none.
           - The only way there can be no lookahead present (in tok) is
             if this state is a consistent state with a default action.
             Thus, detecting the absence of a lookahead is sufficient to
             determine that there is no unexpected or expected token to
             report.  In that case, just report a simple "syntax error".
           - Don't assume there isn't a lookahead just because this
             state is a consistent state with a default action.  There
             might have been a previous inconsistent state, consistent
             state with a non-default action, or user semantic action
             that manipulated yychar.  (However, yychar is currently out
             of scope during semantic actions.)
           - Of course, the expected token list depends on states to
             have correct lookahead information, and it depends on the
             parser not to perform extra reductions after fetching a
             lookahead from the scanner and before detecting a syntax
             error.  Thus, state merging (from LALR or IELR) and default
             reductions corrupt the expected token list.  However, the
             list is correct for canonical LR with one exception: it
             will still contain any token that will not be accepted due
             to an error action in a later state.
        */
        if (tok != yyempty_)
          {
            /* FIXME: This method of building the message is not compatible
               with internationalization.  */
            StringBuffer res =
              new StringBuffer ("syntax error, unexpected ");
            res.append (yytnamerr_ (yytname_[tok]));
            int yyn = yypact_[yystate];
            if (!yy_pact_value_is_default_ (yyn))
              {
                /* Start YYX at -YYN if negative to avoid negative
                   indexes in YYCHECK.  In other words, skip the first
                   -YYN actions for this state because they are default
                   actions.  */
                int yyxbegin = yyn < 0 ? -yyn : 0;
                /* Stay within bounds of both yycheck and yytname.  */
                int yychecklim = yylast_ - yyn + 1;
                int yyxend = yychecklim < yyntokens_ ? yychecklim : yyntokens_;
                int count = 0;
                for (int x = yyxbegin; x < yyxend; ++x)
                  if (yycheck_[x + yyn] == x && x != yyterror_
                      && !yy_table_value_is_error_ (yytable_[x + yyn]))
                    ++count;
                if (count < 5)
                  {
                    count = 0;
                    for (int x = yyxbegin; x < yyxend; ++x)
                      if (yycheck_[x + yyn] == x && x != yyterror_
                          && !yy_table_value_is_error_ (yytable_[x + yyn]))
                        {
                          res.append (count++ == 0 ? ", expecting " : " or ");
                          res.append (yytnamerr_ (yytname_[x]));
                        }
                  }
              }
            return res.toString ();
          }
      }

    return "syntax error";
  }

  /**
   * Whether the given <code>yypact_</code> value indicates a defaulted state.
   * @param yyvalue   the value to check
   */
  private static boolean yy_pact_value_is_default_ (int yyvalue)
  {
    return yyvalue == yypact_ninf_;
  }

  /**
   * Whether the given <code>yytable_</code>
   * value indicates a syntax error.
   * @param yyvalue the value to check
   */
  private static boolean yy_table_value_is_error_ (int yyvalue)
  {
    return yyvalue == yytable_ninf_;
  }

  private static final short yypact_ninf_ = -134;
  private static final short yytable_ninf_ = -92;

  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   STATE-NUM.  */
  private static final short yypact_[] = yypact_init();
  private static final short[] yypact_init()
  {
    return new short[]
    {
       3,  -134,  -134,    26,  -134,  -134,   -60,   -60,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,   237,    43,  -134,    53,  -134,
    -134,  -134,    55,    61,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,    65,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,    50,    52,    60,  -134,
    -134,   -60,   -60,   -60,  -134,  -134,   -60,   -60,  -134,   110,
     112,   108,  -134,   141,  -134,  -134,  -134,  -134,    -4,   173,
    -134,  -134,  -134,  -134,   246,   -50,   104,  -134,     8,    96,
     313,   358,    78,   129,   133,  -134,    90,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,   126,  -134,
    -134,   297,  -134,    82,    88,  -134,  -134,  -134,  -134,   -39,
    -134,    89,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,   154,    38,    49,  -134,  -134,
    -134,    14,  -134,  -134,   -60,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,   158,   160,   156,   179,  -134,  -134,   148,   171,
    -134,  -134,  -134,  -134,   173,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,    42,    11,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,  -134,  -134,  -134,  -134
    };
  }

/* YYDEFACT[STATE-NUM] -- Default reduction number in state STATE-NUM.
   Performed when YYTABLE does not specify something else to do.  Zero
   means the default is an error.  */
  private static final short yydefact_[] = yydefact_init();
  private static final short[] yydefact_init()
  {
    return new short[]
    {
       0,   111,   111,     0,     2,     3,     4,   186,     1,   113,
     114,   115,   116,   117,   118,   119,   120,   121,   122,   123,
     124,   125,     8,   112,   188,     0,     0,     5,     0,   111,
     111,   111,     0,     0,   111,   111,    32,    33,    34,    35,
      36,    37,    38,    39,    40,    41,    42,    43,    44,    45,
      46,    13,    10,     9,    11,    24,    25,     0,    26,    27,
      12,    85,    86,    87,    88,   187,     0,     0,     0,   189,
       6,    14,    20,    30,    69,    76,    93,   104,    28,     0,
       0,     0,     8,     0,    83,    63,    71,    78,    96,     0,
      63,   190,   191,   192,     0,     0,     0,    16,     0,     0,
       0,     0,     0,     0,     0,    94,     0,   126,   127,   128,
     129,   130,   131,   132,   133,   134,   135,   136,   137,   138,
     139,   140,   141,   142,   143,   144,   145,   146,   147,   148,
     149,   150,   151,   152,   153,   154,   110,   155,     0,   106,
     111,     0,     7,     0,     0,    15,    17,    21,    84,     0,
      31,     0,    64,    65,    66,    70,    72,    73,    74,    75,
      77,    79,    80,    81,    82,     0,     0,     0,   100,    92,
      98,     0,   105,   107,   108,    62,    47,    48,    49,    50,
      51,    52,    53,    54,    55,    56,    57,    58,    59,    60,
      61,    29,     0,     0,     0,     0,    67,    95,     0,     0,
      90,   101,    97,    99,     0,    18,    19,    22,    23,    83,
     103,   102,     0,     0,   156,   157,   158,   159,   160,   161,
     162,   163,   164,   165,   166,   167,   168,   169,   170,   171,
     172,   173,   174,   175,   176,   177,   178,   179,   180,   181,
     182,   183,   184,   185,   109,    68
    };
  }

/* YYPGOTO[NTERM-NUM].  */
  private static final short yypgoto_[] = yypgoto_init();
  private static final short[] yypgoto_init()
  {
    return new short[]
    {
    -134,  -134,  -134,  -134,  -134,  -134,   117,  -134,  -134,  -134,
     105,  -134,  -134,    21,    57,  -134,  -134,  -134,  -134,  -134,
    -134,   113,    59,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
      -5,   -97,   -98,  -134,  -134,  -134,  -134,  -134,  -134,  -134,
    -134,  -134,    39,  -134,  -134,     1,  -133,  -134,    -2,  -134,
    -134,  -134,  -134,  -134,  -134,  -134
    };
  }

/* YYDEFGOTO[NTERM-NUM].  */
  private static final short yydefgoto_[] = yydefgoto_init();
  private static final short[] yydefgoto_init()
  {
    return new short[]
    {
      -1,     3,     4,    22,    51,    82,    25,    52,    83,    96,
      97,    53,    84,   152,    54,    55,    90,    56,    85,    57,
     191,    99,   153,   209,    58,    86,   100,    59,    87,   101,
      98,    60,    61,    62,   103,   104,    88,   105,    63,   106,
     171,   167,   168,    64,    89,   138,   139,   204,     6,    23,
     140,   244,     5,    24,    26,    69
    };
  }

/* YYTABLE[YYPACT[STATE-NUM]] -- What to do in state STATE-NUM.  If
   positive, shift that token.  If negative, reduce the rule whose
   number is the opposite.  If YYTABLE_NINF, syntax error.  */
  private static final short yytable_[] = yytable_init();
  private static final short[] yytable_init()
  {
    return new short[]
    {
       7,   148,   154,   159,   164,   173,     1,   102,   170,     9,
      10,    11,    12,    13,    14,    15,    16,    17,    18,    19,
      20,   -89,   147,    21,   -91,   143,     8,    71,    72,    73,
     144,   245,    76,    77,     2,    34,   194,    35,    34,   195,
      35,    34,   202,    35,   154,   107,   214,   108,   215,   109,
     216,   110,   217,   111,   218,   112,   219,   113,   220,   114,
     221,   115,   222,   116,   223,   117,   224,   118,   225,   119,
     226,   120,   227,   203,   166,    65,    66,   200,    67,   173,
      68,   121,   228,   122,   229,   123,   230,   124,   231,   125,
     232,   126,   233,   127,   234,   128,   235,   129,   236,   130,
     237,   131,   238,   132,   239,   133,   240,   134,   241,   135,
     242,   149,   145,    95,   150,   151,   148,    34,   198,    35,
     199,   156,   161,    34,   136,    35,   137,   243,    70,   107,
      74,   108,    79,   109,    80,   110,    75,   111,   174,   112,
      78,   113,    81,   114,    91,   115,    93,   116,    92,   117,
      95,   118,   165,   119,   166,   120,   172,   157,   162,   158,
     163,   169,   192,   193,   196,   121,   197,   122,   205,   123,
     206,   124,   207,   125,   210,   126,   107,   127,   108,   128,
     109,   129,   110,   130,   111,   131,   112,   132,   113,   133,
     114,   134,   115,   135,   116,   208,   117,   211,   118,    94,
     119,   146,   120,   141,   213,   212,   201,     0,   136,     0,
     137,     0,   121,     0,   122,     0,   123,     0,   124,     0,
     125,     0,   126,     0,   127,     0,   128,     0,   129,     0,
     130,     0,   131,     0,   132,     0,   133,     0,   134,     0,
     135,    27,    28,     0,    29,     0,     0,     0,     0,     0,
      30,    28,   142,    29,    31,   136,     0,   137,    32,    30,
      33,     0,     0,    31,    34,     0,    35,    32,     0,    33,
       0,     0,     0,    34,     0,    35,    36,     0,    37,     0,
      38,     0,    39,     0,    40,    36,    41,    37,    42,    38,
      43,    39,    44,    40,    45,    41,    46,    42,    47,    43,
      48,    44,    49,    45,    50,    46,     0,    47,     0,    48,
       0,    49,   149,    50,     0,   175,   151,     0,     0,     0,
       0,     0,     0,     0,    34,     0,    35,     0,   149,     0,
      31,     0,   151,     0,    32,   155,    33,   176,     0,   177,
      34,   178,    35,   179,     0,   180,     0,   181,     0,   182,
       0,   183,    36,   184,    37,   185,    38,   186,    39,   187,
      40,   188,    41,   189,    42,   190,    43,     0,    44,     0,
      45,     0,    46,   149,    47,    31,    48,   151,    49,    32,
      50,    33,   160,     0,     0,    34,     0,    35,     0,     0,
       0,     0,     0,     0,     0,     0,     0,    36,     0,    37,
       0,    38,     0,    39,     0,    40,     0,    41,     0,    42,
       0,    43,     0,    44,     0,    45,     0,    46,     0,    47,
       0,    48,     0,    49,     0,    50
    };
  }

private static final short yycheck_[] = yycheck_init();
  private static final short[] yycheck_init()
  {
    return new short[]
    {
       2,    98,    99,   100,   101,   138,     3,    11,   106,    69,
      70,    71,    72,    73,    74,    75,    76,    77,    78,    79,
      80,    25,    14,    83,    28,    75,     0,    29,    30,    31,
      80,    20,    34,    35,    31,    27,    75,    29,    27,    78,
      29,    27,    28,    29,   141,     3,     4,     5,     6,     7,
       8,     9,    10,    11,    12,    13,    14,    15,    16,    17,
      18,    19,    20,    21,    22,    23,    24,    25,    26,    27,
      28,    29,    30,   171,    25,    32,    33,    28,    35,   212,
      37,    39,    40,    41,    42,    43,    44,    45,    46,    47,
      48,    49,    50,    51,    52,    53,    54,    55,    56,    57,
      58,    59,    60,    61,    62,    63,    64,    65,    66,    67,
      68,    15,     8,     9,    18,    19,   213,    27,    80,    29,
      82,   100,   101,    27,    82,    29,    84,    85,    75,     3,
      75,     5,    82,     7,    82,     9,    75,    11,   140,    13,
      75,    15,    82,    17,    34,    19,    38,    21,    36,    23,
       9,    25,    74,    27,    25,    29,    30,   100,   101,   100,
     101,    28,    80,    75,    75,    39,    12,    41,    10,    43,
      10,    45,    16,    47,    26,    49,     3,    51,     5,    53,
       7,    55,     9,    57,    11,    59,    13,    61,    15,    63,
      17,    65,    19,    67,    21,    16,    23,    26,    25,    82,
      27,    96,    29,    90,   209,   204,   167,    -1,    82,    -1,
      84,    -1,    39,    -1,    41,    -1,    43,    -1,    45,    -1,
      47,    -1,    49,    -1,    51,    -1,    53,    -1,    55,    -1,
      57,    -1,    59,    -1,    61,    -1,    63,    -1,    65,    -1,
      67,     4,     5,    -1,     7,    -1,    -1,    -1,    -1,    -1,
      13,     5,     6,     7,    17,    82,    -1,    84,    21,    13,
      23,    -1,    -1,    17,    27,    -1,    29,    21,    -1,    23,
      -1,    -1,    -1,    27,    -1,    29,    39,    -1,    41,    -1,
      43,    -1,    45,    -1,    47,    39,    49,    41,    51,    43,
      53,    45,    55,    47,    57,    49,    59,    51,    61,    53,
      63,    55,    65,    57,    67,    59,    -1,    61,    -1,    63,
      -1,    65,    15,    67,    -1,    18,    19,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    27,    -1,    29,    -1,    15,    -1,
      17,    -1,    19,    -1,    21,    22,    23,    40,    -1,    42,
      27,    44,    29,    46,    -1,    48,    -1,    50,    -1,    52,
      -1,    54,    39,    56,    41,    58,    43,    60,    45,    62,
      47,    64,    49,    66,    51,    68,    53,    -1,    55,    -1,
      57,    -1,    59,    15,    61,    17,    63,    19,    65,    21,
      67,    23,    24,    -1,    -1,    27,    -1,    29,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    39,    -1,    41,
      -1,    43,    -1,    45,    -1,    47,    -1,    49,    -1,    51,
      -1,    53,    -1,    55,    -1,    57,    -1,    59,    -1,    61,
      -1,    63,    -1,    65,    -1,    67
    };
  }

/* YYSTOS[STATE-NUM] -- The (internal number of the) accessing
   symbol of state STATE-NUM.  */
  private static final short yystos_[] = yystos_init();
  private static final short[] yystos_init()
  {
    return new short[]
    {
       0,     3,    31,    87,    88,   138,   134,   134,     0,    69,
      70,    71,    72,    73,    74,    75,    76,    77,    78,    79,
      80,    83,    89,   135,   139,    92,   140,     4,     5,     7,
      13,    17,    21,    23,    27,    29,    39,    41,    43,    45,
      47,    49,    51,    53,    55,    57,    59,    61,    63,    65,
      67,    90,    93,    97,   100,   101,   103,   105,   110,   113,
     117,   118,   119,   124,   129,    32,    33,    35,    37,   141,
      75,   134,   134,   134,    75,    75,   134,   134,    75,    82,
      82,    82,    91,    94,    98,   104,   111,   114,   122,   130,
     102,    34,    36,    38,    92,     9,    95,    96,   116,   107,
     112,   115,    11,   120,   121,   123,   125,     3,     5,     7,
       9,    11,    13,    15,    17,    19,    21,    23,    25,    27,
      29,    39,    41,    43,    45,    47,    49,    51,    53,    55,
      57,    59,    61,    63,    65,    67,    82,    84,   131,   132,
     136,   107,     6,    75,    80,     8,    96,    14,   117,    15,
      18,    19,    99,   108,   117,    22,    99,   100,   108,   117,
      24,    99,   100,   108,   117,    74,    25,   127,   128,    28,
     118,   126,    30,   132,   134,    18,    40,    42,    44,    46,
      48,    50,    52,    54,    56,    58,    60,    62,    64,    66,
      68,   106,    80,    75,    75,    78,    75,    12,    80,    82,
      28,   128,    28,   118,   133,    10,    10,    16,    16,   109,
      26,    26,   131,   116,     4,     6,     8,    10,    12,    14,
      16,    18,    20,    22,    24,    26,    28,    30,    40,    42,
      44,    46,    48,    50,    52,    54,    56,    58,    60,    62,
      64,    66,    68,    85,   137,    20
    };
  }

/* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final short yyr1_[] = yyr1_init();
  private static final short[] yyr1_init()
  {
    return new short[]
    {
       0,    86,    87,    87,    89,    88,    91,    90,    92,    92,
      92,    92,    92,    92,    94,    93,    95,    95,    96,    96,
      98,    97,    99,    99,   100,   100,   100,   100,   102,   101,
     104,   103,   105,   105,   105,   105,   105,   105,   105,   105,
     105,   105,   105,   105,   105,   105,   105,   106,   106,   106,
     106,   106,   106,   106,   106,   106,   106,   106,   106,   106,
     106,   106,   106,   107,   107,   107,   107,   109,   108,   111,
     110,   112,   112,   112,   112,   112,   114,   113,   115,   115,
     115,   115,   115,   116,   116,   117,   118,   118,   118,   120,
     119,   121,   119,   122,   122,   123,   125,   124,   126,   126,
     127,   127,   128,   128,   130,   129,   131,   131,   133,   132,
     132,   134,   134,   135,   135,   135,   135,   135,   135,   135,
     135,   135,   135,   135,   135,   135,   136,   136,   136,   136,
     136,   136,   136,   136,   136,   136,   136,   136,   136,   136,
     136,   136,   136,   136,   136,   136,   136,   136,   136,   136,
     136,   136,   136,   136,   136,   136,   137,   137,   137,   137,
     137,   137,   137,   137,   137,   137,   137,   137,   137,   137,
     137,   137,   137,   137,   137,   137,   137,   137,   137,   137,
     137,   137,   137,   137,   137,   137,   139,   138,   140,   140,
     141,   141,   141
    };
  }

/* YYR2[YYN] -- Number of symbols on the right hand side of rule YYN.  */
  private static final byte yyr2_[] = yyr2_init();
  private static final byte[] yyr2_init()
  {
    return new byte[]
    {
       0,     2,     1,     1,     0,     5,     0,     5,     0,     2,
       2,     2,     2,     2,     0,     5,     1,     2,     4,     4,
       0,     5,     3,     3,     1,     1,     1,     1,     0,     5,
       0,     5,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     0,     2,     2,     2,     0,     5,     0,
       5,     0,     2,     2,     2,     2,     0,     5,     0,     2,
       2,     2,     2,     0,     2,     1,     1,     1,     1,     0,
       6,     0,     5,     0,     2,     3,     0,     6,     1,     2,
       1,     2,     3,     3,     0,     5,     1,     2,     0,     5,
       1,     0,     2,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     0,     5,     0,     2,
       3,     3,     3
    };
  }

  /* YYTOKEN_NUMBER[YYLEX-NUM] -- Internal symbol number corresponding
      to YYLEX-NUM.  */
  private static final short yytoken_number_[] = yytoken_number_init();
  private static final short[] yytoken_number_init()
  {
    return new short[]
    {
       0,   256,   257,   258,   259,   260,   261,   262,   263,   264,
     265,   266,   267,   268,   269,   270,   271,   272,   273,   274,
     275,   276,   277,   278,   279,   280,   281,   282,   283,   284,
     285,   286,   287,   288,   289,   290,   291,   292,   293,   294,
     295,   296,   297,   298,   299,   300,   301,   302,   303,   304,
     305,   306,   307,   308,   309,   310,   311,   312,   313,   314,
     315,   316,   317,   318,   319,   320,   321,   322,   323,   324,
     325,   326,   327,   328,   329,   330,   331,   332,   333,   334,
     335,   336,   337,   338,   339,   340
    };
  }

  /* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     First, the terminals, then, starting at \a yyntokens_, nonterminals.  */
  private static final String yytname_[] = yytname_init();
  private static final String[] yytname_init()
  {
    return new String[]
    {
  "$end", "error", "$undefined", "DATASET_", "_DATASET", "GROUP_",
  "_GROUP", "ENUMERATION_", "_ENUMERATION", "ENUMCONST_", "_ENUMCONST",
  "NAMESPACE_", "_NAMESPACE", "DIMENSION_", "_DIMENSION", "DIM_", "_DIM",
  "ENUM_", "_ENUM", "MAP_", "_MAP", "STRUCTURE_", "_STRUCTURE",
  "SEQUENCE_", "_SEQUENCE", "VALUE_", "_VALUE", "ATTRIBUTE_", "_ATTRIBUTE",
  "OTHERXML_", "_OTHERXML", "ERROR_", "_ERROR", "MESSAGE_", "_MESSAGE",
  "CONTEXT_", "_CONTEXT", "OTHERINFO_", "_OTHERINFO", "CHAR_", "_CHAR",
  "BYTE_", "_BYTE", "INT8_", "_INT8", "UINT8_", "_UINT8", "INT16_",
  "_INT16", "UINT16_", "_UINT16", "INT32_", "_INT32", "UINT32_", "_UINT32",
  "INT64_", "_INT64", "UINT64_", "_UINT64", "FLOAT32_", "_FLOAT32",
  "FLOAT64_", "_FLOAT64", "STRING_", "_STRING", "URL_", "_URL", "OPAQUE_",
  "_OPAQUE", "ATTR_BASE", "ATTR_BASETYPE", "ATTR_DAPVERSION",
  "ATTR_DMRVERSION", "ATTR_ENUM", "ATTR_HREF", "ATTR_NAME",
  "ATTR_NAMESPACE", "ATTR_NS", "ATTR_SIZE", "ATTR_TYPE", "ATTR_VALUE",
  "ATTR_HTTPCODE", "TEXT", "UNKNOWN_ATTR", "UNKNOWN_ELEMENT_",
  "_UNKNOWN_ELEMENT", "$accept", "response", "dataset", "$@1", "group",
  "$@2", "groupbody", "enumdef", "$@3", "enumconst_list", "enumconst",
  "dimdef", "$@4", "dimref", "variable", "atomicvariable", "$@5",
  "enumvariable", "$@6", "atomictype_", "_atomictype", "varbody", "mapref",
  "$@7", "structurevariable", "$@8", "structbody", "sequencevariable",
  "$@9", "sequencebody", "metadatalist", "metadata", "attribute",
  "atomicattribute", "$@10", "$@11", "namespace_list", "namespace",
  "containerattribute", "$@12", "attributelist", "valuelist", "value",
  "otherxml", "$@13", "xml_body", "element_or_text", "$@14",
  "xml_attribute_map", "xml_attribute", "xml_open", "xml_close",
  "error_response", "$@15", "error_body", "error_element", null
    };
  }

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] = yyrline_init();
  private static final short[] yyrline_init()
  {
    return new short[]
    {
       0,    94,    94,    95,   101,    99,   110,   108,   122,   124,
     125,   126,   127,   128,   134,   132,   141,   142,   146,   148,
     155,   153,   162,   164,   169,   170,   171,   172,   179,   177,
     189,   187,   198,   199,   200,   201,   202,   203,   204,   205,
     206,   207,   208,   209,   210,   211,   212,   216,   217,   218,
     219,   220,   221,   222,   223,   224,   225,   226,   227,   228,
     229,   230,   231,   234,   236,   237,   238,   244,   242,   253,
     251,   259,   261,   262,   263,   264,   270,   268,   276,   278,
     279,   280,   281,   284,   286,   290,   294,   295,   296,   305,
     302,   313,   310,   320,   321,   326,   336,   333,   344,   345,
     350,   351,   355,   357,   364,   362,   371,   372,   378,   376,
     382,   391,   392,   399,   400,   401,   402,   403,   404,   405,
     406,   407,   408,   409,   410,   411,   417,   418,   419,   420,
     421,   422,   423,   424,   425,   426,   427,   428,   429,   430,
     431,   432,   433,   434,   435,   436,   437,   438,   439,   440,
     441,   442,   443,   444,   445,   446,   450,   451,   452,   453,
     454,   455,   456,   457,   458,   459,   460,   461,   462,   463,
     464,   465,   466,   467,   468,   469,   470,   471,   472,   473,
     474,   475,   476,   477,   478,   479,   486,   483,   493,   495,
     499,   501,   503
    };
  }


  // Report on the debug stream that the rule yyrule is going to be reduced.
  private void yy_reduce_print (int yyrule, YYStack yystack)
  {
    if (yydebug == 0)
      return;

    int yylno = yyrline_[yyrule];
    int yynrhs = yyr2_[yyrule];
    /* Print the symbols being reduced, and their result.  */
    yycdebug ("Reducing stack by rule " + (yyrule - 1)
              + " (line " + yylno + "), ");

    /* The symbols being reduced.  */
    for (int yyi = 0; yyi < yynrhs; yyi++)
      yy_symbol_print ("   $" + (yyi + 1) + " =",
                       yystos_[yystack.stateAt(yynrhs - (yyi + 1))],
                       ((yystack.valueAt (yynrhs-(yyi + 1)))),
                       yystack.locationAt (yynrhs-(yyi + 1)));
  }

  /* YYTRANSLATE(YYLEX) -- Bison symbol number corresponding to YYLEX.  */
  private static final byte yytranslate_table_[] = yytranslate_table_init();
  private static final byte[] yytranslate_table_init()
  {
    return new byte[]
    {
       0,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     1,     2,     3,     4,
       5,     6,     7,     8,     9,    10,    11,    12,    13,    14,
      15,    16,    17,    18,    19,    20,    21,    22,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    33,    34,
      35,    36,    37,    38,    39,    40,    41,    42,    43,    44,
      45,    46,    47,    48,    49,    50,    51,    52,    53,    54,
      55,    56,    57,    58,    59,    60,    61,    62,    63,    64,
      65,    66,    67,    68,    69,    70,    71,    72,    73,    74,
      75,    76,    77,    78,    79,    80,    81,    82,    83,    84,
      85
    };
  }

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 425;
  private static final int yynnts_ = 56;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 8;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 86;

  private static final int yyuser_token_number_max_ = 340;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */

}


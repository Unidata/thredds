/* A Bison parser, made by GNU Bison 3.0.4.  */

/* Skeleton implementation for Bison LALR(1) parsers in Java

   Copyright (C) 2007-2015 Free Software Foundation, Inc.

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

/* "Dap4BisonParser.java":37  */ /* lalr1.java:91  */

/* "Dap4BisonParser.java":39  */ /* lalr1.java:92  */
/* "%code imports" blocks.  */
/* "dap4.y":17  */ /* lalr1.java:93  */

import dap4.core.util.DapException;
import dap4.core.dmr.DapXML;

/* "Dap4BisonParser.java":46  */ /* lalr1.java:93  */

/**
 * A Bison parser, automatically generated from <tt>dap4.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
abstract class Dap4BisonParser extends Dap4Actions
{
    /** Version number for the Bison executable that generated this parser.  */
  public static final String bisonVersion = "3.0.4";

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
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>Dap4BisonParser</tt>.
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
    static final int ATTR_SPECIAL = 337;
    /** Token number,to be returned by the scanner.  */
    static final int TEXT = 338;
    /** Token number,to be returned by the scanner.  */
    static final int UNKNOWN_ATTR = 339;
    /** Token number,to be returned by the scanner.  */
    static final int UNKNOWN_ELEMENT_ = 340;
    /** Token number,to be returned by the scanner.  */
    static final int _UNKNOWN_ELEMENT = 341;


    

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * @return the semantic value of the last scanned token.
     */
    Object getLVal ();

    /**
     * Entry point for the scanner.  Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * of the token.
     * @return the token identifier corresponding to the next token.
     */
    int yylex () throws DapException;

    /**
     * Entry point for error reporting.  Emits an error
     * in a user-defined way.
     *
     * 
     * @param msg The string for the error message.
     */
     void yyerror (String msg);
  }

  private class YYLexer implements Lexer {
/* "%code lexer" blocks.  */
/* "dap4.y":22  */ /* lalr1.java:236  */

public Object getLVal() {return null;}
public int yylex() {return 0;}
public Bison.Position getStartPos() {return null;}
public Bison.Position getEndPos() {return null;}
public void yyerror(String s)
{
System.err.println(s);
System.err.printf("near %s%n",getLocator());
}


/* "Dap4BisonParser.java":304  */ /* lalr1.java:236  */

  }

  /**
   * The object doing lexical analysis for us.
   */
  private Lexer yylexer;
  
  


  /**
   * Instantiates the Bison-generated parser.
   */
  public Dap4BisonParser () 
  {
    
    this.yylexer = new YYLexer();
    
  }


  /**
   * Instantiates the Bison-generated parser.
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  protected Dap4BisonParser (Lexer yylexer) 
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
   *
   * @param msg The error message.
   */
  public final void yyerror (String msg)
  {
    yylexer.yyerror (msg);
  }


  protected final void yycdebug (String s) {
    if (yydebug > 0)
      yyDebugStream.println (s);
  }

  private final class YYStack {
    private int[] stateStack = new int[16];
    
    private Object[] valueStack = new Object[16];

    public int size = 16;
    public int height = -1;

    public final void push (int state, Object value                            ) {
      height++;
      if (size == height)
        {
          int[] newStateStack = new int[size * 2];
          System.arraycopy (stateStack, 0, newStateStack, 0, height);
          stateStack = newStateStack;
          

          Object[] newValueStack = new Object[size * 2];
          System.arraycopy (valueStack, 0, newValueStack, 0, height);
          valueStack = newValueStack;

          size *= 2;
        }

      stateStack[height] = state;
      
      valueStack[height] = value;
    }

    public final void pop () {
      pop (1);
    }

    public final void pop (int num) {
      // Avoid memory leaks... garbage collection is a white lie!
      if (num > 0) {
        java.util.Arrays.fill (valueStack, height - num + 1, height + 1, null);
        
      }
      height -= num;
    }

    public final int stateAt (int i) {
      return stateStack[height - i];
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

  /** Compute post-reduction state.
   * @param yystate   the current state
   * @param yysym     the nonterminal to push on the stack
   */
  private int yy_lr_goto_state_ (int yystate, int yysym)
  {
    int yyr = yypgoto_[yysym - yyntokens_] + yystate;
    if (0 <= yyr && yyr <= yylast_ && yycheck_[yyr] == yystate)
      return yytable_[yyr];
    else
      return yydefgoto_[yysym - yyntokens_];
  }

  private int yyaction (int yyn, YYStack yystack, int yylen) throws DapException
  {
    Object yyval;
    

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
    /* "dap4.y":106  */ /* lalr1.java:489  */
    {leavedataset();};
  break;
    

  case 5:
  if (yyn == 5)
    /* "dap4.y":112  */ /* lalr1.java:489  */
    {enterdataset(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 6:
  if (yyn == 6)
    /* "dap4.y":119  */ /* lalr1.java:489  */
    {leavegroup();};
  break;
    

  case 7:
  if (yyn == 7)
    /* "dap4.y":125  */ /* lalr1.java:489  */
    {entergroup(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 14:
  if (yyn == 14)
    /* "dap4.y":147  */ /* lalr1.java:489  */
    {leaveenumdef();};
  break;
    

  case 15:
  if (yyn == 15)
    /* "dap4.y":153  */ /* lalr1.java:489  */
    {enterenumdef(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 18:
  if (yyn == 18)
    /* "dap4.y":163  */ /* lalr1.java:489  */
    {enumconst(((SaxEvent)(yystack.valueAt (4-(2)))),((SaxEvent)(yystack.valueAt (4-(3)))));};
  break;
    

  case 19:
  if (yyn == 19)
    /* "dap4.y":165  */ /* lalr1.java:489  */
    {enumconst(((SaxEvent)(yystack.valueAt (4-(3)))),((SaxEvent)(yystack.valueAt (4-(2)))));};
  break;
    

  case 20:
  if (yyn == 20)
    /* "dap4.y":172  */ /* lalr1.java:489  */
    {leavedimdef();};
  break;
    

  case 21:
  if (yyn == 21)
    /* "dap4.y":178  */ /* lalr1.java:489  */
    {enterdimdef(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 22:
  if (yyn == 22)
    /* "dap4.y":183  */ /* lalr1.java:489  */
    {dimref(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 23:
  if (yyn == 23)
    /* "dap4.y":185  */ /* lalr1.java:489  */
    {dimref(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 28:
  if (yyn == 28)
    /* "dap4.y":200  */ /* lalr1.java:489  */
    {leaveatomicvariable(((SaxEvent)(yystack.valueAt (3-(3)))));};
  break;
    

  case 29:
  if (yyn == 29)
    /* "dap4.y":207  */ /* lalr1.java:489  */
    {enteratomicvariable(((SaxEvent)(yystack.valueAt (2-(1)))),((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 30:
  if (yyn == 30)
    /* "dap4.y":214  */ /* lalr1.java:489  */
    {leaveenumvariable(((SaxEvent)(yystack.valueAt (3-(3)))));};
  break;
    

  case 31:
  if (yyn == 31)
    /* "dap4.y":221  */ /* lalr1.java:489  */
    {enterenumvariable(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 32:
  if (yyn == 32)
    /* "dap4.y":226  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 33:
  if (yyn == 33)
    /* "dap4.y":227  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 34:
  if (yyn == 34)
    /* "dap4.y":228  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 35:
  if (yyn == 35)
    /* "dap4.y":229  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 36:
  if (yyn == 36)
    /* "dap4.y":230  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 37:
  if (yyn == 37)
    /* "dap4.y":231  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 38:
  if (yyn == 38)
    /* "dap4.y":232  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 39:
  if (yyn == 39)
    /* "dap4.y":233  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 40:
  if (yyn == 40)
    /* "dap4.y":234  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 41:
  if (yyn == 41)
    /* "dap4.y":235  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 42:
  if (yyn == 42)
    /* "dap4.y":236  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 43:
  if (yyn == 43)
    /* "dap4.y":237  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 44:
  if (yyn == 44)
    /* "dap4.y":238  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 45:
  if (yyn == 45)
    /* "dap4.y":239  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 46:
  if (yyn == 46)
    /* "dap4.y":240  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 47:
  if (yyn == 47)
    /* "dap4.y":244  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 48:
  if (yyn == 48)
    /* "dap4.y":245  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 49:
  if (yyn == 49)
    /* "dap4.y":246  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 50:
  if (yyn == 50)
    /* "dap4.y":247  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 51:
  if (yyn == 51)
    /* "dap4.y":248  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 52:
  if (yyn == 52)
    /* "dap4.y":249  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 53:
  if (yyn == 53)
    /* "dap4.y":250  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 54:
  if (yyn == 54)
    /* "dap4.y":251  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 55:
  if (yyn == 55)
    /* "dap4.y":252  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 56:
  if (yyn == 56)
    /* "dap4.y":253  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 57:
  if (yyn == 57)
    /* "dap4.y":254  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 58:
  if (yyn == 58)
    /* "dap4.y":255  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 59:
  if (yyn == 59)
    /* "dap4.y":256  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 60:
  if (yyn == 60)
    /* "dap4.y":257  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 61:
  if (yyn == 61)
    /* "dap4.y":258  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 62:
  if (yyn == 62)
    /* "dap4.y":259  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 67:
  if (yyn == 67)
    /* "dap4.y":273  */ /* lalr1.java:489  */
    {leavemap();};
  break;
    

  case 68:
  if (yyn == 68)
    /* "dap4.y":279  */ /* lalr1.java:489  */
    {entermap(((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 69:
  if (yyn == 69)
    /* "dap4.y":286  */ /* lalr1.java:489  */
    {leavestructurevariable(((SaxEvent)(yystack.valueAt (3-(3)))));};
  break;
    

  case 70:
  if (yyn == 70)
    /* "dap4.y":292  */ /* lalr1.java:489  */
    {enterstructurevariable(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 76:
  if (yyn == 76)
    /* "dap4.y":307  */ /* lalr1.java:489  */
    {leavesequencevariable(((SaxEvent)(yystack.valueAt (3-(3)))));};
  break;
    

  case 77:
  if (yyn == 77)
    /* "dap4.y":313  */ /* lalr1.java:489  */
    {entersequencevariable(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 89:
  if (yyn == 89)
    /* "dap4.y":345  */ /* lalr1.java:489  */
    {leaveatomicattribute();};
  break;
    

  case 90:
  if (yyn == 90)
    /* "dap4.y":349  */ /* lalr1.java:489  */
    {leaveatomicattribute();};
  break;
    

  case 91:
  if (yyn == 91)
    /* "dap4.y":356  */ /* lalr1.java:489  */
    {enteratomicattribute(((XMLAttributeMap)(yystack.valueAt (3-(2)))),((NamespaceList)(yystack.valueAt (3-(3)))));};
  break;
    

  case 92:
  if (yyn == 92)
    /* "dap4.y":361  */ /* lalr1.java:489  */
    {yyval=namespace_list();};
  break;
    

  case 93:
  if (yyn == 93)
    /* "dap4.y":363  */ /* lalr1.java:489  */
    {yyval=namespace_list(((NamespaceList)(yystack.valueAt (2-(1)))),((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 94:
  if (yyn == 94)
    /* "dap4.y":370  */ /* lalr1.java:489  */
    {yyval=(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 95:
  if (yyn == 95)
    /* "dap4.y":377  */ /* lalr1.java:489  */
    {leavecontainerattribute();};
  break;
    

  case 96:
  if (yyn == 96)
    /* "dap4.y":384  */ /* lalr1.java:489  */
    {entercontainerattribute(((XMLAttributeMap)(yystack.valueAt (3-(2)))),((NamespaceList)(yystack.valueAt (3-(3)))));};
  break;
    

  case 101:
  if (yyn == 101)
    /* "dap4.y":401  */ /* lalr1.java:489  */
    {value(((String)(yystack.valueAt (3-(2)))));};
  break;
    

  case 102:
  if (yyn == 102)
    /* "dap4.y":403  */ /* lalr1.java:489  */
    {value(((SaxEvent)(yystack.valueAt (3-(2)))));};
  break;
    

  case 103:
  if (yyn == 103)
    /* "dap4.y":411  */ /* lalr1.java:489  */
    {otherxml(((XMLAttributeMap)(yystack.valueAt (4-(2)))),((DapXML)(yystack.valueAt (4-(3)))));};
  break;
    

  case 104:
  if (yyn == 104)
    /* "dap4.y":415  */ /* lalr1.java:489  */
    {yyval=xml_body(null,((DapXML)(yystack.valueAt (1-(1)))));};
  break;
    

  case 105:
  if (yyn == 105)
    /* "dap4.y":416  */ /* lalr1.java:489  */
    {yyval=xml_body(((DapXML.XMLList)(yystack.valueAt (2-(1)))),((DapXML)(yystack.valueAt (2-(2)))));};
  break;
    

  case 106:
  if (yyn == 106)
    /* "dap4.y":424  */ /* lalr1.java:489  */
    {yyval=element_or_text(((SaxEvent)(yystack.valueAt (4-(1)))),((XMLAttributeMap)(yystack.valueAt (4-(2)))),((DapXML.XMLList)(yystack.valueAt (4-(3)))),((SaxEvent)(yystack.valueAt (4-(4)))));};
  break;
    

  case 107:
  if (yyn == 107)
    /* "dap4.y":426  */ /* lalr1.java:489  */
    {yyval=xmltext(((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 108:
  if (yyn == 108)
    /* "dap4.y":434  */ /* lalr1.java:489  */
    {yyval=xml_attribute_map();};
  break;
    

  case 109:
  if (yyn == 109)
    /* "dap4.y":436  */ /* lalr1.java:489  */
    {yyval=xml_attribute_map(((XMLAttributeMap)(yystack.valueAt (2-(1)))),((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    

  case 184:
  if (yyn == 184)
    /* "dap4.y":530  */ /* lalr1.java:489  */
    {leaveerror();};
  break;
    

  case 185:
  if (yyn == 185)
    /* "dap4.y":537  */ /* lalr1.java:489  */
    {entererror(((XMLAttributeMap)(yystack.valueAt (2-(2)))));};
  break;
    

  case 188:
  if (yyn == 188)
    /* "dap4.y":547  */ /* lalr1.java:489  */
    {errormessage(((String)(yystack.valueAt (3-(2)))));};
  break;
    

  case 189:
  if (yyn == 189)
    /* "dap4.y":549  */ /* lalr1.java:489  */
    {errorcontext(((String)(yystack.valueAt (3-(2)))));};
  break;
    

  case 190:
  if (yyn == 190)
    /* "dap4.y":551  */ /* lalr1.java:489  */
    {errorotherinfo(((String)(yystack.valueAt (3-(2)))));};
  break;
    

  case 191:
  if (yyn == 191)
    /* "dap4.y":556  */ /* lalr1.java:489  */
    {yyval=textstring(null,((SaxEvent)(yystack.valueAt (1-(1)))));};
  break;
    

  case 192:
  if (yyn == 192)
    /* "dap4.y":558  */ /* lalr1.java:489  */
    {yyval=textstring(((String)(yystack.valueAt (2-(1)))),((SaxEvent)(yystack.valueAt (2-(2)))));};
  break;
    


/* "Dap4BisonParser.java":1087  */ /* lalr1.java:489  */
        default: break;
      }

    yy_symbol_print ("-> $$ =", yyr1_[yyn], yyval);

    yystack.pop (yylen);
    yylen = 0;

    /* Shift the result of the reduction.  */
    int yystate = yy_lr_goto_state_ (yystack.stateAt (0), yyr1_[yyn]);
    yystack.push (yystate, yyval);
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
                                 Object yyvaluep                                 )
  {
    if (yydebug > 0)
    yycdebug (s + (yytype < yyntokens_ ? " token " : " nterm ")
              + yytname_[yytype] + " ("
              + (yyvaluep == null ? "(null)" : yyvaluep.toString ()) + ")");
  }



  /**
   * Push Parse input from external lexer
   *
   * @param yylextoken current token
   * @param yylexval current lval

   *
   * @return <tt>YYACCEPT, YYABORT, YYPUSH_MORE</tt>
   */
  public int push_parse (int yylextoken, Object yylexval)
      throws DapException, DapException
  {
    


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
                             yylval);
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
                             yylval);

            /* Discard the token being shifted.  */
            yychar = yyempty_;

            /* Count tokens shifted since error; after three, turn off error
               status.  */
            if (yyerrstatus_ > 0)
              --yyerrstatus_;

            yystate = yyn;
            yystack.push (yystate, yylval);
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
            yyerror (yysyntax_error (yystate, yytoken));
          }

        
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

            
            yystack.pop ();
            yystate = yystack.stateAt (0);
            if (yydebug > 0)
              yystack.print (yyDebugStream);
          }

        if (label == YYABORT)
            /* Leave the switch.  */
            break;



        /* Shift the error token.  */
        yy_symbol_print ("Shifting", yystos_[yyn],
                         yylval);

        yystate = yyn;
        yystack.push (yyn, yylval);
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
    

    /* Semantic value of the lookahead.  */
    this.yylval = null;

    yystack.push (this.yystate, this.yylval);

    this.push_parse_initialized = true;

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

  private static final short yypact_ninf_ = -186;
  private static final short yytable_ninf_ = -97;

  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   STATE-NUM.  */
  private static final short yypact_[] = yypact_init();
  private static final short[] yypact_init()
  {
    return new short[]
    {
       5,  -186,  -186,    12,  -186,  -186,  -186,  -186,   315,   315,
    -186,    36,    41,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,    29,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,    14,  -186,   -10,  -186,  -186,   -38,   -38,   -38,  -186,
     315,   315,   315,   315,   315,   315,   315,    43,   204,   -62,
     129,  -186,    -5,   255,   217,   315,   271,   316,   -36,  -186,
     104,  -186,  -186,   107,  -186,   -14,   -32,   -12,   197,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
      25,  -186,  -186,     0,    30,  -186,  -186,  -186,  -186,    55,
    -186,    32,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,    83,   -16,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,    50,  -186,  -186,    43,   121,   164,   159,
     190,  -186,    -4,  -186,  -186,   195,   137,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,  -186,  -186,  -186
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
       0,   108,   108,     0,     2,     8,     3,   186,     5,   185,
       1,     0,     0,   110,   111,   112,   113,   114,   115,   116,
     117,   118,   119,   120,   121,   122,   123,   109,     4,   108,
     108,   108,   108,   108,   108,   108,   108,    32,    33,    34,
      35,    36,    37,    38,    39,    40,    41,    42,    43,    44,
      45,    46,    13,     8,    10,     0,     9,    83,    11,    24,
      63,    25,    63,   108,    26,    71,    27,    78,    12,    85,
      86,     0,    87,     0,    88,   184,     0,     0,     0,   187,
       7,    15,    21,    31,    70,    77,    92,     0,     0,     0,
       0,    16,     0,     0,     0,    29,     0,     0,     0,    90,
       0,    99,    97,     0,   191,     0,     0,     0,    91,   124,
     125,   126,   127,   128,   129,   130,   131,   132,   133,   134,
     135,   136,   137,   138,   139,   140,   141,   142,   143,   144,
     145,   146,   147,   148,   149,   150,   151,   152,   107,   153,
       0,   108,     6,     0,     0,    14,    17,    20,    84,     0,
      62,     0,    47,    48,    49,    50,    51,    52,    53,    54,
      55,    56,    57,    58,    59,    60,    61,    64,    28,    65,
      83,    66,    30,    69,    72,    73,    74,    75,    76,    79,
      80,    81,    82,     0,     0,    89,   100,    95,    98,   188,
     192,   189,   190,     0,    93,   103,     0,     0,     0,     0,
       0,    68,     0,   102,   101,     0,     0,   104,    18,    19,
      22,    23,    67,    94,   154,   155,   156,   157,   158,   159,
     160,   161,   162,   163,   164,   165,   166,   167,   168,   169,
     170,   171,   172,   173,   174,   175,   176,   177,   178,   179,
     180,   181,   182,   183,   105,   106
    };
  }

/* YYPGOTO[NTERM-NUM].  */
  private static final short yypgoto_[] = yypgoto_init();
  private static final short[] yypgoto_init()
  {
    return new short[]
    {
    -186,  -186,  -186,  -186,  -186,  -186,   160,  -186,  -186,  -186,
     122,  -186,  -186,    72,    74,  -186,  -186,  -186,  -186,  -186,
    -186,   152,    76,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
      45,   -91,   -66,  -186,  -186,  -186,  -186,  -186,  -186,  -186,
    -186,   116,  -186,  -186,  -185,    -2,  -186,  -186,  -186,  -186,
    -186,  -186,  -186,   -63
    };
  }

/* YYDEFGOTO[NTERM-NUM].  */
  private static final short yydefgoto_[] = yydefgoto_init();
  private static final short[] yydefgoto_init()
  {
    return new short[]
    {
      -1,     3,     4,     5,    52,    53,    11,    54,    55,    90,
      91,    56,    57,   167,    58,    59,    60,    61,    62,    63,
     168,    93,   169,   170,    64,    65,    96,    66,    67,    97,
      92,    68,    69,    70,    71,   108,   194,    72,    73,   103,
     100,   101,    74,   206,   140,     8,    27,   141,   245,     6,
       7,    12,    79,   105
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
       9,   148,   171,   171,   191,   177,   182,   102,     1,   147,
     204,   207,    10,   143,   106,   107,   212,    35,   144,    36,
     189,   244,    35,    35,    36,    36,   192,    80,    81,    82,
      83,    84,    85,    86,    87,   184,     2,   188,    89,    98,
      28,    29,    99,    30,   183,   104,   109,   104,   110,    31,
     111,   190,   112,    32,   113,   195,   114,    33,   115,    34,
     116,    95,   117,    35,   118,    36,   119,   190,   120,   190,
     121,   190,   122,    75,    76,    37,    77,    38,    78,    39,
     197,    40,   123,    41,   124,    42,   125,    43,   126,    44,
     127,    45,   128,    46,   129,    47,   130,    48,   131,    49,
     132,    50,   133,    51,   134,   198,   135,   201,   136,   203,
     137,   148,    13,    14,    15,    16,    17,    18,    19,    20,
      21,    22,    23,    24,   205,    25,   138,    26,   139,    98,
     199,   208,   185,   200,    35,   187,    36,   145,    89,   196,
     109,   214,   110,   215,   111,   216,   112,   217,   113,   218,
     114,   219,   115,   220,   116,   221,   117,   222,   118,   223,
     119,   224,   120,   225,   121,   226,   122,   227,   174,   179,
     175,   180,   176,   181,   209,   210,   123,   228,   124,   229,
     125,   230,   126,   231,   127,   232,   128,   233,   129,   234,
     130,   235,   131,   236,   132,   237,   133,   238,   134,   239,
     135,   240,   136,   241,   137,   242,   211,   213,   193,    29,
     142,    30,   146,    88,    94,   202,   186,    31,     0,     0,
     138,    32,   139,   243,   -96,    33,   -96,    34,     0,     0,
       0,    35,   149,    36,     0,   172,   151,     0,     0,     0,
       0,     0,     0,    37,    35,    38,    36,    39,     0,    40,
       0,    41,     0,    42,     0,    43,     0,    44,     0,    45,
       0,    46,     0,    47,     0,    48,     0,    49,     0,    50,
     149,    51,     0,   150,   151,     0,     0,     0,     0,     0,
       0,     0,    35,     0,    36,     0,   149,     0,    32,     0,
     151,     0,    33,   173,    34,   152,     0,   153,    35,   154,
      36,   155,     0,   156,     0,   157,     0,   158,     0,   159,
      37,   160,    38,   161,    39,   162,    40,   163,    41,   164,
      42,   165,    43,   166,    44,     0,    45,     0,    46,     0,
      47,   149,    48,    32,    49,   151,    50,    33,    51,    34,
     178,     0,     0,    35,     0,    36,     0,     0,     0,     0,
       0,     0,     0,     0,     0,    37,     0,    38,     0,    39,
       0,    40,     0,    41,     0,    42,     0,    43,     0,    44,
       0,    45,     0,    46,     0,    47,     0,    48,     0,    49,
       0,    50,     0,    51,    13,    14,    15,    16,    17,    18,
      19,    20,    21,    22,    23,    24,     0,    25,     0,    26
    };
  }

private static final short yycheck_[] = yycheck_init();
  private static final short[] yycheck_init()
  {
    return new short[]
    {
       2,    92,    93,    94,    36,    96,    97,    73,     3,    14,
      26,   196,     0,    75,    77,    78,    20,    27,    80,    29,
      34,   206,    27,    27,    29,    29,    38,    29,    30,    31,
      32,    33,    34,    35,    36,    98,    31,   103,     9,    25,
       4,     5,    28,     7,    80,    83,     3,    83,     5,    13,
       7,    83,     9,    17,    11,    30,    13,    21,    15,    23,
      17,    63,    19,    27,    21,    29,    23,    83,    25,    83,
      27,    83,    29,    32,    33,    39,    35,    41,    37,    43,
      80,    45,    39,    47,    41,    49,    43,    51,    45,    53,
      47,    55,    49,    57,    51,    59,    53,    61,    55,    63,
      57,    65,    59,    67,    61,    75,    63,    75,    65,    26,
      67,   202,    69,    70,    71,    72,    73,    74,    75,    76,
      77,    78,    79,    80,    74,    82,    83,    84,    85,    25,
      75,    10,    28,    78,    27,    28,    29,     8,     9,   141,
       3,     4,     5,     6,     7,     8,     9,    10,    11,    12,
      13,    14,    15,    16,    17,    18,    19,    20,    21,    22,
      23,    24,    25,    26,    27,    28,    29,    30,    96,    97,
      96,    97,    96,    97,    10,    16,    39,    40,    41,    42,
      43,    44,    45,    46,    47,    48,    49,    50,    51,    52,
      53,    54,    55,    56,    57,    58,    59,    60,    61,    62,
      63,    64,    65,    66,    67,    68,    16,    12,    11,     5,
       6,     7,    90,    53,    62,   170,   100,    13,    -1,    -1,
      83,    17,    85,    86,    27,    21,    29,    23,    -1,    -1,
      -1,    27,    15,    29,    -1,    18,    19,    -1,    -1,    -1,
      -1,    -1,    -1,    39,    27,    41,    29,    43,    -1,    45,
      -1,    47,    -1,    49,    -1,    51,    -1,    53,    -1,    55,
      -1,    57,    -1,    59,    -1,    61,    -1,    63,    -1,    65,
      15,    67,    -1,    18,    19,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    27,    -1,    29,    -1,    15,    -1,    17,    -1,
      19,    -1,    21,    22,    23,    40,    -1,    42,    27,    44,
      29,    46,    -1,    48,    -1,    50,    -1,    52,    -1,    54,
      39,    56,    41,    58,    43,    60,    45,    62,    47,    64,
      49,    66,    51,    68,    53,    -1,    55,    -1,    57,    -1,
      59,    15,    61,    17,    63,    19,    65,    21,    67,    23,
      24,    -1,    -1,    27,    -1,    29,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    39,    -1,    41,    -1,    43,
      -1,    45,    -1,    47,    -1,    49,    -1,    51,    -1,    53,
      -1,    55,    -1,    57,    -1,    59,    -1,    61,    -1,    63,
      -1,    65,    -1,    67,    69,    70,    71,    72,    73,    74,
      75,    76,    77,    78,    79,    80,    -1,    82,    -1,    84
    };
  }

/* YYSTOS[STATE-NUM] -- The (internal number of the) accessing
   symbol of state STATE-NUM.  */
  private static final short yystos_[] = yystos_init();
  private static final short[] yystos_init()
  {
    return new short[]
    {
       0,     3,    31,    88,    89,    90,   136,   137,   132,   132,
       0,    93,   138,    69,    70,    71,    72,    73,    74,    75,
      76,    77,    78,    79,    80,    82,    84,   133,     4,     5,
       7,    13,    17,    21,    23,    27,    29,    39,    41,    43,
      45,    47,    49,    51,    53,    55,    57,    59,    61,    63,
      65,    67,    91,    92,    94,    95,    98,    99,   101,   102,
     103,   104,   105,   106,   111,   112,   114,   115,   118,   119,
     120,   121,   124,   125,   129,    32,    33,    35,    37,   139,
     132,   132,   132,   132,   132,   132,   132,   132,    93,     9,
      96,    97,   117,   108,   108,   132,   113,   116,    25,    28,
     127,   128,   119,   126,    83,   140,   140,   140,   122,     3,
       5,     7,     9,    11,    13,    15,    17,    19,    21,    23,
      25,    27,    29,    39,    41,    43,    45,    47,    49,    51,
      53,    55,    57,    59,    61,    63,    65,    67,    83,    85,
     131,   134,     6,    75,    80,     8,    97,    14,   118,    15,
      18,    19,    40,    42,    44,    46,    48,    50,    52,    54,
      56,    58,    60,    62,    64,    66,    68,   100,   107,   109,
     110,   118,    18,    22,   100,   101,   109,   118,    24,   100,
     101,   109,   118,    80,   140,    28,   128,    28,   119,    34,
      83,    36,    38,    11,   123,    30,   132,    80,    75,    75,
      78,    75,   117,    26,    26,    74,   130,   131,    10,    10,
      16,    16,    20,    12,     4,     6,     8,    10,    12,    14,
      16,    18,    20,    22,    24,    26,    28,    30,    40,    42,
      44,    46,    48,    50,    52,    54,    56,    58,    60,    62,
      64,    66,    68,    86,   131,   135
    };
  }

/* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final short yyr1_[] = yyr1_init();
  private static final short[] yyr1_init()
  {
    return new short[]
    {
       0,    87,    88,    88,    89,    90,    91,    92,    93,    93,
      93,    93,    93,    93,    94,    95,    96,    96,    97,    97,
      98,    99,   100,   100,   101,   101,   101,   101,   102,   103,
     104,   105,   106,   106,   106,   106,   106,   106,   106,   106,
     106,   106,   106,   106,   106,   106,   106,   107,   107,   107,
     107,   107,   107,   107,   107,   107,   107,   107,   107,   107,
     107,   107,   107,   108,   108,   108,   108,   109,   110,   111,
     112,   113,   113,   113,   113,   113,   114,   115,   116,   116,
     116,   116,   116,   117,   117,   118,   119,   119,   119,   120,
     120,   121,   122,   122,   123,   124,   125,   126,   126,   127,
     127,   128,   128,   129,   130,   130,   131,   131,   132,   132,
     133,   133,   133,   133,   133,   133,   133,   133,   133,   133,
     133,   133,   133,   133,   134,   134,   134,   134,   134,   134,
     134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
     134,   134,   134,   134,   134,   134,   134,   134,   134,   134,
     134,   134,   134,   134,   135,   135,   135,   135,   135,   135,
     135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
     135,   135,   135,   135,   135,   135,   135,   135,   135,   135,
     135,   135,   135,   135,   136,   137,   138,   138,   139,   139,
     139,   140,   140
    };
  }

/* YYR2[YYN] -- Number of symbols on the right hand side of rule YYN.  */
  private static final byte yyr2_[] = yyr2_init();
  private static final byte[] yyr2_init()
  {
    return new byte[]
    {
       0,     2,     1,     1,     3,     2,     3,     2,     0,     2,
       2,     2,     2,     2,     3,     2,     1,     2,     4,     4,
       3,     2,     3,     3,     1,     1,     1,     1,     3,     2,
       3,     2,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     0,     2,     2,     2,     3,     2,     3,
       2,     0,     2,     2,     2,     2,     3,     2,     0,     2,
       2,     2,     2,     0,     2,     1,     1,     1,     1,     3,
       2,     3,     0,     2,     3,     3,     3,     1,     2,     1,
       2,     3,     3,     4,     1,     2,     4,     1,     0,     2,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     3,     2,     0,     2,     3,     3,
       3,     1,     2
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
     335,   336,   337,   338,   339,   340,   341
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
  "ATTR_HTTPCODE", "ATTR_SPECIAL", "TEXT", "UNKNOWN_ATTR",
  "UNKNOWN_ELEMENT_", "_UNKNOWN_ELEMENT", "$accept", "response", "dataset",
  "datasetprefix", "group", "groupprefix", "groupbody", "enumdef",
  "enumdefprefix", "enumconst_list", "enumconst", "dimdef", "dimdefprefix",
  "dimref", "variable", "atomicvariable", "atomicvariableprefix",
  "enumvariable", "enumvariableprefix", "atomictype_", "_atomictype",
  "varbody", "mapref", "maprefprefix", "structurevariable",
  "structurevariableprefix", "structbody", "sequencevariable",
  "sequencevariableprefix", "sequencebody", "metadatalist", "metadata",
  "attribute", "atomicattribute", "atomicattributeprefix",
  "namespace_list", "namespace", "containerattribute",
  "containerattributeprefix", "attributelist", "valuelist", "value",
  "otherxml", "xml_body", "element_or_text", "xml_attribute_map",
  "xml_attribute", "xml_open", "xml_close", "error_response",
  "error_responseprefix", "error_body", "error_element", "textstring", null
    };
  }

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] = yyrline_init();
  private static final short[] yyrline_init()
  {
    return new short[]
    {
       0,    98,    98,    99,   103,   110,   116,   123,   134,   136,
     137,   138,   139,   140,   144,   151,   157,   158,   162,   164,
     169,   176,   182,   184,   189,   190,   191,   192,   197,   205,
     211,   219,   226,   227,   228,   229,   230,   231,   232,   233,
     234,   235,   236,   237,   238,   239,   240,   244,   245,   246,
     247,   248,   249,   250,   251,   252,   253,   254,   255,   256,
     257,   258,   259,   262,   264,   265,   266,   270,   277,   283,
     290,   295,   297,   298,   299,   300,   304,   311,   316,   318,
     319,   320,   321,   324,   326,   330,   334,   335,   336,   342,
     347,   353,   361,   362,   367,   374,   381,   389,   390,   395,
     396,   400,   402,   407,   415,   416,   420,   425,   434,   435,
     442,   443,   444,   445,   446,   447,   448,   449,   450,   451,
     452,   453,   454,   455,   461,   462,   463,   464,   465,   466,
     467,   468,   469,   470,   471,   472,   473,   474,   475,   476,
     477,   478,   479,   480,   481,   482,   483,   484,   485,   486,
     487,   488,   489,   490,   494,   495,   496,   497,   498,   499,
     500,   501,   502,   503,   504,   505,   506,   507,   508,   509,
     510,   511,   512,   513,   514,   515,   516,   517,   518,   519,
     520,   521,   522,   523,   527,   534,   540,   542,   546,   548,
     550,   555,   557
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
                       ((yystack.valueAt (yynrhs-(yyi + 1)))));
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
      85,    86
    };
  }

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 399;
  private static final int yynnts_ = 54;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 10;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 87;

  private static final int yyuser_token_number_max_ = 341;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */

}


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

package opendap.dap.parsers;
/* First part of user declarations.  */

/* "Dap2Parser.java":37  */ /* lalr1.java:91  */

/* "Dap2Parser.java":39  */ /* lalr1.java:92  */
/* "%code imports" blocks.  */
/* "dap2.y":18  */ /* lalr1.java:93  */

import opendap.dap.*;
import opendap.dap.BaseTypeFactory;
import opendap.dap.parsers.ParseException;
import java.io.*;
import static opendap.dap.parsers.Dap2Parser.Lexer.*;

/* "Dap2Parser.java":49  */ /* lalr1.java:93  */

/**
 * A Bison parser, automatically generated from <tt>dap2.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
public class Dap2Parser extends Dap2Parse
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
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>Dap2Parser</tt>.
   */
  public interface Lexer {
    /** Token returned by the scanner to signal the end of its input.  */
    public static final int EOF = 0;

/* Tokens.  */
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_ALIAS = 258;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_ARRAY = 259;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_ATTR = 260;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_BYTE = 261;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_CODE = 262;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_DATASET = 263;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_DATA = 264;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_ERROR = 265;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_FLOAT32 = 266;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_FLOAT64 = 267;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_GRID = 268;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_INT16 = 269;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_INT32 = 270;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_MAPS = 271;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_MESSAGE = 272;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_SEQUENCE = 273;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_STRING = 274;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_STRUCTURE = 275;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_UINT16 = 276;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_UINT32 = 277;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_URL = 278;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_PTYPE = 279;
    /** Token number,to be returned by the scanner.  */
    static final int SCAN_PROG = 280;
    /** Token number,to be returned by the scanner.  */
    static final int WORD_WORD = 281;
    /** Token number,to be returned by the scanner.  */
    static final int WORD_STRING = 282;


    

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
    int yylex () throws ParseException;

    /**
     * Entry point for error reporting.  Emits an error
     * in a user-defined way.
     *
     * 
     * @param msg The string for the error message.
     */
     void yyerror (String msg);
  }

  /**
   * The object doing lexical analysis for us.
   */
  private Lexer yylexer;
  
  



  /**
   * Instantiates the Bison-generated parser.
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  public Dap2Parser (Lexer yylexer) 
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

  static private final class YYStack {
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


  private int yyerrstatus_ = 0;


  /**
   * Return whether error recovery is being done.  In this state, the parser
   * reads token until it reaches a known state, and then restarts normal
   * operation.
   */
  public final boolean recovering ()
  {
    return yyerrstatus_ == 0;
  }

  private int yyaction (int yyn, YYStack yystack, int yylen) throws ParseException
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
          case 6:
  if (yyn == 6)
    /* "dap2.y":99  */ /* lalr1.java:480  */
    {unrecognizedresponse(parsestate); return YYABORT;};
  break;
    

  case 7:
  if (yyn == 7)
    /* "dap2.y":104  */ /* lalr1.java:480  */
    {tagparse(parsestate,SCAN_DATASET);};
  break;
    

  case 8:
  if (yyn == 8)
    /* "dap2.y":108  */ /* lalr1.java:480  */
    {tagparse(parsestate,SCAN_ATTR);};
  break;
    

  case 9:
  if (yyn == 9)
    /* "dap2.y":112  */ /* lalr1.java:480  */
    {tagparse(parsestate,SCAN_ERROR);};
  break;
    

  case 10:
  if (yyn == 10)
    /* "dap2.y":117  */ /* lalr1.java:480  */
    {datasetbody(parsestate,((yystack.valueAt (5-(4)))),((yystack.valueAt (5-(2)))));};
  break;
    

  case 11:
  if (yyn == 11)
    /* "dap2.y":122  */ /* lalr1.java:480  */
    {yyval=declarations(parsestate,null,null);};
  break;
    

  case 12:
  if (yyn == 12)
    /* "dap2.y":123  */ /* lalr1.java:480  */
    {yyval=declarations(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 13:
  if (yyn == 13)
    /* "dap2.y":130  */ /* lalr1.java:480  */
    {yyval=makebase(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 14:
  if (yyn == 14)
    /* "dap2.y":132  */ /* lalr1.java:480  */
    {if((yyval=makestructure(parsestate,((yystack.valueAt (7-(5)))),((yystack.valueAt (7-(6)))),((yystack.valueAt (7-(3))))))==null) {return YYABORT;}};
  break;
    

  case 15:
  if (yyn == 15)
    /* "dap2.y":134  */ /* lalr1.java:480  */
    {if((yyval=makesequence(parsestate,((yystack.valueAt (6-(5)))),((yystack.valueAt (6-(3))))))==null) {return YYABORT;}};
  break;
    

  case 16:
  if (yyn == 16)
    /* "dap2.y":137  */ /* lalr1.java:480  */
    {if((yyval=makegrid(parsestate,((yystack.valueAt (11-(10)))),((yystack.valueAt (11-(5)))),((yystack.valueAt (11-(8))))))==null) {return YYABORT;}};
  break;
    

  case 17:
  if (yyn == 17)
    /* "dap2.y":139  */ /* lalr1.java:480  */
    {dapsemanticerror(parsestate,EBADTYPE,"Unrecognized type"); return YYABORT;};
  break;
    

  case 18:
  if (yyn == 18)
    /* "dap2.y":144  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_BYTE;};
  break;
    

  case 19:
  if (yyn == 19)
    /* "dap2.y":145  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_INT16;};
  break;
    

  case 20:
  if (yyn == 20)
    /* "dap2.y":146  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_UINT16;};
  break;
    

  case 21:
  if (yyn == 21)
    /* "dap2.y":147  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_INT32;};
  break;
    

  case 22:
  if (yyn == 22)
    /* "dap2.y":148  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_UINT32;};
  break;
    

  case 23:
  if (yyn == 23)
    /* "dap2.y":149  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_FLOAT32;};
  break;
    

  case 24:
  if (yyn == 24)
    /* "dap2.y":150  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_FLOAT64;};
  break;
    

  case 25:
  if (yyn == 25)
    /* "dap2.y":151  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_URL;};
  break;
    

  case 26:
  if (yyn == 26)
    /* "dap2.y":152  */ /* lalr1.java:480  */
    {yyval=(Object)SCAN_STRING;};
  break;
    

  case 27:
  if (yyn == 27)
    /* "dap2.y":156  */ /* lalr1.java:480  */
    {yyval=arraydecls(parsestate,null,null);};
  break;
    

  case 28:
  if (yyn == 28)
    /* "dap2.y":157  */ /* lalr1.java:480  */
    {yyval=arraydecls(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 29:
  if (yyn == 29)
    /* "dap2.y":161  */ /* lalr1.java:480  */
    {yyval=arraydecl(parsestate,null,((yystack.valueAt (3-(2)))));};
  break;
    

  case 30:
  if (yyn == 30)
    /* "dap2.y":162  */ /* lalr1.java:480  */
    {yyval=arraydecl(parsestate,null,((yystack.valueAt (4-(3)))));};
  break;
    

  case 31:
  if (yyn == 31)
    /* "dap2.y":163  */ /* lalr1.java:480  */
    {yyval=arraydecl(parsestate,((yystack.valueAt (5-(2)))),((yystack.valueAt (5-(4)))));};
  break;
    

  case 32:
  if (yyn == 32)
    /* "dap2.y":165  */ /* lalr1.java:480  */
    {dapsemanticerror(parsestate,EDIMSIZE,"Illegal dimension declaration"); return YYABORT;};
  break;
    

  case 33:
  if (yyn == 33)
    /* "dap2.y":169  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 34:
  if (yyn == 34)
    /* "dap2.y":171  */ /* lalr1.java:480  */
    {dapsemanticerror(parsestate,EDDS,"Illegal dataset declaration"); return YYABORT;};
  break;
    

  case 35:
  if (yyn == 35)
    /* "dap2.y":174  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 36:
  if (yyn == 36)
    /* "dap2.y":177  */ /* lalr1.java:480  */
    {attributebody(parsestate,((yystack.valueAt (3-(2)))));};
  break;
    

  case 37:
  if (yyn == 37)
    /* "dap2.y":179  */ /* lalr1.java:480  */
    {dapsemanticerror(parsestate,EDAS,"Illegal DAS body"); return YYABORT;};
  break;
    

  case 38:
  if (yyn == 38)
    /* "dap2.y":183  */ /* lalr1.java:480  */
    {yyval=attrlist(parsestate,null,null);};
  break;
    

  case 39:
  if (yyn == 39)
    /* "dap2.y":184  */ /* lalr1.java:480  */
    {yyval=attrlist(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 40:
  if (yyn == 40)
    /* "dap2.y":188  */ /* lalr1.java:480  */
    {yyval=null;};
  break;
    

  case 41:
  if (yyn == 41)
    /* "dap2.y":190  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_BYTE);};
  break;
    

  case 42:
  if (yyn == 42)
    /* "dap2.y":192  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_INT16);};
  break;
    

  case 43:
  if (yyn == 43)
    /* "dap2.y":194  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_UINT16);};
  break;
    

  case 44:
  if (yyn == 44)
    /* "dap2.y":196  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_INT32);};
  break;
    

  case 45:
  if (yyn == 45)
    /* "dap2.y":198  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_UINT32);};
  break;
    

  case 46:
  if (yyn == 46)
    /* "dap2.y":200  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 47:
  if (yyn == 47)
    /* "dap2.y":202  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 48:
  if (yyn == 48)
    /* "dap2.y":204  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_STRING);};
  break;
    

  case 49:
  if (yyn == 49)
    /* "dap2.y":206  */ /* lalr1.java:480  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_URL);};
  break;
    

  case 50:
  if (yyn == 50)
    /* "dap2.y":207  */ /* lalr1.java:480  */
    {yyval=attrset(parsestate,((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 51:
  if (yyn == 51)
    /* "dap2.y":209  */ /* lalr1.java:480  */
    {dapsemanticerror(parsestate,EDAS,"Illegal attribute"); return YYABORT;};
  break;
    

  case 52:
  if (yyn == 52)
    /* "dap2.y":213  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_BYTE);};
  break;
    

  case 53:
  if (yyn == 53)
    /* "dap2.y":215  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_BYTE);};
  break;
    

  case 54:
  if (yyn == 54)
    /* "dap2.y":218  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_INT16);};
  break;
    

  case 55:
  if (yyn == 55)
    /* "dap2.y":220  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_INT16);};
  break;
    

  case 56:
  if (yyn == 56)
    /* "dap2.y":223  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_UINT16);};
  break;
    

  case 57:
  if (yyn == 57)
    /* "dap2.y":225  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_UINT16);};
  break;
    

  case 58:
  if (yyn == 58)
    /* "dap2.y":228  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_INT32);};
  break;
    

  case 59:
  if (yyn == 59)
    /* "dap2.y":230  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_INT32);};
  break;
    

  case 60:
  if (yyn == 60)
    /* "dap2.y":233  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_UINT32);};
  break;
    

  case 61:
  if (yyn == 61)
    /* "dap2.y":234  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_UINT32);};
  break;
    

  case 62:
  if (yyn == 62)
    /* "dap2.y":237  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 63:
  if (yyn == 63)
    /* "dap2.y":238  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 64:
  if (yyn == 64)
    /* "dap2.y":241  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 65:
  if (yyn == 65)
    /* "dap2.y":242  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 66:
  if (yyn == 66)
    /* "dap2.y":245  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_STRING);};
  break;
    

  case 67:
  if (yyn == 67)
    /* "dap2.y":246  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_STRING);};
  break;
    

  case 68:
  if (yyn == 68)
    /* "dap2.y":250  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_URL);};
  break;
    

  case 69:
  if (yyn == 69)
    /* "dap2.y":251  */ /* lalr1.java:480  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_URL);};
  break;
    

  case 70:
  if (yyn == 70)
    /* "dap2.y":255  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 71:
  if (yyn == 71)
    /* "dap2.y":259  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 72:
  if (yyn == 72)
    /* "dap2.y":260  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 73:
  if (yyn == 73)
    /* "dap2.y":271  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (3-(2)))); yyval=((yystack.valueAt (3-(3)))); yyval=null;};
  break;
    

  case 74:
  if (yyn == 74)
    /* "dap2.y":276  */ /* lalr1.java:480  */
    {errorbody(parsestate,((yystack.valueAt (7-(2)))),((yystack.valueAt (7-(3)))),((yystack.valueAt (7-(4)))),((yystack.valueAt (7-(5)))));};
  break;
    

  case 75:
  if (yyn == 75)
    /* "dap2.y":279  */ /* lalr1.java:480  */
    {yyval=null;};
  break;
    

  case 76:
  if (yyn == 76)
    /* "dap2.y":279  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 77:
  if (yyn == 77)
    /* "dap2.y":280  */ /* lalr1.java:480  */
    {yyval=null;};
  break;
    

  case 78:
  if (yyn == 78)
    /* "dap2.y":280  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 79:
  if (yyn == 79)
    /* "dap2.y":281  */ /* lalr1.java:480  */
    {yyval=null;};
  break;
    

  case 80:
  if (yyn == 80)
    /* "dap2.y":281  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 81:
  if (yyn == 81)
    /* "dap2.y":282  */ /* lalr1.java:480  */
    {yyval=null;};
  break;
    

  case 82:
  if (yyn == 82)
    /* "dap2.y":282  */ /* lalr1.java:480  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 83:
  if (yyn == 83)
    /* "dap2.y":288  */ /* lalr1.java:480  */
    {yyval=dapdecode(parsestate.lexstate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 84:
  if (yyn == 84)
    /* "dap2.y":289  */ /* lalr1.java:480  */
    {yyval=strdup("alias");};
  break;
    

  case 85:
  if (yyn == 85)
    /* "dap2.y":290  */ /* lalr1.java:480  */
    {yyval=strdup("array");};
  break;
    

  case 86:
  if (yyn == 86)
    /* "dap2.y":291  */ /* lalr1.java:480  */
    {yyval=strdup("attributes");};
  break;
    

  case 87:
  if (yyn == 87)
    /* "dap2.y":292  */ /* lalr1.java:480  */
    {yyval=strdup("byte");};
  break;
    

  case 88:
  if (yyn == 88)
    /* "dap2.y":293  */ /* lalr1.java:480  */
    {yyval=strdup("dataset");};
  break;
    

  case 89:
  if (yyn == 89)
    /* "dap2.y":294  */ /* lalr1.java:480  */
    {yyval=strdup("data");};
  break;
    

  case 90:
  if (yyn == 90)
    /* "dap2.y":295  */ /* lalr1.java:480  */
    {yyval=strdup("error");};
  break;
    

  case 91:
  if (yyn == 91)
    /* "dap2.y":296  */ /* lalr1.java:480  */
    {yyval=strdup("float32");};
  break;
    

  case 92:
  if (yyn == 92)
    /* "dap2.y":297  */ /* lalr1.java:480  */
    {yyval=strdup("float64");};
  break;
    

  case 93:
  if (yyn == 93)
    /* "dap2.y":298  */ /* lalr1.java:480  */
    {yyval=strdup("grid");};
  break;
    

  case 94:
  if (yyn == 94)
    /* "dap2.y":299  */ /* lalr1.java:480  */
    {yyval=strdup("int16");};
  break;
    

  case 95:
  if (yyn == 95)
    /* "dap2.y":300  */ /* lalr1.java:480  */
    {yyval=strdup("int32");};
  break;
    

  case 96:
  if (yyn == 96)
    /* "dap2.y":301  */ /* lalr1.java:480  */
    {yyval=strdup("maps");};
  break;
    

  case 97:
  if (yyn == 97)
    /* "dap2.y":302  */ /* lalr1.java:480  */
    {yyval=strdup("sequence");};
  break;
    

  case 98:
  if (yyn == 98)
    /* "dap2.y":303  */ /* lalr1.java:480  */
    {yyval=strdup("string");};
  break;
    

  case 99:
  if (yyn == 99)
    /* "dap2.y":304  */ /* lalr1.java:480  */
    {yyval=strdup("structure");};
  break;
    

  case 100:
  if (yyn == 100)
    /* "dap2.y":305  */ /* lalr1.java:480  */
    {yyval=strdup("uint16");};
  break;
    

  case 101:
  if (yyn == 101)
    /* "dap2.y":306  */ /* lalr1.java:480  */
    {yyval=strdup("uint32");};
  break;
    

  case 102:
  if (yyn == 102)
    /* "dap2.y":307  */ /* lalr1.java:480  */
    {yyval=strdup("url");};
  break;
    

  case 103:
  if (yyn == 103)
    /* "dap2.y":308  */ /* lalr1.java:480  */
    {yyval=strdup("code");};
  break;
    

  case 104:
  if (yyn == 104)
    /* "dap2.y":309  */ /* lalr1.java:480  */
    {yyval=strdup("message");};
  break;
    

  case 105:
  if (yyn == 105)
    /* "dap2.y":310  */ /* lalr1.java:480  */
    {yyval=strdup("program");};
  break;
    

  case 106:
  if (yyn == 106)
    /* "dap2.y":311  */ /* lalr1.java:480  */
    {yyval=strdup("program_type");};
  break;
    


/* "Dap2Parser.java":1076  */ /* lalr1.java:480  */
        default: break;
      }

    yy_symbol_print ("-> $$ =", yyr1_[yyn], yyval);

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
   * Parse input from the scanner that was specified at object construction
   * time.  Return whether the end of the input was reached successfully.
   *
   * @return <tt>true</tt> if the parsing succeeds.  Note that this does not
   *          imply that there were no syntax errors.
   */
   public boolean parse () throws ParseException, ParseException

  {
    


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

    yycdebug ("Starting parse\n");
    yyerrstatus_ = 0;

    /* Initialize the stack.  */
    yystack.push (yystate, yylval );



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
          return true;

        /* Take a decision.  First try without lookahead.  */
        yyn = yypact_[yystate];
        if (yy_pact_value_is_default_ (yyn))
          {
            label = YYDEFAULT;
            break;
          }

        /* Read a lookahead token.  */
        if (yychar == yyempty_)
          {


            yycdebug ("Reading a token: ");
            yychar = yylexer.yylex ();
            yylval = yylexer.getLVal ();

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
            return false;
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
              return false;

            
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
        return true;

        /* Abort.  */
      case YYABORT:
        return false;
      }
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

  private static final short yypact_ninf_ = -91;
  private static final short yytable_ninf_ = -1;

  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   STATE-NUM.  */
  private static final short yypact_[] = yypact_init();
  private static final short[] yypact_init()
  {
    return new short[]
    {
       6,   -91,   -91,   -91,   -91,     9,   -22,     7,   -16,   -91,
     -91,    10,   -91,   -91,   -91,    20,   -91,    37,   -91,   191,
      -6,    14,   -91,   -91,   -91,   -91,    17,   -91,   -91,    18,
     -91,    19,   -91,   -91,   -91,   271,   -91,   320,   -91,    27,
     -91,   -91,   320,   -91,   -91,   -91,   -91,   320,   320,   -91,
     320,   320,   -91,   -91,   -91,   320,   -91,   320,   320,   320,
     -91,   -91,   -91,   -91,   -91,    24,    43,    35,    39,    50,
      74,   -91,   -91,   -91,   -91,   -91,   -91,   -91,   -91,   -91,
     -91,   -91,   -91,   -91,    55,   -91,   -91,   -91,    60,    67,
      68,    70,    71,    73,   295,    77,    78,   295,   -91,   -91,
      65,    79,    66,    81,    76,    69,   127,   -91,     4,   -91,
     -91,   -20,   -91,   -13,   -91,   -12,   -91,   -10,   -91,    -9,
     -91,    32,   -91,   -91,   -91,    33,   -91,    34,    42,   -91,
     -91,   218,   -91,    80,    82,    75,    83,   346,   320,   320,
     -91,   -91,   159,   -91,   -91,    85,   -91,    88,   -91,    89,
     -91,    90,   -91,    91,   -91,   295,   -91,    92,   -91,    93,
     -91,   295,   -91,   -91,    95,    94,    96,   105,    97,   -91,
      98,   103,   100,   -91,   -91,   -91,   -91,   -91,   -91,   -91,
     -91,   -91,   -91,   102,   -91,    99,   -91,    12,   -91,   111,
     109,   -91,   -91,   -91,   -91,   118,   244,   -91,   320,   106,
     -91
    };
  }

/* YYDEFACT[STATE-NUM] -- Default reduction number in state STATE-NUM.
   Performed when YYTABLE does not specify something else to do.  Zero
   means the default is an error.  */
  private static final byte yydefact_[] = yydefact_init();
  private static final byte[] yydefact_init()
  {
    return new byte[]
    {
       0,     6,     8,     7,     9,     0,     0,     0,     0,     1,
      11,     2,    37,    38,     4,    75,     5,     0,     3,     0,
       0,    77,    17,    18,    23,    24,     0,    19,    21,     0,
      26,     0,    20,    22,    25,     0,    12,     0,    51,    84,
      85,    86,    87,   103,    88,    89,    90,    91,    92,    93,
      94,    95,    96,   104,    97,    98,    99,   100,   101,   102,
     106,   105,    83,    36,    39,     0,     0,     0,     0,    79,
       0,    11,    11,    34,    84,    87,    91,    92,    94,    95,
      98,   100,   101,   102,     0,    33,    35,    27,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,    40,    38,
       0,     0,     0,    81,     0,     0,     0,    10,     0,    73,
      52,     0,    62,     0,    64,     0,    54,     0,    58,     0,
      72,     0,    66,    71,    56,     0,    60,     0,     0,    68,
      70,     0,    76,     0,     0,     0,     0,     0,     0,     0,
      32,    13,     0,    28,    41,     0,    46,     0,    47,     0,
      42,     0,    44,     0,    48,     0,    43,     0,    45,     0,
      49,     0,    50,    78,     0,     0,     0,     0,     0,    27,
      83,     0,     0,    53,    63,    65,    55,    59,    67,    57,
      61,    69,    80,     0,    74,     0,    15,     0,    29,     0,
       0,    82,    11,    14,    30,     0,     0,    31,     0,     0,
      16
    };
  }

/* YYPGOTO[NTERM-NUM].  */
  private static final byte yypgoto_[] = yypgoto_init();
  private static final byte[] yypgoto_init()
  {
    return new byte[]
    {
     -91,   -91,   -91,   -91,   -91,   -91,   -69,   -15,   -91,   -17,
     -91,   -91,   -37,   -91,    54,   -91,   -91,   -91,   -91,   -91,
     -91,   -91,   -91,   -91,   -91,    -7,   -90,   -91,   -91,   -91,
     -91,   -91,   -91,   -18
    };
  }

/* YYDEFGOTO[NTERM-NUM].  */
  private static final short yydefgoto_[] = yydefgoto_init();
  private static final short[] yydefgoto_init()
  {
    return new short[]
    {
      -1,     5,     6,     7,     8,    11,    17,    36,    37,   108,
     143,    84,    85,    14,    19,    64,   111,   117,   125,   119,
     127,   113,   115,   121,   128,   129,   130,    65,    16,    21,
      69,   103,   136,    86
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
      87,    66,   105,   106,   122,   140,    10,     1,    12,     9,
     144,     2,    15,   140,     3,   145,     4,   146,   148,    18,
     150,   152,   147,   149,    89,   151,   153,    20,    67,    90,
      91,    68,    92,    93,   141,    13,   142,    94,    22,    95,
      96,    97,   193,    23,   142,    70,    71,    72,    24,    25,
      26,    27,    28,    88,    98,    29,    30,    31,    32,    33,
      34,   100,   154,   156,   158,   178,    35,   155,   157,   159,
      22,    99,   160,   101,   102,    23,   123,   161,   104,   123,
      24,    25,    26,    27,    28,   107,   109,    29,    30,    31,
      32,    33,    34,   110,   112,   132,   114,   116,   138,   118,
     134,   168,   169,   124,   126,   133,   135,   137,   164,   165,
     163,   173,   166,    66,   174,   175,   176,   177,   179,   180,
     183,   185,   167,   196,   172,   182,   184,   186,    22,   189,
     192,   188,   191,    23,   190,   195,   200,   123,    24,    25,
      26,    27,    28,   123,   194,    29,    30,    31,    32,    33,
      34,   197,   187,   131,   181,     0,   139,     0,     0,     0,
       0,   199,    74,    40,    41,    75,    43,    44,    45,    46,
      76,    77,    49,    78,    79,    52,    53,    54,    80,    56,
      81,    82,    83,    60,    61,   170,     0,     0,     0,     0,
       0,     0,    38,   171,    39,    40,    41,    42,    43,    44,
      45,    46,    47,    48,    49,    50,    51,    52,    53,    54,
      55,    56,    57,    58,    59,    60,    61,    62,     0,    38,
      63,    39,    40,    41,    42,    43,    44,    45,    46,    47,
      48,    49,    50,    51,    52,    53,    54,    55,    56,    57,
      58,    59,    60,    61,    62,    22,     0,   162,     0,     0,
      23,     0,     0,     0,     0,    24,    25,    26,    27,    28,
       0,     0,    29,    30,    31,    32,    33,    34,     0,     0,
       0,     0,    73,   198,    74,    40,    41,    75,    43,    44,
      45,    46,    76,    77,    49,    78,    79,    52,    53,    54,
      80,    56,    81,    82,    83,    60,    61,    62,    74,    40,
      41,    75,    43,    44,    45,    46,    76,    77,    49,    78,
      79,    52,    53,    54,    80,    56,    81,    82,    83,    60,
      61,    62,   120,    74,    40,    41,    75,    43,    44,    45,
      46,    76,    77,    49,    78,    79,    52,    53,    54,    80,
      56,    81,    82,    83,    60,    61,    62,    22,     0,     0,
       0,     0,    23,     0,     0,     0,     0,    24,    25,    26,
      27,    28,     0,     0,    29,    30,    31,    32,    33,    34
    };
  }

private static final short yycheck_[] = yycheck_init();
  private static final short[] yycheck_init()
  {
    return new short[]
    {
      37,    19,    71,    72,    94,     1,    28,     1,     1,     0,
      30,     5,    28,     1,     8,    35,    10,    30,    30,     9,
      30,    30,    35,    35,    42,    35,    35,     7,    34,    47,
      48,    17,    50,    51,    30,    28,    32,    55,     1,    57,
      58,    59,    30,     6,    32,    28,    28,    28,    11,    12,
      13,    14,    15,    26,    30,    18,    19,    20,    21,    22,
      23,    26,    30,    30,    30,   155,    29,    35,    35,    35,
       1,    28,    30,    34,    24,     6,    94,    35,     4,    97,
      11,    12,    13,    14,    15,    30,    26,    18,    19,    20,
      21,    22,    23,    26,    26,    30,    26,    26,    29,    26,
      34,   138,   139,    26,    26,    26,    25,    31,    26,    34,
      30,    26,    29,   131,    26,    26,    26,    26,    26,    26,
      26,    16,   137,   192,   142,    30,    30,    30,     1,    26,
      31,    33,    30,     6,    34,    26,    30,   155,    11,    12,
      13,    14,    15,   161,    33,    18,    19,    20,    21,    22,
      23,    33,   169,    99,   161,    -1,    29,    -1,    -1,    -1,
      -1,   198,     3,     4,     5,     6,     7,     8,     9,    10,
      11,    12,    13,    14,    15,    16,    17,    18,    19,    20,
      21,    22,    23,    24,    25,    26,    -1,    -1,    -1,    -1,
      -1,    -1,     1,    34,     3,     4,     5,     6,     7,     8,
       9,    10,    11,    12,    13,    14,    15,    16,    17,    18,
      19,    20,    21,    22,    23,    24,    25,    26,    -1,     1,
      29,     3,     4,     5,     6,     7,     8,     9,    10,    11,
      12,    13,    14,    15,    16,    17,    18,    19,    20,    21,
      22,    23,    24,    25,    26,     1,    -1,    29,    -1,    -1,
       6,    -1,    -1,    -1,    -1,    11,    12,    13,    14,    15,
      -1,    -1,    18,    19,    20,    21,    22,    23,    -1,    -1,
      -1,    -1,     1,    29,     3,     4,     5,     6,     7,     8,
       9,    10,    11,    12,    13,    14,    15,    16,    17,    18,
      19,    20,    21,    22,    23,    24,    25,    26,     3,     4,
       5,     6,     7,     8,     9,    10,    11,    12,    13,    14,
      15,    16,    17,    18,    19,    20,    21,    22,    23,    24,
      25,    26,    27,     3,     4,     5,     6,     7,     8,     9,
      10,    11,    12,    13,    14,    15,    16,    17,    18,    19,
      20,    21,    22,    23,    24,    25,    26,     1,    -1,    -1,
      -1,    -1,     6,    -1,    -1,    -1,    -1,    11,    12,    13,
      14,    15,    -1,    -1,    18,    19,    20,    21,    22,    23
    };
  }

/* YYSTOS[STATE-NUM] -- The (internal number of the) accessing
   symbol of state STATE-NUM.  */
  private static final byte yystos_[] = yystos_init();
  private static final byte[] yystos_init()
  {
    return new byte[]
    {
       0,     1,     5,     8,    10,    37,    38,    39,    40,     0,
      28,    41,     1,    28,    49,    28,    64,    42,     9,    50,
       7,    65,     1,     6,    11,    12,    13,    14,    15,    18,
      19,    20,    21,    22,    23,    29,    43,    44,     1,     3,
       4,     5,     6,     7,     8,     9,    10,    11,    12,    13,
      14,    15,    16,    17,    18,    19,    20,    21,    22,    23,
      24,    25,    26,    29,    51,    63,    69,    34,    17,    66,
      28,    28,    28,     1,     3,     6,    11,    12,    14,    15,
      19,    21,    22,    23,    47,    48,    69,    48,    26,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    30,    28,
      26,    34,    24,    67,     4,    42,    42,    30,    45,    26,
      26,    52,    26,    57,    26,    58,    26,    53,    26,    55,
      27,    59,    62,    69,    26,    54,    26,    56,    60,    61,
      62,    50,    30,    26,    34,    25,    68,    31,    29,    29,
       1,    30,    32,    46,    30,    35,    30,    35,    30,    35,
      30,    35,    30,    35,    30,    35,    30,    35,    30,    35,
      30,    35,    29,    30,    26,    34,    29,    43,    48,    48,
      26,    34,    69,    26,    26,    26,    26,    26,    62,    26,
      26,    61,    30,    26,    30,    16,    30,    45,    33,    26,
      34,    30,    31,    30,    33,    26,    42,    33,    29,    48,
      30
    };
  }

/* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final byte yyr1_[] = yyr1_init();
  private static final byte[] yyr1_init()
  {
    return new byte[]
    {
       0,    36,    37,    37,    37,    37,    37,    38,    39,    40,
      41,    42,    42,    43,    43,    43,    43,    43,    44,    44,
      44,    44,    44,    44,    44,    44,    44,    45,    45,    46,
      46,    46,    46,    47,    47,    48,    49,    49,    50,    50,
      51,    51,    51,    51,    51,    51,    51,    51,    51,    51,
      51,    51,    52,    52,    53,    53,    54,    54,    55,    55,
      56,    56,    57,    57,    58,    58,    59,    59,    60,    60,
      61,    62,    62,    63,    64,    65,    65,    66,    66,    67,
      67,    68,    68,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69
    };
  }

/* YYR2[YYN] -- Number of symbols on the right hand side of rule YYN.  */
  private static final byte yyr2_[] = yyr2_init();
  private static final byte[] yyr2_init()
  {
    return new byte[]
    {
       0,     2,     2,     3,     2,     2,     1,     1,     1,     1,
       5,     0,     2,     4,     7,     6,    11,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     0,     2,     3,
       4,     5,     1,     1,     1,     1,     3,     1,     0,     2,
       2,     4,     4,     4,     4,     4,     4,     4,     4,     4,
       4,     1,     1,     3,     1,     3,     1,     3,     1,     3,
       1,     3,     1,     3,     1,     3,     1,     3,     1,     3,
       1,     1,     1,     3,     7,     0,     4,     0,     4,     0,
       4,     0,     4,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1
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
     275,   276,   277,   278,   279,   280,   281,   282,   123,   125,
      59,    58,    91,    93,    61,    44
    };
  }

  /* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     First, the terminals, then, starting at \a yyntokens_, nonterminals.  */
  private static final String yytname_[] = yytname_init();
  private static final String[] yytname_init()
  {
    return new String[]
    {
  "$end", "error", "$undefined", "SCAN_ALIAS", "SCAN_ARRAY", "SCAN_ATTR",
  "SCAN_BYTE", "SCAN_CODE", "SCAN_DATASET", "SCAN_DATA", "SCAN_ERROR",
  "SCAN_FLOAT32", "SCAN_FLOAT64", "SCAN_GRID", "SCAN_INT16", "SCAN_INT32",
  "SCAN_MAPS", "SCAN_MESSAGE", "SCAN_SEQUENCE", "SCAN_STRING",
  "SCAN_STRUCTURE", "SCAN_UINT16", "SCAN_UINT32", "SCAN_URL", "SCAN_PTYPE",
  "SCAN_PROG", "WORD_WORD", "WORD_STRING", "'{'", "'}'", "';'", "':'",
  "'['", "']'", "'='", "','", "$accept", "start", "dataset", "attr", "err",
  "datasetbody", "declarations", "declaration", "base_type", "array_decls",
  "array_decl", "datasetname", "var_name", "attributebody", "attr_list",
  "attribute", "bytes", "int16", "uint16", "int32", "uint32", "float32",
  "float64", "strs", "urls", "url", "str_or_id", "alias", "errorbody",
  "errorcode", "errormsg", "errorptype", "errorprog", "name", null
    };
  }

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] = yyrline_init();
  private static final short[] yyrline_init()
  {
    return new short[]
    {
       0,    95,    95,    96,    97,    98,    99,   103,   107,   111,
     116,   122,   123,   129,   131,   133,   135,   138,   144,   145,
     146,   147,   148,   149,   150,   151,   152,   156,   157,   161,
     162,   163,   164,   169,   170,   174,   177,   178,   183,   184,
     188,   189,   191,   193,   195,   197,   199,   201,   203,   205,
     207,   208,   213,   214,   218,   219,   223,   224,   228,   229,
     233,   234,   237,   238,   241,   242,   245,   246,   250,   251,
     255,   259,   260,   271,   275,   279,   279,   280,   280,   281,
     281,   282,   282,   288,   289,   290,   291,   292,   293,   294,
     295,   296,   297,   298,   299,   300,   301,   302,   303,   304,
     305,   306,   307,   308,   309,   310,   311
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
       2,     2,     2,     2,    35,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,    31,    30,
       2,    34,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,    32,     2,    33,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    28,     2,    29,     2,     2,     2,     2,
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
      25,    26,    27
    };
  }

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 369;
  private static final int yynnts_ = 34;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 9;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 36;

  private static final int yyuser_token_number_max_ = 282;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */
/* Unqualified %code blocks.  */
/* "dap2.y":26  */ /* lalr1.java:1064  */

 
    /**
     * Instantiates the Bison-generated parser.
     * @param factory the factory for generating tree nodes
     */

    public Dap2Parser(BaseTypeFactory factory)
    {
	super(factory);
	this.yylexer = new Dap2Lex(this);
	super.lexstate = (Dap2Lex)this.yylexer;
    }

    /* the parse function allows the specification of a
       new stream in case one is reusing the parser
    */

    public boolean parse(String input) throws ParseException
    {
	((Dap2Lex)yylexer).reset(parsestate);
	((Dap2Lex)yylexer).setText(input);
	return parse();
    }

    String url = null;

    public void setURL(String url) {
        this.url = url;
    }

    public String getURL() {return this.url;}

/* "Dap2Parser.java":1917  */ /* lalr1.java:1064  */

}

/* "dap2.y":314  */ /* lalr1.java:1068  */


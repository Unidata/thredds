/* A Bison parser, made by GNU Bison 2.6.2.  */

/* Skeleton implementation for Bison LALR(1) parsers in Java
   
      Copyright (C) 2007-2012 Free Software Foundation, Inc.
   
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

/* "%code imports" blocks.  */
/* Line 33 of lalr1.java  */
/* Line 12 of "dap.y"  */

import opendap.dap.*;
import opendap.dap.BaseTypeFactory;
import opendap.dap.parsers.ParseException;
import java.io.*;


/* Line 33 of lalr1.java  */
/* Line 48 of "DapParser.java"  */

/**
 * A Bison parser, automatically generated from <tt>dap.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
public class DapParser extends Dapparse
{
    /** Version number for the Bison executable that generated this parser.  */
  public static final String bisonVersion = "2.6.2";

  /** Name of the skeleton that generated this parser.  */
  public static final String bisonSkeleton = "lalr1.java";


  /** True if verbose error messages are enabled.  */
  public boolean errorVerbose = true;



  /** Token returned by the scanner to signal the end of its input.  */
  public static final int EOF = 0;

/* Tokens.  */
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_ALIAS = 258;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_ARRAY = 259;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_ATTR = 260;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_BYTE = 261;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_CODE = 262;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_DATASET = 263;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_DATA = 264;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_ERROR = 265;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_FLOAT32 = 266;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_FLOAT64 = 267;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_GRID = 268;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_INT16 = 269;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_INT32 = 270;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_MAPS = 271;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_MESSAGE = 272;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_SEQUENCE = 273;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_STRING = 274;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_STRUCTURE = 275;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_UINT16 = 276;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_UINT32 = 277;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_URL = 278;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_WORD = 279;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_PTYPE = 280;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_PROG = 281;



  

  /**
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>DapParser</tt>.
   */
  public interface Lexer {
    

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * @return the semantic value of the last scanned token.  */
    Object getLVal ();

    /**
     * Entry point for the scanner.  Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * of the token.
     * @return the token identifier corresponding to the next token. */
    int yylex () throws ParseException;

    /**
     * Entry point for error reporting.  Emits an error
     * in a user-defined way.
     *
     * 
     * @param s The string for the error message.  */
     void yyerror (String s);
  }

  /** The object doing lexical analysis for us.  */
  private Lexer yylexer;
  
  



  /**
   * Instantiates the Bison-generated parser.
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  public DapParser (Lexer yylexer) {
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

  private final int yylex () throws ParseException {
    return yylexer.yylex ();
  }
  protected final void yyerror (String s) {
    yylexer.yyerror (s);
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

    public final void push (int state, Object value			    ) {
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
   * return success (<tt>true</tt>).  */
  public static final int YYACCEPT = 0;

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return failure (<tt>false</tt>).  */
  public static final int YYABORT = 1;

  /**
   * Returned by a Bison action in order to start error recovery without
   * printing an error message.  */
  public static final int YYERROR = 2;

  // Internal return codes that are not supported for user semantic
  // actions.
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
   * operation.  */
  public final boolean recovering ()
  {
    return yyerrstatus_ == 0;
  }

  private int yyaction (int yyn, YYStack yystack, int yylen) throws ParseException
  {
    Object yyval;
    

    /* If YYLEN is nonzero, implement the default value of the action:
       `$$ = $1'.  Otherwise, use the top of the stack.

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
	  case 3:
  if (yyn == 3)
    /* Line 351 of lalr1.java  */
/* Line 86 of "dap.y"  */
    {return YYACCEPT;};
  break;
    

  case 6:
  if (yyn == 6)
    /* Line 351 of lalr1.java  */
/* Line 89 of "dap.y"  */
    {unrecognizedresponse(parsestate);};
  break;
    

  case 7:
  if (yyn == 7)
    /* Line 351 of lalr1.java  */
/* Line 94 of "dap.y"  */
    {tagparse(parsestate,SCAN_DATASET);};
  break;
    

  case 8:
  if (yyn == 8)
    /* Line 351 of lalr1.java  */
/* Line 98 of "dap.y"  */
    {tagparse(parsestate,SCAN_ATTR);};
  break;
    

  case 9:
  if (yyn == 9)
    /* Line 351 of lalr1.java  */
/* Line 102 of "dap.y"  */
    {tagparse(parsestate,SCAN_ERROR);};
  break;
    

  case 10:
  if (yyn == 10)
    /* Line 351 of lalr1.java  */
/* Line 107 of "dap.y"  */
    {datasetbody(parsestate,((yystack.valueAt (5-(4)))),((yystack.valueAt (5-(2)))));};
  break;
    

  case 11:
  if (yyn == 11)
    /* Line 351 of lalr1.java  */
/* Line 112 of "dap.y"  */
    {yyval=declarations(parsestate,null,null);};
  break;
    

  case 12:
  if (yyn == 12)
    /* Line 351 of lalr1.java  */
/* Line 113 of "dap.y"  */
    {yyval=declarations(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 13:
  if (yyn == 13)
    /* Line 351 of lalr1.java  */
/* Line 120 of "dap.y"  */
    {yyval=makebase(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 14:
  if (yyn == 14)
    /* Line 351 of lalr1.java  */
/* Line 122 of "dap.y"  */
    {if((yyval = makestructure(parsestate,((yystack.valueAt (7-(5)))),((yystack.valueAt (7-(6)))),((yystack.valueAt (7-(3))))))==null) {return YYABORT;}};
  break;
    

  case 15:
  if (yyn == 15)
    /* Line 351 of lalr1.java  */
/* Line 124 of "dap.y"  */
    {if((yyval = makesequence(parsestate,((yystack.valueAt (6-(5)))),((yystack.valueAt (6-(3))))))==null) {return YYABORT;}};
  break;
    

  case 16:
  if (yyn == 16)
    /* Line 351 of lalr1.java  */
/* Line 127 of "dap.y"  */
    {if((yyval = makegrid(parsestate,((yystack.valueAt (11-(10)))),((yystack.valueAt (11-(5)))),((yystack.valueAt (11-(8))))))==null) {return YYABORT;}};
  break;
    

  case 17:
  if (yyn == 17)
    /* Line 351 of lalr1.java  */
/* Line 129 of "dap.y"  */
    {daperror(parsestate,"Unrecognized type"); return YYABORT;};
  break;
    

  case 18:
  if (yyn == 18)
    /* Line 351 of lalr1.java  */
/* Line 134 of "dap.y"  */
    {yyval=(Object)SCAN_BYTE;};
  break;
    

  case 19:
  if (yyn == 19)
    /* Line 351 of lalr1.java  */
/* Line 135 of "dap.y"  */
    {yyval=(Object)SCAN_INT16;};
  break;
    

  case 20:
  if (yyn == 20)
    /* Line 351 of lalr1.java  */
/* Line 136 of "dap.y"  */
    {yyval=(Object)SCAN_UINT16;};
  break;
    

  case 21:
  if (yyn == 21)
    /* Line 351 of lalr1.java  */
/* Line 137 of "dap.y"  */
    {yyval=(Object)SCAN_INT32;};
  break;
    

  case 22:
  if (yyn == 22)
    /* Line 351 of lalr1.java  */
/* Line 138 of "dap.y"  */
    {yyval=(Object)SCAN_UINT32;};
  break;
    

  case 23:
  if (yyn == 23)
    /* Line 351 of lalr1.java  */
/* Line 139 of "dap.y"  */
    {yyval=(Object)SCAN_FLOAT32;};
  break;
    

  case 24:
  if (yyn == 24)
    /* Line 351 of lalr1.java  */
/* Line 140 of "dap.y"  */
    {yyval=(Object)SCAN_FLOAT64;};
  break;
    

  case 25:
  if (yyn == 25)
    /* Line 351 of lalr1.java  */
/* Line 141 of "dap.y"  */
    {yyval=(Object)SCAN_URL;};
  break;
    

  case 26:
  if (yyn == 26)
    /* Line 351 of lalr1.java  */
/* Line 142 of "dap.y"  */
    {yyval=(Object)SCAN_STRING;};
  break;
    

  case 27:
  if (yyn == 27)
    /* Line 351 of lalr1.java  */
/* Line 146 of "dap.y"  */
    {yyval=arraydecls(parsestate,null,null);};
  break;
    

  case 28:
  if (yyn == 28)
    /* Line 351 of lalr1.java  */
/* Line 147 of "dap.y"  */
    {yyval=arraydecls(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 29:
  if (yyn == 29)
    /* Line 351 of lalr1.java  */
/* Line 151 of "dap.y"  */
    {yyval=arraydecl(parsestate,null,((yystack.valueAt (3-(2)))));};
  break;
    

  case 30:
  if (yyn == 30)
    /* Line 351 of lalr1.java  */
/* Line 152 of "dap.y"  */
    {yyval=arraydecl(parsestate,null,((yystack.valueAt (4-(3)))));};
  break;
    

  case 31:
  if (yyn == 31)
    /* Line 351 of lalr1.java  */
/* Line 153 of "dap.y"  */
    {yyval=arraydecl(parsestate,((yystack.valueAt (5-(2)))),((yystack.valueAt (5-(4)))));};
  break;
    

  case 32:
  if (yyn == 32)
    /* Line 351 of lalr1.java  */
/* Line 155 of "dap.y"  */
    {daperror(parsestate,"Illegal dimension declaration/reference"); return YYABORT;};
  break;
    

  case 33:
  if (yyn == 33)
    /* Line 351 of lalr1.java  */
/* Line 159 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 34:
  if (yyn == 34)
    /* Line 351 of lalr1.java  */
/* Line 161 of "dap.y"  */
    {daperror(parsestate,"Illegal dataset declaration"); return YYABORT;};
  break;
    

  case 35:
  if (yyn == 35)
    /* Line 351 of lalr1.java  */
/* Line 164 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 36:
  if (yyn == 36)
    /* Line 351 of lalr1.java  */
/* Line 167 of "dap.y"  */
    {attributebody(parsestate,((yystack.valueAt (3-(2)))));};
  break;
    

  case 37:
  if (yyn == 37)
    /* Line 351 of lalr1.java  */
/* Line 169 of "dap.y"  */
    {daperror(parsestate,"Illegal DAS body"); return YYABORT;};
  break;
    

  case 38:
  if (yyn == 38)
    /* Line 351 of lalr1.java  */
/* Line 173 of "dap.y"  */
    {yyval=attrlist(parsestate,null,null);};
  break;
    

  case 39:
  if (yyn == 39)
    /* Line 351 of lalr1.java  */
/* Line 174 of "dap.y"  */
    {yyval=attrlist(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 40:
  if (yyn == 40)
    /* Line 351 of lalr1.java  */
/* Line 178 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 41:
  if (yyn == 41)
    /* Line 351 of lalr1.java  */
/* Line 180 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_BYTE);};
  break;
    

  case 42:
  if (yyn == 42)
    /* Line 351 of lalr1.java  */
/* Line 182 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_INT16);};
  break;
    

  case 43:
  if (yyn == 43)
    /* Line 351 of lalr1.java  */
/* Line 184 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_UINT16);};
  break;
    

  case 44:
  if (yyn == 44)
    /* Line 351 of lalr1.java  */
/* Line 186 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_INT32);};
  break;
    

  case 45:
  if (yyn == 45)
    /* Line 351 of lalr1.java  */
/* Line 188 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_UINT32);};
  break;
    

  case 46:
  if (yyn == 46)
    /* Line 351 of lalr1.java  */
/* Line 190 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 47:
  if (yyn == 47)
    /* Line 351 of lalr1.java  */
/* Line 192 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 48:
  if (yyn == 48)
    /* Line 351 of lalr1.java  */
/* Line 194 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_STRING);};
  break;
    

  case 49:
  if (yyn == 49)
    /* Line 351 of lalr1.java  */
/* Line 196 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_URL);};
  break;
    

  case 50:
  if (yyn == 50)
    /* Line 351 of lalr1.java  */
/* Line 197 of "dap.y"  */
    {yyval=attrset(parsestate,((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 51:
  if (yyn == 51)
    /* Line 351 of lalr1.java  */
/* Line 199 of "dap.y"  */
    {daperror(parsestate,"Illegal attribute"); return YYABORT;};
  break;
    

  case 52:
  if (yyn == 52)
    /* Line 351 of lalr1.java  */
/* Line 203 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_BYTE);};
  break;
    

  case 53:
  if (yyn == 53)
    /* Line 351 of lalr1.java  */
/* Line 205 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_BYTE);};
  break;
    

  case 54:
  if (yyn == 54)
    /* Line 351 of lalr1.java  */
/* Line 208 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_INT16);};
  break;
    

  case 55:
  if (yyn == 55)
    /* Line 351 of lalr1.java  */
/* Line 210 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_INT16);};
  break;
    

  case 56:
  if (yyn == 56)
    /* Line 351 of lalr1.java  */
/* Line 213 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_UINT16);};
  break;
    

  case 57:
  if (yyn == 57)
    /* Line 351 of lalr1.java  */
/* Line 215 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_UINT16);};
  break;
    

  case 58:
  if (yyn == 58)
    /* Line 351 of lalr1.java  */
/* Line 218 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_INT32);};
  break;
    

  case 59:
  if (yyn == 59)
    /* Line 351 of lalr1.java  */
/* Line 220 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_INT32);};
  break;
    

  case 60:
  if (yyn == 60)
    /* Line 351 of lalr1.java  */
/* Line 223 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_UINT32);};
  break;
    

  case 61:
  if (yyn == 61)
    /* Line 351 of lalr1.java  */
/* Line 224 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_UINT32);};
  break;
    

  case 62:
  if (yyn == 62)
    /* Line 351 of lalr1.java  */
/* Line 227 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 63:
  if (yyn == 63)
    /* Line 351 of lalr1.java  */
/* Line 228 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 64:
  if (yyn == 64)
    /* Line 351 of lalr1.java  */
/* Line 231 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 65:
  if (yyn == 65)
    /* Line 351 of lalr1.java  */
/* Line 232 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 66:
  if (yyn == 66)
    /* Line 351 of lalr1.java  */
/* Line 235 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_STRING);};
  break;
    

  case 67:
  if (yyn == 67)
    /* Line 351 of lalr1.java  */
/* Line 236 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_STRING);};
  break;
    

  case 68:
  if (yyn == 68)
    /* Line 351 of lalr1.java  */
/* Line 240 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_URL);};
  break;
    

  case 69:
  if (yyn == 69)
    /* Line 351 of lalr1.java  */
/* Line 241 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_URL);};
  break;
    

  case 70:
  if (yyn == 70)
    /* Line 351 of lalr1.java  */
/* Line 245 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 71:
  if (yyn == 71)
    /* Line 351 of lalr1.java  */
/* Line 249 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 72:
  if (yyn == 72)
    /* Line 351 of lalr1.java  */
/* Line 259 of "dap.y"  */
    {yyval=((yystack.valueAt (3-(2)))); yyval=((yystack.valueAt (3-(3)))); yyval=null;};
  break;
    

  case 73:
  if (yyn == 73)
    /* Line 351 of lalr1.java  */
/* Line 264 of "dap.y"  */
    {errorbody(parsestate,((yystack.valueAt (7-(2)))),((yystack.valueAt (7-(3)))),((yystack.valueAt (7-(4)))),((yystack.valueAt (7-(5)))));};
  break;
    

  case 74:
  if (yyn == 74)
    /* Line 351 of lalr1.java  */
/* Line 267 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 75:
  if (yyn == 75)
    /* Line 351 of lalr1.java  */
/* Line 267 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 76:
  if (yyn == 76)
    /* Line 351 of lalr1.java  */
/* Line 268 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 77:
  if (yyn == 77)
    /* Line 351 of lalr1.java  */
/* Line 268 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 78:
  if (yyn == 78)
    /* Line 351 of lalr1.java  */
/* Line 269 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 79:
  if (yyn == 79)
    /* Line 351 of lalr1.java  */
/* Line 269 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 80:
  if (yyn == 80)
    /* Line 351 of lalr1.java  */
/* Line 270 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 81:
  if (yyn == 81)
    /* Line 351 of lalr1.java  */
/* Line 270 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 82:
  if (yyn == 82)
    /* Line 351 of lalr1.java  */
/* Line 276 of "dap.y"  */
    {yyval=unescapeDAPName(((yystack.valueAt (1-(1)))));};
  break;
    

  case 83:
  if (yyn == 83)
    /* Line 351 of lalr1.java  */
/* Line 277 of "dap.y"  */
    {yyval=strdup("alias");};
  break;
    

  case 84:
  if (yyn == 84)
    /* Line 351 of lalr1.java  */
/* Line 278 of "dap.y"  */
    {yyval=strdup("array");};
  break;
    

  case 85:
  if (yyn == 85)
    /* Line 351 of lalr1.java  */
/* Line 279 of "dap.y"  */
    {yyval=strdup("attributes");};
  break;
    

  case 86:
  if (yyn == 86)
    /* Line 351 of lalr1.java  */
/* Line 280 of "dap.y"  */
    {yyval=strdup("byte");};
  break;
    

  case 87:
  if (yyn == 87)
    /* Line 351 of lalr1.java  */
/* Line 281 of "dap.y"  */
    {yyval=strdup("dataset");};
  break;
    

  case 88:
  if (yyn == 88)
    /* Line 351 of lalr1.java  */
/* Line 282 of "dap.y"  */
    {yyval=strdup("data");};
  break;
    

  case 89:
  if (yyn == 89)
    /* Line 351 of lalr1.java  */
/* Line 283 of "dap.y"  */
    {yyval=strdup("error");};
  break;
    

  case 90:
  if (yyn == 90)
    /* Line 351 of lalr1.java  */
/* Line 284 of "dap.y"  */
    {yyval=strdup("float32");};
  break;
    

  case 91:
  if (yyn == 91)
    /* Line 351 of lalr1.java  */
/* Line 285 of "dap.y"  */
    {yyval=strdup("float64");};
  break;
    

  case 92:
  if (yyn == 92)
    /* Line 351 of lalr1.java  */
/* Line 286 of "dap.y"  */
    {yyval=strdup("grid");};
  break;
    

  case 93:
  if (yyn == 93)
    /* Line 351 of lalr1.java  */
/* Line 287 of "dap.y"  */
    {yyval=strdup("int16");};
  break;
    

  case 94:
  if (yyn == 94)
    /* Line 351 of lalr1.java  */
/* Line 288 of "dap.y"  */
    {yyval=strdup("int32");};
  break;
    

  case 95:
  if (yyn == 95)
    /* Line 351 of lalr1.java  */
/* Line 289 of "dap.y"  */
    {yyval=strdup("maps");};
  break;
    

  case 96:
  if (yyn == 96)
    /* Line 351 of lalr1.java  */
/* Line 290 of "dap.y"  */
    {yyval=strdup("sequence");};
  break;
    

  case 97:
  if (yyn == 97)
    /* Line 351 of lalr1.java  */
/* Line 291 of "dap.y"  */
    {yyval=strdup("string");};
  break;
    

  case 98:
  if (yyn == 98)
    /* Line 351 of lalr1.java  */
/* Line 292 of "dap.y"  */
    {yyval=strdup("structure");};
  break;
    

  case 99:
  if (yyn == 99)
    /* Line 351 of lalr1.java  */
/* Line 293 of "dap.y"  */
    {yyval=strdup("uint16");};
  break;
    

  case 100:
  if (yyn == 100)
    /* Line 351 of lalr1.java  */
/* Line 294 of "dap.y"  */
    {yyval=strdup("uint32");};
  break;
    

  case 101:
  if (yyn == 101)
    /* Line 351 of lalr1.java  */
/* Line 295 of "dap.y"  */
    {yyval=strdup("url");};
  break;
    

  case 102:
  if (yyn == 102)
    /* Line 351 of lalr1.java  */
/* Line 296 of "dap.y"  */
    {yyval=strdup("code");};
  break;
    

  case 103:
  if (yyn == 103)
    /* Line 351 of lalr1.java  */
/* Line 297 of "dap.y"  */
    {yyval=strdup("message");};
  break;
    

  case 104:
  if (yyn == 104)
    /* Line 351 of lalr1.java  */
/* Line 298 of "dap.y"  */
    {yyval=strdup("program");};
  break;
    


/* Line 351 of lalr1.java  */
/* Line 1134 of "DapParser.java"  */
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
			         Object yyvaluep				 )
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
    /// Lookahead and lookahead in internal form.
    int yychar = yyempty_;
    int yytoken = 0;

    /* State.  */
    int yyn = 0;
    int yylen = 0;
    int yystate = 0;

    YYStack yystack = new YYStack ();

    /* Error handling.  */
    int yynerrs_ = 0;
    

    /// Semantic value of the lookahead.
    Object yylval = null;

    yycdebug ("Starting parse\n");
    yyerrstatus_ = 0;


    /* Initialize the stack.  */
    yystack.push (yystate, yylval);

    int label = YYNEWSTATE;
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
	    yychar = yylex ();
            
            yylval = yylexer.getLVal ();
          }

        /* Convert token to internal form.  */
        if (yychar <= EOF)
          {
	    yychar = yytoken = EOF;
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

	    if (yychar <= EOF)
	      {
	      /* Return failure if at end of input.  */
	      if (yychar == EOF)
	        return false;
	      }
	    else
	      yychar = yyempty_;
          }

        /* Else will try to reuse lookahead token after shifting the error
           token.  */
        label = YYERRLAB1;
        break;

      /*---------------------------------------------------.
      | errorlab -- error raised explicitly by YYERROR.  |
      `---------------------------------------------------*/
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
        yyerrstatus_ = 3;	/* Each real token shifted decrements this.  */

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

	    /* Pop the current state because it cannot handle the error token.  */
	    if (yystack.height == 0)
	      return false;

	    
	    yystack.pop ();
	    yystate = yystack.stateAt (0);
	    if (yydebug > 0)
	      yystack.print (yyDebugStream);
          }

	

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
    if (errorVerbose)
      {
        /* There are many possibilities here to consider:
           - Assume YYFAIL is not used.  It's too flawed to consider.
             See
             <http://lists.gnu.org/archive/html/bison-patches/2009-12/msg00024.html>
             for details.  YYERROR is fine as it does not invoke this
             function.
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
            // FIXME: This method of building the message is not compatible
            // with internationalization.
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
   * Whether the given <code>yytable_</code> value indicates a syntax error.
   * @param yyvalue   the value to check
   */
  private static boolean yy_table_value_is_error_ (int yyvalue)
  {
    return yyvalue == yytable_ninf_;
  }

  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
     STATE-NUM.  */
  private static final short yypact_ninf_ = -68;
  private static final short yypact_[] =
  {
         4,   -68,   -68,   -68,   -68,     7,   -16,     1,    -8,   -68,
     -68,    18,   -68,   -68,   -68,     9,   -68,   149,   -68,    70,
       2,    47,   -68,   -68,   -68,   -68,    40,   -68,   -68,    43,
     -68,    77,   -68,   -68,   -68,   244,   -68,   268,   -68,   268,
     -68,   -68,   268,   -68,   -68,   -68,   -68,   268,   268,   -68,
     268,   268,   -68,   -68,   -68,   268,   -68,   268,   268,   268,
     -68,   -68,   -68,   -68,    66,    78,    83,    76,    85,   108,
     -68,   -68,   -68,   -68,   -68,   -68,   -68,   -68,   -68,   -68,
     -68,   -68,   -68,    84,   -68,   -68,   -68,   268,    90,    91,
      92,    93,    94,    95,    96,    97,   101,   -68,   -68,   119,
     128,   120,   130,   124,   172,   195,   -68,     5,   -68,   -68,
     -21,   -68,   -19,   -68,   -12,   -68,   -11,   -68,    -9,   -68,
      -3,   -68,   -68,    68,   -68,    72,   -68,    74,   -68,   123,
     -68,   129,   133,   132,   138,   292,   268,   268,   -68,   -68,
      39,   -68,   -68,   150,   -68,   151,   -68,   152,   -68,   155,
     -68,   156,   -68,    95,   -68,   157,   -68,   158,   -68,   101,
     -68,   -68,   159,   165,   168,   182,   170,   -68,   171,   178,
     179,   -68,   -68,   -68,   -68,   -68,   -68,   -68,   -68,   -68,
     -68,   175,   -68,   181,   -68,    37,   -68,   173,   196,   -68,
     -68,   -68,   -68,   189,   218,   -68,   268,   193,   -68
  };

  /* YYDEFACT[S] -- default reduction number in state S.  Performed when
     YYTABLE doesn't specify something else to do.  Zero means the
     default is an error.  */
  private static final byte yydefact_[] =
  {
         0,     6,     8,     7,     9,     0,     0,     0,     0,     1,
      11,     2,    37,    38,     4,    74,     5,     0,     3,     0,
       0,    76,    17,    18,    23,    24,     0,    19,    21,     0,
      26,     0,    20,    22,    25,     0,    12,     0,    51,    83,
      84,    85,    86,   102,    87,    88,    89,    90,    91,    92,
      93,    94,    95,   103,    96,    97,    98,    99,   100,   101,
      82,   104,    36,    39,     0,     0,     0,     0,    78,     0,
      11,    11,    34,    83,    86,    90,    91,    93,    94,    97,
      99,   100,   101,     0,    33,    35,    27,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,    40,    38,     0,
       0,     0,    80,     0,     0,     0,    10,     0,    72,    52,
       0,    62,     0,    64,     0,    54,     0,    58,     0,    71,
       0,    66,    56,     0,    60,     0,    70,     0,    68,     0,
      75,     0,     0,     0,     0,     0,     0,     0,    32,    13,
       0,    28,    41,     0,    46,     0,    47,     0,    42,     0,
      44,     0,    48,     0,    43,     0,    45,     0,    49,     0,
      50,    77,     0,     0,     0,     0,     0,    27,    82,     0,
       0,    53,    63,    65,    55,    59,    67,    57,    61,    69,
      79,     0,    73,     0,    15,     0,    29,     0,     0,    81,
      11,    14,    30,     0,     0,    31,     0,     0,    16
  };

  /* YYPGOTO[NTERM-NUM].  */
  private static final short yypgoto_[] =
  {
       -68,   -68,   -68,   -68,   -68,   -68,   -67,    99,   -68,    58,
     -68,   -68,   -37,   -68,   137,   -68,   -68,   -68,   -68,   -68,
     -68,   -68,   -68,   -68,   -68,    67,    75,   -68,   -68,   -68,
     -68,   -68,   -68,   -18
  };

  /* YYDEFGOTO[NTERM-NUM].  */
  private static final short
  yydefgoto_[] =
  {
        -1,     5,     6,     7,     8,    11,    17,    36,    37,   107,
     141,    83,    84,    14,    19,    63,   110,   116,   123,   118,
     125,   112,   114,   120,   127,   128,   121,    64,    16,    21,
      68,   102,   134,    85
  };

  /* YYTABLE[YYPACT[STATE-NUM]].  What to do in state STATE-NUM.  If
     positive, shift that token.  If negative, reduce the rule which
     number is the opposite.  If YYTABLE_NINF_, syntax error.  */
  private static final short yytable_ninf_ = -1;
  private static final short
  yytable_[] =
  {
        86,    65,    12,   104,   105,     1,   138,     9,   142,     2,
     144,    10,     3,   143,     4,   145,    20,   146,   148,    15,
     150,    87,   147,   149,    88,   151,   152,    18,    13,    89,
      90,   153,    91,    92,   139,    66,   140,    93,   138,    94,
      95,    96,    73,    40,    41,    74,    43,    44,    45,    46,
      75,    76,    49,    77,    78,    52,    53,    54,    79,    56,
      80,    81,    82,   168,    67,    61,   191,    69,   140,   108,
      70,    38,   169,    39,    40,    41,    42,    43,    44,    45,
      46,    47,    48,    49,    50,    51,    52,    53,    54,    55,
      56,    57,    58,    59,    60,    97,    61,   154,    62,   166,
     167,   156,   155,   158,    71,    98,   157,    99,   159,   100,
     101,    65,   103,   106,   109,   111,   113,   115,   117,   119,
     122,   124,   170,   194,    38,   126,    39,    40,    41,    42,
      43,    44,    45,    46,    47,    48,    49,    50,    51,    52,
      53,    54,    55,    56,    57,    58,    59,    60,   130,    61,
      22,   160,   131,   132,   135,    23,   133,   162,   161,   197,
      24,    25,    26,    27,    28,   163,   164,    29,    30,    31,
      32,    33,    34,    22,   171,   172,   173,    35,    23,   174,
     175,   177,   178,    24,    25,    26,    27,    28,   180,   181,
      29,    30,    31,    32,    33,    34,    22,   182,   183,   184,
     136,    23,   187,   186,   189,   192,    24,    25,    26,    27,
      28,   190,   188,    29,    30,    31,    32,    33,    34,    22,
     193,   195,   198,   137,    23,   185,   179,     0,   176,    24,
      25,    26,    27,    28,   165,   129,    29,    30,    31,    32,
      33,    34,     0,     0,     0,    72,   196,    73,    40,    41,
      74,    43,    44,    45,    46,    75,    76,    49,    77,    78,
      52,    53,    54,    79,    56,    80,    81,    82,    60,     0,
      61,    73,    40,    41,    74,    43,    44,    45,    46,    75,
      76,    49,    77,    78,    52,    53,    54,    79,    56,    80,
      81,    82,    60,    22,    61,     0,     0,     0,    23,     0,
       0,     0,     0,    24,    25,    26,    27,    28,     0,     0,
      29,    30,    31,    32,    33,    34
  };

  /* YYCHECK.  */
  private static final short
  yycheck_[] =
  {
        37,    19,     1,    70,    71,     1,     1,     0,    29,     5,
      29,    27,     8,    34,    10,    34,     7,    29,    29,    27,
      29,    39,    34,    34,    42,    34,    29,     9,    27,    47,
      48,    34,    50,    51,    29,    33,    31,    55,     1,    57,
      58,    59,     3,     4,     5,     6,     7,     8,     9,    10,
      11,    12,    13,    14,    15,    16,    17,    18,    19,    20,
      21,    22,    23,    24,    17,    26,    29,    27,    31,    87,
      27,     1,    33,     3,     4,     5,     6,     7,     8,     9,
      10,    11,    12,    13,    14,    15,    16,    17,    18,    19,
      20,    21,    22,    23,    24,    29,    26,    29,    28,   136,
     137,    29,    34,    29,    27,    27,    34,    24,    34,    33,
      25,   129,     4,    29,    24,    24,    24,    24,    24,    24,
      24,    24,   140,   190,     1,    24,     3,     4,     5,     6,
       7,     8,     9,    10,    11,    12,    13,    14,    15,    16,
      17,    18,    19,    20,    21,    22,    23,    24,    29,    26,
       1,    28,    24,    33,    30,     6,    26,    24,    29,   196,
      11,    12,    13,    14,    15,    33,    28,    18,    19,    20,
      21,    22,    23,     1,    24,    24,    24,    28,     6,    24,
      24,    24,    24,    11,    12,    13,    14,    15,    29,    24,
      18,    19,    20,    21,    22,    23,     1,    29,    16,    29,
      28,     6,    24,    32,    29,    32,    11,    12,    13,    14,
      15,    30,    33,    18,    19,    20,    21,    22,    23,     1,
      24,    32,    29,    28,     6,   167,   159,    -1,   153,    11,
      12,    13,    14,    15,   135,    98,    18,    19,    20,    21,
      22,    23,    -1,    -1,    -1,     1,    28,     3,     4,     5,
       6,     7,     8,     9,    10,    11,    12,    13,    14,    15,
      16,    17,    18,    19,    20,    21,    22,    23,    24,    -1,
      26,     3,     4,     5,     6,     7,     8,     9,    10,    11,
      12,    13,    14,    15,    16,    17,    18,    19,    20,    21,
      22,    23,    24,     1,    26,    -1,    -1,    -1,     6,    -1,
      -1,    -1,    -1,    11,    12,    13,    14,    15,    -1,    -1,
      18,    19,    20,    21,    22,    23
  };

  /* STOS_[STATE-NUM] -- The (internal number of the) accessing
     symbol of state STATE-NUM.  */
  private static final byte
  yystos_[] =
  {
         0,     1,     5,     8,    10,    36,    37,    38,    39,     0,
      27,    40,     1,    27,    48,    27,    63,    41,     9,    49,
       7,    64,     1,     6,    11,    12,    13,    14,    15,    18,
      19,    20,    21,    22,    23,    28,    42,    43,     1,     3,
       4,     5,     6,     7,     8,     9,    10,    11,    12,    13,
      14,    15,    16,    17,    18,    19,    20,    21,    22,    23,
      24,    26,    28,    50,    62,    68,    33,    17,    65,    27,
      27,    27,     1,     3,     6,    11,    12,    14,    15,    19,
      21,    22,    23,    46,    47,    68,    47,    68,    68,    68,
      68,    68,    68,    68,    68,    68,    68,    29,    27,    24,
      33,    25,    66,     4,    41,    41,    29,    44,    68,    24,
      51,    24,    56,    24,    57,    24,    52,    24,    54,    24,
      58,    61,    24,    53,    24,    55,    24,    59,    60,    49,
      29,    24,    33,    26,    67,    30,    28,    28,     1,    29,
      31,    45,    29,    34,    29,    34,    29,    34,    29,    34,
      29,    34,    29,    34,    29,    34,    29,    34,    29,    34,
      28,    29,    24,    33,    28,    42,    47,    47,    24,    33,
      68,    24,    24,    24,    24,    24,    61,    24,    24,    60,
      29,    24,    29,    16,    29,    44,    32,    24,    33,    29,
      30,    29,    32,    24,    41,    32,    28,    47,    29
  };

  /* TOKEN_NUMBER_[YYLEX-NUM] -- Internal symbol number corresponding
     to YYLEX-NUM.  */
  private static final short
  yytoken_number_[] =
  {
         0,   256,   257,   258,   259,   260,   261,   262,   263,   264,
     265,   266,   267,   268,   269,   270,   271,   272,   273,   274,
     275,   276,   277,   278,   279,   280,   281,   123,   125,    59,
      58,    91,    93,    61,    44
  };

  /* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final byte
  yyr1_[] =
  {
         0,    35,    36,    36,    36,    36,    36,    37,    38,    39,
      40,    41,    41,    42,    42,    42,    42,    42,    43,    43,
      43,    43,    43,    43,    43,    43,    43,    44,    44,    45,
      45,    45,    45,    46,    46,    47,    48,    48,    49,    49,
      50,    50,    50,    50,    50,    50,    50,    50,    50,    50,
      50,    50,    51,    51,    52,    52,    53,    53,    54,    54,
      55,    55,    56,    56,    57,    57,    58,    58,    59,    59,
      60,    61,    62,    63,    64,    64,    65,    65,    66,    66,
      67,    67,    68,    68,    68,    68,    68,    68,    68,    68,
      68,    68,    68,    68,    68,    68,    68,    68,    68,    68,
      68,    68,    68,    68,    68
  };

  /* YYR2[YYN] -- Number of symbols composing right hand side of rule YYN.  */
  private static final byte
  yyr2_[] =
  {
         0,     2,     2,     3,     2,     2,     1,     1,     1,     1,
       5,     0,     2,     4,     7,     6,    11,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     0,     2,     3,
       4,     5,     1,     1,     1,     1,     3,     1,     0,     2,
       2,     4,     4,     4,     4,     4,     4,     4,     4,     4,
       4,     1,     1,     3,     1,     3,     1,     3,     1,     3,
       1,     3,     1,     3,     1,     3,     1,     3,     1,     3,
       1,     1,     3,     7,     0,     4,     0,     4,     0,     4,
       0,     4,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1
  };

  /* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     First, the terminals, then, starting at \a yyntokens_, nonterminals.  */
  private static final String yytname_[] =
  {
    "$end", "error", "$undefined", "SCAN_ALIAS", "SCAN_ARRAY", "SCAN_ATTR",
  "SCAN_BYTE", "SCAN_CODE", "SCAN_DATASET", "SCAN_DATA", "SCAN_ERROR",
  "SCAN_FLOAT32", "SCAN_FLOAT64", "SCAN_GRID", "SCAN_INT16", "SCAN_INT32",
  "SCAN_MAPS", "SCAN_MESSAGE", "SCAN_SEQUENCE", "SCAN_STRING",
  "SCAN_STRUCTURE", "SCAN_UINT16", "SCAN_UINT32", "SCAN_URL", "SCAN_WORD",
  "SCAN_PTYPE", "SCAN_PROG", "'{'", "'}'", "';'", "':'", "'['", "']'",
  "'='", "','", "$accept", "start", "dataset", "attr", "err",
  "datasetbody", "declarations", "declaration", "base_type", "array_decls",
  "array_decl", "datasetname", "var_name", "attributebody", "attr_list",
  "attribute", "bytes", "int16", "uint16", "int32", "uint32", "float32",
  "float64", "strs", "urls", "url", "str_or_id", "alias", "errorbody",
  "errorcode", "errormsg", "errorptype", "errorprog", "name", null
  };

  /* YYRHS -- A `-1'-separated list of the rules' RHS.  */
  private static final byte yyrhs_[] =
  {
        36,     0,    -1,    37,    40,    -1,    37,    40,     9,    -1,
      38,    48,    -1,    39,    63,    -1,     1,    -1,     8,    -1,
       5,    -1,    10,    -1,    27,    41,    28,    46,    29,    -1,
      -1,    41,    42,    -1,    43,    47,    44,    29,    -1,    20,
      27,    41,    28,    47,    44,    29,    -1,    18,    27,    41,
      28,    47,    29,    -1,    13,    27,     4,    30,    42,    16,
      30,    41,    28,    47,    29,    -1,     1,    -1,     6,    -1,
      14,    -1,    21,    -1,    15,    -1,    22,    -1,    11,    -1,
      12,    -1,    23,    -1,    19,    -1,    -1,    44,    45,    -1,
      31,    24,    32,    -1,    31,    33,    24,    32,    -1,    31,
      68,    33,    24,    32,    -1,     1,    -1,    47,    -1,     1,
      -1,    68,    -1,    27,    49,    28,    -1,     1,    -1,    -1,
      49,    50,    -1,    62,    29,    -1,     6,    68,    51,    29,
      -1,    14,    68,    52,    29,    -1,    21,    68,    53,    29,
      -1,    15,    68,    54,    29,    -1,    22,    68,    55,    29,
      -1,    11,    68,    56,    29,    -1,    12,    68,    57,    29,
      -1,    19,    68,    58,    29,    -1,    23,    68,    59,    29,
      -1,    68,    27,    49,    28,    -1,     1,    -1,    24,    -1,
      51,    34,    24,    -1,    24,    -1,    52,    34,    24,    -1,
      24,    -1,    53,    34,    24,    -1,    24,    -1,    54,    34,
      24,    -1,    24,    -1,    55,    34,    24,    -1,    24,    -1,
      56,    34,    24,    -1,    24,    -1,    57,    34,    24,    -1,
      61,    -1,    58,    34,    61,    -1,    60,    -1,    59,    34,
      60,    -1,    24,    -1,    24,    -1,     3,    68,    68,    -1,
      27,    64,    65,    66,    67,    28,    29,    -1,    -1,     7,
      33,    24,    29,    -1,    -1,    17,    33,    24,    29,    -1,
      -1,    25,    33,    24,    29,    -1,    -1,    26,    33,    24,
      29,    -1,    24,    -1,     3,    -1,     4,    -1,     5,    -1,
       6,    -1,     8,    -1,     9,    -1,    10,    -1,    11,    -1,
      12,    -1,    13,    -1,    14,    -1,    15,    -1,    16,    -1,
      18,    -1,    19,    -1,    20,    -1,    21,    -1,    22,    -1,
      23,    -1,     7,    -1,    17,    -1,    26,    -1
  };

  /* YYPRHS[YYN] -- Index of the first RHS symbol of rule number YYN in
     YYRHS.  */
  private static final short yyprhs_[] =
  {
         0,     0,     3,     6,    10,    13,    16,    18,    20,    22,
      24,    30,    31,    34,    39,    47,    54,    66,    68,    70,
      72,    74,    76,    78,    80,    82,    84,    86,    87,    90,
      94,    99,   105,   107,   109,   111,   113,   117,   119,   120,
     123,   126,   131,   136,   141,   146,   151,   156,   161,   166,
     171,   176,   178,   180,   184,   186,   190,   192,   196,   198,
     202,   204,   208,   210,   214,   216,   220,   222,   226,   228,
     232,   234,   236,   240,   248,   249,   254,   255,   260,   261,
     266,   267,   272,   274,   276,   278,   280,   282,   284,   286,
     288,   290,   292,   294,   296,   298,   300,   302,   304,   306,
     308,   310,   312,   314,   316
  };

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] =
  {
         0,    85,    85,    86,    87,    88,    89,    93,    97,   101,
     106,   112,   113,   119,   121,   123,   125,   128,   134,   135,
     136,   137,   138,   139,   140,   141,   142,   146,   147,   151,
     152,   153,   154,   159,   160,   164,   167,   168,   173,   174,
     178,   179,   181,   183,   185,   187,   189,   191,   193,   195,
     197,   198,   203,   204,   208,   209,   213,   214,   218,   219,
     223,   224,   227,   228,   231,   232,   235,   236,   240,   241,
     245,   249,   259,   263,   267,   267,   268,   268,   269,   269,
     270,   270,   276,   277,   278,   279,   280,   281,   282,   283,
     284,   285,   286,   287,   288,   289,   290,   291,   292,   293,
     294,   295,   296,   297,   298
  };

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
		       yyrhs_[yyprhs_[yyrule] + yyi],
		       ((yystack.valueAt (yynrhs-(yyi + 1)))));
  }

  /* YYTRANSLATE(YYLEX) -- Bison symbol number corresponding to YYLEX.  */
  private static final byte yytranslate_table_[] =
  {
         0,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,    34,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,    30,    29,
       2,    33,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,    31,     2,    32,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    27,     2,    28,     2,     2,     2,     2,
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
      25,    26
  };

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 315;
  private static final int yynnts_ = 34;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 9;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 35;

  private static final int yyuser_token_number_max_ = 281;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */
/* Unqualified %code blocks.  */
/* Line 923 of lalr1.java  */
/* Line 19 of "dap.y"  */

 
    /**
     * Instantiates the Bison-generated parser.
     * @param yylexer The scanner that will supply tokens to the parser.
     */

    public DapParser(BaseTypeFactory factory)
    {
	super(factory);
	this.yylexer = new Daplex(this);
	super.lexstate = (Daplex)this.yylexer;
    }

    /* the parse function allows the specification of a
       new stream in case one is reusing the parser
    */

    public boolean parse(String input) throws ParseException
    {
	((Daplex)yylexer).reset(parsestate);
	((Daplex)yylexer).setText(input);
	return parse();
    }

    String url = null;

    public void setURL(String url) {
        this.url = url;
    }

    public String getURL() {return this.url;}


/* Line 923 of lalr1.java  */
/* Line 1960 of "DapParser.java"  */

}

/* Line 927 of lalr1.java  */
/* Line 301 of "dap.y"  */



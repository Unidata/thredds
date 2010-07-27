/* A Bison parser, made by GNU Bison 2.4.2.  */

/* Skeleton implementation for Bison LALR(1) parsers in Java
   
      Copyright (C) 2007-2010 Free Software Foundation, Inc.
   
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

package opendap.dap.parser;
/* First part of user declarations.  */

/* "%code imports" blocks.  */

/* Line 33 of lalr1.java  */
/* Line 12 of "dap.y"  */

import opendap.dap.BaseTypeFactory;
import opendap.dap.parser.ParseException;
import java.io.*;



/* Line 33 of lalr1.java  */
/* Line 49 of "DapParser.java"  */

/**
 * A Bison parser, automatically generated from <tt>dap.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
public class DapParser extends opendap.dap.parser.Dapparse
{
    /** Version number for the Bison executable that generated this parser.  */
  public static final String bisonVersion = "2.4.2";

  /** Name of the skeleton that generated this parser.  */
  public static final String bisonSkeleton = "lalr1.java";


  /** True if verbose error messages are enabled.  */
  public boolean errorVerbose = false;



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
  
  
    /* User arguments.  */
    protected final InputStream stream;



  /**
   * Instantiates the Bison-generated parser.
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  public DapParser (Lexer yylexer, InputStream stream) {
    this.yylexer = yylexer;
    this.stream = stream;
	  
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
      height--;
    }

    public final void pop (int num) {
      // Avoid memory leaks... garbage collection is a white lie!
      if (num > 0) {
	java.util.Arrays.fill (valueStack, height - num + 1, height, null);
        
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

      for (int i = 0; i < height; i++)
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

  /**
   * Returned by a Bison action in order to print an error message and start
   * error recovery.  Formally deprecated in Bison 2.4.2's NEWS entry, where
   * a plan to phase it out is discussed.  */
  public static final int YYFAIL = 3;

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
	  case 6:
  if (yyn == 6)
    
/* Line 354 of lalr1.java  */
/* Line 76 of "dap.y"  */
    {yyval=unrecognizedresponse(parsestate);};
  break;
    

  case 7:
  if (yyn == 7)
    
/* Line 354 of lalr1.java  */
/* Line 81 of "dap.y"  */
    {datasetbody(parsestate,((yystack.valueAt (5-(4)))),((yystack.valueAt (5-(2)))));};
  break;
    

  case 8:
  if (yyn == 8)
    
/* Line 354 of lalr1.java  */
/* Line 86 of "dap.y"  */
    {yyval=declarations(parsestate,null,null);};
  break;
    

  case 9:
  if (yyn == 9)
    
/* Line 354 of lalr1.java  */
/* Line 87 of "dap.y"  */
    {yyval=declarations(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 10:
  if (yyn == 10)
    
/* Line 354 of lalr1.java  */
/* Line 94 of "dap.y"  */
    {yyval=makebase(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 11:
  if (yyn == 11)
    
/* Line 354 of lalr1.java  */
/* Line 96 of "dap.y"  */
    {if((yyval = makestructure(parsestate,((yystack.valueAt (7-(5)))),((yystack.valueAt (7-(6)))),((yystack.valueAt (7-(3))))))==null) {return YYABORT;}};
  break;
    

  case 12:
  if (yyn == 12)
    
/* Line 354 of lalr1.java  */
/* Line 98 of "dap.y"  */
    {if((yyval = makesequence(parsestate,((yystack.valueAt (6-(5)))),((yystack.valueAt (6-(3))))))==null) {return YYABORT;}};
  break;
    

  case 13:
  if (yyn == 13)
    
/* Line 354 of lalr1.java  */
/* Line 101 of "dap.y"  */
    {if((yyval = makegrid(parsestate,((yystack.valueAt (11-(10)))),((yystack.valueAt (11-(5)))),((yystack.valueAt (11-(8))))))==null) {return YYABORT;}};
  break;
    

  case 14:
  if (yyn == 14)
    
/* Line 354 of lalr1.java  */
/* Line 103 of "dap.y"  */
    {daperror(parsestate,"Unrecognized type"); return YYABORT;};
  break;
    

  case 15:
  if (yyn == 15)
    
/* Line 354 of lalr1.java  */
/* Line 108 of "dap.y"  */
    {yyval=(Object)SCAN_BYTE;};
  break;
    

  case 16:
  if (yyn == 16)
    
/* Line 354 of lalr1.java  */
/* Line 109 of "dap.y"  */
    {yyval=(Object)SCAN_INT16;};
  break;
    

  case 17:
  if (yyn == 17)
    
/* Line 354 of lalr1.java  */
/* Line 110 of "dap.y"  */
    {yyval=(Object)SCAN_UINT16;};
  break;
    

  case 18:
  if (yyn == 18)
    
/* Line 354 of lalr1.java  */
/* Line 111 of "dap.y"  */
    {yyval=(Object)SCAN_INT32;};
  break;
    

  case 19:
  if (yyn == 19)
    
/* Line 354 of lalr1.java  */
/* Line 112 of "dap.y"  */
    {yyval=(Object)SCAN_UINT32;};
  break;
    

  case 20:
  if (yyn == 20)
    
/* Line 354 of lalr1.java  */
/* Line 113 of "dap.y"  */
    {yyval=(Object)SCAN_FLOAT32;};
  break;
    

  case 21:
  if (yyn == 21)
    
/* Line 354 of lalr1.java  */
/* Line 114 of "dap.y"  */
    {yyval=(Object)SCAN_FLOAT64;};
  break;
    

  case 22:
  if (yyn == 22)
    
/* Line 354 of lalr1.java  */
/* Line 115 of "dap.y"  */
    {yyval=(Object)SCAN_URL;};
  break;
    

  case 23:
  if (yyn == 23)
    
/* Line 354 of lalr1.java  */
/* Line 116 of "dap.y"  */
    {yyval=(Object)SCAN_STRING;};
  break;
    

  case 24:
  if (yyn == 24)
    
/* Line 354 of lalr1.java  */
/* Line 120 of "dap.y"  */
    {yyval=arraydecls(parsestate,null,null);};
  break;
    

  case 25:
  if (yyn == 25)
    
/* Line 354 of lalr1.java  */
/* Line 121 of "dap.y"  */
    {yyval=arraydecls(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 26:
  if (yyn == 26)
    
/* Line 354 of lalr1.java  */
/* Line 125 of "dap.y"  */
    {yyval=arraydecl(parsestate,null,((yystack.valueAt (3-(2)))));};
  break;
    

  case 27:
  if (yyn == 27)
    
/* Line 354 of lalr1.java  */
/* Line 126 of "dap.y"  */
    {yyval=arraydecl(parsestate,((yystack.valueAt (5-(2)))),((yystack.valueAt (5-(4)))));};
  break;
    

  case 28:
  if (yyn == 28)
    
/* Line 354 of lalr1.java  */
/* Line 128 of "dap.y"  */
    {daperror(parsestate,"Illegal dimension declaration"); return YYABORT;};
  break;
    

  case 29:
  if (yyn == 29)
    
/* Line 354 of lalr1.java  */
/* Line 132 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 30:
  if (yyn == 30)
    
/* Line 354 of lalr1.java  */
/* Line 134 of "dap.y"  */
    {daperror(parsestate,"Illegal dataset declaration"); return YYABORT;};
  break;
    

  case 31:
  if (yyn == 31)
    
/* Line 354 of lalr1.java  */
/* Line 137 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 32:
  if (yyn == 32)
    
/* Line 354 of lalr1.java  */
/* Line 139 of "dap.y"  */
    {dassetup(parsestate);};
  break;
    

  case 33:
  if (yyn == 33)
    
/* Line 354 of lalr1.java  */
/* Line 142 of "dap.y"  */
    {attributebody(parsestate,((yystack.valueAt (3-(2)))));};
  break;
    

  case 34:
  if (yyn == 34)
    
/* Line 354 of lalr1.java  */
/* Line 144 of "dap.y"  */
    {daperror(parsestate,"Illegal DAS body"); return YYABORT;};
  break;
    

  case 35:
  if (yyn == 35)
    
/* Line 354 of lalr1.java  */
/* Line 148 of "dap.y"  */
    {yyval=attrlist(parsestate,null,null);};
  break;
    

  case 36:
  if (yyn == 36)
    
/* Line 354 of lalr1.java  */
/* Line 149 of "dap.y"  */
    {yyval=attrlist(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 37:
  if (yyn == 37)
    
/* Line 354 of lalr1.java  */
/* Line 153 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 38:
  if (yyn == 38)
    
/* Line 354 of lalr1.java  */
/* Line 155 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_BYTE);};
  break;
    

  case 39:
  if (yyn == 39)
    
/* Line 354 of lalr1.java  */
/* Line 157 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_INT16);};
  break;
    

  case 40:
  if (yyn == 40)
    
/* Line 354 of lalr1.java  */
/* Line 159 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_UINT16);};
  break;
    

  case 41:
  if (yyn == 41)
    
/* Line 354 of lalr1.java  */
/* Line 161 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_INT32);};
  break;
    

  case 42:
  if (yyn == 42)
    
/* Line 354 of lalr1.java  */
/* Line 163 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_UINT32);};
  break;
    

  case 43:
  if (yyn == 43)
    
/* Line 354 of lalr1.java  */
/* Line 165 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 44:
  if (yyn == 44)
    
/* Line 354 of lalr1.java  */
/* Line 167 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 45:
  if (yyn == 45)
    
/* Line 354 of lalr1.java  */
/* Line 169 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_STRING);};
  break;
    

  case 46:
  if (yyn == 46)
    
/* Line 354 of lalr1.java  */
/* Line 171 of "dap.y"  */
    {yyval=attribute(parsestate,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),(Object)SCAN_URL);};
  break;
    

  case 47:
  if (yyn == 47)
    
/* Line 354 of lalr1.java  */
/* Line 172 of "dap.y"  */
    {yyval=attrset(parsestate,((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 48:
  if (yyn == 48)
    
/* Line 354 of lalr1.java  */
/* Line 174 of "dap.y"  */
    {daperror(parsestate,"Illegal attribute"); return YYABORT;};
  break;
    

  case 49:
  if (yyn == 49)
    
/* Line 354 of lalr1.java  */
/* Line 178 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_BYTE);};
  break;
    

  case 50:
  if (yyn == 50)
    
/* Line 354 of lalr1.java  */
/* Line 180 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_BYTE);};
  break;
    

  case 51:
  if (yyn == 51)
    
/* Line 354 of lalr1.java  */
/* Line 183 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_INT16);};
  break;
    

  case 52:
  if (yyn == 52)
    
/* Line 354 of lalr1.java  */
/* Line 185 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_INT16);};
  break;
    

  case 53:
  if (yyn == 53)
    
/* Line 354 of lalr1.java  */
/* Line 188 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_UINT16);};
  break;
    

  case 54:
  if (yyn == 54)
    
/* Line 354 of lalr1.java  */
/* Line 190 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_UINT16);};
  break;
    

  case 55:
  if (yyn == 55)
    
/* Line 354 of lalr1.java  */
/* Line 193 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_INT32);};
  break;
    

  case 56:
  if (yyn == 56)
    
/* Line 354 of lalr1.java  */
/* Line 195 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_INT32);};
  break;
    

  case 57:
  if (yyn == 57)
    
/* Line 354 of lalr1.java  */
/* Line 198 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_UINT32);};
  break;
    

  case 58:
  if (yyn == 58)
    
/* Line 354 of lalr1.java  */
/* Line 199 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_UINT32);};
  break;
    

  case 59:
  if (yyn == 59)
    
/* Line 354 of lalr1.java  */
/* Line 202 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 60:
  if (yyn == 60)
    
/* Line 354 of lalr1.java  */
/* Line 203 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_FLOAT32);};
  break;
    

  case 61:
  if (yyn == 61)
    
/* Line 354 of lalr1.java  */
/* Line 206 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 62:
  if (yyn == 62)
    
/* Line 354 of lalr1.java  */
/* Line 207 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_FLOAT64);};
  break;
    

  case 63:
  if (yyn == 63)
    
/* Line 354 of lalr1.java  */
/* Line 210 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_STRING);};
  break;
    

  case 64:
  if (yyn == 64)
    
/* Line 354 of lalr1.java  */
/* Line 211 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_STRING);};
  break;
    

  case 65:
  if (yyn == 65)
    
/* Line 354 of lalr1.java  */
/* Line 215 of "dap.y"  */
    {yyval=attrvalue(parsestate,null,((yystack.valueAt (1-(1)))),(Object)SCAN_URL);};
  break;
    

  case 66:
  if (yyn == 66)
    
/* Line 354 of lalr1.java  */
/* Line 216 of "dap.y"  */
    {yyval=attrvalue(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))),(Object)SCAN_URL);};
  break;
    

  case 67:
  if (yyn == 67)
    
/* Line 354 of lalr1.java  */
/* Line 220 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 68:
  if (yyn == 68)
    
/* Line 354 of lalr1.java  */
/* Line 224 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 69:
  if (yyn == 69)
    
/* Line 354 of lalr1.java  */
/* Line 234 of "dap.y"  */
    {yyval=((yystack.valueAt (3-(2)))); yyval=((yystack.valueAt (3-(3)))); yyval=null;};
  break;
    

  case 70:
  if (yyn == 70)
    
/* Line 354 of lalr1.java  */
/* Line 239 of "dap.y"  */
    {yyval=errorbody(parsestate,((yystack.valueAt (7-(2)))),((yystack.valueAt (7-(3)))),((yystack.valueAt (7-(4)))),((yystack.valueAt (7-(5)))));};
  break;
    

  case 71:
  if (yyn == 71)
    
/* Line 354 of lalr1.java  */
/* Line 242 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 72:
  if (yyn == 72)
    
/* Line 354 of lalr1.java  */
/* Line 242 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 73:
  if (yyn == 73)
    
/* Line 354 of lalr1.java  */
/* Line 243 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 74:
  if (yyn == 74)
    
/* Line 354 of lalr1.java  */
/* Line 243 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 75:
  if (yyn == 75)
    
/* Line 354 of lalr1.java  */
/* Line 244 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 76:
  if (yyn == 76)
    
/* Line 354 of lalr1.java  */
/* Line 244 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 77:
  if (yyn == 77)
    
/* Line 354 of lalr1.java  */
/* Line 245 of "dap.y"  */
    {yyval=null;};
  break;
    

  case 78:
  if (yyn == 78)
    
/* Line 354 of lalr1.java  */
/* Line 245 of "dap.y"  */
    {yyval=((yystack.valueAt (4-(3))));};
  break;
    

  case 79:
  if (yyn == 79)
    
/* Line 354 of lalr1.java  */
/* Line 251 of "dap.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 80:
  if (yyn == 80)
    
/* Line 354 of lalr1.java  */
/* Line 252 of "dap.y"  */
    {yyval=strdup("alias");};
  break;
    

  case 81:
  if (yyn == 81)
    
/* Line 354 of lalr1.java  */
/* Line 253 of "dap.y"  */
    {yyval=strdup("array");};
  break;
    

  case 82:
  if (yyn == 82)
    
/* Line 354 of lalr1.java  */
/* Line 254 of "dap.y"  */
    {yyval=strdup("attributes");};
  break;
    

  case 83:
  if (yyn == 83)
    
/* Line 354 of lalr1.java  */
/* Line 255 of "dap.y"  */
    {yyval=strdup("byte");};
  break;
    

  case 84:
  if (yyn == 84)
    
/* Line 354 of lalr1.java  */
/* Line 256 of "dap.y"  */
    {yyval=strdup("dataset");};
  break;
    

  case 85:
  if (yyn == 85)
    
/* Line 354 of lalr1.java  */
/* Line 257 of "dap.y"  */
    {yyval=strdup("data");};
  break;
    

  case 86:
  if (yyn == 86)
    
/* Line 354 of lalr1.java  */
/* Line 258 of "dap.y"  */
    {yyval=strdup("error");};
  break;
    

  case 87:
  if (yyn == 87)
    
/* Line 354 of lalr1.java  */
/* Line 259 of "dap.y"  */
    {yyval=strdup("float32");};
  break;
    

  case 88:
  if (yyn == 88)
    
/* Line 354 of lalr1.java  */
/* Line 260 of "dap.y"  */
    {yyval=strdup("float64");};
  break;
    

  case 89:
  if (yyn == 89)
    
/* Line 354 of lalr1.java  */
/* Line 261 of "dap.y"  */
    {yyval=strdup("grid");};
  break;
    

  case 90:
  if (yyn == 90)
    
/* Line 354 of lalr1.java  */
/* Line 262 of "dap.y"  */
    {yyval=strdup("int16");};
  break;
    

  case 91:
  if (yyn == 91)
    
/* Line 354 of lalr1.java  */
/* Line 263 of "dap.y"  */
    {yyval=strdup("int32");};
  break;
    

  case 92:
  if (yyn == 92)
    
/* Line 354 of lalr1.java  */
/* Line 264 of "dap.y"  */
    {yyval=strdup("maps");};
  break;
    

  case 93:
  if (yyn == 93)
    
/* Line 354 of lalr1.java  */
/* Line 265 of "dap.y"  */
    {yyval=strdup("sequence");};
  break;
    

  case 94:
  if (yyn == 94)
    
/* Line 354 of lalr1.java  */
/* Line 266 of "dap.y"  */
    {yyval=strdup("string");};
  break;
    

  case 95:
  if (yyn == 95)
    
/* Line 354 of lalr1.java  */
/* Line 267 of "dap.y"  */
    {yyval=strdup("structure");};
  break;
    

  case 96:
  if (yyn == 96)
    
/* Line 354 of lalr1.java  */
/* Line 268 of "dap.y"  */
    {yyval=strdup("uint16");};
  break;
    

  case 97:
  if (yyn == 97)
    
/* Line 354 of lalr1.java  */
/* Line 269 of "dap.y"  */
    {yyval=strdup("uint32");};
  break;
    

  case 98:
  if (yyn == 98)
    
/* Line 354 of lalr1.java  */
/* Line 270 of "dap.y"  */
    {yyval=strdup("url");};
  break;
    

  case 99:
  if (yyn == 99)
    
/* Line 354 of lalr1.java  */
/* Line 271 of "dap.y"  */
    {yyval=strdup("code");};
  break;
    

  case 100:
  if (yyn == 100)
    
/* Line 354 of lalr1.java  */
/* Line 272 of "dap.y"  */
    {yyval=strdup("message");};
  break;
    



/* Line 354 of lalr1.java  */
/* Line 1197 of "DapParser.java"  */
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

    int yyresult;

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
        if (yyn == yypact_ninf_)
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
	    if (yyn == 0 || yyn == yytable_ninf_)
	      label = YYFAIL;
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
          label = YYFAIL;
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
      case YYFAIL:
        /* If not already recovering from an error, report this error.  */
        if (yyerrstatus_ == 0)
          {
	    ++yynerrs_;
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
	    if (yyn != yypact_ninf_)
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
	    if (yystack.height == 1)
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
        int yyn = yypact_[yystate];
        if (yypact_ninf_ < yyn && yyn <= yylast_)
          {
	    StringBuffer res;

	    /* Start YYX at -YYN if negative to avoid negative indexes in
	       YYCHECK.  */
	    int yyxbegin = yyn < 0 ? -yyn : 0;

	    /* Stay within bounds of both yycheck and yytname.  */
	    int yychecklim = yylast_ - yyn + 1;
	    int yyxend = yychecklim < yyntokens_ ? yychecklim : yyntokens_;
	    int count = 0;
	    for (int x = yyxbegin; x < yyxend; ++x)
	      if (yycheck_[x + yyn] == x && x != yyterror_)
	        ++count;

	    // FIXME: This method of building the message is not compatible
	    // with internationalization.
	    res = new StringBuffer ("syntax error, unexpected ");
	    res.append (yytnamerr_ (yytname_[tok]));
	    if (count < 5)
	      {
	        count = 0;
	        for (int x = yyxbegin; x < yyxend; ++x)
	          if (yycheck_[x + yyn] == x && x != yyterror_)
		    {
		      res.append (count++ == 0 ? ", expecting " : " or ");
		      res.append (yytnamerr_ (yytname_[x]));
		    }
	      }
	    return res.toString ();
          }
      }

    return "syntax error";
  }


  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
     STATE-NUM.  */
  private static final short yypact_ninf_ = -66;
  private static final short yypact_[] =
  {
         3,   -66,   -66,    -7,    -1,    27,     5,   -66,    19,    22,
     -66,   -66,   -66,   -66,   -66,   103,   -66,    35,    55,    43,
     -66,   -66,   -66,   -66,     9,   -66,   -66,    12,   -66,    78,
     -66,   -66,   -66,   199,   -66,   244,    77,    73,    82,   -66,
     244,   -66,   -66,   244,   -66,   -66,   -66,   -66,   244,   244,
     -66,   244,   244,   -66,   -66,   -66,   244,   -66,   244,   244,
     244,   -66,   -66,   -66,    81,    84,   108,   -66,   -66,   -66,
     -66,   -66,   -66,   -66,   -66,   -66,   -66,   -66,   -66,   -66,
      99,   -66,   -66,   -66,   100,   106,   101,   107,   244,   111,
     112,   118,   119,   128,   129,   133,   134,   135,   -66,   -66,
     120,   126,   150,   -66,     6,   -66,   131,   142,   143,   139,
     -66,   -66,   -24,   -66,   -20,   -66,   -17,   -66,   -13,   -66,
     -11,   -66,   -10,   -66,   -66,    40,   -66,    68,   -66,    74,
     -66,    72,   223,   244,   244,   -66,   -66,   266,   -66,   -66,
     146,   153,   151,   -66,   157,   -66,   158,   -66,   159,   -66,
     165,   -66,   166,   -66,   129,   -66,   174,   -66,   175,   -66,
     135,   -66,   181,   196,   -66,   194,   195,   -66,   198,   -66,
     -66,   -66,   -66,   -66,   -66,   -66,   -66,   -66,   -66,   200,
     -66,    14,   -66,   207,   -66,   -66,   -66,   201,   173,   -66,
     244,   203,   -66
  };

  /* YYDEFACT[S] -- default rule to reduce with in state S when YYTABLE
     doesn't specify something else to do.  Zero means the default is an
     error.  */
  private static final byte yydefact_[] =
  {
         0,     6,    32,     0,     0,     0,     0,     8,     2,    71,
       5,     1,    34,    35,     4,     0,     3,     0,    73,     0,
      14,    15,    20,    21,     0,    16,    18,     0,    23,     0,
      17,    19,    22,     0,     9,     0,     0,     0,    75,    48,
      80,    81,    82,    83,    99,    84,    85,    86,    87,    88,
      89,    90,    91,    92,   100,    93,    94,    95,    96,    97,
      98,    79,    33,    36,     0,     0,     0,     8,     8,    30,
      80,    83,    87,    88,    90,    91,    94,    96,    97,    98,
       0,    29,    31,    24,     0,     0,     0,    77,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,    37,    35,
       0,     0,     0,     7,     0,    72,     0,     0,     0,     0,
      69,    49,     0,    59,     0,    61,     0,    51,     0,    55,
       0,    68,     0,    63,    53,     0,    57,     0,    67,     0,
      65,     0,     0,     0,     0,    28,    10,     0,    25,    74,
       0,     0,     0,    38,     0,    43,     0,    44,     0,    39,
       0,    41,     0,    45,     0,    40,     0,    42,     0,    46,
       0,    47,     0,     0,    24,    79,     0,    76,     0,    70,
      50,    60,    62,    52,    56,    64,    54,    58,    66,     0,
      12,     0,    26,     0,    78,     8,    11,     0,     0,    27,
       0,     0,    13
  };

  /* YYPGOTO[NTERM-NUM].  */
  private static final short yypgoto_[] =
  {
       -66,   -66,   -66,   -65,   160,   -66,    75,   -66,   -66,   -35,
     -66,   -66,   141,   -66,   -66,   -66,   -66,   -66,   -66,   -66,
     -66,   -66,   -66,   136,   137,   -66,   -66,   -66,   -66,   -66,
     -66,   -18
  };

  /* YYDEFGOTO[NTERM-NUM].  */
  private static final short
  yydefgoto_[] =
  {
        -1,     5,     8,    15,    34,    35,   104,   138,    80,    81,
       6,    14,    19,    63,   112,   118,   125,   120,   127,   114,
     116,   122,   129,   130,   123,    64,    10,    18,    38,    87,
     109,    82
  };

  /* YYTABLE[YYPACT[STATE-NUM]].  What to do in state STATE-NUM.  If
     positive, shift that token.  If negative, reduce the rule which
     number is the opposite.  If zero, do what YYDEFACT says.  */
  private static final short yytable_ninf_ = -1;
  private static final short
  yytable_[] =
  {
        83,    65,   101,   102,     1,   143,    12,   135,     2,   145,
     144,     3,   147,     4,   146,   135,   149,   148,   151,   153,
       7,   150,    88,   152,   154,    89,     9,    11,    16,    17,
      90,    91,    13,    92,    93,   136,    66,   137,    94,    67,
      95,    96,    97,   186,    39,   137,    40,    41,    42,    43,
      44,    45,    46,    47,    48,    49,    50,    51,    52,    53,
      54,    55,    56,    57,    58,    59,    60,    61,    36,   155,
     110,    62,    37,    39,   156,    40,    41,    42,    43,    44,
      45,    46,    47,    48,    49,    50,    51,    52,    53,    54,
      55,    56,    57,    58,    59,    60,    61,   157,   163,   164,
     161,    84,   158,   159,    20,    68,    85,    86,   160,    21,
      98,    99,   100,    65,    22,    23,    24,    25,    26,   166,
     188,    27,    28,    29,    30,    31,    32,    20,   103,   105,
     106,    33,    21,   108,   107,   111,   113,    22,    23,    24,
      25,    26,   115,   117,    27,    28,    29,    30,    31,    32,
     132,    20,   119,   121,   133,   191,    21,   124,   126,   128,
     139,    22,    23,    24,    25,    26,   140,   142,    27,    28,
      29,    30,    31,    32,    20,   167,   141,   168,   134,    21,
     169,   170,   171,   172,    22,    23,    24,    25,    26,   173,
     174,    27,    28,    29,    30,    31,    32,   179,   176,   177,
      69,   190,    70,    41,    42,    71,    44,    45,    46,    47,
      72,    73,    50,    74,    75,    53,    54,    55,    76,    57,
      77,    78,    79,    61,    20,   180,   182,   184,   183,    21,
     185,   187,   192,   189,    22,    23,    24,    25,    26,   181,
     131,    27,    28,    29,    30,    31,    32,    70,    41,    42,
      71,    44,    45,    46,    47,    72,    73,    50,    74,    75,
      53,    54,    55,    76,    57,    77,    78,    79,    61,    70,
      41,    42,    71,    44,    45,    46,    47,    72,    73,    50,
      74,    75,    53,    54,    55,    76,    57,    77,    78,    79,
     165,   175,   162,     0,     0,     0,   178
  };

  /* YYCHECK.  */
  private static final short
  yycheck_[] =
  {
        35,    19,    67,    68,     1,    29,     1,     1,     5,    29,
      34,     8,    29,    10,    34,     1,    29,    34,    29,    29,
      27,    34,    40,    34,    34,    43,    27,     0,     9,     7,
      48,    49,    27,    51,    52,    29,    27,    31,    56,    27,
      58,    59,    60,    29,     1,    31,     3,     4,     5,     6,
       7,     8,     9,    10,    11,    12,    13,    14,    15,    16,
      17,    18,    19,    20,    21,    22,    23,    24,    33,    29,
      88,    28,    17,     1,    34,     3,     4,     5,     6,     7,
       8,     9,    10,    11,    12,    13,    14,    15,    16,    17,
      18,    19,    20,    21,    22,    23,    24,    29,   133,   134,
      28,    24,    34,    29,     1,    27,    33,    25,    34,     6,
      29,    27,     4,   131,    11,    12,    13,    14,    15,   137,
     185,    18,    19,    20,    21,    22,    23,     1,    29,    29,
      24,    28,     6,    26,    33,    24,    24,    11,    12,    13,
      14,    15,    24,    24,    18,    19,    20,    21,    22,    23,
      30,     1,    24,    24,    28,   190,     6,    24,    24,    24,
      29,    11,    12,    13,    14,    15,    24,    28,    18,    19,
      20,    21,    22,    23,     1,    29,    33,    24,    28,     6,
      29,    24,    24,    24,    11,    12,    13,    14,    15,    24,
      24,    18,    19,    20,    21,    22,    23,    16,    24,    24,
       1,    28,     3,     4,     5,     6,     7,     8,     9,    10,
      11,    12,    13,    14,    15,    16,    17,    18,    19,    20,
      21,    22,    23,    24,     1,    29,    32,    29,    33,     6,
      30,    24,    29,    32,    11,    12,    13,    14,    15,   164,
      99,    18,    19,    20,    21,    22,    23,     3,     4,     5,
       6,     7,     8,     9,    10,    11,    12,    13,    14,    15,
      16,    17,    18,    19,    20,    21,    22,    23,    24,     3,
       4,     5,     6,     7,     8,     9,    10,    11,    12,    13,
      14,    15,    16,    17,    18,    19,    20,    21,    22,    23,
      24,   154,   132,    -1,    -1,    -1,   160
  };

  /* STOS_[STATE-NUM] -- The (internal number of the) accessing
     symbol of state STATE-NUM.  */
  private static final byte
  yystos_[] =
  {
         0,     1,     5,     8,    10,    36,    45,    27,    37,    27,
      61,     0,     1,    27,    46,    38,     9,     7,    62,    47,
       1,     6,    11,    12,    13,    14,    15,    18,    19,    20,
      21,    22,    23,    28,    39,    40,    33,    17,    63,     1,
       3,     4,     5,     6,     7,     8,     9,    10,    11,    12,
      13,    14,    15,    16,    17,    18,    19,    20,    21,    22,
      23,    24,    28,    48,    60,    66,    27,    27,    27,     1,
       3,     6,    11,    12,    14,    15,    19,    21,    22,    23,
      43,    44,    66,    44,    24,    33,    25,    64,    66,    66,
      66,    66,    66,    66,    66,    66,    66,    66,    29,    27,
       4,    38,    38,    29,    41,    29,    24,    33,    26,    65,
      66,    24,    49,    24,    54,    24,    55,    24,    50,    24,
      52,    24,    56,    59,    24,    51,    24,    53,    24,    57,
      58,    47,    30,    28,    28,     1,    29,    31,    42,    29,
      24,    33,    28,    29,    34,    29,    34,    29,    34,    29,
      34,    29,    34,    29,    34,    29,    34,    29,    34,    29,
      34,    28,    39,    44,    44,    24,    66,    29,    24,    29,
      24,    24,    24,    24,    24,    59,    24,    24,    58,    16,
      29,    41,    32,    33,    29,    30,    29,    24,    38,    32,
      28,    44,    29
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
         0,    35,    36,    36,    36,    36,    36,    37,    38,    38,
      39,    39,    39,    39,    39,    40,    40,    40,    40,    40,
      40,    40,    40,    40,    41,    41,    42,    42,    42,    43,
      43,    44,    45,    46,    46,    47,    47,    48,    48,    48,
      48,    48,    48,    48,    48,    48,    48,    48,    48,    49,
      49,    50,    50,    51,    51,    52,    52,    53,    53,    54,
      54,    55,    55,    56,    56,    57,    57,    58,    59,    60,
      61,    62,    62,    63,    63,    64,    64,    65,    65,    66,
      66,    66,    66,    66,    66,    66,    66,    66,    66,    66,
      66,    66,    66,    66,    66,    66,    66,    66,    66,    66,
      66
  };

  /* YYR2[YYN] -- Number of symbols composing right hand side of rule YYN.  */
  private static final byte
  yyr2_[] =
  {
         0,     2,     2,     3,     3,     2,     1,     5,     0,     2,
       4,     7,     6,    11,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     0,     2,     3,     5,     1,     1,
       1,     1,     0,     3,     1,     0,     2,     2,     4,     4,
       4,     4,     4,     4,     4,     4,     4,     4,     1,     1,
       3,     1,     3,     1,     3,     1,     3,     1,     3,     1,
       3,     1,     3,     1,     3,     1,     3,     1,     1,     3,
       7,     0,     4,     0,     4,     0,     4,     0,     4,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
       1
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
  "'='", "','", "$accept", "start", "datasetbody", "declarations",
  "declaration", "base_type", "array_decls", "array_decl", "datasetname",
  "var_name", "dassetup", "attributebody", "attr_list", "attribute",
  "bytes", "int16", "uint16", "int32", "uint32", "float32", "float64",
  "strs", "urls", "url", "str_or_id", "alias", "errorbody", "errorcode",
  "errormsg", "errorptype", "errorprog", "name", null
  };

  /* YYRHS -- A `-1'-separated list of the rules' RHS.  */
  private static final byte yyrhs_[] =
  {
        36,     0,    -1,     8,    37,    -1,     8,    37,     9,    -1,
       5,    45,    46,    -1,    10,    61,    -1,     1,    -1,    27,
      38,    28,    43,    29,    -1,    -1,    38,    39,    -1,    40,
      44,    41,    29,    -1,    20,    27,    38,    28,    44,    41,
      29,    -1,    18,    27,    38,    28,    44,    29,    -1,    13,
      27,     4,    30,    39,    16,    30,    38,    28,    44,    29,
      -1,     1,    -1,     6,    -1,    14,    -1,    21,    -1,    15,
      -1,    22,    -1,    11,    -1,    12,    -1,    23,    -1,    19,
      -1,    -1,    41,    42,    -1,    31,    24,    32,    -1,    31,
      66,    33,    24,    32,    -1,     1,    -1,    44,    -1,     1,
      -1,    66,    -1,    -1,    27,    47,    28,    -1,     1,    -1,
      -1,    47,    48,    -1,    60,    29,    -1,     6,    66,    49,
      29,    -1,    14,    66,    50,    29,    -1,    21,    66,    51,
      29,    -1,    15,    66,    52,    29,    -1,    22,    66,    53,
      29,    -1,    11,    66,    54,    29,    -1,    12,    66,    55,
      29,    -1,    19,    66,    56,    29,    -1,    23,    66,    57,
      29,    -1,    66,    27,    47,    28,    -1,     1,    -1,    24,
      -1,    49,    34,    24,    -1,    24,    -1,    50,    34,    24,
      -1,    24,    -1,    51,    34,    24,    -1,    24,    -1,    52,
      34,    24,    -1,    24,    -1,    53,    34,    24,    -1,    24,
      -1,    54,    34,    24,    -1,    24,    -1,    55,    34,    24,
      -1,    59,    -1,    56,    34,    59,    -1,    58,    -1,    57,
      34,    58,    -1,    24,    -1,    24,    -1,     3,    66,    66,
      -1,    27,    62,    63,    64,    65,    28,    29,    -1,    -1,
       7,    33,    24,    29,    -1,    -1,    17,    33,    24,    29,
      -1,    -1,    25,    33,    24,    29,    -1,    -1,    26,    33,
      24,    29,    -1,    24,    -1,     3,    -1,     4,    -1,     5,
      -1,     6,    -1,     8,    -1,     9,    -1,    10,    -1,    11,
      -1,    12,    -1,    13,    -1,    14,    -1,    15,    -1,    16,
      -1,    18,    -1,    19,    -1,    20,    -1,    21,    -1,    22,
      -1,    23,    -1,     7,    -1,    17,    -1
  };

  /* YYPRHS[YYN] -- Index of the first RHS symbol of rule number YYN in
     YYRHS.  */
  private static final short yyprhs_[] =
  {
         0,     0,     3,     6,    10,    14,    17,    19,    25,    26,
      29,    34,    42,    49,    61,    63,    65,    67,    69,    71,
      73,    75,    77,    79,    81,    82,    85,    89,    95,    97,
      99,   101,   103,   104,   108,   110,   111,   114,   117,   122,
     127,   132,   137,   142,   147,   152,   157,   162,   167,   169,
     171,   175,   177,   181,   183,   187,   189,   193,   195,   199,
     201,   205,   207,   211,   213,   217,   219,   223,   225,   227,
     231,   239,   240,   245,   246,   251,   252,   257,   258,   263,
     265,   267,   269,   271,   273,   275,   277,   279,   281,   283,
     285,   287,   289,   291,   293,   295,   297,   299,   301,   303,
     305
  };

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] =
  {
         0,    71,    71,    72,    73,    74,    75,    80,    86,    87,
      93,    95,    97,    99,   102,   108,   109,   110,   111,   112,
     113,   114,   115,   116,   120,   121,   125,   126,   127,   132,
     133,   137,   139,   142,   143,   148,   149,   153,   154,   156,
     158,   160,   162,   164,   166,   168,   170,   172,   173,   178,
     179,   183,   184,   188,   189,   193,   194,   198,   199,   202,
     203,   206,   207,   210,   211,   215,   216,   220,   224,   234,
     238,   242,   242,   243,   243,   244,   244,   245,   245,   251,
     252,   253,   254,   255,   256,   257,   258,   259,   260,   261,
     262,   263,   264,   265,   266,   267,   268,   269,   270,   271,
     272
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

  private static final int yylast_ = 296;
  private static final int yynnts_ = 32;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 11;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 35;

  private static final int yyuser_token_number_max_ = 281;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */
/* Unqualified %code blocks.  */

/* Line 876 of lalr1.java  */
/* Line 18 of "dap.y"  */

 
    /**
     * Instantiates the Bison-generated parser.
     * @param yylexer The scanner that will supply tokens to the parser.
     */

    public DapParser(InputStream stream)
    {
	this(stream,(BaseTypeFactory)null);
    }

    public DapParser(InputStream stream, BaseTypeFactory factory)
    {
	super(factory);
	this.yylexer = new Daplex(stream,this);
	this.stream = stream;
	super.lexstate = (Daplex)this.yylexer;
    }



/* Line 876 of lalr1.java  */
/* Line 1955 of "DapParser.java"  */

}


/* Line 880 of lalr1.java  */
/* Line 275 of "dap.y"  */



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

package opendap.Server.parsers;
/* First part of user declarations.  */

/* "%code imports" blocks.  */

/* Line 33 of lalr1.java  */
/* Line 12 of "ce.y"  */

import opendap.dap.*;
import opendap.Server.*;
import opendap.dap.parsers.ParseException;

import java.io.*;



/* Line 33 of lalr1.java  */
/* Line 49 of "CeParser.java"  */

/**
 * A Bison parser, automatically generated from <tt>ce.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
public class CeParser extends Ceparse
{
    /** Version number for the Bison executable that generated this parser.  */
  public static final String bisonVersion = "2.4.2";

  /** Name of the skeleton that generated this parser.  */
  public static final String bisonSkeleton = "lalr1.java";


  /** True if verbose error messages are enabled.  */
  public boolean errorVerbose = true;



  /** Token returned by the scanner to signal the end of its input.  */
  public static final int EOF = 0;

/* Tokens.  */
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_WORD = 258;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_STRINGCONST = 259;
  /** Token number, to be returned by the scanner.  */
  public static final int SCAN_NUMBERCONST = 260;



  

  /**
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>CeParser</tt>.
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
  public CeParser (Lexer yylexer) {
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

  private final int yylex () throws ParseException
  {
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
	  case 8:
  if (yyn == 8)
    
/* Line 354 of lalr1.java  */
/* Line 83 of "ce.y"  */
    {projections(parsestate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 9:
  if (yyn == 9)
    
/* Line 354 of lalr1.java  */
/* Line 88 of "ce.y"  */
    {selections(parsestate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 10:
  if (yyn == 10)
    
/* Line 354 of lalr1.java  */
/* Line 94 of "ce.y"  */
    {yyval=projectionlist(parsestate,(Object)null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 11:
  if (yyn == 11)
    
/* Line 354 of lalr1.java  */
/* Line 96 of "ce.y"  */
    {yyval=projectionlist(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));};
  break;
    

  case 12:
  if (yyn == 12)
    
/* Line 354 of lalr1.java  */
/* Line 102 of "ce.y"  */
    {yyval=projection(parsestate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 13:
  if (yyn == 13)
    
/* Line 354 of lalr1.java  */
/* Line 104 of "ce.y"  */
    {yyval=((yystack.valueAt (1-(1))));};
  break;
    

  case 14:
  if (yyn == 14)
    
/* Line 354 of lalr1.java  */
/* Line 109 of "ce.y"  */
    {yyval=function(parsestate,((yystack.valueAt (3-(1)))),null);};
  break;
    

  case 15:
  if (yyn == 15)
    
/* Line 354 of lalr1.java  */
/* Line 111 of "ce.y"  */
    {yyval=function(parsestate,((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 16:
  if (yyn == 16)
    
/* Line 354 of lalr1.java  */
/* Line 117 of "ce.y"  */
    {yyval=segmentlist(parsestate,null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 17:
  if (yyn == 17)
    
/* Line 354 of lalr1.java  */
/* Line 119 of "ce.y"  */
    {yyval=segmentlist(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));};
  break;
    

  case 18:
  if (yyn == 18)
    
/* Line 354 of lalr1.java  */
/* Line 125 of "ce.y"  */
    {yyval=segment(parsestate,((yystack.valueAt (1-(1)))),null);};
  break;
    

  case 19:
  if (yyn == 19)
    
/* Line 354 of lalr1.java  */
/* Line 127 of "ce.y"  */
    {yyval=segment(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 20:
  if (yyn == 20)
    
/* Line 354 of lalr1.java  */
/* Line 133 of "ce.y"  */
    {yyval=rangelist(parsestate,null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 21:
  if (yyn == 21)
    
/* Line 354 of lalr1.java  */
/* Line 135 of "ce.y"  */
    {yyval=rangelist(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 22:
  if (yyn == 22)
    
/* Line 354 of lalr1.java  */
/* Line 141 of "ce.y"  */
    {yyval=range(parsestate,((yystack.valueAt (1-(1)))),null,null);};
  break;
    

  case 23:
  if (yyn == 23)
    
/* Line 354 of lalr1.java  */
/* Line 143 of "ce.y"  */
    {yyval=range(parsestate,((yystack.valueAt (5-(2)))),null,((yystack.valueAt (5-(4)))));};
  break;
    

  case 24:
  if (yyn == 24)
    
/* Line 354 of lalr1.java  */
/* Line 145 of "ce.y"  */
    {yyval=range(parsestate,((yystack.valueAt (7-(2)))),((yystack.valueAt (7-(4)))),((yystack.valueAt (7-(6)))));};
  break;
    

  case 25:
  if (yyn == 25)
    
/* Line 354 of lalr1.java  */
/* Line 149 of "ce.y"  */
    {yyval = range1(parsestate,((yystack.valueAt (3-(2)))));};
  break;
    

  case 26:
  if (yyn == 26)
    
/* Line 354 of lalr1.java  */
/* Line 156 of "ce.y"  */
    {yyval=clauselist(parsestate,null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 27:
  if (yyn == 27)
    
/* Line 354 of lalr1.java  */
/* Line 158 of "ce.y"  */
    {yyval=clauselist(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 28:
  if (yyn == 28)
    
/* Line 354 of lalr1.java  */
/* Line 164 of "ce.y"  */
    {yyval=sel_clause(parsestate,1,((yystack.valueAt (6-(2)))),((yystack.valueAt (6-(3)))),((yystack.valueAt (6-(5)))));};
  break;
    

  case 29:
  if (yyn == 29)
    
/* Line 354 of lalr1.java  */
/* Line 166 of "ce.y"  */
    {yyval=sel_clause(parsestate,2,((yystack.valueAt (4-(2)))),((yystack.valueAt (4-(3)))),((yystack.valueAt (4-(4)))));};
  break;
    

  case 30:
  if (yyn == 30)
    
/* Line 354 of lalr1.java  */
/* Line 168 of "ce.y"  */
    {yyval=((yystack.valueAt (2-(1))));};
  break;
    

  case 31:
  if (yyn == 31)
    
/* Line 354 of lalr1.java  */
/* Line 173 of "ce.y"  */
    {yyval=value_list(parsestate,null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 32:
  if (yyn == 32)
    
/* Line 354 of lalr1.java  */
/* Line 175 of "ce.y"  */
    {yyval=value_list(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));};
  break;
    

  case 33:
  if (yyn == 33)
    
/* Line 354 of lalr1.java  */
/* Line 180 of "ce.y"  */
    {yyval=value(parsestate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 34:
  if (yyn == 34)
    
/* Line 354 of lalr1.java  */
/* Line 182 of "ce.y"  */
    {yyval=value(parsestate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 35:
  if (yyn == 35)
    
/* Line 354 of lalr1.java  */
/* Line 184 of "ce.y"  */
    {yyval=value(parsestate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 36:
  if (yyn == 36)
    
/* Line 354 of lalr1.java  */
/* Line 189 of "ce.y"  */
    {yyval=constant(parsestate,((yystack.valueAt (1-(1)))),SCAN_NUMBERCONST);};
  break;
    

  case 37:
  if (yyn == 37)
    
/* Line 354 of lalr1.java  */
/* Line 191 of "ce.y"  */
    {yyval=constant(parsestate,((yystack.valueAt (1-(1)))),SCAN_STRINGCONST);};
  break;
    

  case 38:
  if (yyn == 38)
    
/* Line 354 of lalr1.java  */
/* Line 196 of "ce.y"  */
    {yyval=var(parsestate,((yystack.valueAt (1-(1)))));};
  break;
    

  case 39:
  if (yyn == 39)
    
/* Line 354 of lalr1.java  */
/* Line 205 of "ce.y"  */
    {yyval=indexpath(parsestate,null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 40:
  if (yyn == 40)
    
/* Line 354 of lalr1.java  */
/* Line 207 of "ce.y"  */
    {yyval=indexpath(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));};
  break;
    

  case 41:
  if (yyn == 41)
    
/* Line 354 of lalr1.java  */
/* Line 212 of "ce.y"  */
    {yyval=index(parsestate,((yystack.valueAt (1-(1)))),null);};
  break;
    

  case 42:
  if (yyn == 42)
    
/* Line 354 of lalr1.java  */
/* Line 214 of "ce.y"  */
    {yyval=index(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 43:
  if (yyn == 43)
    
/* Line 354 of lalr1.java  */
/* Line 220 of "ce.y"  */
    {yyval=array_indices(parsestate,null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 44:
  if (yyn == 44)
    
/* Line 354 of lalr1.java  */
/* Line 222 of "ce.y"  */
    {yyval=array_indices(parsestate,((yystack.valueAt (2-(1)))),((yystack.valueAt (2-(2)))));};
  break;
    

  case 45:
  if (yyn == 45)
    
/* Line 354 of lalr1.java  */
/* Line 227 of "ce.y"  */
    {yyval=function(parsestate,((yystack.valueAt (3-(1)))),null);};
  break;
    

  case 46:
  if (yyn == 46)
    
/* Line 354 of lalr1.java  */
/* Line 229 of "ce.y"  */
    {yyval=function(parsestate,((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))));};
  break;
    

  case 47:
  if (yyn == 47)
    
/* Line 354 of lalr1.java  */
/* Line 234 of "ce.y"  */
    {yyval=arg_list(parsestate,null,((yystack.valueAt (1-(1)))));};
  break;
    

  case 48:
  if (yyn == 48)
    
/* Line 354 of lalr1.java  */
/* Line 236 of "ce.y"  */
    {yyval=arg_list(parsestate,((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));};
  break;
    

  case 49:
  if (yyn == 49)
    
/* Line 354 of lalr1.java  */
/* Line 241 of "ce.y"  */
    {yyval=new Integer(EQUAL);};
  break;
    

  case 50:
  if (yyn == 50)
    
/* Line 354 of lalr1.java  */
/* Line 242 of "ce.y"  */
    {yyval=new Integer(GREATER);};
  break;
    

  case 51:
  if (yyn == 51)
    
/* Line 354 of lalr1.java  */
/* Line 243 of "ce.y"  */
    {yyval=new Integer(LESS);};
  break;
    

  case 52:
  if (yyn == 52)
    
/* Line 354 of lalr1.java  */
/* Line 244 of "ce.y"  */
    {yyval=new Integer(NOT_EQUAL);};
  break;
    

  case 53:
  if (yyn == 53)
    
/* Line 354 of lalr1.java  */
/* Line 245 of "ce.y"  */
    {yyval=new Integer(GREATER_EQL);};
  break;
    

  case 54:
  if (yyn == 54)
    
/* Line 354 of lalr1.java  */
/* Line 246 of "ce.y"  */
    {yyval=new Integer(LESS_EQL);};
  break;
    

  case 55:
  if (yyn == 55)
    
/* Line 354 of lalr1.java  */
/* Line 247 of "ce.y"  */
    {yyval=new Integer(REGEXP);};
  break;
    

  case 56:
  if (yyn == 56)
    
/* Line 354 of lalr1.java  */
/* Line 251 of "ce.y"  */
    {yyval = ((yystack.valueAt (1-(1))));};
  break;
    

  case 57:
  if (yyn == 57)
    
/* Line 354 of lalr1.java  */
/* Line 255 of "ce.y"  */
    {yyval = ((yystack.valueAt (1-(1))));};
  break;
    

  case 58:
  if (yyn == 58)
    
/* Line 354 of lalr1.java  */
/* Line 259 of "ce.y"  */
    {yyval = ((yystack.valueAt (1-(1))));};
  break;
    

  case 59:
  if (yyn == 59)
    
/* Line 354 of lalr1.java  */
/* Line 263 of "ce.y"  */
    {yyval = ((yystack.valueAt (1-(1))));};
  break;
    



/* Line 354 of lalr1.java  */
/* Line 765 of "CeParser.java"  */
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
  private static final byte yypact_ninf_ = -35;
  private static final byte yypact_[] =
  {
         8,   -35,    15,     4,   -35,   -35,    51,    -5,   -35,    10,
     -35,   -35,    11,   -35,    -5,   -35,    21,    41,   -35,   -35,
     -35,    27,   -35,   -35,    13,   -35,   -35,    29,    42,   -35,
     -35,   -35,    39,    39,   -35,    23,    57,    40,   -35,   -35,
      43,    49,    50,    52,     7,    39,    31,    57,   -35,    59,
     -35,   -35,    40,   -35,    56,    62,    63,    46,   -35,   -35,
     -35,   -35,   -35,    51,   -35,   -35,    59,     5,    64,    55,
     -35,    51,   -35,    57,   -35,     9,   -35,     6,    66,    48,
      51,   -35,    57,   -35,   -35,    65,   -35
  };

  /* YYDEFACT[S] -- default rule to reduce with in state S when YYTABLE
     doesn't specify something else to do.  Zero means the default is an
     error.  */
  private static final byte yydefact_[] =
  {
         7,     6,     0,     0,     1,    57,     0,     2,     3,     8,
      10,    13,    12,    16,     9,    26,     0,    18,    59,    58,
      34,     0,    35,    33,    38,    39,    30,     0,    41,    36,
      37,     4,     0,     0,    27,     0,     0,    19,    20,    22,
      49,    50,    51,     0,     0,     0,     0,     0,    43,    42,
      11,    17,    18,    14,     0,    31,     0,     0,    21,    55,
      53,    54,    52,     0,    29,    40,    41,    14,     0,     0,
      44,     0,    15,     0,    25,     0,    31,    15,    32,     0,
       0,    28,     0,    23,    32,     0,    24
  };

  /* YYPGOTO[NTERM-NUM].  */
  private static final byte yypgoto_[] =
  {
       -35,   -35,   -35,   -35,    69,   -35,    45,     1,   -35,    47,
     -35,    44,   -25,   -35,    68,    16,    -6,   -35,   -35,   -35,
      38,   -35,   -35,    53,   -35,    78,    -2,   -34,   -35
  };

  /* YYDEFGOTO[NTERM-NUM].  */
  private static final byte
  yydefgoto_[] =
  {
        -1,     2,     3,     7,     8,     9,    10,    20,    12,    13,
      37,    38,    39,    14,    15,    54,    55,    22,    23,    24,
      25,    49,    26,    56,    44,    16,    28,    29,    30
  };

  /* YYTABLE[YYPACT[STATE-NUM]].  What to do in state STATE-NUM.  If
     positive, shift that token.  If negative, reduce the rule which
     number is the opposite.  If zero, do what YYDEFACT says.  */
  private static final byte yytable_ninf_ = -57;
  private static final byte
  yytable_[] =
  {
        21,    17,    57,    48,    11,   -45,   -46,     5,    -5,     6,
       5,    18,    19,    69,     1,     4,    80,    32,     6,   -45,
     -46,    33,    63,    45,    70,    81,     5,    18,    19,    35,
      17,    52,    53,    11,     5,    18,    19,    46,    64,    79,
      67,    48,     5,    66,    40,    41,    42,    43,    85,   -56,
     -56,    36,    36,    47,     5,    18,    19,    76,    73,    74,
      82,    83,    19,    71,    59,    78,    60,    61,    74,    62,
      47,   -47,    72,    77,    84,   -48,    31,    50,    86,    75,
      51,    58,    34,    65,    27,     0,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,    68
  };

  /* YYCHECK.  */
  private static final byte
  yycheck_[] =
  {
         6,     3,    36,    28,     3,     0,     0,     3,     0,    14,
       3,     4,     5,    47,     6,     0,     7,     7,    14,    14,
      14,    10,    15,    10,    49,    16,     3,     4,     5,     8,
      32,    33,     9,    32,     3,     4,     5,     8,    44,    73,
       9,    66,     3,    45,    17,    18,    19,    20,    82,     8,
       8,    11,    11,    11,     3,     4,     5,    63,    12,    13,
      12,    13,     5,     7,    21,    71,    17,    17,    13,    17,
      11,     9,     9,     9,    80,     9,     7,    32,    13,    63,
      33,    37,    14,    45,     6,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    46
  };

  /* STOS_[STATE-NUM] -- The (internal number of the) accessing
     symbol of state STATE-NUM.  */
  private static final byte
  yystos_[] =
  {
         0,     6,    23,    24,     0,     3,    14,    25,    26,    27,
      28,    29,    30,    31,    35,    36,    47,    48,     4,     5,
      29,    38,    39,    40,    41,    42,    44,    47,    48,    49,
      50,    26,     7,    10,    36,     8,    11,    32,    33,    34,
      17,    18,    19,    20,    46,    10,     8,    11,    34,    43,
      28,    31,    48,     9,    37,    38,    45,    49,    33,    21,
      17,    17,    17,    15,    38,    42,    48,     9,    45,    49,
      34,     7,     9,    12,    13,    37,    38,     9,    38,    49,
       7,    16,    12,    13,    38,    49,    13
  };

  /* TOKEN_NUMBER_[YYLEX-NUM] -- Internal symbol number corresponding
     to YYLEX-NUM.  */
  private static final short
  yytoken_number_[] =
  {
         0,   256,   257,   258,   259,   260,    63,    44,    40,    41,
      46,    91,    58,    93,    38,   123,   125,    61,    62,    60,
      33,   126
  };

  /* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final byte
  yyr1_[] =
  {
         0,    22,    23,    23,    23,    23,    24,    24,    25,    26,
      27,    27,    28,    28,    29,    29,    30,    30,    31,    31,
      32,    32,    33,    33,    33,    34,    35,    35,    36,    36,
      36,    37,    37,    38,    38,    38,    39,    39,    40,    41,
      41,    42,    42,    43,    43,    44,    44,    45,    45,    46,
      46,    46,    46,    46,    46,    46,    47,    48,    49,    50
  };

  /* YYR2[YYN] -- Number of symbols composing right hand side of rule YYN.  */
  private static final byte
  yyr2_[] =
  {
         0,     2,     2,     2,     3,     0,     1,     0,     1,     1,
       1,     3,     1,     1,     3,     4,     1,     3,     1,     2,
       1,     2,     1,     5,     7,     3,     1,     2,     6,     4,
       2,     1,     3,     1,     1,     1,     1,     1,     1,     1,
       3,     1,     2,     1,     2,     3,     4,     1,     3,     1,
       1,     1,     2,     2,     2,     2,     1,     1,     1,     1
  };

  /* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     First, the terminals, then, starting at \a yyntokens_, nonterminals.  */
  private static final String yytname_[] =
  {
    "$end", "error", "$undefined", "SCAN_WORD", "SCAN_STRINGCONST",
  "SCAN_NUMBERCONST", "'?'", "','", "'('", "')'", "'.'", "'['", "':'",
  "']'", "'&'", "'{'", "'}'", "'='", "'>'", "'<'", "'!'", "'~'", "$accept",
  "constraints", "optquestionmark", "projections", "selections",
  "projectionlist", "projection", "function", "segmentlist", "segment",
  "rangelist", "range", "range1", "clauselist", "sel_clause", "value_list",
  "value", "constant", "var", "indexpath", "index", "array_indices",
  "boolfunction", "arg_list", "rel_op", "ident", "word", "number",
  "string", null
  };

  /* YYRHS -- A `-1'-separated list of the rules' RHS.  */
  private static final byte yyrhs_[] =
  {
        23,     0,    -1,    24,    25,    -1,    24,    26,    -1,    24,
      25,    26,    -1,    -1,     6,    -1,    -1,    27,    -1,    35,
      -1,    28,    -1,    27,     7,    28,    -1,    30,    -1,    29,
      -1,    47,     8,     9,    -1,    47,     8,    45,     9,    -1,
      31,    -1,    30,    10,    31,    -1,    48,    -1,    48,    32,
      -1,    33,    -1,    32,    33,    -1,    34,    -1,    11,    49,
      12,    49,    13,    -1,    11,    49,    12,    49,    12,    49,
      13,    -1,    11,    49,    13,    -1,    36,    -1,    35,    36,
      -1,    14,    38,    46,    15,    37,    16,    -1,    14,    38,
      46,    38,    -1,    14,    44,    -1,    38,    -1,    37,     7,
      38,    -1,    40,    -1,    29,    -1,    39,    -1,    49,    -1,
      50,    -1,    41,    -1,    42,    -1,    41,    10,    42,    -1,
      48,    -1,    48,    43,    -1,    34,    -1,    43,    34,    -1,
      47,     8,     9,    -1,    47,     8,    45,     9,    -1,    38,
      -1,    37,     7,    38,    -1,    17,    -1,    18,    -1,    19,
      -1,    20,    17,    -1,    18,    17,    -1,    19,    17,    -1,
      17,    21,    -1,    48,    -1,     3,    -1,     5,    -1,     4,
      -1
  };

  /* YYPRHS[YYN] -- Index of the first RHS symbol of rule number YYN in
     YYRHS.  */
  private static final short yyprhs_[] =
  {
         0,     0,     3,     6,     9,    13,    14,    16,    17,    19,
      21,    23,    27,    29,    31,    35,    40,    42,    46,    48,
      51,    53,    56,    58,    64,    72,    76,    78,    81,    88,
      93,    96,    98,   102,   104,   106,   108,   110,   112,   114,
     116,   120,   122,   125,   127,   130,   134,   139,   141,   145,
     147,   149,   151,   154,   157,   160,   163,   165,   167,   169
  };

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] =
  {
         0,    73,    73,    74,    75,    76,    79,    79,    83,    88,
      93,    95,   101,   103,   108,   110,   116,   118,   124,   126,
     132,   134,   140,   142,   144,   148,   155,   157,   163,   165,
     167,   172,   174,   179,   181,   183,   188,   190,   195,   204,
     206,   211,   213,   219,   221,   226,   228,   233,   235,   241,
     242,   243,   244,   245,   246,   247,   250,   254,   258,   262
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
       2,     2,     2,    20,     2,     2,     2,     2,    14,     2,
       8,     9,     2,     2,     7,     2,    10,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,    12,     2,
      19,    17,    18,     6,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,    11,     2,    13,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    15,     2,    16,    21,     2,     2,     2,
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
       5
  };

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 99;
  private static final int yynnts_ = 29;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 4;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 22;

  private static final int yyuser_token_number_max_ = 260;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */
/* Unqualified %code blocks.  */

/* Line 876 of lalr1.java  */
/* Line 18 of "ce.y"  */

 
    /**
     * Instantiates the Bison-generated parser.
     * @param yylexer The scanner that will supply tokens to the parser.
     */

    public CeParser(BaseTypeFactory factory)
    {
	super(factory);
	this.yylexer = new Celex(this);
	super.lexstate = (Celex)this.yylexer;
    }


    /* the parse function allows the specification of a
       new stream in case one is reusing the parser
    */

    public boolean parse(StringReader sreader) throws ParseException
    {
	((Celex)yylexer).reset(parsestate);
	((Celex)yylexer).setStream(sreader);
	return parse();
    }


    // Static entry point to be called by CEEvaluator
    // This parses, then fills in the evaluator from the AST

    static public boolean constraint_expression(CEEvaluator ceEval,
                                         BaseTypeFactory factory,
					 ClauseFactory clauseFactory,
					 StringReader sreader)
            throws DAP2Exception, ParseException
    {
	CeParser parser = new CeParser(factory);
        ServerDDS sdds = ceEval.getDDS();
        if(!parser.parse(sreader)) return false;
        ASTconstraint root = (ASTconstraint)parser.getAST();
	root.init(ceEval,factory,clauseFactory,sdds,parser.getASTnodeset());
	root.walkConstraint();
        return true;
    }



/* Line 876 of lalr1.java  */
/* Line 1434 of "CeParser.java"  */

}


/* Line 880 of lalr1.java  */
/* Line 266 of "ce.y"  */



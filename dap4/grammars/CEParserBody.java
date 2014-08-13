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

package dap4.ce.parser;
/* First part of user declarations.  */

/* "CEParserBody.java":37  */ /* lalr1.java:91  */

/* "CEParserBody.java":39  */ /* lalr1.java:92  */
/* "%code imports" blocks.  */
/* "ce.y":18  */ /* lalr1.java:93  */

import dap4.core.util.Slice;
import dap4.core.dmr.parser.ParseException;
import dap4.ce.CEAST;

/* "CEParserBody.java":47  */ /* lalr1.java:93  */

/**
 * A Bison parser, automatically generated from <tt>ce.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
abstract class CEParserBody
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
   * parser <tt>CEParserBody</tt>.
   */
  public interface Lexer {
    /** Token returned by the scanner to signal the end of its input.  */
    public static final int EOF = 0;

/* Tokens.  */
    /** Token number,to be returned by the scanner.  */
    static final int NAME = 258;
    /** Token number,to be returned by the scanner.  */
    static final int STRING = 259;
    /** Token number,to be returned by the scanner.  */
    static final int LONG = 260;
    /** Token number,to be returned by the scanner.  */
    static final int DOUBLE = 261;
    /** Token number,to be returned by the scanner.  */
    static final int BOOLEAN = 262;
    /** Token number,to be returned by the scanner.  */
    static final int NOT = 263;


    

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
  public CEParserBody (Lexer yylexer) 
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
          case 2:
  if (yyn == 2)
    /* "ce.y":103  */ /* lalr1.java:476  */
    {yyval=constraint(((CEAST.NodeList)(yystack.valueAt (2-(2)))));};
  break;
    

  case 5:
  if (yyn == 5)
    /* "ce.y":113  */ /* lalr1.java:476  */
    {yyval=nodelist(null,((CEAST)(yystack.valueAt (1-(1)))));};
  break;
    

  case 6:
  if (yyn == 6)
    /* "ce.y":115  */ /* lalr1.java:476  */
    {yyval=nodelist(((CEAST.NodeList)(yystack.valueAt (3-(1)))),((CEAST)(yystack.valueAt (3-(3)))));};
  break;
    

  case 9:
  if (yyn == 9)
    /* "ce.y":133  */ /* lalr1.java:476  */
    {yyval=projection(((CEAST)(yystack.valueAt (1-(1)))));};
  break;
    

  case 10:
  if (yyn == 10)
    /* "ce.y":138  */ /* lalr1.java:476  */
    {yyval=segmenttree(null,((CEAST)(yystack.valueAt (1-(1)))));};
  break;
    

  case 11:
  if (yyn == 11)
    /* "ce.y":140  */ /* lalr1.java:476  */
    {yyval=segmenttree(((CEAST)(yystack.valueAt (3-(1)))),((CEAST)(yystack.valueAt (3-(3)))));};
  break;
    

  case 12:
  if (yyn == 12)
    /* "ce.y":142  */ /* lalr1.java:476  */
    {yyval=segmenttree(((CEAST)(yystack.valueAt (5-(1)))),((CEAST.NodeList)(yystack.valueAt (5-(4)))));};
  break;
    

  case 13:
  if (yyn == 13)
    /* "ce.y":144  */ /* lalr1.java:476  */
    {yyval=segmenttree(((CEAST)(yystack.valueAt (4-(1)))),((CEAST.NodeList)(yystack.valueAt (4-(3)))));};
  break;
    

  case 14:
  if (yyn == 14)
    /* "ce.y":149  */ /* lalr1.java:476  */
    {yyval=nodelist(null,((CEAST)(yystack.valueAt (1-(1)))));};
  break;
    

  case 15:
  if (yyn == 15)
    /* "ce.y":151  */ /* lalr1.java:476  */
    {yyval=nodelist(((CEAST.NodeList)(yystack.valueAt (3-(1)))),((CEAST)(yystack.valueAt (3-(3)))));};
  break;
    

  case 16:
  if (yyn == 16)
    /* "ce.y":156  */ /* lalr1.java:476  */
    {yyval=segment(((String)(yystack.valueAt (1-(1)))),null);};
  break;
    

  case 17:
  if (yyn == 17)
    /* "ce.y":158  */ /* lalr1.java:476  */
    {yyval=segment(((String)(yystack.valueAt (2-(1)))),((CEAST.SliceList)(yystack.valueAt (2-(2)))));};
  break;
    

  case 18:
  if (yyn == 18)
    /* "ce.y":163  */ /* lalr1.java:476  */
    {yyval=slicelist(null,((Slice)(yystack.valueAt (1-(1)))));};
  break;
    

  case 19:
  if (yyn == 19)
    /* "ce.y":165  */ /* lalr1.java:476  */
    {yyval=slicelist(((CEAST.SliceList)(yystack.valueAt (2-(1)))),((Slice)(yystack.valueAt (2-(2)))));};
  break;
    

  case 20:
  if (yyn == 20)
    /* "ce.y":170  */ /* lalr1.java:476  */
    {yyval=slice(null);};
  break;
    

  case 21:
  if (yyn == 21)
    /* "ce.y":172  */ /* lalr1.java:476  */
    {yyval=slice(((CEAST.SliceList)(yystack.valueAt (3-(2)))));};
  break;
    

  case 22:
  if (yyn == 22)
    /* "ce.y":177  */ /* lalr1.java:476  */
    {yyval=slicelist(null,((Slice)(yystack.valueAt (1-(1)))));};
  break;
    

  case 23:
  if (yyn == 23)
    /* "ce.y":179  */ /* lalr1.java:476  */
    {yyval=slicelist(((CEAST.SliceList)(yystack.valueAt (3-(1)))),((Slice)(yystack.valueAt (3-(3)))));};
  break;
    

  case 24:
  if (yyn == 24)
    /* "ce.y":184  */ /* lalr1.java:476  */
    {yyval=subslice(1,((String)(yystack.valueAt (1-(1)))),null,null);};
  break;
    

  case 25:
  if (yyn == 25)
    /* "ce.y":186  */ /* lalr1.java:476  */
    {yyval=subslice(2,((String)(yystack.valueAt (3-(1)))),((String)(yystack.valueAt (3-(3)))),null);};
  break;
    

  case 26:
  if (yyn == 26)
    /* "ce.y":188  */ /* lalr1.java:476  */
    {yyval=subslice(3,((String)(yystack.valueAt (5-(1)))),((String)(yystack.valueAt (5-(5)))),((String)(yystack.valueAt (5-(3)))));};
  break;
    

  case 27:
  if (yyn == 27)
    /* "ce.y":190  */ /* lalr1.java:476  */
    {yyval=subslice(4,((String)(yystack.valueAt (2-(1)))),null,null);};
  break;
    

  case 28:
  if (yyn == 28)
    /* "ce.y":192  */ /* lalr1.java:476  */
    {yyval=subslice(5,((String)(yystack.valueAt (4-(1)))),null,((String)(yystack.valueAt (4-(3)))));};
  break;
    

  case 30:
  if (yyn == 30)
    /* "ce.y":203  */ /* lalr1.java:476  */
    {yyval=selection(((CEAST)(yystack.valueAt (3-(1)))),((CEAST)(yystack.valueAt (3-(3)))));};
  break;
    

  case 32:
  if (yyn == 32)
    /* "ce.y":209  */ /* lalr1.java:476  */
    {yyval=logicalAnd(((CEAST)(yystack.valueAt (3-(1)))),((CEAST)(yystack.valueAt (3-(3)))));};
  break;
    

  case 33:
  if (yyn == 33)
    /* "ce.y":211  */ /* lalr1.java:476  */
    {yyval=logicalNot(((CEAST)(yystack.valueAt (2-(2)))));};
  break;
    

  case 34:
  if (yyn == 34)
    /* "ce.y":216  */ /* lalr1.java:476  */
    {yyval=predicate(((CEAST.Operator)(yystack.valueAt (3-(2)))),((CEAST)(yystack.valueAt (3-(1)))),((CEAST)(yystack.valueAt (3-(3)))));};
  break;
    

  case 35:
  if (yyn == 35)
    /* "ce.y":218  */ /* lalr1.java:476  */
    {yyval=predicaterange(((CEAST.Operator)(yystack.valueAt (5-(2)))),((CEAST.Operator)(yystack.valueAt (5-(4)))),((CEAST)(yystack.valueAt (5-(1)))),((CEAST)(yystack.valueAt (5-(3)))),((CEAST)(yystack.valueAt (5-(5)))));};
  break;
    

  case 36:
  if (yyn == 36)
    /* "ce.y":220  */ /* lalr1.java:476  */
    {yyval=predicate(((CEAST.Operator)(yystack.valueAt (3-(2)))),((CEAST)(yystack.valueAt (3-(1)))),((CEAST)(yystack.valueAt (3-(3)))));};
  break;
    

  case 37:
  if (yyn == 37)
    /* "ce.y":224  */ /* lalr1.java:476  */
    {yyval=CEAST.Operator.LE;};
  break;
    

  case 38:
  if (yyn == 38)
    /* "ce.y":225  */ /* lalr1.java:476  */
    {yyval=CEAST.Operator.GE;};
  break;
    

  case 39:
  if (yyn == 39)
    /* "ce.y":226  */ /* lalr1.java:476  */
    {yyval=CEAST.Operator.LT;};
  break;
    

  case 40:
  if (yyn == 40)
    /* "ce.y":227  */ /* lalr1.java:476  */
    {yyval=CEAST.Operator.GT;};
  break;
    

  case 41:
  if (yyn == 41)
    /* "ce.y":231  */ /* lalr1.java:476  */
    {yyval=CEAST.Operator.EQ;};
  break;
    

  case 42:
  if (yyn == 42)
    /* "ce.y":232  */ /* lalr1.java:476  */
    {yyval=CEAST.Operator.NEQ;};
  break;
    

  case 43:
  if (yyn == 43)
    /* "ce.y":233  */ /* lalr1.java:476  */
    {yyval=CEAST.Operator.REQ;};
  break;
    

  case 46:
  if (yyn == 46)
    /* "ce.y":239  */ /* lalr1.java:476  */
    {yyval=((CEAST)(yystack.valueAt (3-(2))));};
  break;
    

  case 47:
  if (yyn == 47)
    /* "ce.y":245  */ /* lalr1.java:476  */
    {yyval=null; dimredef(((String)(yystack.valueAt (3-(1)))),((Slice)(yystack.valueAt (3-(3)))));};
  break;
    

  case 48:
  if (yyn == 48)
    /* "ce.y":249  */ /* lalr1.java:476  */
    {yyval=fieldname(((String)(yystack.valueAt (1-(1)))));};
  break;
    

  case 49:
  if (yyn == 49)
    /* "ce.y":253  */ /* lalr1.java:476  */
    {yyval=constant(CEAST.Constant.STRING,((String)(yystack.valueAt (1-(1)))));};
  break;
    

  case 50:
  if (yyn == 50)
    /* "ce.y":254  */ /* lalr1.java:476  */
    {yyval=constant(CEAST.Constant.LONG,((String)(yystack.valueAt (1-(1)))));};
  break;
    

  case 51:
  if (yyn == 51)
    /* "ce.y":255  */ /* lalr1.java:476  */
    {yyval=constant(CEAST.Constant.DOUBLE,((String)(yystack.valueAt (1-(1)))));};
  break;
    

  case 52:
  if (yyn == 52)
    /* "ce.y":256  */ /* lalr1.java:476  */
    {yyval=constant(CEAST.Constant.BOOLEAN,((String)(yystack.valueAt (1-(1)))));};
  break;
    


/* "CEParserBody.java":630  */ /* lalr1.java:476  */
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

  private static final byte yypact_ninf_ = -47;
  private static final byte yytable_ninf_ = -1;

  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   STATE-NUM.  */
  private static final byte yypact_[] = yypact_init();
  private static final byte[] yypact_init()
  {
    return new byte[]
    {
     -47,    40,    18,   -47,    -5,    23,    17,   -47,   -47,    26,
     -47,   -47,    15,    -5,   -47,    35,   -47,    50,    21,    50,
      -1,   -47,   -47,    27,   -47,    41,   -47,    -5,   -47,    50,
     -47,    34,    28,   -47,   -47,   -47,   -47,   -47,     9,     9,
     -47,    48,    29,   -47,   -47,    53,   -47,    53,   -47,    31,
      50,   -47,   -47,    36,     9,    39,    42,    43,    44,    45,
       9,     9,   -47,    51,   -47,    34,   -47,   -47,   -47,   -47,
     -47,   -47,   -47,    33,   -47,    53,     9,   -47,   -47
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
       3,     0,     0,     1,    16,     0,     2,     5,     7,     9,
      10,     8,     0,    17,    18,     0,     4,     0,     0,     0,
       0,    29,    20,     0,    22,    24,    19,     0,     6,     0,
      11,    14,     0,    48,    49,    50,    51,    52,     0,     0,
      30,    31,     0,    44,    45,     0,    21,    27,    47,     0,
       0,    13,    33,     0,     0,     0,    39,     0,    40,     0,
       0,     0,    23,    25,    12,    15,    46,    32,    42,    37,
      41,    38,    43,    34,    36,    28,     0,    26,    35
    };
  }

/* YYPGOTO[NTERM-NUM].  */
  private static final byte yypgoto_[] = yypgoto_init();
  private static final byte[] yypgoto_init()
  {
    return new byte[]
    {
     -47,   -47,   -47,   -47,    49,   -47,   -19,    32,    52,   -47,
      -2,   -47,    24,   -46,   -47,   -47,   -31,     0,   -47,   -42,
     -47,   -47,   -47
    };
  }

/* YYDEFGOTO[NTERM-NUM].  */
  private static final byte yydefgoto_[] = yydefgoto_init();
  private static final byte[] yydefgoto_init()
  {
    return new byte[]
    {
      -1,     1,     2,     6,     7,     8,     9,    32,    10,    13,
      14,    23,    24,    25,    11,    40,    41,    60,    61,    42,
      16,    43,    44
    };
  }

/* YYTABLE[YYPACT[STATE-NUM]] -- What to do in state STATE-NUM.  If
   positive, shift that token.  If negative, reduce the rule whose
   number is the opposite.  If YYTABLE_NINF, syntax error.  */
  private static final byte yytable_[] = yytable_init();
  private static final byte[] yytable_init()
  {
    return new byte[]
    {
      31,    63,    33,    34,    35,    36,    37,    52,    53,    12,
      31,    26,    33,    34,    35,    36,    37,    38,    73,    74,
      21,     4,    39,    67,     4,    48,    15,    17,     5,    77,
      22,    65,    39,    29,    78,    45,    50,    18,    19,    50,
       3,    51,    46,    20,    64,    18,    19,    55,    56,    57,
      58,    59,    56,     4,    58,    27,    54,    47,    21,    68,
      66,    49,    69,    70,    71,    72,    28,    75,     0,    62,
      30,     0,     0,    76
    };
  }

private static final byte yycheck_[] = yycheck_init();
  private static final byte[] yycheck_init()
  {
    return new byte[]
    {
      19,    47,     3,     4,     5,     6,     7,    38,    39,    14,
      29,    13,     3,     4,     5,     6,     7,    18,    60,    61,
       5,     3,    23,    54,     3,    27,     3,    10,    10,    75,
      15,    50,    23,    12,    76,     8,     8,    11,    12,     8,
       0,    13,    15,    17,    13,    11,    12,    18,    19,    20,
      21,    22,    19,     3,    21,    20,     8,    16,     5,    20,
      24,    29,    20,    20,    20,    20,    17,    16,    -1,    45,
      18,    -1,    -1,    73
    };
  }

/* YYSTOS[STATE-NUM] -- The (internal number of the) accessing
   symbol of state STATE-NUM.  */
  private static final byte yystos_[] = yystos_init();
  private static final byte[] yystos_init()
  {
    return new byte[]
    {
       0,    26,    27,     0,     3,    10,    28,    29,    30,    31,
      33,    39,    14,    34,    35,     3,    45,    10,    11,    12,
      17,     5,    15,    36,    37,    38,    35,    20,    29,    12,
      33,    31,    32,     3,     4,     5,     6,     7,    18,    23,
      40,    41,    44,    46,    47,     8,    15,    16,    35,    32,
       8,    13,    41,    41,     8,    18,    19,    20,    21,    22,
      42,    43,    37,    38,    13,    31,    24,    41,    20,    20,
      20,    20,    20,    44,    44,    16,    42,    38,    44
    };
  }

/* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final byte yyr1_[] = yyr1_init();
  private static final byte[] yyr1_init()
  {
    return new byte[]
    {
       0,    25,    26,    27,    27,    28,    28,    29,    29,    30,
      31,    31,    31,    31,    32,    32,    33,    33,    34,    34,
      35,    35,    36,    36,    37,    37,    37,    37,    37,    38,
      39,    40,    40,    40,    41,    41,    41,    42,    42,    42,
      42,    43,    43,    43,    44,    44,    44,    45,    46,    47,
      47,    47,    47
    };
  }

/* YYR2[YYN] -- Number of symbols on the right hand side of rule YYN.  */
  private static final byte yyr2_[] = yyr2_init();
  private static final byte[] yyr2_init()
  {
    return new byte[]
    {
       0,     2,     2,     0,     3,     1,     3,     1,     1,     1,
       1,     3,     5,     4,     1,     3,     1,     2,     1,     2,
       2,     3,     1,     3,     1,     3,     5,     2,     4,     1,
       3,     1,     3,     2,     3,     5,     3,     2,     2,     1,
       1,     2,     2,     2,     1,     1,     3,     3,     1,     1,
       1,     1,     1
    };
  }

  /* YYTOKEN_NUMBER[YYLEX-NUM] -- Internal symbol number corresponding
      to YYLEX-NUM.  */
  private static final short yytoken_number_[] = yytoken_number_init();
  private static final short[] yytoken_number_init()
  {
    return new short[]
    {
       0,   256,   257,   258,   259,   260,   261,   262,    44,   263,
      59,    46,   123,   125,    91,    93,    58,   124,    33,    60,
      61,    62,   126,    40,    41
    };
  }

  /* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     First, the terminals, then, starting at \a yyntokens_, nonterminals.  */
  private static final String yytname_[] = yytname_init();
  private static final String[] yytname_init()
  {
    return new String[]
    {
  "$end", "error", "$undefined", "NAME", "STRING", "LONG", "DOUBLE",
  "BOOLEAN", "','", "NOT", "';'", "'.'", "'{'", "'}'", "'['", "']'", "':'",
  "'|'", "'!'", "'<'", "'='", "'>'", "'~'", "'('", "')'", "$accept",
  "constraint", "dimredeflist", "clauselist", "clause", "projection",
  "segmenttree", "segmentforest", "segment", "slicelist", "slice",
  "subslicelist", "subslice", "index", "selection", "filter", "predicate",
  "relop", "eqop", "primary", "dimredef", "fieldname", "constant", null
    };
  }

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] = yyrline_init();
  private static final short[] yyrline_init()
  {
    return new short[]
    {
       0,   101,   101,   106,   108,   112,   114,   119,   120,   132,
     137,   139,   141,   143,   148,   150,   155,   157,   162,   164,
     169,   171,   176,   178,   183,   185,   187,   189,   191,   195,
     202,   207,   208,   210,   215,   217,   219,   224,   225,   226,
     227,   231,   232,   233,   237,   238,   239,   244,   249,   253,
     254,   255,   256
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
       2,     2,     2,    18,     2,     2,     2,     2,     2,     2,
      23,    24,     2,     2,     8,     2,    11,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,    16,    10,
      19,    20,    21,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,    14,     2,    15,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    12,    17,    13,    22,     2,     2,     2,
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
       5,     6,     7,     9
    };
  }

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 73;
  private static final int yynnts_ = 23;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 3;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 25;

  private static final int yyuser_token_number_max_ = 263;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */
/* Unqualified %code blocks.  */
/* "ce.y":24  */ /* lalr1.java:1060  */


    // Provide accessors for the parser lexer
    Lexer getLexer() {return this.yylexer;}
    void setLexer(Lexer lexer) {this.yylexer = lexer;}
/* "ce.y":31  */ /* lalr1.java:1060  */
// Abstract Parser actions

abstract CEAST constraint(CEAST.NodeList clauses) throws ParseException;
abstract CEAST projection(CEAST segmenttree) throws ParseException;
abstract CEAST segment(String name, CEAST.SliceList slices) throws ParseException;
abstract Slice slice(CEAST.SliceList subslices) throws ParseException;
abstract Slice subslice(int state, String sfirst, String send, String sstride) throws ParseException;
abstract void dimredef(String name, Slice slice) throws ParseException;
abstract CEAST selection(CEAST projection, CEAST filter) throws ParseException;
abstract CEAST logicalAnd(CEAST lhs, CEAST rhs) throws ParseException;
abstract CEAST logicalNot(CEAST lhs) throws ParseException;
abstract CEAST predicate(CEAST.Operator op, CEAST lhs, CEAST rhs) throws ParseException;
abstract CEAST predicaterange(CEAST.Operator op1, CEAST.Operator op2, CEAST lhs, CEAST mid, CEAST rhs) throws ParseException;
abstract CEAST fieldname(String value) throws ParseException;
abstract CEAST constant(CEAST.Constant sort, String value) throws ParseException;
abstract CEAST segmenttree(CEAST tree, CEAST segment);
abstract CEAST segmenttree(CEAST tree, CEAST.NodeList forest);

abstract CEAST.NodeList nodelist(CEAST.NodeList list, CEAST ast);
abstract CEAST.SliceList slicelist(CEAST.SliceList list, Slice slice);


/* "CEParserBody.java":1344  */ /* lalr1.java:1060  */

}


/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the COPYRIGHT file for more information. */

%error-verbose

%define public
%define package "opendap.servers.parsers"
%define extends {Ceparse}
%define throws {ParseException}
%define lex_throws {ParseException}

%code imports {
import opendap.dap.*;
import opendap.dap.parsers.*;
import opendap.servers.*;
import java.io.*;
import java.util.*;
}

%code {
 
    static public boolean DEBUG = false;

    /**
     * Instantiates the Bison-generated parser.
     * @param yylexer The scanner that will supply tokens to the parser.
     */

    public CeParser(BaseTypeFactory factory)
    {
	super(factory);
	this.yylexer = new Celex(this);
	super.lexstate = (Celex)this.yylexer;
	this.yyDebugStream = System.out;
	if(DEBUG)
	    setDebugLevel(1);
    }


    /* the parse function allows the specification of a
       new stream in case one is reusing the parser
    */

    boolean parse(String constraint) throws ParseException
    {
	((Celex)yylexer).reset(parsestate,constraint);
	return parse();
    }

    // Static entry point to be called by CEEvaluator
    // This parses, then fills in the evaluator from the AST

    static public boolean constraint_expression(CEEvaluator ceEval,
                                         BaseTypeFactory factory,
					 ClauseFactory clauseFactory,
					 String constraint,
				         String url // for error reporting
					 )
            throws DAP2Exception, ParseException
    {
	CeParser parser = new CeParser(factory);
	parser.setURL(url);
	parser.setConstraint(constraint);
        ServerDDS sdds = ceEval.getDDS();
        if(!parser.parse(constraint)) return false;
        ASTconstraint root = (ASTconstraint)parser.getAST();
	root.init(ceEval,factory,clauseFactory,sdds,parser.getASTnodeset());
	root.walkConstraint();
        return true;
    }

    public  void setURL(String url) {
        lexstate.url = url;
    }
    public String getURL() {return lexstate.url;}

    public void setConstraint(String constraint) {lexstate.constraint = constraint;}
    public String getConstraint() {return lexstate.constraint;}

}

%token  SCAN_WORD
%token  SCAN_STRINGCONST
%token  SCAN_NUMBERCONST

%start constraints

%%

constraints:
	  optquestionmark projections
	| optquestionmark selections
	| optquestionmark projections selections
	| /*empty*/
	;

optquestionmark: '?' | /*empty*/ ;

/* %type NClist<NCprojection*> */
projections:
	projectionlist {projections(parsestate,$1);}
	;

/* %type NClist<NCselection*> */
selections:
	clauselist {selections(parsestate,$1);}
	;

/* %type NClist<NCprojection*> */
projectionlist: //==expr.projection
	  projection
	    {$$=projectionlist(parsestate,(Object)null,$1);}
	| projectionlist ',' projection
	    {$$=projectionlist(parsestate,$1,$3);}
	;

/* %type NCprojection* */
projection: //==expr.proj_clause
	  segmentlist
	    {$$=projection(parsestate,$1);}
	| function
	    {$$=$1;}
	;

function:
	  ident '(' ')'
	    {$$=function(parsestate,$1,null);}
	| ident '(' arg_list ')'
	    {$$=function(parsestate,$1,$3);}
	;

/* %type NClist<OCsegment> */
segmentlist: //==expr.proj_variable
	  segment
	    {$$=segmentlist(parsestate,null,$1);}
	| segmentlist '.' segment
	    {$$=segmentlist(parsestate,$1,$3);}
	;

/* %type OCsegment */
segment: //==expr.component
	  word
	    {$$=segment(parsestate,$1,null);}
	| word rangelist
	    {$$=segment(parsestate,$1,$2);}
	;

/* %type NClist<NCslice*> */
rangelist: 
	  range
	    {$$=rangelist(parsestate,null,$1);}
        | rangelist range
	    {$$=rangelist(parsestate,$1,$2);}
	;

/* %type NCslice* */
range:
	  range1
	    {$$=range(parsestate,$1,null,null);}
	| '[' number ':' number ']'
	    {$$=range(parsestate,$2,null,$4);}
	| '[' number ':' number ':' number ']'
	    {$$=range(parsestate,$2,$4,$6);}
	;

range1: '[' number ']'
	    {$$ = range1(parsestate,$2);}
	;


/* %type NClist<NCselection*> */
clauselist: //==expr.selection
	  sel_clause
	    {$$=clauselist(parsestate,null,$1);}
	| clauselist sel_clause
	    {$$=clauselist(parsestate,$1,$2);}
	;

/* %type NCselection* */
sel_clause: //==expr.clause
	  '&' value rel_op '{' value_list '}'
	    {$$=sel_clause(parsestate,1,$2,$3,$5);} /*1,2 distinguish cases*/
	| '&' value rel_op value
	    {$$=sel_clause(parsestate,2,$2,$3,$4);}
	| '&' boolfunction
	    {$$=$1;}
        ;

value_list:
	  value
	    {$$=value_list(parsestate,null,$1);}
	| value_list ',' value
	    {$$=value_list(parsestate,$1,$3);}
	;

value:
	  var /* can be variable ref or a function */
	    {$$=value(parsestate,$1);}
	| function
	    {$$=value(parsestate,$1);}
	| constant
	    {$$=value(parsestate,$1);}
	;

constant:
	  number
	    {$$=constant(parsestate,$1,Lexer.SCAN_NUMBERCONST);}
	| string
	    {$$=constant(parsestate,$1,Lexer.SCAN_STRINGCONST);}
	;

var:
	indexpath
	    {$$=var(parsestate,$1);}
	;




/* %type NClist<NCselection*> */
indexpath:
	  index
	    {$$=indexpath(parsestate,null,$1);}
	| indexpath '.' index
	    {$$=indexpath(parsestate,$1,$3);}
	;

index:
	  word
	    {$$=index(parsestate,$1,null);}
	| word array_indices
	    {$$=index(parsestate,$1,$2);}
	;

/* %type NClist<NCslice*> */
array_indices:
	  range1
	    {$$=array_indices(parsestate,null,$1);}
        | array_indices range1
	    {$$=array_indices(parsestate,$1,$2);}
	;

boolfunction:
	  ident '(' ')'
	    {$$=function(parsestate,$1,null);}
	| ident '(' arg_list ')'
	    {$$=function(parsestate,$1,$3);}
	;

arg_list: //==expr.arg_list
	  value
	    {$$=arg_list(parsestate,null,$1);}
	| value_list ',' value
	    {$$=arg_list(parsestate,$1,$3);}
	;

/* %type SelectionTag */
rel_op:
	  '='     {$$=new Integer(EQUAL);}
	| '>'     {$$=new Integer(GREATER);}
	| '<'     {$$=new Integer(LESS);}
	| '!' '=' {$$=new Integer(NOT_EQUAL);}
	| '>' '=' {$$=new Integer(GREATER_EQL);}
	| '<' '=' {$$=new Integer(LESS_EQL);}
	| '=' '~' {$$=new Integer(REGEXP);}
	;

ident:  word
	    {$$ = $1;}
	;

word:  SCAN_WORD
	    {$$=unescapeDAPName($1);}
	;

number:  SCAN_NUMBERCONST
	    {$$ = $1;}
	;

string: SCAN_STRINGCONST
	    {$$ = $1;}
	;

%%

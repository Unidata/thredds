/* Copyright 2012, UCAR/Unidata
   See the LICENSE file for more information. */

%language "Java"
%debug
%error-verbose

/*
Comment out in case we are using an older version of bison
%define api.push-pull pull
*/
%define abstract
%define package {opuls.ce.parser}
%define parser_class_name {CEParserBody}
%define throws {ParseException}
%define lex_throws {ParseException}

%code imports {
import opuls.core.util.Slice;
import opuls.core.dmr.parser.ParseException;
import static opuls.ce.parser.CEAST.*;
}

%code {

    // Provide accessors for the parser lexer
    Lexer getLexer() {return this.yylexer;}
    void setLexer(Lexer lexer) {this.yylexer = lexer;}
}

%code {// Abstract Parser actions

abstract CEAST constraint(CEAST.NodeList clauses) throws ParseException;
abstract CEAST projection(CEAST segmenttree) throws ParseException;
abstract CEAST segment(String name, CEAST.SliceList slices) throws ParseException;
abstract Slice slice(int state, String sfirst, String send, String sstride) throws ParseException;
abstract void dimredef(String name, Slice slice) throws ParseException;
abstract CEAST selection(CEAST projection, CEAST filter) throws ParseException;
abstract CEAST conjunction(CEAST lhs, CEAST rhs) throws ParseException;
abstract CEAST negation(CEAST lhs) throws ParseException;
abstract CEAST predicate(CEAST.Operator op, Object lhs, Object rhs) throws ParseException;
abstract CEAST predicaterange(CEAST.Operator op1, CEAST.Operator op2, Object lhs, Object mid, Object rhs) throws ParseException;
abstract CEAST constant(CEAST.Constant sort, String value) throws ParseException;
abstract CEAST segmenttree(CEAST tree, CEAST segment);
abstract CEAST segmenttree(CEAST tree, CEAST.NodeList forest);

abstract CEAST.NodeList nodelist(CEAST.NodeList list, CEAST ast);
abstract CEAST.SliceList slicelist(CEAST.SliceList list, Slice slice);
abstract CEAST.StringList stringlist(CEAST.StringList list, String string);

}

%token <String> NAME STRING LONG DOUBLE BOOLEAN

%left ','
%precedence NOT

%type <CEAST> constraint clause projection selection segment
%type <Slice> slice
%type <CEAST> filter predicate
%type <CEAST> dimredef
%type <String> index
%type <CEAST.Operator> relop eqop 
%type <CEAST.NodeList> clauselist
%type <CEAST.NodeList> segmentforest
%type <CEAST> segmenttree
%type <CEAST.SliceList> slicelist
%type <CEAST.StringList> fieldpath
%type <Object> primary constant

%start constraint

%%

/**
A note on terminology.  In DAP2, something of the form
x[0:1:3].y[0] was called a "projection".  That term
continues to be used here, although it is misleading.  Each
of the dotted pieces (e.g x[0:1:3] and y[0]) is referred to
as a "segment". Each of the [...]  is referred to as a
"slice".  Note that this terminology is specific to this
grammar and the objects produced by the parser may have quite
different names.
*/

/* Note that generally, no value is returned because it
   is stored directly into the parser state.
*/

/*
With the introduction of the {...} notation, each clause
not actually can represent multiple variables to include
in the output. This complicates parsing actions because
the constraint now will construct a forest of trees
with each clause being the root of a tree and each tree
representing one or more variables.
*/

constraint:
	dimredeflist
	clauselist
            {$$=constraint($2);}
	;

dimredeflist:
          /*empty*/
        | dimredeflist ';' dimredef
        ;

clauselist:
          clause
	    {$$=nodelist(null,$1);}
        | clauselist ';' clause
	    {$$=nodelist($1,$3);}
        ;

clause:
          projection
	| selection
        ;

/*
A projection is assumed to produce
a tree formed from segments.
If {...} is never used, then the tree is just
a linear list.
In any case, the root of the projection must
not be an instance of {...}.
*/
projection:
	segmenttree
	    {$$=projection($1);}
        ;

segmenttree:
          segment
	    {$$=segmenttree(null,$1);}
        | segmenttree '.' segment
	    {$$=segmenttree($1,$3);}
        | segmenttree '.' '{' segmentforest '}'
	    {$$=segmenttree($1,$4);}
        | segmenttree '{' segmentforest '}'
	    {$$=segmenttree($1,$3);}
        ;

segmentforest:
	  segmenttree
	    {$$=nodelist(null,$1);}
	| segmentforest ',' segmenttree
	    {$$=nodelist($1,$3);}
	;

segment:
          NAME
            {$$=segment($1,null);}
        | NAME slicelist
            {$$=segment($1,$2);}
        ;

slicelist: 
          slice
	    {$$=slicelist(null,$1);}
        | slicelist slice
	    {$$=slicelist($1,$2);}
        ;

slice:
          '[' ']' /* total dimension */ /* case 0 */
            {$$=slice(0,null,null,null);} 
        | '[' '*' ']' /* total dimension */
            {$$=slice(0,null,null,null);} 
        |  '[' index ']' /* case 1 */
            {$$=slice(1,$2,null,null);} 
        | '[' index ':' index ']' /* case 2 */
            {$$=slice(2,$2,$4,null);}
        | '[' index ':' index ':' index ']' /*case 3*/
            {$$=slice(3,$2,$6,$4);}
        | '[' index ':' ']' /* case 4 */
            {$$=slice(4,$2,null,null);}
        | '[' index ':' '*' ']'
            {$$=slice(4,$2,null,null);}
        | '[' index ':' index ':' ']' /* case 5 */
            {$$=slice(5,$2,null,$4);}
        | '[' index ':' index ':' '*' ']'
            {$$=slice(5,$2,null,$4);}
        ;

index:  LONG ;

/* 
Semantics: The projection in a selection cannot have any
slices attached. It is purely a walk to a field of a sequence object.
*/
selection:
        projection '|' filter
            {$$=selection($1,$3);}
        ;

filter:
          predicate
        | predicate ',' predicate  /* ',' == AND */
            {$$=conjunction($1,$3);}
        | '!' predicate %prec NOT
            {$$=negation($2);}
        ;

predicate:
          primary relop primary
            {$$=predicate($2,$1,$3);}
        | primary relop primary relop primary
            {$$=predicaterange($2,$4,$1,$3,$5);}
        | primary eqop primary
            {$$=predicate($2,$1,$3);}
        ;

relop:
	  '<' {$$=CEAST.Operator.LT;}
	| '>' {$$=CEAST.Operator.LE;}
	| '<' '=' {$$=CEAST.Operator.GT;}
	| '>' '=' {$$=CEAST.Operator.GE;}
	;

eqop:
	  '=' '=' {$$=CEAST.Operator.EQ;}
	| '!' '=' {$$=CEAST.Operator.NEQ;}
	| '~' '=' {$$=CEAST.Operator.REQ;}
	;

primary:
          fieldpath {$$=(Object)$1;}
        | constant
	;

fieldpath:
          NAME
	    {$$=stringlist(null,$1);}
        | fieldpath '.' NAME
	    {$$=stringlist($1,$3);}
        ;

dimredef:
        NAME '=' slice
            {$$=null; dimredef($1,$3);}
        ;

constant:
	  STRING {$$=constant(CEAST.Constant.STRING,$1);}
	| LONG {$$=constant(CEAST.Constant.LONG,$1);}
	| DOUBLE {$$=constant(CEAST.Constant.DOUBLE,$1);}
	| BOOLEAN {$$=constant(CEAST.Constant.BOOLEAN,$1);}
	;

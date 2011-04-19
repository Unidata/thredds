/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the COPYRIGHT file for more information. */

/*The lines down to DO NOT DELETE ... comment are specific to the C Parser.
  They will be commennted out when building a java parser.
*/

%pure-parser
%lex-param {CEparsestate* parsestate}
%parse-param {CEparsestate* parsestate}
%{#include "ceparselex.h"%}

/*DO NOT DELETE THIS LINE*/

%token  SCAN_WORD
%token  SCAN_STRINGCONST
%token  SCAN_NUMBERCONST

%start constraints

%%

constraints:
	  projections
	| selections
	| projections selections
	;

projections:
	projectionlist
	;

selections:
	selectionlist
	;

projectionlist: //==projection
	  projection
	| projectionlist ',' projection
	;

projection: //==proj_clause?
	  segmentlist
	;

segmentlist: //==proj_variable
	  segment
	| segmentlist '.' segment
	;

segment: //==component
	  word
	| word array_indices
	;

array_indices: /* appends indices to state->segment */
	  array_index
        | array_indices array_index
	;

array_index: //==array_index
	range
	;

range:
	  range1
	| '[' index ':' index ']'
	| '[' index ':' index ':' index ']'
	;

range1: '[' index ']' {$$=$2;}

selectionlist: //==selection
	  '&' sel_clause
	| selectionlist sel_clause
	;

sel_clause: //==clause
	  selectionvar rel_op '{' value_list '}'
	| selectionvar rel_op value
	| function
        ;

selectionvar: //==value
	selectionpath
	;

selectionpath:
	  arrayelement
	| segment '.' arrayelement
	;

function: //==bool_function?
	  ident '(' ')'
	| ident '(' arg_list ')'
	;

arg_list: //==arg_list
	  value
	| value_list ',' value
	;

value_list:
	  value
	| value_list '|' value
	;

value:
	  selectionpath /* can be variable or an integer */
	| number
	| string
	;

rel_op:
	  '='
	| '>'
	| '<'
	| '!' '='
	| '=' '~'
	| '>' '='
	| '<' '='
	;

arrayelement:
	  word
	| word range1
	;

ident:  word
	;

index:  number
	;

word:  SCAN_WORD
	;

number:  SCAN_NUMBERCONST
	;

string: SCAN_STRINGCONST
	;

%%

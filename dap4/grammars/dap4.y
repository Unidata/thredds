%language "Java"
%debug
%error-verbose
%locations

%define api.push-pull push
%define api.position.type {Bison.Position}
%define abstract
%define package {dap4.core.dmr.parser}
%define parser_class_name {Dap4ParserBody}
%define extends {Dap4Actions}
%define throws {DapException}
%define lex_throws {DapException}

%code imports {
import dap4.core.util.DapException;
}

%code lexer {
public Object getLVal() {return null;}
public int yylex() {return 0;}
public Bison.Position getStartPos() {return null;}
public Bison.Position getEndPos() {return null;}
public void yyerror(Location loc, String s)
{
System.err.println(s);
System.err.printf("near %s%n",getLocator());
}

}

%token <SaxEvent> DATASET_ _DATASET
%token <SaxEvent> GROUP_ _GROUP
%token <SaxEvent> ENUMERATION_ _ENUMERATION
%token <SaxEvent> ENUMCONST_ _ENUMCONST
%token <SaxEvent> NAMESPACE_ _NAMESPACE
%token <SaxEvent> DIMENSION_ _DIMENSION
%token <SaxEvent> DIM_ _DIM
%token <SaxEvent> ENUM_ _ENUM
%token <SaxEvent> MAP_ _MAP
%token <SaxEvent> STRUCTURE_ _STRUCTURE
%token <SaxEvent> SEQUENCE_ _SEQUENCE
%token <SaxEvent> VALUE_ _VALUE
%token <SaxEvent> ATTRIBUTE_ _ATTRIBUTE
%token <SaxEvent> OTHERXML_ _OTHERXML
%token <SaxEvent> ERROR_ _ERROR
%token <SaxEvent> MESSAGE_ _MESSAGE
%token <SaxEvent> CONTEXT_ _CONTEXT
%token <SaxEvent> OTHERINFO_ _OTHERINFO

/* atomictype lexemes */
%token <SaxEvent> CHAR_ _CHAR
%token <SaxEvent> BYTE_ _BYTE
%token <SaxEvent> INT8_ _INT8
%token <SaxEvent> UINT8_ _UINT8
%token <SaxEvent> INT16_ _INT16
%token <SaxEvent> UINT16_ _UINT16
%token <SaxEvent> INT32_ _INT32
%token <SaxEvent> UINT32_ _UINT32
%token <SaxEvent> INT64_ _INT64
%token <SaxEvent> UINT64_ _UINT64
%token <SaxEvent> FLOAT32_ _FLOAT32
%token <SaxEvent> FLOAT64_ _FLOAT64
%token <SaxEvent> STRING_ _STRING
%token <SaxEvent> URL_ _URL
%token <SaxEvent> OPAQUE_ _OPAQUE

/* Standard attributes */
%token <SaxEvent> ATTR_BASE ATTR_BASETYPE ATTR_DAPVERSION ATTR_DMRVERSION
%token <SaxEvent> ATTR_ENUM ATTR_HREF ATTR_NAME ATTR_NAMESPACE
%token <SaxEvent> ATTR_NS ATTR_SIZE ATTR_TYPE ATTR_VALUE 
%token <SaxEvent> ATTR_HTTPCODE

%token <SaxEvent> TEXT

/* Unexpected elements or attributes */
%token <SaxEvent> UNKNOWN_ATTR UNKNOWN_ELEMENT_ _UNKNOWN_ELEMENT

/* XML Attribute List*/
%type <XMLAttributeMap> xml_attribute_map
%type <NamespaceList> namespace_list
%type <SaxEvent> namespace
%type <SaxEvent> atomictype_
%type <SaxEvent> _atomictype
%type <DapNode> element_or_text
%type <SaxEvent> xml_open xml_close xml_attribute


%start response

%%

response:
	  dataset
	| error_response
	;

dataset:
	DATASET_
	xml_attribute_map
		{enterdataset($2);}
	groupbody
	_DATASET
		{leavedataset();}
	;

group:
	GROUP_
	ATTR_NAME
		{entergroup($2);}
	groupbody
	_GROUP
		{leavegroup();}
	;

/* The decls are directly inserted into the current group,
   so no actions required here.
   Note that this is more expansive than the Dap4 RNG
   grammar in that it allows decls to be interleaved in
   any order.
*/
groupbody:
	  /*empty*/
	| groupbody dimdef
	| groupbody enumdef
	| groupbody variable
	| groupbody metadata
	| groupbody group
	;

enumdef:
	ENUMERATION_
	xml_attribute_map
		{enterenumdef($2);}
	enumconst_list
	_ENUMERATION
		{leaveenumdef();}
	;

enumconst_list:
	  enumconst
	| enumconst_list enumconst
	;

enumconst:
	  ENUMCONST_ ATTR_NAME ATTR_VALUE _ENUMCONST
		{enumconst($2,$3);}
	| ENUMCONST_ ATTR_VALUE ATTR_NAME _ENUMCONST
		{enumconst($3,$2);}
	;

dimdef:
	DIMENSION_
	xml_attribute_map
		{enterdimdef($2);}
	metadatalist
	_DIMENSION
		{leavedimdef();}
	;

dimref:
	  DIM_ ATTR_NAME _DIM
		{dimref($2);}
	| DIM_ ATTR_SIZE _DIM
		{dimref($2);}
	;

variable:
	  atomicvariable
	| enumvariable
	| structurevariable
	| sequencevariable
	;

/* Use atomic type to avoid rule explosion */
atomicvariable:
	atomictype_
	ATTR_NAME
		{enteratomicvariable($1,$2);}
	varbody
	_atomictype
		{leaveatomicvariable($5);}

	;

enumvariable:
	ENUM_
	xml_attribute_map
		{enterenumvariable($2);}
	varbody
	_ENUM
		{leaveenumvariable($5);}

	;

/* Does not include enum */
atomictype_:
	  CHAR_ {$$=($1);}
	| BYTE_ {$$=($1);}
	| INT8_ {$$=($1);}
	| UINT8_ {$$=($1);}
	| INT16_ {$$=($1);}
	| UINT16_ {$$=($1);}
	| INT32_ {$$=($1);}
	| UINT32_ {$$=($1);}
	| INT64_ {$$=($1);}
	| UINT64_ {$$=($1);}
	| FLOAT32_ {$$=($1);}
	| FLOAT64_ {$$=($1);}
	| STRING_ {$$=($1);}
	| URL_ {$$=($1);}
	| OPAQUE_ {$$=($1);}
	;

_atomictype:
	  _CHAR {$$=($1);}
	| _BYTE {$$=($1);}
	| _INT8 {$$=($1);}
	| _UINT8 {$$=($1);}
	| _INT16 {$$=($1);}
	| _UINT16 {$$=($1);}
	| _INT32 {$$=($1);}
	| _UINT32 {$$=($1);}
	| _INT64 {$$=($1);}
	| _UINT64 {$$=($1);}
	| _FLOAT32 {$$=($1);}
	| _FLOAT64 {$$=($1);}
	| _STRING {$$=($1);}
	| _URL {$$=($1);}
	| _OPAQUE {$$=($1);}
	| _ENUM {$$=($1);}
	;

varbody:
	  /*empty*/
	| varbody dimref
	| varbody mapref
	| varbody metadata
	;

mapref:
	MAP_
	ATTR_NAME
		{entermap($2);}
	metadatalist
	_MAP
		{leavemap();}
	;

structurevariable:
	STRUCTURE_
	ATTR_NAME
		{enterstructurevariable($2);}
	structbody
	_STRUCTURE
		{leavestructurevariable($5);}
	;

structbody:
	  /*empty*/
	| structbody dimref
	| structbody variable
	| structbody mapref
	| structbody metadata
	;

sequencevariable:
	SEQUENCE_
	ATTR_NAME
		{entersequencevariable($2);}
	sequencebody
	_SEQUENCE
		{leavesequencevariable($5);}
	;

sequencebody:
	  /*empty*/
	| sequencebody dimref
	| sequencebody variable
	| sequencebody mapref
	| sequencebody metadata
	;

metadatalist:
	  /*empty*/
	| metadatalist metadata
	;

metadata:
	  attribute
	;

attribute:
	  atomicattribute
	| containerattribute
	| otherxml
	;


/* We have to case this out to avoid empty list followed by empty list */
atomicattribute:
	  ATTRIBUTE_
	  xml_attribute_map
	  namespace_list
		{enteratomicattribute($2,$3);}
	  valuelist
	  _ATTRIBUTE
		{leaveatomicattribute();}
	|
	  ATTRIBUTE_
	  xml_attribute_map
	  namespace_list
		{enteratomicattribute($2,$3);}
	  _ATTRIBUTE
		{leaveatomicattribute();}
	;

namespace_list:
	  /*empty*/
		{$$=namespace_list();}
	| namespace_list namespace
		{$$=namespace_list($1,$2);}
	;

namespace:
	NAMESPACE_
	ATTR_HREF
	_NAMESPACE
		{$$=($2);}
	;

containerattribute:
	  ATTRIBUTE_
	  xml_attribute_map
	  namespace_list
		{entercontainerattribute($2,$3);}
	  attributelist
	  _ATTRIBUTE
		{leavecontainerattribute();}
	;

/* Cannot be empty */
attributelist:
	  attribute
	| attributelist attribute
	;

/* Cannot be empty (see atomicattribute above) */
valuelist:
	  value
	| valuelist value
	;

value:
	  VALUE_ TEXT _VALUE /* text can contain multiple values */
		{value($2);}
	| VALUE_ ATTR_VALUE _VALUE /* Single Value */
		{value($2);}
	;

otherxml:
	OTHERXML_
	xml_attribute_map
		{enterotherxml($2);}
	xml_body
	_OTHERXML
		{leaveotherxml();}
	;

xml_body:
	  element_or_text
	| xml_body element_or_text
	;

element_or_text:
	  xml_open
	  xml_attribute_map
		{enterxmlelement($1,$2);}
	  xml_body
	  xml_close
		{leavexmlelement($5);}
	| TEXT
		{xmltext($1);}
	;

/* Use a generic map of xml attributes; action
   is responsible for checking correctness.
*/
xml_attribute_map:
	  /*empty*/
		{$$=xml_attribute_map();}
	| xml_attribute_map xml_attribute
		{$$=xml_attribute_map($1,$2);}
	;

/* Collect the set of all known attributes */
/* Assume default action: $$=$1 */
xml_attribute:
	  ATTR_BASE
	| ATTR_BASETYPE
	| ATTR_DAPVERSION
	| ATTR_DMRVERSION
	| ATTR_ENUM
	| ATTR_HREF
	| ATTR_NAME
	| ATTR_NAMESPACE
	| ATTR_NS
	| ATTR_SIZE
	| ATTR_TYPE
	| ATTR_VALUE
	| UNKNOWN_ATTR
	;

/* Collect the set of all known elements */
/* Assume default action: $$=$1 */
xml_open:
	  DATASET_
	| GROUP_
	| ENUMERATION_
	| ENUMCONST_
	| NAMESPACE_
	| DIMENSION_
	| DIM_
	| ENUM_
	| MAP_
	| STRUCTURE_
	| SEQUENCE_
	| VALUE_
	| ATTRIBUTE_
	| OTHERXML_
	| CHAR_
	| BYTE_
	| INT8_
	| UINT8_
	| INT16_
	| UINT16_
	| INT32_
	| UINT32_
	| INT64_
	| UINT64_
	| FLOAT32_
	| FLOAT64_
	| STRING_
	| URL_
	| OPAQUE_
	| UNKNOWN_ELEMENT_
	;

xml_close:
	  _DATASET
	| _GROUP
	| _ENUMERATION
	| _ENUMCONST
	| _NAMESPACE
	| _DIMENSION
	| _DIM
	| _ENUM
	| _MAP
	| _STRUCTURE
	| _SEQUENCE
	| _VALUE
	| _ATTRIBUTE
	| _OTHERXML
	| _CHAR
	| _BYTE
	| _INT8
	| _UINT8
	| _INT16
	| _UINT16
	| _INT32
	| _UINT32
	| _INT64
	| _UINT64
	| _FLOAT32
	| _FLOAT64
	| _STRING
	| _URL
	| _OPAQUE
	| _UNKNOWN_ELEMENT
	;

error_response:
	ERROR_
	xml_attribute_map
	    /* optional attribute name="httpcode" data type="dap4_integer" */
	    {entererror($2);}

	error_body
	_ERROR
	    {leaveerror();}
	;

error_body:
	  /*empty*/
	| error_body error_element
	;

error_element:
	  MESSAGE_ TEXT _MESSAGE
		{errormessage($2);}
	| CONTEXT_ TEXT _CONTEXT
		{errorcontext($2);}
	| OTHERINFO_ TEXT _OTHERINFO
		{errorotherinfo($2);}
	
	;

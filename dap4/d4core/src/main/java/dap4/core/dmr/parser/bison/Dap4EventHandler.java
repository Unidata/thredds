/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser.bison;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;

import static dap4.core.dmr.parser.bison.Dap4BisonParser.Lexer.*;
import static dap4.core.dmr.parser.bison.Dap4BisonParser.YYABORT;
import static dap4.core.dmr.parser.bison.Dap4BisonParser.YYACCEPT;

public class Dap4EventHandler extends SaxEventHandler
{
    //////////////////////////////////////////////////
    // Constants

    static public String SPECIALATTRPREFIX = "_";

    //////////////////////////////////////////////////
    // static types

    //////////////////////////////////////////////////
    // static fields

    static Map<String, Lexeme> elementmap;
    static Map<String, Lexeme> attributemap;

    static {
        elementmap = new HashMap<String, Lexeme>();
        attributemap = new HashMap<String, Lexeme>();

        elementmap.put("Dataset",
            new Lexeme("Dataset", DATASET_, _DATASET,
                new String[]{"name", "dapVersion", "dmrVersion"})); // legal attributes (lower case)
        elementmap.put("Group",
            new Lexeme("Group", GROUP_, _GROUP,
                new String[]{"name"}));
        elementmap.put("Enumeration",
            new Lexeme("Enumeration", ENUMERATION_, _ENUMERATION,
                new String[]{"name", "basetype"}));
        elementmap.put("EnumConst",
            new Lexeme("EnumConst", ENUMCONST_, _ENUMCONST,
                new String[]{"name", "value"}));
        elementmap.put("Namespace",
            new Lexeme("Namespace", NAMESPACE_, _NAMESPACE,
                new String[]{"href"}));
        elementmap.put("Dimension",
            new Lexeme("Dimension", DIMENSION_, _DIMENSION,
                new String[]{"name", "size"}));
        elementmap.put("Dim",
            new Lexeme("Dim", DIM_, _DIM,
                new String[]{"name", "size"}));
        elementmap.put("Enum",
            new Lexeme("Enum", ENUM_, _ENUM,
                new String[]{"enum", "name"}));
        elementmap.put("Map",
            new Lexeme("Map", MAP_, _MAP,
                new String[]{"name"}));
        elementmap.put("Structure",
            new Lexeme("Structure", STRUCTURE_, _STRUCTURE,
                new String[]{"name"}));
        elementmap.put("Sequence",
            new Lexeme("Sequence", SEQUENCE_, _SEQUENCE,
                new String[]{"name"}));
        elementmap.put("Value",
            new Lexeme("Value", VALUE_, _VALUE,
                new String[]{"value"}));
        elementmap.put("Attribute",
            new Lexeme("Attribute", ATTRIBUTE_, _ATTRIBUTE,
                new String[]{"name", "type"}));

        elementmap.put("Char",
            new Lexeme("Char", CHAR_, _CHAR,
                new String[]{"name"}));
        elementmap.put("Byte",
            new Lexeme("Byte", BYTE_, _BYTE,
                new String[]{"name"}));
        elementmap.put("Int8",
            new Lexeme("Int8", INT8_, _INT8,
                new String[]{"name"}));
        elementmap.put("UInt8",
            new Lexeme("UInt8", UINT8_, _UINT8,
                new String[]{"name"}));
        elementmap.put("Int16",
            new Lexeme("Int16", INT16_, _INT16,
                new String[]{"name"}));
        elementmap.put("UInt16",
            new Lexeme("UInt16", UINT16_, _UINT16,
                new String[]{"name"}));
        elementmap.put("Int32",
            new Lexeme("Int32", INT32_, _INT32,
                new String[]{"name"}));
        elementmap.put("UInt32",
            new Lexeme("UInt32", UINT32_, _UINT32,
                new String[]{"name"}));
        elementmap.put("Int64",
            new Lexeme("Int64", INT64_, _INT64,
                new String[]{"name"}));
        elementmap.put("UInt64",
            new Lexeme("UInt64", UINT64_, _UINT64,
                new String[]{"name"}));
        elementmap.put("Float32",
            new Lexeme("Float32", FLOAT32_, _FLOAT32,
                new String[]{"name"}));
        elementmap.put("Float64",
            new Lexeme("Float64", FLOAT64_, _FLOAT64,
                new String[]{"name"}));
        elementmap.put("String",
            new Lexeme("String", STRING_, _STRING,
                new String[]{"name"}));
        elementmap.put("URL",
            new Lexeme("URL", URL_, _URL,
                new String[]{"name"}));
        elementmap.put("Opaque",
            new Lexeme("Opaque", OPAQUE_, _OPAQUE,
                new String[]{"name"}));

        // <Error> related elements
        elementmap.put("Error",
            new Lexeme("Error", ERROR_, _ERROR,
                new String[]{"httpcode"}));
        elementmap.put("Message",
            new Lexeme("Message", MESSAGE_, _MESSAGE, null));
        elementmap.put("Context",
            new Lexeme("Context", CONTEXT_, _CONTEXT, null));
        elementmap.put("OtherInfo",
            new Lexeme("OtherInfo", OTHERINFO_, _OTHERINFO, null));

        // Always insert the lowercase name
        attributemap.put("base", new Lexeme("base", ATTR_BASE));
        attributemap.put("basetype", new Lexeme("basetype", ATTR_BASETYPE));
        attributemap.put("dapversion", new Lexeme("dapversion", ATTR_DAPVERSION));
        attributemap.put("dmrversion", new Lexeme("dmrversion", ATTR_DMRVERSION));
        attributemap.put("enum", new Lexeme("enum", ATTR_ENUM));
        attributemap.put("href", new Lexeme("href", ATTR_HREF));
        attributemap.put("name", new Lexeme("name", ATTR_NAME));
        attributemap.put("namespace", new Lexeme("namespace", ATTR_NAMESPACE));
        attributemap.put("size", new Lexeme("size", ATTR_SIZE));
        attributemap.put("type", new Lexeme("type", ATTR_TYPE));
        attributemap.put("value", new Lexeme("value", ATTR_VALUE));
        attributemap.put("ns", new Lexeme("ns", ATTR_NS));
        // <Error> related xml attributes
        attributemap.put("httpcode", new Lexeme("httpcode", ATTR_HTTPCODE));
    }

    ;

    //////////////////////////////////////////////////
    // Instance variables

    boolean textok = false;
    boolean accepted = false;
    boolean otherxml = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Dap4EventHandler()
    {
    }

    //////////////////////////////////////////////////
    // Abstract method overrides

    // Push the token to the parser
    // @throws SAXException if parser return YYABORT

    public void yyevent(SaxEvent saxtoken)
        throws SAXException
    {
        if(accepted) {
            throw new SAXException("yyevent called after parser has accepted");
        }

        SaxEventType event = saxtoken.eventtype;
        String name = saxtoken.name;
        int yytoken = 0;
        Lexeme element = null;
        Lexeme attr = null;

        element = elementmap.get(name);

        switch (event) {

        case STARTELEMENT:
            if(element == null) {// undefined
                yytoken = UNKNOWN_ELEMENT_;
                break;
            }
            yytoken = element.open;
            if(yytoken == OTHERXML_) {
                otherxml = true;
                textok = true;
            } else if(!otherxml && yytoken == VALUE_)
                textok = true;
            break;

        case ATTRIBUTE:
	    String lcname = name.toLowerCase();
            attr = attributemap.get(lcname);
            if(attr == null) {
	        if(lcname.startsWith(SPECIALATTRPREFIX)) 
		    yytoken = ATTR_SPECIAL;
		else
                    yytoken = UNKNOWN_ATTR;
            } else
                yytoken = attr.atoken;
            break;

        case ENDELEMENT:
            if(element == null) {// undefined
                yytoken = _UNKNOWN_ELEMENT;
                break;
            }
            yytoken = element.close;
            if(yytoken == _OTHERXML) {
                otherxml = false;
                textok = false;
            } else if(!otherxml && yytoken == _VALUE)
                textok = false;
            break;

        case CHARACTERS:
            if(!textok) return; // ignore
            yytoken = TEXT;
            break;

        case ENDDOCUMENT:
            yytoken = EOF;
            break;

        default:
            return; // ignore

        } // switch

        int status = 0;
        try {
            Locator loc = getLocator();
            Dap4BisonParser parser = (Dap4BisonParser)this;
            //Bison.Position pos = new Bison.Position(loc);
            //Dap4BisonParser.Location yyloc = parser.new Location(pos);
            status = parser.push_parse(yytoken, saxtoken);
        } catch (Exception e) {
            throw new SAXException(e);
        }
        if(status == YYABORT)
            throw new SAXException("YYABORT");
        else if(status == YYACCEPT)
            accepted = true;
    }

} // class Dap4EventHandler

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser.bison;

import dap4.core.dmr.DapXML;
import dap4.core.dmr.parser.ParseException;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The role of this class is as follows:
 * 1. Define the abstract action procedures
 * 2. Define useful common constants
 * 3. Define useful common utility procedures
 */

abstract public class Dap4Actions extends Dap4EventHandler
{

    //////////////////////////////////////////////////
    // Constants

    static final float DAPVERSION = 4.0f;
    static final float DMRVERSION = 1.0f;

    static final int RULENULL = 0;
    static final int RULEDIMREF = 1;
    static final int RULEMAPREF = 2;
    static final int RULEVAR = 3;
    static final int RULEMETADATA = 4;

    static final String[] RESERVEDTAGS =  new String[] {
	"_edu.ucar"
    };

    static final BigInteger BIG_INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    static final DapSort[] METADATASCOPES = new DapSort[]{
            DapSort.DATASET,
            DapSort.GROUP,
            DapSort.DIMENSION,
            DapSort.MAP,
            DapSort.VARIABLE,
            DapSort.STRUCTURE,
            DapSort.SEQUENCE,
            DapSort.ATTRIBUTESET
    };

    //////////////////////////////////////////////////
    // Type Decls

    // Predefined map types
    static class XMLAttributeMap extends HashMap<String, SaxEvent>
    {
    }

    static class NamespaceList extends ArrayList<String>
    {
    }


    //////////////////////////////////////////////////
    // Instance variables

    //////////////////////////////////////////////////
    // Constructor(s)

    public Dap4Actions()
    {
    }

    ///////////////////////////////////////////////////
    // Non-abstract parser actions

    XMLAttributeMap
    xml_attribute_map()
            throws DapException
    {
        return new XMLAttributeMap();
    }

    XMLAttributeMap
    xml_attribute_map(XMLAttributeMap map, SaxEvent token)
            throws DapException
    {
        assert (map != null && token != null);
        if(map.containsKey(token.name))
            throw new DapException("XML attribute: duplicate xml attribute: " + token.name);
        map.put(token.name.toLowerCase(), token);
        return map;
    }

    NamespaceList
    namespace_list()
            throws DapException
    {
        return new NamespaceList();
    }

    NamespaceList
    namespace_list(NamespaceList list, SaxEvent token)
            throws DapException
    {
        assert (list != null);
        if(token != null && !list.contains(token.name))
            list.add(token.name);
        return list;
    }

    DapXML
    createxmltext(String text)
            throws DapException
    {
        DapXML node = new DapXML(DapXML.NodeType.TEXT, null);
        node.setText(text);
        return node;
    }

    DapXML
    createxmlelement(SaxEvent open, XMLAttributeMap map)
            throws DapException
    {
        DapXML node = new DapXML(DapXML.NodeType.ELEMENT, open.name);
        for(Map.Entry<String, SaxEvent> entry : map.entrySet()) {
            SaxEvent att = entry.getValue();
            DapXML a = new DapXML(DapXML.NodeType.ATTRIBUTE, att.name);
            a.addXMLAttribute(a);
        }
        return node;
    }

    //////////////////////////////////////////////////
    // Abstract (subclass defined) parser actions

    abstract void enterdataset(XMLAttributeMap attrs) throws ParseException;

    abstract void leavedataset() throws ParseException;

    abstract void entergroup(XMLAttributeMap attrs) throws ParseException;

    abstract void leavegroup() throws ParseException;

    abstract void enterenumdef(XMLAttributeMap attrs) throws ParseException;

    abstract void leaveenumdef() throws ParseException;

    abstract void enumconst(SaxEvent name, SaxEvent value) throws ParseException;

    abstract void enterdimdef(XMLAttributeMap attrs) throws ParseException;

    abstract void leavedimdef() throws ParseException;

    abstract void dimref(SaxEvent nameorsize) throws ParseException;

    abstract void enteratomicvariable(SaxEvent open, XMLAttributeMap attrs) throws ParseException;

    abstract void leaveatomicvariable(SaxEvent close) throws ParseException;

    abstract void enterenumvariable(XMLAttributeMap attrs) throws ParseException;

    abstract void leaveenumvariable(SaxEvent close) throws ParseException;

    abstract void entermap(SaxEvent name) throws ParseException;

    abstract void leavemap() throws ParseException;

    abstract void enterstructurevariable(XMLAttributeMap attrs) throws ParseException;

    abstract void leavestructurevariable(SaxEvent close) throws ParseException;

    abstract void entersequencevariable(XMLAttributeMap attrs) throws ParseException;

    abstract void leavesequencevariable(SaxEvent close) throws ParseException;

    abstract void enteratomicattribute(XMLAttributeMap attrs, NamespaceList nslist) throws ParseException;

    abstract void leaveatomicattribute() throws ParseException;

    abstract void entercontainerattribute(XMLAttributeMap attrs, NamespaceList nslist) throws ParseException;

    abstract void leavecontainerattribute() throws ParseException;

    abstract void value(SaxEvent value) throws ParseException;
    abstract void value(String value) throws ParseException;

    abstract void entererror(XMLAttributeMap attrs) throws ParseException;

    abstract void leaveerror() throws ParseException;

    abstract void errormessage(String value) throws ParseException;

    abstract void errorcontext(String value) throws ParseException;

    abstract void errorotherinfo(String value) throws ParseException;

    abstract void otherxml(XMLAttributeMap attrs, DapXML root) throws ParseException;

    abstract DapXML.XMLList xml_body(DapXML.XMLList body, DapXML elemortext) throws ParseException;

    abstract DapXML element_or_text(SaxEvent open, XMLAttributeMap xmlattrlist, DapXML.XMLList body, SaxEvent close) throws ParseException;

    abstract DapXML xmltext(SaxEvent text) throws ParseException;

    abstract String textstring(String prefix, SaxEvent text) throws ParseException;

}// class Dap4Actions

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;

import java.math.BigInteger;
import java.util.*;

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

    static final BigInteger BIG_INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    static final DapSort[] METADATASCOPES = new DapSort[]{
        DapSort.DATASET,
        DapSort.GROUP,
        DapSort.DIMENSION,
        DapSort.MAP,
        DapSort.ATOMICVARIABLE,
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
    // Constructor(s)

    public Dap4Actions()
    {
    }

    ///////////////////////////////////////////////////
    // Node creation

    DapNode newNode(DapSort sort)
        throws ParseException
    {
        return newNode(null, sort);
    }

    abstract DapNode newNode(String name, DapSort sort) throws ParseException;

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
        DapXML node = (DapXML) newNode(null, DapSort.XML);
        node.setNodeType(DapXML.NodeType.TEXT);
        node.setText(text);
        return node;
    }

    DapXML
    createxmlelement(SaxEvent open, XMLAttributeMap map, DapNode parent)
        throws DapException
    {
        assert(parent != null);
        DapXML node = (DapXML) newNode(open.name, DapSort.XML);
        node.setNodeType(DapXML.NodeType.ELEMENT);
        if(parent.getSort() == DapSort.OTHERXML) {
            DapOtherXML aparent = (DapOtherXML) parent;
            aparent.setRoot(node);
        } else if(parent.getSort() == DapSort.XML) {
            DapXML element = (DapXML) parent;
            assert (element.getNodeType() == DapXML.NodeType.ELEMENT);
            element.addElement(node);
        } else
            throw new DapException("XMLElement: unknown parent type");
        for(Map.Entry<String,SaxEvent> entry : map.entrySet()) {
            SaxEvent att = entry.getValue();
            DapXML a = (DapXML) newNode(att.name, DapSort.XML);
            a.setNodeType(DapXML.NodeType.ATTRIBUTE);
            a.addXMLAttribute(a);
        }
        return node;
    }

    //////////////////////////////////////////////////
    // Abstract (subclass defined) parser actions

    abstract void enterdataset(XMLAttributeMap attrs) throws ParseException;

    abstract void leavedataset() throws ParseException;

    abstract void entergroup(SaxEvent name) throws ParseException;

    abstract void leavegroup() throws ParseException;

    abstract void enterenumdef(XMLAttributeMap attrs) throws ParseException;

    abstract void leaveenumdef() throws ParseException;

    abstract void enumconst(SaxEvent name, SaxEvent value) throws ParseException;

    abstract void enterdimdef(XMLAttributeMap attrs) throws ParseException;

    abstract void leavedimdef() throws ParseException;

    abstract void dimref(SaxEvent nameorsize) throws ParseException;

    abstract void enteratomicvariable(SaxEvent open, SaxEvent nameattr) throws ParseException;

    abstract void leaveatomicvariable(SaxEvent close) throws ParseException;

    abstract void enterenumvariable(XMLAttributeMap attrs) throws ParseException;

    abstract void leaveenumvariable(SaxEvent close) throws ParseException;

    abstract void entermap(SaxEvent name) throws ParseException;

    abstract void leavemap() throws ParseException;

    abstract void enterstructurevariable(SaxEvent name) throws ParseException;

    abstract void leavestructurevariable(SaxEvent close) throws ParseException;

    abstract void entersequencevariable(SaxEvent name) throws ParseException;

    abstract void leavesequencevariable(SaxEvent close) throws ParseException;

    abstract void enteratomicattribute(XMLAttributeMap attrs, NamespaceList nslist) throws ParseException;

    abstract void leaveatomicattribute() throws ParseException;

    abstract void entercontainerattribute(XMLAttributeMap attrs, NamespaceList nslist) throws ParseException;

    abstract void leavecontainerattribute() throws ParseException;

    abstract void value(SaxEvent value) throws ParseException;

    abstract void enterotherxml(XMLAttributeMap attrs) throws ParseException;

    abstract void leaveotherxml() throws ParseException;

    abstract void enterxmlelement(SaxEvent open, XMLAttributeMap map) throws ParseException;

    abstract void leavexmlelement(SaxEvent close) throws ParseException;

    abstract void xmltext(SaxEvent text) throws ParseException;

    abstract void entererror(XMLAttributeMap attrs) throws ParseException;

    abstract void leaveerror() throws ParseException;

    abstract void errormessage(SaxEvent value) throws ParseException;

    abstract void errorcontext(SaxEvent value) throws ParseException;

    abstract void errorotherinfo(SaxEvent value) throws ParseException;


}// class Dap4Actions

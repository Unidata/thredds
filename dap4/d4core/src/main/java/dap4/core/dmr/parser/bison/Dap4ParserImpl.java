/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser.bison;

import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.dmr.parser.ParseException;
import dap4.core.dmr.parser.ParseUtil;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;
import dap4.core.util.DapUtil;
import dap4.core.util.Escape;
import org.xml.sax.SAXException;

import java.math.BigInteger;
import java.util.*;

/**
 * Implement the Dap4 Parse Actions
 */

public class Dap4ParserImpl extends Dap4BisonParser implements Dap4Parser
{

    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // static variables

    static protected int globaldebuglevel = 0;

    //////////////////////////////////////////////////
    // Static methods

    static public void setGlobalDebugLevel(int level)
    {
        globaldebuglevel = level;
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected DMRFactory factory = null;

    protected ErrorResponse errorresponse = null;

    protected Deque<DapNode> scopestack = new ArrayDeque<DapNode>();

    protected DapDataset root = null; // of the parse

    protected boolean debug = false;

    //////////////////////////////////////////////////
    // Constructors

    public Dap4ParserImpl(DMRFactory factory)
    {
        super();
        this.factory = (factory == null ? new DMRFactory() : factory); // see Dap4Actions
        if(globaldebuglevel > 0) setDebugLevel(globaldebuglevel);
    }

    //////////////////////////////////////////////////
    // Accessors

    public ErrorResponse getErrorResponse()
    {
        return errorresponse;
    }

    public DapDataset getDMR()
    {
        return this.root;
    }

    //////////////////////////////////////////////////
    // Parser API

    public boolean
    parse(String input)
            throws SAXException
    {
        return super.parse(input);
    }

    //////////////////////////////////////////////////
    // Parser specific methods

    DapGroup
    getGroupScope()
            throws DapException
    {
        DapGroup gscope = (DapGroup) searchScope(DapSort.GROUP, DapSort.DATASET);
        if(gscope == null) throw new DapException("Undefined Group Scope");
        return gscope;
    }

    DapNode
    getMetadataScope()
            throws DapException
    {
        // Search up the stack for first match.
        DapNode match = searchScope(METADATASCOPES);
        if(match == null)
            throw new ParseException("No enclosing metadata capable scope");
        return match;
    }

    DapNode
    getParentScope()
            throws DapException
    {
        DapNode parent = searchScope(DapSort.VARIABLE, DapSort.GROUP, DapSort.DATASET);
        if(parent == null) throw new DapException("Undefined parent scope");
        return parent;
    }

    DapVariable
    getVariableScope()
            throws DapException
    {
        DapNode match = searchScope(DapSort.VARIABLE);
        if(match == null)
            throw new ParseException("No enclosing variable scope");
        return (DapVariable) match;
    }

    DapNode
    getScope(DapSort... sort)
            throws DapException
    {
        DapNode node = searchScope(sort);
        if(node == null) // return exception if not found
            throw new ParseException("No enclosing scope of specified type");
        return node;
    }

    DapNode
    searchScope(DapSort... sort)
    {
        Iterator it = scopestack.iterator();
        while(it.hasNext()) {
            DapNode node = (DapNode) it.next();
            for(int j = 0; j < sort.length; j++) {
                if(node.getSort() == sort[j])
                    return node;
            }
        }
        return null;
    }

    DapVariable
    findVariable(DapNode parent, String name)
    {
        DapVariable var = null;
        switch (parent.getSort()) {
        case DATASET:
        case GROUP:
            var = ((DapGroup) parent).findVariable(name);
            break;
        case VARIABLE:
            DapVariable v = (DapVariable) parent;
            DapType t = v.getBaseType();
            switch (t.getTypeSort()) {
            case Structure:
                var = (DapVariable) ((DapStructure) t).findByName(name);
                break;
            case Sequence:
                var = (DapVariable) ((DapSequence) t).findByName(name);
                break;
            default:
                assert false : "Container cannot be atomic variable";
            }
        default:
            break;
        }
        return var;
    }

    // Attribute map utilities
    SaxEvent
    pull(XMLAttributeMap map, String name)
    {
        SaxEvent event = map.remove(name.toLowerCase());
        return event;
    }

    /**
     * add any reserved xml attributes to a node unchanged
     */
    void
    passReserved(XMLAttributeMap map, DapNode node)
            throws ParseException
    {
	try {
           DapAttribute attr = null;
           for(Map.Entry<String, SaxEvent> entry : map.entrySet()) {
                SaxEvent event = entry.getValue();
		String key = entry.getKey();
		String value = event.value;
		if(isReserved(key))
		    node.addXMLAttribute(key,value);
           }
       } catch (DapException de) {
           throw new ParseException(de);
       }
    }

    // Attribute map utilities
    SaxEvent
    peek(XMLAttributeMap map, String name)
    {
        SaxEvent event = map.get(name.toLowerCase());
        return event;
    }

    //////////////////////////////////////////////////
    // Attribute construction

    DapAttribute
    makeAttribute(DapSort sort, String name, DapType basetype,
                  List<String> nslist, DapNode parent)
            throws DapException
    {
        DapAttribute attr = new DapAttribute(name, basetype);
        if(sort == DapSort.ATTRIBUTE) {
            attr.setBaseType(basetype);
        }
        parent.addAttribute(attr);
        attr.setNamespaceList(nslist);
        return attr;
    }

    boolean isempty(SaxEvent token)
    {
        return token == null || isempty(token.value);
    }

    boolean isempty(String text)
    {
        return (text == null || text.length() == 0);
    }

    List<String>
    convertNamespaceList(NamespaceList nslist)
    {
        return nslist;
    }

    boolean
    islegalenumtype(DapType kind)
    {
        return kind.isIntegerType();
    }

    boolean
    islegalattributetype(DapType kind)
    {
        return kind.isLegalAttrType();
    }

    DapAttribute
    lookupAttribute(DapNode parent, XMLAttributeMap attrs)
            throws DapException
    {
        SaxEvent name = pull(attrs, "name");
        if(isempty(name))
            throw new ParseException("Attribute: Empty attribute name");
        String attrname = name.value;
        return parent.findAttribute(attrname);
    }

    void
    changeAttribute(DapAttribute attr, XMLAttributeMap description)
            throws DapException
    {
        SaxEvent name = pull(description, "name");
        if(isempty(name))
            throw new ParseException("Attribute: Empty attribute name");
        String attrname = name.value;
        if(!attr.getShortName().equals(attrname))
            throw new ParseException("Attribute: DATA DMR: Attribute name mismatch:" + name.name);
        switch (attr.getSort()) {
        case ATTRIBUTE:
            SaxEvent atype = pull(description, "type");
            String typename = (atype == null ? "Int32" : atype.value);
            if("Byte".equalsIgnoreCase(typename)) typename = "UInt8";
            DapType basetype = (DapType)root.lookup(typename,DapSort.ENUMERATION,DapSort.ATOMICTYPE);
            if(basetype != attr.getBaseType())
                throw new ParseException("Attribute: DATA DMR: Attempt to change attribute type: " + typename);
            attr.clearValues();
            SaxEvent value = pull(description, "value");
            if(value != null)
                attr.setValues(new String[]{value.value});
            break;
        case ATTRIBUTESET:
            // clear the contained attributes
            attr.setAttributes(new HashMap<String, DapAttribute>());
            break;
        case OTHERXML:
            throw new ParseException("Attribute: DATA DMR: OtherXML attributes not supported");
        }
    }

    DapAttribute
    createatomicattribute(XMLAttributeMap attrs, NamespaceList nslist, DapNode parent)
            throws DapException
    {
        SaxEvent name = pull(attrs, "name");
        if(isempty(name))
            throw new ParseException("Attribute: Empty attribute name");
        String attrname = name.value;
        SaxEvent atype = pull(attrs, "type");
        String typename = (atype == null ? "Int32" : atype.value);
        if("Byte".equalsIgnoreCase(typename)) typename = "UInt8";
        DapType basetype = (DapType)root.lookup(typename,DapSort.ENUMERATION,DapSort.ATOMICTYPE);
        if(basetype == null || !islegalattributetype(basetype))
            throw new ParseException("Attribute: Invalid attribute type: " + typename);
        List<String> hreflist = convertNamespaceList(nslist);
        DapAttribute attr = makeAttribute(DapSort.ATTRIBUTE, name.value, basetype, hreflist, parent);
        return attr;
    }

    DapAttribute
    createcontainerattribute(XMLAttributeMap attrs, NamespaceList nslist, DapNode parent)
            throws DapException
    {
        SaxEvent name = pull(attrs, "name");
        if(isempty(name))
            throw new ParseException("ContainerAttribute: Empty attribute name");
        List<String> hreflist = convertNamespaceList(nslist);
        DapAttribute attr
                = makeAttribute(DapSort.ATTRIBUTESET, name.value, null, hreflist, parent);
        return attr;
    }

    void
    createvalue(String value, DapAttribute parent)
            throws DapException
    {
        // Since this came from <Value>...</Value>
        // Clean it up
        value = cleanup(value);
        if(parent != null) parent.setValues(new String[]{value});
    }

    protected String
    cleanup(String value)
    {
        value = value.trim();
        int first = -1;
        for(int i = 0; i < value.length(); i++) {
            if(first < 0 && value.charAt(i) > ' ') {
                first = i;
                break;
            }
        }
        int last = -1;
        for(int i = value.length() - 1; i >= 0; i--) {
            if(last < 0 && value.charAt(i) > ' ') {
                last = i;
                break;
            }
        }
        if(last < 0) last = value.length() - 1;
        if(first < 0) first = 0;
        value = value.substring(first, last + 1);
        return value;
    }

    void
    createvalue(SaxEvent value, DapAttribute parent)
            throws DapException
    {
        List<String> textlist = null;
        if(value.eventtype == SaxEventType.CHARACTERS) {
            textlist = ParseUtil.collectValues(value.text);
        } else if(value.eventtype == SaxEventType.ATTRIBUTE) {
            textlist = new ArrayList<String>();
            textlist.add(value.value);
        }
        if(textlist != null)
            parent.setValues(textlist.toArray(new String[textlist.size()]));
    }

    DapOtherXML
    createotherxml(XMLAttributeMap attrs, DapNode parent)
            throws DapException
    {
        SaxEvent name = pull(attrs, "name");
        SaxEvent href = pull(attrs, "href");
        if(isempty(name))
            throw new ParseException("OtherXML: Empty name");
        List<String> nslist = new ArrayList<String>();
        if(!isempty(href))
            nslist.add(href.value);
        DapOtherXML other
                = (DapOtherXML) makeAttribute(DapSort.OTHERXML, name.value, null, nslist, parent);
        parent.setAttribute(other);
        return other;
    }

    //////////////////////////////////////////////////
    // Abstract action definitions
    @Override
    void
    enterdataset(XMLAttributeMap attrs)
            throws ParseException
    {
        this.debug = getDebugLevel() > 0; // make sure we have the latest value
        if(debug) report("enterdataset");
        SaxEvent name = pull(attrs, "name");
        SaxEvent dapversion = pull(attrs, "dapversion");
        SaxEvent dmrversion = pull(attrs, "dmrversion");
        if(isempty(name))
            throw new ParseException("Empty dataset name attribute");
        // convert and test version numbers
        float ndapversion = DAPVERSION;
        if(dapversion != null)
            try {
                ndapversion = Float.parseFloat(dapversion.value);
            } catch (NumberFormatException nfe) {
                ndapversion = DAPVERSION;
            }
        if(ndapversion != DAPVERSION)
            throw new ParseException("Dataset dapVersion mismatch: " + dapversion.value);
        float ndmrversion = DMRVERSION;
        if(dmrversion != null)
            try {
                ndmrversion = Float.parseFloat(dmrversion.value);
            } catch (NumberFormatException nfe) {
                ndmrversion = DMRVERSION;
            }
        if(ndmrversion != DMRVERSION)
            throw new ParseException("Dataset dmrVersion mismatch: " + dmrversion.value);
        this.root = new DapDataset(name.value);
        this.root.setDapVersion(Float.toString(ndapversion));
        this.root.setDMRVersion(Float.toString(ndmrversion));
        this.root.setDataset(this.root);
        passReserved(attrs, this.root);
        scopestack.push(this.root);
    }

    @Override
    void
    leavedataset()
            throws ParseException
    {
        if(debug) report("leavedataset");
        assert (scopestack.peek() != null && scopestack.peek().getSort() == DapSort.DATASET);
        this.root.sort();
        scopestack.pop();
        if(!scopestack.isEmpty())
            throw new ParseException("Dataset: nested dataset");
        this.root.finish();
    }

    @Override
    void
    entergroup(XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("entergroup");
        SaxEvent name = pull(attrs, "name");
        if(isempty(name))
            throw new ParseException("Empty group name");
        try {
            DapGroup parent = getGroupScope();
            DapGroup group;
            group = new DapGroup(name.value);
            passReserved(attrs, group);
            parent.addDecl(group);
            scopestack.push(group);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leavegroup()
            throws ParseException
    {
        if(debug) report("leavegroup");
        scopestack.pop();
    }

    @Override
    void
    enterenumdef(XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("enterenumdef");
        try {
            SaxEvent name = pull(attrs, "name");
            if(isempty(name))
                throw new ParseException("Enumdef: Empty Enum Declaration name");

            SaxEvent basetype = pull(attrs, "basetype");
            DapType basedaptype = null;
            if(basetype == null) {
                basedaptype = DapEnumeration.DEFAULTBASETYPE;
            } else {
                String typename = basetype.value;
                if("Byte".equalsIgnoreCase(typename)) typename = "UInt8";
                basedaptype = (DapType)this.root.lookup(typename,DapSort.ATOMICTYPE);
                if(basedaptype == null || !islegalenumtype(basedaptype))
                    throw new ParseException("Enumdef: Invalid Enum Declaration Type name: " + basetype.value);
            }
            DapEnumeration dapenum = null;
            dapenum = new DapEnumeration(name.value, basedaptype);
            passReserved(attrs, dapenum);
            DapGroup parent = getGroupScope();
            parent.addDecl(dapenum);
            scopestack.push(dapenum);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }


    @Override
    void
    leaveenumdef()
            throws ParseException
    {
        if(debug) report("leaveenumdef");
        DapEnumeration eparent = (DapEnumeration) scopestack.pop();
        List<String> econsts = eparent.getNames();
        if(econsts.size() == 0)
            throw new ParseException("Enumdef: no enum constants specified");
    }

    @Override
    void
    enumconst(SaxEvent name, SaxEvent value)
            throws ParseException
    {
        if(debug) report("enumconst");
        if(isempty(name))
            throw new ParseException("Enumconst: Empty enum constant name");
        if(isempty(value))
            throw new ParseException("Enumdef: Invalid enum constant value: " + value.value);
        long lvalue = 0;
        try {
            BigInteger bivalue = new BigInteger(value.value);
            bivalue = DapUtil.BIG_UMASK64.and(bivalue);
            lvalue = bivalue.longValue();
        } catch (NumberFormatException nfe) {
            throw new ParseException("Enumconst: illegal value: " + value.value);
        }
        try {
            DapEnumeration parent = (DapEnumeration) getScope(DapSort.ENUMERATION);
            // Verify that the name is a legal enum constant name, which is restricted
            // vis-a-vis other names
            if(!ParseUtil.isLegalEnumConstName(name.value))
                throw new ParseException("Enumconst: illegal enumeration constant name: " + name.value);
            parent.addEnumConst(new DapEnumConst(name.value, lvalue));
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    enterdimdef(XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("enterdimdef");
        SaxEvent name = pull(attrs, "name");
        SaxEvent size = pull(attrs, "size");
        long lvalue = 0;
        if(isempty(name))
            throw new ParseException("Dimdef: Empty dimension declaration name");
        if(isempty(size))
            throw new ParseException("Dimdef: Empty dimension declaration size");
        try {
            lvalue = Long.parseLong(size.value);
            if(lvalue <= 0)
                throw new ParseException("Dimdef: value <= 0: " + lvalue);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Dimdef: non-integer value: " + size.value);
        }
        DapDimension dim = null;
        try {
            dim = new DapDimension(name.value, lvalue);
	    passReserved(attrs,dim);
            dim.setShared(true);
            DapGroup parent = getGroupScope();
            parent.addDecl(dim);
            scopestack.push(dim);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leavedimdef()
            throws ParseException
    {
        if(debug) report("leavedimdef");
        scopestack.pop();
    }

    @Override
    void
    dimref(SaxEvent nameorsize)
            throws ParseException
    {
        if(debug) report("dimref");
        try {
            DapDimension dim = null;
            DapVariable var = getVariableScope();
            assert var != null : "Internal error";

            boolean isname = nameorsize.name.equals("name");
            if(isname && isempty(nameorsize))
                throw new ParseException("Dimref: Empty dimension reference name");
            else if(isempty(nameorsize))
                throw new ParseException("Dimref: Empty dimension size");
            if(isname) {
                DapGroup dg = var.getGroup();
                if(dg == null)
                    throw new ParseException("Internal error: variable has no containing group");
                DapGroup grp = var.getGroup();
                if(grp == null)
                    throw new ParseException("Variable has no group");
                dim = (DapDimension) grp.findByFQN(nameorsize.value, DapSort.DIMENSION);
            } else {// Size only is given; presume a number; create unique anonymous dimension
                String ssize = nameorsize.value.trim();
                {
                    // Note that we create it in the root group
                    assert (root != null);
                    long anonsize;
                    try {
                        anonsize = Long.parseLong(nameorsize.value.trim());
                    } catch (NumberFormatException nfe) {
                        throw new ParseException("Dimref: Illegal dimension size");
                    }
                    dim = root.createAnonymous(anonsize);
                }
            }
            if(dim == null)
                throw new ParseException("Unknown dimension: " + nameorsize.value);
            var.addDimension(dim);
        } catch (DapException de) {
            throw new ParseException(de.getMessage(), de.getCause());
        }
    }

    @Override
    void
    enteratomicvariable(SaxEvent open, XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("enteratomicvariable");
        try {
            SaxEvent name = pull(attrs,"name");
            if(isempty(name))
                throw new ParseException("Atomicvariable: Empty dimension reference name");
            String typename = open.name;
            if("Byte".equals(typename)) typename = "UInt8"; // special case
            DapType basetype = (DapType)this.root.lookup(typename,
                    DapSort.ENUMERATION, DapSort.ATOMICTYPE);
            if(basetype == null)
                throw new ParseException("AtomicVariable: Illegal type: " + open.name);
            DapVariable var = null;
            // Do type substitutions
            var = new DapVariable(name.value, basetype);
            passReserved(attrs, var);
            // Look at the parent scope
            DapNode parent = scopestack.peek();
            if(parent == null)
                throw new ParseException("Variable has no parent");
            switch (parent.getSort()) {
            case DATASET:
            case GROUP:
                ((DapGroup) parent).addDecl(var);
                break;
            case VARIABLE:
                addField((DapVariable) parent, var);
                break;
            default:
                assert false : "Atomic variable in illegal scope";
            }
            scopestack.push(var);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    void openclosematch(SaxEvent close, DapSort sort)
            throws ParseException
    {
        String typename = close.name;
        if("Byte".equals(typename)) typename = "UInt8"; // special case
        switch (sort) {
        case VARIABLE:
            TypeSort atype = TypeSort.getTypeSort(typename);
            DapVariable var = (DapVariable) searchScope(sort);
            assert var != null;
            TypeSort vartype = var.getBaseType().getTypeSort();
            if(atype == null)
                throw new ParseException("Variable: Illegal type: " + typename);
            if(atype != vartype)
                throw new ParseException(String.format("variable: open/close type mismatch: <%s> </%s>",
                        vartype, atype));
            break;
        default:
            throw new ParseException("Variable: Illegal type: " + typename);
        }
    }

    void leavevariable()
            throws ParseException
    {
        scopestack.pop();
    }

    void
    leaveatomicvariable(SaxEvent close)
            throws ParseException
    {
        openclosematch(close, DapSort.VARIABLE);
        leavevariable();
    }

    @Override
    void
    enterenumvariable(XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("enterenumvariable");
        try {
            SaxEvent name = pull(attrs, "name");
            SaxEvent enumtype = pull(attrs, "enum");
            if(isempty(name))
                throw new ParseException("Enumvariable: Empty variable name");
            if(isempty(enumtype))
                throw new ParseException("Enumvariable: Empty enum type name");
            DapEnumeration target = (DapEnumeration) root.findByFQN(enumtype.value, DapSort.ENUMERATION);
            if(target == null)
                throw new ParseException("EnumVariable: no such enum: " + name.value);
            DapVariable var = null;
            var = new DapVariable(name.value, target);
            passReserved(attrs, var);
            // Look at the parent scope
            DapNode parent = scopestack.peek();
            if(parent == null)
                throw new ParseException("Variable has no parent");
            switch (parent.getSort()) {
            case DATASET:
            case GROUP:
                ((DapGroup) parent).addDecl(var);
                break;
            case VARIABLE:
                addField((DapVariable) parent, var);
                break;
            default:
                assert false : "Atomic variable in illegal scope";
            }
            scopestack.push(var);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leaveenumvariable(SaxEvent close)
            throws ParseException
    {
        if(debug) report("leaveenumvariable");
        openclosematch(close, DapSort.VARIABLE);
        leavevariable();
    }

    @Override
    void
    entermap(SaxEvent name)
            throws ParseException
    {
        if(debug) report("entermap");
        if(isempty(name))
            throw new ParseException("Mapref: Empty map name");
        DapVariable var;
        try {
            var = (DapVariable) root.findByFQN(name.value, DapSort.VARIABLE);
        } catch (DapException de) {
            throw new ParseException(de);
        }
        if(var == null)
            throw new ParseException("Mapref: undefined variable: " + name.name);
        // Verify that this is a legal map =>
        // 1. it is outside the scope of its parent if the parent
        //    is a structure.
        DapNode container = var.getContainer();
        DapNode scope;
        try {
            scope = getParentScope();
        } catch (DapException de) {
            throw new ParseException(de);
        }
        if((container.getSort() == DapSort.STRUCTURE || container.getSort() == DapSort.SEQUENCE)
                && container == scope)
            throw new ParseException("Mapref: map variable not in outer scope: " + name.name);
        DapMap map = new DapMap(var);
        try {
            // Pull the top variable scope
            DapVariable parent = (DapVariable) searchScope(DapSort.VARIABLE);
            if(parent == null)
                throw new ParseException("Variable has no parent: " + var);
            parent.addMap(map);
        } catch (DapException de) {
            throw new ParseException(de);
        }
        scopestack.push(map);
    }

    @Override
    void
    leavemap()
            throws ParseException
    {
        if(debug) report("leavemap");
        scopestack.pop();
    }

    @Override
    void
    enterstructurevariable(XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("enterstructurevariable");
        SaxEvent name = pull(attrs,"name");
        if(isempty(name))
            throw new ParseException("Structure: Empty structure name");
        try {
            DapStructure type = null;
            DapVariable var = null;
            type = new DapStructure(name.value);
            passReserved(attrs, type);
            var = new DapVariable(name.value, type);
            // Look at the parent scope
            DapNode parent = scopestack.peek();
            if(parent == null)
                throw new ParseException("Variable has no parent");
            switch (parent.getSort()) {
            case DATASET:
            case GROUP:
                ((DapGroup) parent).addDecl(var);
                var.getGroup().addDecl(type);
                break;
            case VARIABLE:
                addField((DapVariable) parent, var);
                var.getGroup().addDecl(type);
                break;
            default:
                assert false : "Structure variable in illegal scope";
            }
            scopestack.push(var);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leavestructurevariable(SaxEvent close)
            throws ParseException
    {
        if(debug) report("leavestructurevariable");
        openclosematch(close, DapSort.VARIABLE);
        leavevariable();
    }

    @Override
    void
    entersequencevariable(XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("entersequencevariable");
        SaxEvent name = pull(attrs,"name");
        if(isempty(name))
            throw new ParseException("Sequence: Empty sequence name");
        try {
            DapVariable var = null;
            DapType type = null;
            type = new DapSequence(name.value);
            passReserved(attrs, type);
            var = new DapVariable(name.value, type);
            // Look at the parent scope
            DapNode parent = scopestack.peek();
            if(parent == null)
                throw new ParseException("Variable has no parent");
            switch (parent.getSort()) {
            case DATASET:
            case GROUP:
                ((DapGroup) parent).addDecl(var);
                var.getGroup().addDecl(type);
                break;
            case VARIABLE:
                addField((DapVariable) parent, var);
                var.getGroup().addDecl(type);
                break;
            default:
                assert false : "Structure variable in illegal scope";
            }
            scopestack.push(var);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leavesequencevariable(SaxEvent close)
            throws ParseException
    {
        if(debug) report("leavesequencevariable");
        openclosematch(close, DapSort.VARIABLE);
        leavevariable();
    }

    @Override
    void
    enteratomicattribute(XMLAttributeMap attrs, NamespaceList nslist)
            throws ParseException
    {
        if(debug) report("enteratomicattribute");
        try {
            DapNode parent = getMetadataScope();
            DapAttribute attr = null;
            attr = createatomicattribute(attrs, nslist, parent);
            scopestack.push(attr);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leaveatomicattribute()
            throws ParseException
    {
        if(debug) report("leaveatomicattribute");
        DapAttribute attr = (DapAttribute) scopestack.pop();
        // Ensure that the attribute has at least one value
        if(java.lang.reflect.Array.getLength(attr.getValues()) == 0)
            throw new ParseException("AtomicAttribute: attribute has no values");
    }

    @Override
    void
    entercontainerattribute(XMLAttributeMap attrs, NamespaceList nslist)
            throws ParseException
    {
        if(debug) report("entercontainerattribute");
        try {
            DapNode parent = getMetadataScope();
            DapAttribute attr = null;
            attr = createcontainerattribute(attrs, nslist, parent);
            scopestack.push(attr);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leavecontainerattribute()
            throws ParseException
    {
        if(debug) report("leavecontainerattribute");
        scopestack.pop();
    }

    /**
     * This is called for <Value>...</Value>
     *
     * @param value
     * @throws ParseException
     */
    @Override
    void value(String value)
            throws ParseException
    {
        if(debug) report("value");
        try {
            DapAttribute parent = (DapAttribute) getScope(DapSort.ATTRIBUTE);
            createvalue(value, parent);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    /**
     * This is called for <Value value="..."/>
     *
     * @param value
     * @throws ParseException
     */
    @Override
    void value(SaxEvent value)
            throws ParseException
    {
        if(debug) report("value");
        try {
            DapAttribute parent = (DapAttribute) getScope(DapSort.ATTRIBUTE);
            createvalue(value, parent);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    otherxml(XMLAttributeMap attrs, DapXML root)
            throws ParseException
    {
        if(debug) report("enterotherxml");
        try {
            DapNode parent = getMetadataScope();
            DapOtherXML other = createotherxml(attrs, parent);
            parent.setAttribute(other);
            other.setRoot(root);
            if(debug) report("leaveotherxml");
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    DapXML.XMLList
    xml_body(DapXML.XMLList body, DapXML elemortext)
            throws ParseException
    {
        if(debug) report("xml_body.enter");
        if(body == null) body = new DapXML.XMLList();
        if(elemortext != null)
            body.add(elemortext);
        if(debug) report("xml_body.exit");
        return body;
    }

    @Override
    DapXML
    element_or_text(SaxEvent open, XMLAttributeMap map, DapXML.XMLList body, SaxEvent close)
            throws ParseException
    {
        try {
            if(debug) report("element_or_text.enter");
            if(!open.name.equalsIgnoreCase(close.name))
                throw new ParseException(
                        String.format("OtherXML: mismatch: <%s> vs </%s>", open.name, close.name));
            DapXML thisxml = createxmlelement(open, map);
            for(DapXML xml : body) {
                thisxml.addElement(xml);
            }
            if(debug) report("element_or_text.exit");
            return thisxml;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    @Override
    DapXML
    xmltext(SaxEvent text)
            throws ParseException
    {
        try {
            if(debug) report("xmltext");
            DapXML txt = createxmltext(text.text);
            return txt;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    @Override
    void
    entererror(XMLAttributeMap attrs)
            throws ParseException
    {
        if(debug) report("entererror");
        SaxEvent xhttpcode = pull(attrs, "httpcode");
        String shttpcode = (xhttpcode == null ? "400" : xhttpcode.value);
        int httpcode = 0;
        try {
            httpcode = Integer.parseInt(shttpcode);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Error Response; illegal http code: " + shttpcode);
        }
        this.errorresponse = new ErrorResponse();
        this.errorresponse.setCode(httpcode);
    }

    @Override
    void
    leaveerror()
            throws ParseException
    {
        if(debug) report("leaveerror");
        assert (this.errorresponse != null) : "Internal Error";
    }

    @Override
    void
    errormessage(String value)
            throws ParseException
    {
        if(debug) report("errormessage");
        assert (this.errorresponse != null) : "Internal Error";
        String message = value;
        message = Escape.entityUnescape(message); // Remove XML encodings
        this.errorresponse.setMessage(message);
    }

    @Override
    void
    errorcontext(String value)
            throws ParseException
    {
        if(debug) report("errorcontext");
        assert (this.errorresponse != null) : "Internal Error";
        String context = value;
        context = Escape.entityUnescape(context); // Remove XML encodings
        this.errorresponse.setContext(context);
    }

    @Override
    void
    errorotherinfo(String value)
            throws ParseException
    {
        if(debug) report("errorotherinfo");
        assert (this.errorresponse != null) : "Internal Error";
        String other = value;
        other = Escape.entityUnescape(other); // Remove XML encodings
        this.errorresponse.setOtherInfo(other);
    }

    @Override
    String
    textstring(String prefix, SaxEvent text)
            throws ParseException
    {
        if(debug) report("text");
        if(prefix == null)
            return text.text;
        else
            return prefix + text.text;
    }

    //////////////////////////////////////////////////
    // Utilities

    void
    addField(DapVariable instance, DapVariable field)
            throws DapException
    {
        DapType t = instance.getBaseType();
        switch (t.getTypeSort()) {
        case Structure:
        case Sequence:
            ((DapStructure) t).addField(field);
            field.setParent(instance);
            break;
        default:
            assert false : "Container cannot be atomic variable";
        }
    }

    void report(String action)
    {
        getDebugStream().println("ACTION: " + action);
        getDebugStream().flush();
    }

    static boolean
    isReserved(String name)
    {
	for(String tag: RESERVEDTAGS) {
	    if(name.startsWith(tag)) return true;
	}
	return false;
    }
}

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import dap4.core.dmr.*;
import dap4.core.util.*;
import org.xml.sax.SAXException;

import java.math.BigInteger;
import java.util.*;

/**
 * Implement the Dap4 Parse Actions
 * Output depends on isdatadmr flag.
 */

public class Dap4Parser extends Dap4ParserBody
{

    //////////////////////////////////////////////////
    // Constants

    static final protected boolean isdatadmr = false; // temporary

    //////////////////////////////////////////////////
    // Instance variables

    protected DapFactory factory = null;

    protected ErrorResponse errorresponse = null;

    protected Deque<DapNode> scopestack = new ArrayDeque<DapNode>();

    protected DapDataset root = null; // of the parse

    protected boolean debug = false;

    //////////////////////////////////////////////////
    // Constructors

    public Dap4Parser(DapFactory factory)
    {
        super();
        this.factory = factory; // see Dap4Actions
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
        DapNode parent = searchScope(DapSort.STRUCTURE, DapSort.SEQUENCE, DapSort.GROUP, DapSort.DATASET);
        if(parent == null) throw new DapException("Undefined parent Scope");
        return parent;
    }

    DapVariable
    getVariableScope()
        throws DapException
    {
        DapNode match = searchScope(DapSort.ATOMICVARIABLE, DapSort.STRUCTURE, DapSort.SEQUENCE);
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
            for(int j = 0;j < sort.length;j++) {
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
        case STRUCTURE:
            var = (DapVariable) ((DapStructure) parent).findByName(name);
            break;
        case SEQUENCE:
            var = (DapVariable) ((DapSequence) parent).findByName(name);
            break;
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
        DapAttribute attr
            = (DapAttribute) newNode(name, sort);
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
            SaxEvent value = pull(description, "value");
            String typename = (atype == null ? "Int32" : atype.value);
            if("Byte".equalsIgnoreCase(typename)) typename = "UInt8";
            DapType basetype = DapType.reify(typename);
            if(basetype != attr.getBaseType())
                throw new ParseException("Attribute: DATA DMR: Attempt to change attribute type: " + typename);
            attr.clearValues();
            if(value != null)
                attr.addValue(value.value);
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
        SaxEvent atype = pull(attrs, "type");
        if(false) { // if enable, then allow <Attribute type="..." value="..."/>
            SaxEvent value = pull(attrs, "value");
        }
        if(isempty(name))
            throw new ParseException("Attribute: Empty attribute name");
        String typename = (atype == null ? "Int32" : atype.value);
        if("Byte".equalsIgnoreCase(typename)) typename = "UInt8";
        DapType basetype = DapType.reify(typename);
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
            for(String v : textlist) {
                parent.addValue(v);
            }
    }

    DapAttribute
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
        DapAttribute other
            = makeAttribute(DapSort.OTHERXML, name.value, null, nslist, parent);
        parent.setAttribute(other);
        return other;
    }

    /**
     * Add slices to the variable view and
     * do additional semantic checks. Only
     * used when isdatadmr = true.
     * <p/>
     * param var to validate
     *
     * @throws DapException
     */
/*
    void
    validateVar(DapVariable var)
        throws DapException
    {
        assert (isdatadmr);
        ViewVariable vv = annotations.get(var);
        assert vv != null : "Internal error";
        if(var.getRank() > 0 && vv.getRank() == 0) // No dimrefs were defined, complain
            throw new ParseException("No dimrefs for variable with rank > 0");
        vv.validate(); // do additional semantic checks
    }
*/
    //////////////////////////////////////////////////
    // Factory wrapper
    @Override
    DapNode newNode(String name, DapSort sort)
        throws ParseException
    {
        DapNode node = (DapNode) factory.newNode(sort);
        node.setDataset(this.root);
        if(name != null) node.setShortName(name);
        return node;
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
        try {
            ndapversion = Float.parseFloat(dapversion.value);
        } catch (NumberFormatException nfe) {
            ndapversion = DAPVERSION;
        }
        if(ndapversion != DAPVERSION)
            throw new ParseException("Dataset dapVersion mismatch: " + dapversion.value);
        float ndmrversion = DAPVERSION;
        try {
            ndmrversion = Float.parseFloat(dmrversion.value);
        } catch (NumberFormatException nfe) {
            ndmrversion = DMRVERSION;
        }
        if(ndmrversion != DMRVERSION)
            throw new ParseException("Dataset dmrVersion mismatch: " + dmrversion.value);
        this.root = (DapDataset) newNode(name.value, DapSort.DATASET);
        this.root.setDapVersion(Float.toString(ndapversion));
        this.root.setDMRVersion(Float.toString(ndmrversion));
        this.root.setDataset(this.root);
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
    entergroup(SaxEvent name)
        throws ParseException
    {
        if(debug) report("entergroup");
        try {
            if(isempty(name))
                throw new ParseException("Empty group name");
            DapGroup parent = getGroupScope();
            DapGroup group;
            if(isdatadmr) {
                assert parent != null : "Internal error";
                // lookup in parent
                group = (DapGroup) parent.findInGroup(name.value, DapSort.GROUP);
                if(group == null)
                    throw new ParseException("Group name does not match template: " + name.value);
            } else {
                group = (DapGroup) newNode(name.value, DapSort.GROUP);
                parent.addDecl(group);
            }
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
                basedaptype = DapEnum.DEFAULTBASETYPE;
            } else {
                String typename = basetype.value;
                if("Byte".equalsIgnoreCase(typename)) typename = "UInt8";
                basedaptype = DapType.reify(typename);
                if(basedaptype == null || !islegalenumtype(basedaptype))
                    throw new ParseException("Enumdef: Invalid Enum Declaration Type name: " + basetype.value);
            }
            DapEnum dapenum = null;
            if(isdatadmr) {
                // Locate the corresponding template parent: group or structure or sequence
                DapNode parent = getParentScope();
                assert parent != null : "Internal error";
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    dapenum = (DapEnum) ((DapGroup) parent).findByName(name.value, DapSort.ENUMERATION);
                    break;
                default:
                    assert false : "Internal Error";
                }
                // Verify consistency
                if(dapenum == null)
                    throw new ParseException("Template enumeration missing: " + name.value);
                if(dapenum.getBaseType() != basedaptype)
                    throw new ParseException("Template enumeration mismatch: " + name.value);
            } else {
                dapenum = (DapEnum) newNode(name.value, DapSort.ENUMERATION);
                dapenum.setBaseType(basedaptype);
                DapGroup parent = getGroupScope();
                parent.addDecl(dapenum);
            }
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
        DapEnum eparent = (DapEnum) scopestack.pop();
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
            DapEnum parent = (DapEnum) getScope(DapSort.ENUMERATION);
            // Verify that the name is a legal enum constant name, which is restricted
            // vis-a-vis other names
            if(!ParseUtil.isLegalEnumConstName(name.value))
                throw new ParseException("Enumconst: illegal enumeration constant name: " + name.value);
            if(isdatadmr) {// verify that the constant is in the enumeration
                long templatevalue = parent.lookup(name.value);
                String templatename = parent.lookup(lvalue);
                if(lvalue != templatevalue || !name.value.equals(templatename))
                    throw new ParseException(String.format("Template enumeration constant mismatch: %s.%s=%d",
                        parent.getShortName(), templatename, templatevalue));
            } else
                parent.addEnumConst(name.value, lvalue);
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
            if(isdatadmr) {
                // Locate the corresponding template parent: group or structure or sequence
                DapNode parent = getParentScope();
                assert parent != null : "Internal error";
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    dim = (DapDimension) ((DapGroup) parent).findByName(name.value, DapSort.DIMENSION);
                    break;
                default:
                    assert false : "Internal Error";
                }
                // Verify consistency
                if(dim == null)
                    throw new ParseException("Template shared dimension missing: " + name.value);
                if(dim.getSize() != lvalue)
                    throw new ParseException("Template dimension size mismatch: " + name.value);
            } else {
                dim = (DapDimension) newNode(name.value, DapSort.DIMENSION);
                dim.setSize(lvalue);
                dim.setShared(true);
                DapGroup parent = getGroupScope();
                parent.addDecl(dim);

            }
            scopestack.push(dim);
        } catch (
            DapException de
            )

        {
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
                if(isdatadmr) {
                    // We need to look in the underlying full dmr to locate the dimension
                    dim = (DapDimension) var.getGroup().findByFQN(nameorsize.value, DapSort.DIMENSION);
                } else {
                    DapGroup grp = var.getGroup();
                    if(grp == null)
                        throw new ParseException("Variable has no group");
                    dim = (DapDimension) grp.findByFQN(nameorsize.value, DapSort.DIMENSION);
                }
            } else {// Size only is given; presume a number; create unique anonymous dimension
                String ssize = nameorsize.value.trim();
                if(ssize.equals("*"))
                    dim = DapDimension.VLEN;
                else {
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
    enteratomicvariable(SaxEvent open, SaxEvent name)
        throws ParseException
    {
        if(debug) report("enteratomicvariable");
        try {
            if(isempty(name))
                throw new ParseException("Atomicvariable: Empty dimension reference name");
            String typename = open.name;
            if("Byte".equals(typename)) typename = "UInt8"; // special case
            DapType basetype = DapType.reify(typename);
            if(basetype == null)
                throw new ParseException("AtomicVariable: Illegal type: " + open.name);
            DapVariable var = null;
            if(isdatadmr) {
                // Locate the corresponding template parent: group or structure or sequence
                DapNode parent = getParentScope();
                assert parent != null : "Internal error";
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    var = ((DapGroup) parent).findVariable(name.value);
                    break;
                case STRUCTURE:
                    var = ((DapStructure) parent).findByName(name.value);
                    break;
                case SEQUENCE:
                    var = ((DapSequence) parent).findByName(name.value);
                    break;
                default:
                    assert false : "Internal Error";
                }
                // Verify consistency
                if(var.getSort() != DapSort.ATOMICVARIABLE
                    || var.getBaseType() != var.getBaseType())
                    throw new ParseException("Template variable mismatch: " + name.value);
            } else { //!isdatadmr
                // Do type substitutions
                var
                    = (DapVariable) newNode(name.value, DapSort.ATOMICVARIABLE);
                var.setBaseType(basetype);
                // Look at the parent scope
                DapNode parent = scopestack.peek();
                if(parent == null)
                    throw new ParseException("Variable has no parent");
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    ((DapGroup) parent).addDecl(var);
                    break;
                case STRUCTURE:
                    ((DapStructure) parent).addField(var);
                    break;
                case SEQUENCE:
                    ((DapSequence) parent).addField(var);
                    break;
                default:
                    assert false : "Atomic variable in illegal scope";
                }
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
        AtomicType atype = AtomicType.getAtomicType(typename);
        DapVariable var = (DapVariable) searchScope(sort);
        assert var != null;
        AtomicType vartype = var.getBaseType().getAtomicType();
        if(atype == null)
            throw new ParseException("Variable: Illegal type: " + typename);
        if(atype != vartype)
            throw new ParseException(String.format("variable: open/close type mismatch: <%s> </%s>",
                vartype, atype));
    }

    void leavevariable()
        throws ParseException
    {
        //DapVariable var = getVariableScope();
        //if(isdatadmr)
        //    validateVar(var);
        scopestack.pop();
    }

    void
    leaveatomicvariable(SaxEvent close)
        throws ParseException
    {
        openclosematch(close, DapSort.ATOMICVARIABLE);
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
            DapEnum target = (DapEnum) root.findByFQN(enumtype.value, DapSort.ENUMERATION);
            if(target == null)
                throw new ParseException("EnumVariable: no such enum: " + name.value);
            DapVariable var = null;
            if(isdatadmr) {
                // Locate the corresponding template parent: group or structure or sequence
                DapNode parent = getParentScope();
                assert parent != null : "Internal error";
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    var = ((DapGroup) parent).findVariable(name.value);
                    break;
                case STRUCTURE:
                    var = ((DapStructure) parent).findByName(name.value);
                    break;
                case SEQUENCE:
                    var = ((DapSequence) parent).findByName(name.value);
                    break;
                default:
                    assert false : "Internal Error";
                }
                assert var != null : "Internal error";
                if(var.getBaseType() != target)
                    throw new ParseException("Template mismatch: " + name.value);
            } else {
                var = (DapVariable) newNode(name.value, DapSort.ATOMICVARIABLE);
                var.setBaseType(target);
                // Look at the parent scope
                DapNode parent = scopestack.peek();
                if(parent == null)
                    throw new ParseException("Variable has no parent");
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    ((DapGroup) parent).addDecl(var);
                    break;
                case STRUCTURE:
                    ((DapStructure) parent).addField(var);
                    break;
                case SEQUENCE:
                    ((DapSequence) parent).addField(var);
                    break;
                default:
                    assert false : "Atomic variable in illegal scope";
                }
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
        openclosematch(close, DapSort.ATOMICVARIABLE);
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
        DapAtomicVariable var;
        try {
            var = (DapAtomicVariable) root.findByFQN(name.value, DapSort.ATOMICVARIABLE);
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
        DapMap map = (DapMap) newNode(DapSort.MAP);
        map.setVariable(var);
        try {
            // Pull the top variable scope
            DapVariable parent = (DapVariable) searchScope(DapSort.ATOMICVARIABLE, DapSort.STRUCTURE, DapSort.SEQUENCE);
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
    enterstructurevariable(SaxEvent name)
        throws ParseException
    {
        if(debug) report("enterstructurevariable");
        if(isempty(name))
            throw new ParseException("Structure: Empty structure name");
        try {
            DapStructure var = null;
            if(isdatadmr) {
                // Locate the corresponding template parent: group or structureor sequence
                DapNode parent = getParentScope();
                assert parent != null : "Internal error";
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    var = (DapStructure) ((DapGroup) parent).findVariable(name.value);
                    break;
                case STRUCTURE:
                case SEQUENCE:
                    var = (DapStructure) ((DapStructure) parent).findByName(name.value);
                    break;
                default:
                    assert false : "Internal Error";
                }
                assert var != null : "Internal error";

                // Verify consistency
                if(var.getSort() != DapSort.STRUCTURE && var.getSort() != DapSort.SEQUENCE)
                    throw new ParseException("Template variable mismatch: " + name.value);
            } else {
                var = (DapStructure) newNode(name.value, DapSort.STRUCTURE);
                var.setBaseType(DapType.STRUCT);
                // Look at the parent scope
                DapNode parent = scopestack.peek();
                if(parent == null)
                    throw new ParseException("Variable has no parent");
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    ((DapGroup) parent).addDecl(var);
                    break;
                case STRUCTURE:
                case SEQUENCE:
                    ((DapStructure) parent).addField(var);
                    break;
                default:
                    assert false : "Structure variable in illegal scope";
                }
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
        openclosematch(close, DapSort.STRUCTURE);
        leavevariable();
    }

    @Override
    void
    entersequencevariable(SaxEvent name)
        throws ParseException
    {
        if(debug) report("entersequencevariable");
        if(isempty(name))
            throw new ParseException("Sequence: Empty sequence name");
        try {
            DapVariable var = null;
            if(isdatadmr) {
                // Locate the corresponding template parent: group or structure or sequence
                DapNode parent = getParentScope();
                assert parent != null : "Internal error";
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    var = (DapStructure) ((DapGroup) parent).findVariable(name.value);
                    break;
                case STRUCTURE:
                    var = (DapStructure) ((DapStructure) parent).findByName(name.value);
                    break;
                case SEQUENCE:
                    var = (DapSequence) ((DapSequence) parent).findByName(name.value);
                    break;
                default:
                    assert false : "Internal Error";
                }
                assert var != null : "Internal error";

                // Verify consistency
                if(var.getSort() != DapSort.SEQUENCE)
                    throw new ParseException("Template variable mismatch: " + name.value);
            } else {
                var = (DapSequence) newNode(name.value, DapSort.SEQUENCE);
                var.setBaseType(DapType.SEQ);
                // Look at the parent scope
                DapNode parent = scopestack.peek();
                if(parent == null)
                    throw new ParseException("Variable has no parent");
                switch (parent.getSort()) {
                case DATASET:
                case GROUP:
                    ((DapGroup) parent).addDecl(var);
                    break;
                case STRUCTURE:
                    ((DapStructure) parent).addField(var);
                    break;
                case SEQUENCE:
                    ((DapSequence) parent).addField(var);
                    break;
                default:
                    assert false : "Structure variable in illegal scope";
                }
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
        openclosematch(close, DapSort.SEQUENCE);
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
            if(isdatadmr) {
                // The datadmr is allowed to change or add attribute values
                // to the underlying DMR if it is a variable or an attribute
                SaxEvent name = peek(attrs, "name");
                attr = parent.findAttribute(name.value);
                if(attr == null)
                    attr = createatomicattribute(attrs, nslist, parent);
                else
                    changeAttribute(attr, attrs);
            } else {
                attr = createatomicattribute(attrs, nslist, parent);
            }
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
        if(attr.getValues().size() == 0)
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
            if(isdatadmr) {
                // The datadmr is allowed to change or add attribute values
                // to the underlying DMR if it is a variable or an attribute
                SaxEvent name = peek(attrs, "name");
                attr = parent.findAttribute(name.value);
                if(attr == null)
                    attr = createcontainerattribute(attrs, nslist, parent);
                else
                    changeAttribute(attr, attrs);
            } else {
                attr = createcontainerattribute(attrs, nslist, parent);
            }
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
    enterotherxml(XMLAttributeMap attrs)
        throws ParseException
    {
        if(debug) report("enterotherxml");
        try {
            DapNode parent = getMetadataScope();
            DapAttribute other = createotherxml(attrs, parent);
            parent.setAttribute(other);
            scopestack.push(other);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leaveotherxml()
        throws ParseException
    {
        if(debug) report("leaveotherxml");
        scopestack.pop();
    }

    @Override
    void
    enterxmlelement(SaxEvent open, XMLAttributeMap map)
        throws ParseException
    {
        if(debug) report("enterxmlelement");
        try {
            DapNode parent = scopestack.peek();
            DapXML xml = createxmlelement(open, map, parent);
            scopestack.push(xml);
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    @Override
    void
    leavexmlelement(SaxEvent close)
        throws ParseException
    {
        if(debug) report("leavexmlelement");
        try {
            DapXML open = (DapXML) getScope(DapSort.XML);
            if(!open.getShortName().equals(close.name))
                throw new ParseException(String.format("otherxml: open/close name mismatch: <%s> </%s>",
                    open.getShortName(), close.name));
            scopestack.pop();
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    void
    xmltext(SaxEvent text)
        throws ParseException
    {
        if(debug) report("xmltext");
        try {
            DapXML txt = createxmltext(text.text);
            DapXML parent = (DapXML) getScope(DapSort.XML);
            parent.addElement(txt);
        } catch (DapException de) {
            throw new ParseException(de);
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
    errormessage(SaxEvent value)
        throws ParseException
    {
        if(debug) report("errormessage");
        assert (this.errorresponse != null) : "Internal Error";
        assert (value.eventtype == SaxEventType.CHARACTERS) : "Internal error";
        String message = value.text;
        message = Escape.entityUnescape(message); // Remove XML encodings
        this.errorresponse.setMessage(message);
    }

    @Override
    void
    errorcontext(SaxEvent value)
        throws ParseException
    {
        if(debug) report("errorcontext");
        assert (this.errorresponse != null) : "Internal Error";
        assert (value.eventtype == SaxEventType.CHARACTERS) : "Internal error";
        String context = value.text;
        context = Escape.entityUnescape(context); // Remove XML encodings
        this.errorresponse.setContext(context);
    }

    @Override
    void
    errorotherinfo(SaxEvent value)
        throws ParseException
    {
        if(debug) report("errorotherinfo");
        assert (this.errorresponse != null) : "Internal Error";
        assert (value.eventtype == SaxEventType.CHARACTERS) : "Internal error";
        String other = value.text;
        other = Escape.entityUnescape(other); // Remove XML encodings
        this.errorresponse.setOtherInfo(other);
    }

    //////////////////////////////////////////////////
    // Utilities

    void report(String action)
    {
        getDebugStream().println("ACTION: " + action);
        getDebugStream().flush();
    }
}

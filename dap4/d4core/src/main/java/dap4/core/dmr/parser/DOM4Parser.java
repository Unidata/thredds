/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;
import dap4.core.util.DapUtil;
import dap4.core.util.Escape;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.*;

/**
 * Implement the Dap4 Parser Using a DOM Parser
 */

public class DOM4Parser implements Dap4Parser
{

    //////////////////////////////////////////////////
    // Constants

    static final float DAPVERSION = 4.0f;
    static final float DMRVERSION = 1.0f;

    static final String DEFAULTATTRTYPE = "Int32";

    static final int RULENULL = 0;
    static final int RULEDIMREF = 1;
    static final int RULEMAPREF = 2;
    static final int RULEVAR = 3;
    static final int RULEMETADATA = 4;


    static final String[] RESERVEDTAGS = new String[]{
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

    static final Map<String, DapSort> sortmap;
    static final Map<String, TypeSort> typemap;

    static {
        sortmap = new HashMap<String, DapSort>();
        sortmap.put("attribute", DapSort.ATTRIBUTE);
        sortmap.put("dataset", DapSort.DATASET);
        sortmap.put("dim", DapSort.DIMENSION);
        sortmap.put("dimension", DapSort.DIMENSION);
        sortmap.put("enumeration", DapSort.ENUMERATION);
        sortmap.put("enumconst", DapSort.ENUMCONST);
        sortmap.put("group", DapSort.GROUP);
        sortmap.put("map", DapSort.MAP);
        sortmap.put("otherxml", DapSort.OTHERXML);
        sortmap.put("sequence", DapSort.SEQUENCE);
        sortmap.put("structure", DapSort.STRUCTURE);
        sortmap.put("char", DapSort.VARIABLE);
        sortmap.put("int8", DapSort.VARIABLE);
        sortmap.put("uint8", DapSort.VARIABLE);
        sortmap.put("int16", DapSort.VARIABLE);
        sortmap.put("uint16", DapSort.VARIABLE);
        sortmap.put("int32", DapSort.VARIABLE);
        sortmap.put("uint32", DapSort.VARIABLE);
        sortmap.put("int64", DapSort.VARIABLE);
        sortmap.put("uint64", DapSort.VARIABLE);
        sortmap.put("float32", DapSort.VARIABLE);
        sortmap.put("float64", DapSort.VARIABLE);
        sortmap.put("string", DapSort.VARIABLE);
        sortmap.put("url", DapSort.VARIABLE);
        sortmap.put("opaque", DapSort.VARIABLE);
        sortmap.put("enum", DapSort.VARIABLE);

        typemap = new HashMap<String, TypeSort>();
        //se lower cas enames
        typemap.put("char", TypeSort.Char);
        typemap.put("int8", TypeSort.Int8);
        typemap.put("uint8", TypeSort.UInt8);
        typemap.put("int16", TypeSort.Int16);
        typemap.put("uint16", TypeSort.UInt16);
        typemap.put("int32", TypeSort.Int32);
        typemap.put("uint32", TypeSort.UInt32);
        typemap.put("int64", TypeSort.Int64);
        typemap.put("uint64", TypeSort.UInt64);
        typemap.put("float32", TypeSort.Float32);
        typemap.put("float64", TypeSort.Float64);
        typemap.put("string", TypeSort.String);
        typemap.put("url", TypeSort.URL);
        typemap.put("opaque", TypeSort.Opaque);
        typemap.put("enum", TypeSort.Enum);
        typemap.put("structure", TypeSort.Structure);
        typemap.put("sequence", TypeSort.Sequence);


    }

    //////////////////////////////////////////////////
    // static variables

    static protected int globaldebuglevel = 0;
    static protected java.io.PrintStream debugstream = System.err;

    //////////////////////////////////////////////////
    // Static methods

    static public void setGlobalDebugLevel(int level)
    {
        globaldebuglevel = level;
    }

    public void setDebugStream(java.io.PrintStream stream)
    {
        if(stream != null)
            debugstream = stream;
    }

    static DapSort
    nodesort(Node n)
    {
        String elem = n.getNodeName();
        DapSort sort = sortmap.get(elem.toLowerCase());
        return sort;
    }

    static TypeSort
    nodetypesort(Node n)
    {
        String elem = n.getNodeName();
        TypeSort sort = typemap.get(elem.toLowerCase());
        return sort;
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected DMRFactory factory = null;
    protected ErrorResponse errorresponse = null;
    protected Deque<DapNode> scopestack = new ArrayDeque<>();
    protected DapDataset root = null; // of the parse
    protected boolean trace = false;
    protected boolean debug = false;

    //////////////////////////////////////////////////
    // Parser State

    // Need to be able to map a DOM <Group> node to DapNode 
    protected Map<Node, DapGroup> groupmap = new HashMap<>();

    //////////////////////////////////////////////////
    // Constructors

    public DOM4Parser(DMRFactory factory)
    {
        super();
        this.factory = (factory == null ? new DMRFactory() : factory);
        if(globaldebuglevel > 0) setDebugLevel(globaldebuglevel);
    }

    //////////////////////////////////////////////////
    // Accessors

    public void
    setDebugLevel(int level)
    {
        setGlobalDebugLevel(level);
    }


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
        try {
            DocumentBuilderFactory domfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dombuilder = domfactory.newDocumentBuilder();
            StringReader rdr = new StringReader(input);
            InputSource src = new InputSource(rdr);
            Document doc = dombuilder.parse(src);
            doc.getDocumentElement().normalize();
            rdr.close();
            parseresponse(doc.getDocumentElement());
            return true;
        } catch (ParserConfigurationException | IOException e) {
            throw new SAXException(e);
        }
    }

    //////////////////////////////////////////////////
    // Parser specific methods

    DapGroup
    getGroupScope()
            throws ParseException
    {
        DapGroup gscope = (DapGroup) searchScope(DapSort.GROUP, DapSort.DATASET);
        if(gscope == null) throw new ParseException("Undefined Group Scope");
        return gscope;
    }

    DapNode
    getMetadataScope()
            throws ParseException
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
        DapNode parent = searchScope(DapSort.STRUCTURE, DapSort.SEQUENCE, DapSort.GROUP, DapSort.DATASET, DapSort.ENUMERATION);
        if(parent == null) throw new DapException("Undefined parent Scope");
        return parent;
    }

    DapVariable
    getVariableScope()
            throws DapException
    {
        DapNode match = searchScope(DapSort.VARIABLE, DapSort.STRUCTURE, DapSort.SEQUENCE);
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

    // XML Attribute utilities
    protected String
    pull(Node n, String name)
    {
        NamedNodeMap map = n.getAttributes();
        Node attr = map.getNamedItem(name);
        if(attr == null)
            return null;
        return attr.getNodeValue();
    }

    //////////////////////////////////////////////////
    // Attribute construction

    DapAttribute
    makeAttribute(DapSort sort, String name, DapType basetype, List<String> nslist)
            throws ParseException
    {
        DapAttribute attr = factory.newAttribute(name, basetype);
        if(sort == DapSort.ATTRIBUTE) {
            attr.setBaseType(basetype);
        }
        attr.setNamespaceList(nslist);
        return attr;
    }

    boolean isempty(String text)
    {
        return (text == null || text.length() == 0);
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

    //////////////////////////////////////////////////
    // Utilities

    protected void trace(String action)
    {
        if(!trace) return;
        debugstream.println("ACTION: " + action);
        debugstream.flush();
    }

    /**
     * Return the subnodes of a node with non-element nodes suppressed
     *
     * @param parent
     * @return list of element children
     */
    List<Node>
    getSubnodes(Node parent)
    {
        List<Node> subs = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for(int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE)
                subs.add(n);
        }
        return subs;
    }

    /**
     * Return all the text of a node else null
     *
     * @param n
     * @return text string | null
     */
    String
    getNodeText(Node n)
    {
        return n.getTextContent();
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

    protected void
    addField(DapVariable instance, DapVariable field)
            throws DapException
    {
        DapType t = instance.getBaseType();
        addField(t, field);
    }

    protected void
    addField(DapType t, DapVariable field)
            throws DapException
    {
        switch (t.getTypeSort()) {
        case Structure:
        case Sequence:
            ((DapStructure) t).addField(field);
            field.setParent(t);
            break;
        default:
            assert false : "Container must be struct or seq";
        }
    }

    protected void
    recorddecl(DapNode n, DapGroup parent)
            throws ParseException
    {
        try {
            parent.addDecl(n);
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected void
    recordfield(DapVariable var, DapStructure parent)
            throws ParseException
    {
        try {
            addField(parent, var);
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }


    protected void
    recordattr(DapAttribute attr, DapNode parent)
            throws ParseException
    {
        try {
            parent.addAttribute(attr);
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    //////////////////////////////////////////////////
    // Recursive descent parser

    protected void
    parseresponse(Node root)
            throws ParseException
    {
        String elemname = root.getNodeName();
        if(elemname.equalsIgnoreCase("Error")) {
            parseerror(root);
        } else if(elemname.equalsIgnoreCase("Dataset")) {
            parsedataset(root);
        } else
            throw new ParseException("Unexpected response root: " + elemname);
    }

    protected void
    parsedataset(Node rootnode)
            throws ParseException
    {
        if(trace) trace("dataset.enter");
        String name = pull(rootnode, "name");
        String dapversion = pull(rootnode, "dapVersion");
        if(dapversion == null) dapversion = pull(rootnode, "dapversion");
        String dmrversion = pull(rootnode, "dmrVersion");
        if(dmrversion == null) dmrversion = pull(rootnode, "dmrversion");
        if(isempty(name))
            throw new ParseException("Empty dataset name attribute");
        // convert and test version numbers
        float ndapversion = DAPVERSION;
        if(dapversion != null)
            try {
                ndapversion = Float.parseFloat(dapversion);
            } catch (NumberFormatException nfe) {
                ndapversion = DAPVERSION;
            }
        if(ndapversion != DAPVERSION)
            throw new ParseException("Dataset dapVersion mismatch: " + dapversion);
        float ndmrversion = DAPVERSION;
        if(dmrversion != null)
            try {
                ndmrversion = Float.parseFloat(dmrversion);
            } catch (NumberFormatException nfe) {
                ndmrversion = DMRVERSION;
            }
        if(ndmrversion != DMRVERSION)
            throw new ParseException("Dataset dmrVersion mismatch: " + dmrversion);
        this.root = factory.newDataset(name);
        this.root.setDapVersion(Float.toString(ndapversion));
        this.root.setDMRVersion(Float.toString(ndmrversion));
        this.root.setDataset(this.root);
        passReserved(rootnode, this.root);
        scopestack.push(this.root);
        // We need to first process (recursively) all
        // definitional nodes: subgroups, enumdefs, and dimensions.
        fillgroupdefs(rootnode, this.root);
        // Second, we need to recursively define all variables
        fillgroupvars(rootnode, this.root);
        if(trace) trace("dataset.exit");
        assert (scopestack.peek() != null && scopestack.peek().getSort() == DapSort.DATASET);
        // finalize
        this.root.sort();
        scopestack.pop();
        if(!scopestack.isEmpty())
            throw new ParseException("Dataset: nested dataset");
        this.root.finish();
    }

    protected void
    fillgroupdefs(Node domgroup, DapGroup group)
            throws ParseException
    {
        try {
            if(trace) trace("fillgroupdefs.enter");
            List<Node> nodes = getSubnodes(domgroup);
            for(int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                String name = pull(n, "name");
                if(isempty(name))
                    throw new ParseException("Fillgroup: Empty node name");
                DapSort sort = nodesort(n);
                switch (sort) {
                case ENUMERATION:
                    recorddecl(parseenumdef(n), group);
                    break;
                case DIMENSION:
                    recorddecl(parsedimdef(n), group);
                    break;
                case GROUP:
                    DapGroup g = parsegroupdefs(n);
                    recorddecl(g, group);
                    groupmap.put(n, g);
                    break;
                default:
                    break; // ignore
                }
            }
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected void
    fillgroupvars(Node domgroup, DapGroup group)
            throws ParseException
    {
        try {
            if(trace) trace("fillgroupvars.enter");
            List<Node> nodes = getSubnodes(domgroup);
            for(int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                String name = pull(n, "name");
                if(isempty(name))
                    throw new ParseException("Fillgroup: Empty node name");
                DapSort sort = nodesort(n);
                switch (sort) {
                case GROUP:
                    // Get the DapGroup (should have been defined
                    // by parsegroupdefs)
                    DapGroup ngroup = groupmap.get(n);
                    assert (ngroup != null);
                    parsegroupvars(n, ngroup);
                    break;
                case VARIABLE:
                    TypeSort type = nodetypesort(n);
                    if(type.isEnumType())
                        recorddecl(parseenumvar(n), group);
                    else if(type.isAtomic())
                        recorddecl(parseatomicvar(n), group);
                    else
                        throw new ParseException("Illegal variable type: " + name);
                    break;
                case STRUCTURE:
                    DapVariable v = parsestructvar(n);
                    // Track variable and its type
                    recorddecl(v.getBaseType(), group);
                    recorddecl(v, group);
                    break;
                case SEQUENCE:
                    v = parseseqvar(n);
                    // Track variable and its type
                    recorddecl(v.getBaseType(), group);
                    recorddecl(v, group);
                    break;
                case ATTRIBUTESET:
                    recordattr(parseattrset(n), group);
                    break;
                case ATTRIBUTE:
                    recordattr(parseattr(n), group);
                    break;
                case OTHERXML:
                    recordattr(parseotherxml(n), group);
                    break;
                case ENUMERATION:
                case DIMENSION:
                    break; // ignore
                default:
                    throw new ParseException("Unexpected element: " + n);
                }
            }
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected DapGroup
    parsegroupdefs(Node node)
            throws DapException
    {
        if(trace) trace("groupdefs.enter");
        String name = pull(node, "name");
        if(isempty(name))
            throw new ParseException("Empty group name");
        try {
            DapGroup g = factory.newGroup(name);
            passReserved(node, g);
            scopestack.push(g);
            fillgroupdefs(node, g);
            if(trace) trace("group.exit");
            scopestack.pop();
            return g;
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    protected DapEnumeration
    parseenumdef(Node node)
            throws ParseException
    {
        try {
            if(trace) trace("enumdef.enter");
            String name = pull(node, "name");
            if(isempty(name))
                throw new ParseException("Enumdef: Empty Enum Declaration name");
            String typename = pull(node, "basetype");
            DapType basedaptype = null;
            if(typename == null) {
                basedaptype = DapEnumeration.DEFAULTBASETYPE;
            } else {
                if("Byte".equalsIgnoreCase(typename)) typename = "UInt8";
                basedaptype = (DapType) this.root.lookup(typename, DapSort.ATOMICTYPE);
                if(basedaptype == null || !islegalenumtype(basedaptype))
                    throw new ParseException("Enumdef: Invalid Enum Declaration Type name: " + typename);
            }
            DapEnumeration dapenum = factory.newEnumeration(name, basedaptype);
            scopestack.push(dapenum);
            List<DapEnumConst> econsts = parseenumconsts(node);
            if(econsts.size() == 0)
                throw new ParseException("Enumdef: no enum constants specified");
            DapEnumeration eparent = (DapEnumeration) scopestack.pop();
            eparent.setEnumConsts(econsts);
            if(trace) trace("enumdef.exit");
            return dapenum;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected List<DapEnumConst>
    parseenumconsts(Node enumdef)
            throws ParseException
    {
        if(trace) trace("enumconsts.enter");
        List<DapEnumConst> econsts = new ArrayList<>();
        List<Node> nodes = getSubnodes(enumdef);
        for(int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            DapEnumConst dec = parseenumconst(n);
            econsts.add(dec);
        }
        if(trace) trace("enumconsts.exit");
        return econsts;
    }

    protected DapEnumConst
    parseenumconst(Node node)
            throws ParseException
    {
        try {
            if(trace) trace("enumconst.enter");
            String name = pull(node, "name");
            String value = pull(node, "value");
            if(isempty(name))
                throw new ParseException("Enumconst: Empty enum constant name");
            if(isempty(value))
                throw new ParseException("Enumdef: Invalid enum constant value: " + value);
            long lvalue = 0;
            try {
                BigInteger bivalue = new BigInteger(value);
                bivalue = DapUtil.BIG_UMASK64.and(bivalue);
                lvalue = bivalue.longValue();
            } catch (NumberFormatException nfe) {
                throw new ParseException("Enumconst: illegal value: " + value);
            }
            DapEnumeration parent = (DapEnumeration) getScope(DapSort.ENUMERATION);
            // Verify that the name is a legal enum constant name, which is restricted
            // vis-a-vis other names
            if(!ParseUtil.isLegalEnumConstName(name))
                throw new ParseException("Enumconst: illegal enumeration constant name: " + name);
            DapEnumConst dec = new DapEnumConst(name, lvalue);
            return dec;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected DapGroup
    parsegroupvars(Node node, DapGroup group)
            throws DapException
    {
        if(trace) trace("groupvars.enter");
        String name = pull(node, "name");
        try {
            scopestack.push(group);
            fillgroupvars(node, group);
            if(trace) trace("groupvars.exit");
            scopestack.pop();
            return group;
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    protected DapDimension
    parsedimdef(Node node)
            throws ParseException
    {
        if(trace) trace("dimdef.enter");
        String name = pull(node, "name");
        String size = pull(node, "size");
        long lvalue = 0;
        if(isempty(name))
            throw new ParseException("Dimdef: Empty dimension declaration name");
        if(isempty(size))
            throw new ParseException("Dimdef: Empty dimension declaration size");
        try {
            lvalue = Long.parseLong(size);
            if(lvalue <= 0)
                throw new ParseException("Dimdef: value <= 0: " + lvalue);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Dimdef: non-integer value: " + size);
        }
        DapDimension dim = factory.newDimension(name, lvalue);
        dim.setShared(true);
        if(trace) trace("dimdef.exit");
        return dim;
    }

    protected DapVariable
    parseenumvar(Node node)
            throws ParseException
    {
        if(trace) trace("enumvar.enter");
        String typename = node.getNodeName();
        assert ("enum".equalsIgnoreCase(typename));
        try {
            String enumfqn = pull(node, "enum");
            if(isempty(enumfqn))
                throw new ParseException("Enumvariable: Empty enum type name");
            DapType basetype = (DapEnumeration) root.findByFQN(enumfqn, DapSort.ENUMERATION);
            if(basetype == null)
                throw new ParseException("EnumVariable: no such enum: " + enumfqn);
            String name = pull(node, "name");
            if(isempty(name))
                throw new ParseException("Enumvar: Empty variable name");
            DapVariable var = factory.newVariable(name, basetype);
            passReserved(node, var);
            scopestack.push(var);
            fillmetadata(node, var);
            scopestack.pop();
            return var;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected DapVariable
    parseatomicvar(Node node)
            throws ParseException
    {
        if(trace) trace("atomicvariable.enter");
        String name = pull(node, "name");
        if(isempty(name))
            throw new ParseException("Atomicvariable: Empty variable name");
        String typename = node.getNodeName();
        if("Byte".equals(typename)) typename = "UInt8"; // special case
        try {
            DapType basetype = (DapType) this.root.lookup(typename, DapSort.ATOMICTYPE);
            if(basetype == null)
                throw new ParseException("AtomicVariable: Illegal type: " + typename);
            DapVariable var = factory.newVariable(name, basetype);
            passReserved(node, var);
            scopestack.push(var);
            fillmetadata(node, var);
            scopestack.pop();
            return var;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected DapVariable
    parsestructvar(Node node)
            throws ParseException
    {
        if(trace) trace("structvariable.enter");
        String name = pull(node, "name");
        if(isempty(name))
            throw new ParseException("Structvar: Empty variable name");
        String typename = node.getNodeName();
        try {
            DapStructure type = factory.newStructure(name);
            DapVariable var = factory.newVariable(name, type);
            passReserved(node, var);
            scopestack.push(type);
            fillcontainer(node, type);
            scopestack.pop();
            scopestack.push(var);
            fillmetadata(node, var);
            scopestack.pop();
            return var;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected DapVariable
    parseseqvar(Node node)
            throws ParseException
    {
        if(trace) trace("seqvariable.enter");
        String name = pull(node, "name");
        if(isempty(name))
            throw new ParseException("Seqvar: Empty variable name");
        String typename = node.getNodeName();
        try {
            DapSequence type = factory.newSequence(name);
            DapVariable var = factory.newVariable(name, type);
            passReserved(node, var);
            scopestack.push(type);
            fillcontainer(node, type);
            scopestack.pop();
            scopestack.push(var);
            fillmetadata(node, var);
            scopestack.pop();
            return var;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected void
    fillcontainer(Node node, DapStructure parent)
            throws ParseException
    {
        try {
            if(trace) trace("fillcontainer.enter");
            List<Node> nodes = getSubnodes(node);
            // Fields first
            for(int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                DapSort sort = nodesort(n);
                switch (sort) {
                case VARIABLE:
                    recordfield(parseatomicvar(n), parent);
                    break;
                case STRUCTURE:
                    DapVariable dv = parsestructvar(n);
                    // Track variable and its type
                    recorddecl(dv.getBaseType(), getGroupScope());
                    // In order to get fqns correct, we need to re-parent
                    // the type as parent
                    dv.getBaseType().overrideParent(parent);
                    recordfield(dv, parent);
                    break;
                case SEQUENCE:
                    dv = parseseqvar(n);
                    // Track variable and its type
                    recorddecl(dv.getBaseType(), getGroupScope());
                    recordfield(dv, parent);
                    break;
                default:
                    break; // ignore for now
                }
            }
            if(trace) trace("fillcontainer.exit");
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected void
    fillmetadata(Node node, DapVariable var)
            throws ParseException
    {
        try {
            if(trace) trace("fillmetadata.enter");
            List<Node> nodes = getSubnodes(node);
            for(int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                DapSort sort = nodesort(n);
                switch (sort) {
                case DIMENSION:
                    var.addDimension(parsedimref(n));
                    break;
                case MAP:
                    var.addMap(parsemap(n));
                    break;
                case ATTRIBUTE:
                    recordattr(parseattr(n), var);
                    break;
                case ATTRIBUTESET:
                    recordattr(parseattrset(n), var);
                    break;
                case OTHERXML:
                    recordattr(parseotherxml(n), var);
                    break;
                default:
                    break; // ignore for now
                }
            }
            if(trace) trace("fillmetadata.exit");
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected DapDimension
    parsedimref(Node node)
            throws ParseException
    {
        try {
            if(trace) trace("dimref.enter");
            String dimname = pull(node, "name");
            String size = pull(node, "size");
            DapDimension dim;
            if(dimname != null && size != null)
                throw new ParseException("Dimref: both name and size specified");
            if(dimname == null && size == null)
                throw new ParseException("Dimref: no name or size specified");
            if(dimname != null && isempty(dimname))
                throw new ParseException("Dimref: Empty dimension reference name");
            else if(size != null && isempty(size))
                throw new ParseException("Dimref: Empty dimension size");
            if(dimname != null) {
                dim = (DapDimension) this.root.findByFQN(dimname, DapSort.DIMENSION);
            } else {// size != null; presume a number; create unique anonymous dimension
                size = size.trim();
                long anonsize;
                try {
                    anonsize = Long.parseLong(size.trim());
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Dimref: Illegal dimension size");
                }
                // Create in root group
                dim = this.root.createAnonymous(anonsize);
            }
            if(dim == null)
                throw new ParseException("Unknown dimension: " + dimname);
            return dim;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected DapMap
    parsemap(Node node)
            throws ParseException
    {
        if(trace) trace("map.enter");
        String name = pull(node, "name");
        if(isempty(name))
            throw new ParseException("Mapref: Empty map name");
        DapVariable target;
        try {
            target = (DapVariable) this.root.findByFQN(name, DapSort.VARIABLE, DapSort.SEQUENCE, DapSort.STRUCTURE);
        } catch (DapException de) {
            throw new ParseException(de);
        }
        if(target == null)
            throw new ParseException("Mapref: undefined target variable: " + name);
        // Verify that this is a legal map =>
        // 1. it is outside the scope of its parent if the parent
        //    is a structure.
        DapNode container = target.getContainer();
        DapNode scope;
        try {
            scope = getParentScope();
        } catch (DapException de) {
            throw new ParseException(de);
        }
        if((container.getSort() == DapSort.STRUCTURE || container.getSort() == DapSort.SEQUENCE)
                && container == scope)
            throw new ParseException("Mapref: map target variable not in outer scope: " + name);
        DapMap map = factory.newMap(target);
        if(trace) trace("map.exit");
        return map;
    }

    protected DapAttribute
    parseattr(Node node)
            throws ParseException
    {
       try {
           if(trace) trace("attribute.enter");
           String name = pull(node, "name");
           if(isempty(name))
               throw new ParseException("Attribute: Empty attribute name");
           String type = pull(node, "type");
           if(isempty(type))
               type = DEFAULTATTRTYPE;
           else if("Byte".equalsIgnoreCase(type)) type = "UInt8";
           // Convert type to basetype
           DapType basetype = (DapType) this.root.lookup(type,
                   DapSort.ENUMERATION, DapSort.ATOMICTYPE);
           if(basetype == null)
               throw new ParseException("parseattr: Illegal type: " + type);
           List<String> nslist = parsenamespaces(node);
           DapAttribute attr = makeAttribute(DapSort.ATTRIBUTE, name, basetype, nslist);
           scopestack.push(attr);
           List<String> values = new ArrayList<String>();
           // See first if we have a "value" xml attribute
           String val = pull(node, "value");
           if(val != null) {
               values.add(val);
           } else {  // Look for <value> subnodes
               if(node.hasChildNodes()) {
                   List<Node> nodes = getSubnodes(node);
                   for(int i = 0; i < nodes.size(); i++) {
                       Node n = nodes.get(i);
                       String kind = n.getNodeName();
                       if(kind.equalsIgnoreCase("Value")) {
                           // two case: <value value="..."/>
                           // or <value>...</value>
                           val = pull(n, "value");
                           if(val != null)
                               values.add(val);
                           else
                               values.add(getNodeText(n));
                       } else
                           throw new ParseException("Unexpected non-value element in attribute");
                   }
               } else {
                   values.add(cleanup(node.getTextContent()));
               }
           }
           if(values.size() == 0)
               throw new ParseException("Attribute: attribute has no values");
           // Need to normalize the values
           for(int i = 0; i < values.size(); i++) {
               String s = values.get(i);
               if(s == null) s = "";
               String ds = Escape.backslashUnescape(s);
               values.set(i, ds);
           }
           attr.setValues(values.toArray(new String[values.size()]));
           scopestack.pop();
           if(trace) trace("attribute.exit");
           return attr;
       } catch (DapException e) {
           throw new ParseException(e);
       }
    }

    protected DapAttributeSet
    parseattrset(Node node)
            throws ParseException
    {
        try {
            if(trace) trace("attrset.enter");
            String name = pull(node, "name");
            if(isempty(name))
                throw new ParseException("AttributeSet: Empty attribute name");
            List<String> nslist = parsenamespaces(node);
            DapAttributeSet attrset = (DapAttributeSet) makeAttribute(DapSort.ATTRIBUTESET, name, null, nslist);
            scopestack.push(attrset);
            List<Node> nodes = getSubnodes(node);
            for(int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                DapSort sort = nodesort(n);
                switch (sort) {
                case ATTRIBUTE:
                    recordattr(parseattr(n), attrset);
                    break;
                case ATTRIBUTESET:
                    recordattr(parseattrset(n), attrset);
                    break;
                default:
                    throw new ParseException("Unexpected attribute set element: " + n);
                }
            }
            scopestack.pop();
            if(trace) trace("attributeset.exit");
            return attrset;
        } catch (DapException e) {
            throw new ParseException(e);
        }
    }

    protected List<String>
    parsenamespaces(Node node)
            throws ParseException
    {
        List<String> nslist = new ArrayList<>();
        List<Node> nodes = getSubnodes(node);
        for(int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if("namespace".equalsIgnoreCase(n.getNodeName())) {
                String ns = pull(n, "href");
                if(isempty(ns))
                    throw new ParseException("Illegal null namespace href: " + node);
                if(!nslist.contains(ns)) nslist.add(ns);
            }
        }
        return nslist;
    }

    protected DapAttribute
    parseotherxml(Node node)
            throws ParseException
    {
        if(trace) trace("otherxml.enter");
        String name = pull(node, "name");
        DapOtherXML other = factory.newOtherXML(name);
        // Get the child node(s)
        List<Node> nodes = getSubnodes(node);
        switch (nodes.size()) {
        case 0:
            break;
        case 1:
            other.setRoot(nodes.get(0));
            break;
        default:
            throw new ParseException("OtherXML: multiple top level nodes not supported");
        }
        if(trace) trace("otherxml.exit");
        return other;
    }

    protected void
    parseerror(Node node)
            throws ParseException
    {
        if(trace) trace("error.enter");
        String xhttpcode = pull(node, "httpcode");
        String shttpcode = (xhttpcode == null ? "400" : xhttpcode);
        int httpcode = 0;
        try {
            httpcode = Integer.parseInt(shttpcode);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Error Response; illegal http code: " + shttpcode);
        }
        this.errorresponse = new ErrorResponse();
        this.errorresponse.setCode(httpcode);
        if(trace) trace("error.exit");
    }

    protected void
    errormessage(String value)
            throws ParseException
    {
        if(trace) trace("errormessage.enter");
        assert (this.errorresponse != null) : "Internal Error";
        String message = value;
        message = Escape.entityUnescape(message); // Remove XML encodings
        this.errorresponse.setMessage(message);
        if(trace) trace("errormessage.exit");
    }

    protected void
    errorcontext(String value)
            throws ParseException
    {
        if(trace) trace("errorcontext.enter");
        assert (this.errorresponse != null) : "Internal Error";
        String context = value;
        context = Escape.entityUnescape(context); // Remove XML encodings
        this.errorresponse.setContext(context);
        if(trace) trace("errorcontext.exit");
    }

    protected void
    errorotherinfo(String value)
            throws ParseException
    {
        if(trace) trace("errorotherinfo.enter");
        assert (this.errorresponse != null) : "Internal Error";
        String other = value;
        other = Escape.entityUnescape(other); // Remove XML encodings
        this.errorresponse.setOtherInfo(other);
        if(trace) trace("errorotherinfo.exit");
    }

    /**
     * Pass reserved xml attributes unchanged
     *
     * @param node
     * @param dap
     * @throws ParseException
     */
    protected void
    passReserved(Node node, DapNode dap)
            throws ParseException
    {
        try {
            NamedNodeMap attrs = node.getAttributes();
            for(int i = 0; i < attrs.getLength(); i++) {
                Node n = attrs.item(i);
                String key = n.getNodeName();
                String value = n.getNodeValue();
                if(isReserved(key))
                    dap.addXMLAttribute(key, value);
            }
        } catch (DapException de) {
            throw new ParseException(de);
        }
    }

    static boolean
    isReserved(String name)
    {
        for(String tag : RESERVEDTAGS) {
            if(name.startsWith(tag)) return true;
        }
        return false;
    }
}

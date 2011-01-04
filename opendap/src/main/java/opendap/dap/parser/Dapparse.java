/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the COPYRIGHT file for more information. */

/****************************************************/

package opendap.dap.parser;

import opendap.dap.*;
import opendap.dap.parser.ParseException;

import java.io.InputStream;
import java.util.*;


import static opendap.dap.parser.DapParser.*;

public abstract class Dapparse
{

    // Track what kind of file we parsed; use int instead of enum to simplify access
    static public final int DapNUL = 0;
    static public final int DapDAS = 1;
    static public final int DapDDS = 2;
    static public final int DapERR = 3;

    static final int NA = DGrid.ARRAY;

    BaseTypeFactory factory = null;
    Daplex lexstate = null;

    Dapparse parsestate = null; /* Slight kludge */

    DDS ddsobject = null;
    DAS dasobject = null;
    DAP2Exception errobject = null;

    int parseClass = DapNUL;


    protected int dapdebug = 0;

    /**
     * **********************************************
     */
    /* Constructor(s) */

    // This contructor is needed because generated DapParse
    // constructor requires it.
    public Dapparse()
    {
        this(null);
    }

    public Dapparse(BaseTypeFactory factory)
    {
        parsestate = this;
	if(factory == null) {
	    throw new RuntimeException("Dapparse: no factory specified");
	}
        this.factory = factory;
    }

    /**
     * **********************************************
     */
    /* Access into the DapParser for otherwise inaccessible fiels */
    abstract public boolean parse(InputStream stream) throws ParseException;

    abstract int getDebugLevel();

    abstract void setDebugLevel(int level);

    /**
     * **********************************************
     */
    /* Utilities */

    String strdup(String s)
    {
        return s;
    }


    /**
     * **********************************************
     */

    public DDS getDDS()
    {
        return ddsobject;
    }

    public DAS getDAS()
    {
        return dasobject;
    }

    public DAP2Exception getERR()
    {
        return errobject;
    }

    /**
     * **********************************************
     */
    /* Parse invocation interface */


    public int dapparse(InputStream stream, DDS dds, DAS das, DAP2Exception err) throws ParseException
    {
//        setDebugLevel(1);
        ddsobject = dds;
        dasobject = das;
        errobject = (err == null?new DAP2Exception():err);

        dapdebug = getDebugLevel();
        Boolean accept = parse(stream);
	return parseClass;
    }

    // Call this to parse a DDS
    public int
    ddsparse(InputStream stream, DDS dds) throws ParseException
    {
        return dapparse(stream, dds, null, null);
    }

    // Call this to parse a DAS
    public int
    dasparse(InputStream stream, DAS das)
            throws ParseException
    {
        return dapparse(stream, null, das, null);
    }

    // Call this to parse an error{} body
    public int
    errparse(InputStream stream, DAP2Exception err)
	throws ParseException
    {
        return dapparse(stream, null, null, err);
    }

    /**
     * **********************************************
     */
    /* Parser core */

    /* Use the initial keyword to indicate what we are parsing */
    void
    tagparse(Dapparse parsestate, int kind) throws ParseException
    {
        /* Create the storage object corresponding to what we are parsing */
        String expected = parseactual();
        switch (kind) {
        case SCAN_DATASET:
	    parseClass = DapDDS;
            if (ddsobject == null)
                throw new ParseException("DapParse: found DDS, expected " + expected);
            break;
        case SCAN_ATTR:
	    parseClass = DapDAS;
            if (dasobject == null)
                throw new ParseException("DapParse: found DAS, expected " + parseactual());
            lexstate.dassetup();
            break;
        case SCAN_ERROR:
	    parseClass = DapERR;
            if (errobject == null)
                throw new ParseException("DapParse: found error{}, expected " + parseactual());
            break;
        default:
            throw new ParseException("Unknown tag argument: " + kind);
        }
    }


    private String parseactual()
    {
        String actual = "";
        if (ddsobject != null) actual = actual + " DDS";
        if (dasobject != null) actual = actual + " DAS";
        if (errobject != null) actual = actual + " error{}";
        return "one of" + actual;
    }


    Object
    datasetbody(Dapparse state, Object name, Object decls)
            throws ParseException
    {
        ddsobject.setName((String) name);
        for (Object o : (List<Object>) decls)
            ddsobject.addVariable((BaseType) o);
        if (dapdebug > 0) {
            System.err.println("datasetbody:");
            ddsobject.print(System.err);
        }
        return null;
    }

    Object
    attributebody(Dapparse state, Object attrlist)
            throws ParseException
    {
        try {

            for (Object o : (List<Object>) attrlist) {
                if (o instanceof Attribute) {
                    Attribute a = (Attribute) o;
                    Iterator it = a.getValuesIterator();
                    while (it.hasNext())
                        dasobject.appendAttribute(a.getName(),
                                a.getType(),
                                (String) it.next()); /*UGH*/
                } else if (o instanceof AttributeTable) {
                    AttributeTable aset = (AttributeTable) o;
                    dasobject.addAttributeTable(aset.getName(), aset);
                } else
                    throw new Exception("attribute body: unknown object: " + o);
            }

        } catch (Exception e) {
            throw new ParseException(e);
        }
        return null;
    }

    void
    errorbody(Dapparse state,
              Object code, Object msg, Object ptype, Object prog)
            throws ParseException
    {
        errobject.setErrorMessage(msg.toString());
        errobject.setProgramSource((String) prog);
        try {
            int n = (code == null ? -1 : Integer.decode((String) code));
            errobject.setErrorCode(n);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Error code is not a legal integer");
        }
        try {
            int n = (ptype == null ? -1 : Integer.decode((String) ptype));
            errobject.setProgramType(n);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Error program type is not a legal integer");
        }
    }

    void
    unrecognizedresponse(Dapparse state)
            throws ParseException
    {
        errorbody(state, "0", state.lexstate.input, null, null);
    }

    Object
    declarations(Dapparse state, Object decls, Object decl)
            throws ParseException
    {
        List<Object> alist = (List<Object>) decls;
        if (alist == null)
            alist = new ArrayList<Object>();
        else
            alist.add((BaseType) decl);
        return alist;
    }

    Object
    arraydecls(Dapparse state, Object arraydecls, Object arraydecl)
            throws ParseException
    {
        List<Object> alist = (List<Object>) arraydecls;
        if (alist == null)
            alist = new ArrayList<Object>();
        else
            alist.add(arraydecl);
        return alist;
    }

    Object
    arraydecl(Dapparse state, Object name, Object size)
            throws ParseException
    {
        int value;
        DArrayDimension dim;
        try {
            value = Integer.decode((String) size);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Dimension " + name + " is not an integer: " + size, nfe);
        }
        dim = new DArrayDimension(value, (String) name);
        return dim;
    }

    Object
    attrlist(Dapparse state, Object attrlist, Object attrtuple)
            throws ParseException
    {
        List<Object> alist = (List<Object>) attrlist;
        if (alist == null)
            alist = new ArrayList<Object>();
        else {
            String dupname;
            if (attrtuple != null) {/* null=>alias encountered, ignore */
                alist.add(attrtuple);
                if ((dupname = scopeduplicates(alist)) != null) {
                    dap_parse_error(state, "Duplicate attribute names in same scope: %s", dupname);
                    /* Remove this attribute */
                    alist.remove(alist.size()-1);
                }
            }
        }
        return alist;
    }

    Object
    attrvalue(Dapparse state, Object valuelist, Object value, Object etype)
            throws ParseException
    {
        List<Object> alist = (List<Object>) valuelist;
        if (alist == null) alist = new ArrayList<Object>();
        /* Watch out for null values */
        if (value == null) value = "";
        alist.add(value);
        return alist;
    }

    Object
    attribute(Dapparse state, Object name, Object values, Object etype)
            throws ParseException
    {
        Attribute att;
        att = new Attribute((String) name, attributetypefor((Integer) etype));
        try {
            for (Object o : (List<Object>) values) att.appendValue((String) o, true);
        } catch (Exception e) {
            throw new ParseException(e);
        }
        return att;
    }

    Object
    attrset(Dapparse state, Object name, Object attributes)
            throws ParseException
    {
        try {

            AttributeTable attset;
            attset = new AttributeTable((String) name);
            for (Object o : (List<Object>) attributes) {
                if (o instanceof Attribute) {
                    /* ugh, Attributetable needs an additional addAttribute fcn*/
                    Attribute a = (Attribute) o;
                    Iterator it = a.getValuesIterator();
                    while (it.hasNext())
                        attset.appendAttribute(a.getName(), a.getType(), (String) it.next());
                } else if (o instanceof AttributeTable) {
                    AttributeTable at = (AttributeTable) o;
                    attset.addContainer(at.getName(), at);
                } else
                    throw new ParseException("attrset: unexpected object: " + o);
            }
            return attset;

        } catch (Exception e) {
            throw new ParseException(e);
        }
    }

    Object
    makebase(Dapparse state, Object name, Object etype, Object dimensions)
            throws ParseException
    {
        BaseType node;
        List<Object> dims = (List<Object>) dimensions;
        node = basetypefor((Integer) etype, (String) name);
        if (dims.size() > 0) {
            DArray array = state.factory.newDArray();
            array.addVariable(node);
            dimension(array, dims);
            node = array;
        }
        return node;
    }

    void
    dimension(DArray node, List<Object> dimensions)
            throws ParseException
    {
        int i;
        int rank = dimensions.size();
        /* Interface requires rebuilding the dimensions */
        for (Object o : dimensions) {
            DArrayDimension dim = (DArrayDimension) o;
            node.appendDim(dim.getSize(), dim.getName());
        }
    }

    Object
    makestructure(Dapparse state, Object name, Object dimensions, Object fields)
            throws ParseException
    {
        BaseType node;
        String dupname;
        List<Object> dimset = (List<Object>) dimensions;
        if ((dupname = scopeduplicates((List<Object>) fields)) != null) {
            dap_parse_error(state, "Duplicate structure field names in same scope: %s.%s", (String) name, dupname);
            return (Object) null;
        }
        node = factory.newDStructure((String) name);
        List<Object> list = (List<Object>) fields;
        for (Object o : list)
            ((DStructure) node).addVariable((BaseType) o, NA);
        /* If this is dimensioned, then we need to wrap with DArray */
        if (dimset.size() > 0) {
            DArray anode = factory.newDArray();
            anode.addVariable(node);
            dimension(anode, (List<Object>) dimensions);
            node = anode;
        }
        return node;
    }

    Object
    makesequence(Dapparse state, Object name, Object members)
            throws ParseException
    {
        DSequence node;
        String dupname;
        if ((dupname = scopeduplicates((List<Object>) members)) != null) {
            dap_parse_error(state, "Duplicate sequence member names in same scope: %s.%s", (String) name, dupname);
            return (Object) null;
        }
        node = factory.newDSequence((String) name);
        List<Object> list = (List<Object>) members;
        for (Object o : list)
            node.addVariable((BaseType) o, NA);
        return node;
    }

    Object
    makegrid(Dapparse state, Object name, Object arraydecl0, Object mapdecls0)
            throws ParseException
    {
        DGrid node;
        DArray arraydecl = (DArray) arraydecl0;
        List<Object> mapdecls = (List<Object>) mapdecls0;
        /* Check for duplicate map names */
        String dupname;
        if ((dupname = scopeduplicates(mapdecls)) != null) {
            dap_parse_error(state, "Duplicate grid map names in same scope: %s.%s", (String) name, dupname);
            return (Object) null;
        }
        node = factory.newDGrid((String) name);
        node.addVariable(arraydecl, DGrid.ARRAY);
        for (Object m : mapdecls)
            node.addVariable((BaseType) m, DGrid.MAPS);
        return node;
    }

    String
    flatten(String s)
            throws ParseException
    {
        boolean whitespace = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\r':
            case '\n':
                break;
            case '\t':
                buf.append(' ');
                break;
            default:
                buf.append(c);
                break;
            }
        }
        return buf.toString();
    }

    int
    daperror(Dapparse state, String msg)
            throws ParseException
    {
        dap_parse_error(state, msg);
        return 0;
    }

    void
    dap_parse_error(Dapparse state, String fmt, Object... args)
            throws ParseException
    {
        int len;
        System.err.println(String.format(fmt, args));
        String tmp = null;
        len = lexstate.getInput().length();
        tmp = flatten(lexstate.getInput());
        throw new ParseException("context: " + tmp + "^");
    }

    BaseType
    basetypefor(Integer etype, String name)
            throws ParseException
    {
        switch (etype) {
        case SCAN_BYTE:
            return factory.newDByte(name);
        case SCAN_INT16:
            return factory.newDInt16(name);
        case SCAN_UINT16:
            return factory.newDUInt16(name);
        case SCAN_INT32:
            return factory.newDInt32(name);
        case SCAN_UINT32:
            return factory.newDUInt32(name);
        case SCAN_FLOAT32:
            return factory.newDFloat32(name);
        case SCAN_FLOAT64:
            return factory.newDFloat64(name);
        case SCAN_URL:
            return factory.newDURL(name);
        case SCAN_STRING:
            return factory.newDString(name);
        default:
            throw new ParseException("basetypefor: illegal type: " + etype);
        }
    }

    int
    attributetypefor(Integer etype)
            throws ParseException
    {
        switch (etype) {
        case SCAN_BYTE:
            return Attribute.BYTE;
        case SCAN_INT16:
            return Attribute.INT16;
        case SCAN_UINT16:
            return Attribute.UINT16;
        case SCAN_INT32:
            return Attribute.INT32;
        case SCAN_UINT32:
            return Attribute.UINT32;
        case SCAN_FLOAT32:
            return Attribute.FLOAT32;
        case SCAN_FLOAT64:
            return Attribute.FLOAT64;
        case SCAN_URL:
            return Attribute.URL;
        case SCAN_STRING:
            return Attribute.STRING;
        default:
            throw new ParseException("attributetypefor: illegal type: " + etype);
        }
    }

    String
    scopeduplicates(List<Object> list)
            throws ParseException
    {
        for (int i = 0; i < list.size(); i++) {
            Object io = list.get(i);
            String iname = extractname(io);
            for (int j = i + 1; j < list.size(); j++) {
                Object jo = list.get(j);
                String jname = extractname(jo);
                if (iname.equals(jname))
                    return iname;
            }
        }
        return null;
    }

    /* Since there is no common parent class for BaseType, Attribute,
       and AttributeTable, provide a type specific name getter.
    */

    String
    extractname(Object o)
            throws ParseException
    {
        if (o instanceof BaseType)
            return ((BaseType) o).getName();
        if (o instanceof Attribute)
            return ((Attribute) o).getName();
        if (o instanceof AttributeTable)
            return ((AttributeTable) o).getName();
        throw new ParseException("extractname: illegal object class: " + o);
    }


}

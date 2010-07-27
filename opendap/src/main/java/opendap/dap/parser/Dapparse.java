/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the COPYRIGHT file for more information. */

/****************************************************/

package opendap.dap.parser;

import opendap.dap.*;
import opendap.dap.parser.ParseException;
import opendap.dap.parser.OClist.*;
import java.io.InputStream;
import java.util.Iterator;

import static opendap.dap.parser.DapParser.*;

abstract class Dapparse
{

    /* Convenience definition */
    static final int NA = DGrid.ARRAY;

    BaseTypeFactory factory = null;
    Daplex lexstate = null;

    Dapparse parsestate = null; /* Slight kludge */

    DDS ddsroot = null;
    DAS dasroot = null;

    /* Errorbody output */
    boolean svcerr = false;
    String code = null;
    String message = null;
    String ptype = null;
    String prog = null;

    protected int dapdebug = 0;

    /**************************************************/
    /* Constructor(s) */
    
    public Dapparse()
    {
	this(new DefaultFactory());
    }

    public Dapparse(BaseTypeFactory factory)
    {
	parsestate = this;
	if(factory == null)
	    this.factory = new DefaultFactory();
	else
	    this.factory = factory;
    }

    /**************************************************/
    /* Access into the DapParser for otherwise inaccessible fiels */

    abstract public boolean parse() throws ParseException;
    abstract int getDebugLevel();
    abstract void setDebugLevel(int level);

    /**************************************************/
    /* Utilities */

    String strdup(String s) {return s;}

    void cleanup()
    {
	ddsroot = null;
	dasroot = null;
	factory = null;
	lexstate = null;
    }

    /**************************************************/
    /* get/set */
    public DDS getDDSroot() {return ddsroot;}
    public DAS getDASroot() {return dasroot;}

    /**************************************************/
    /* Keep (almost) the original invocation methods */

    /* Call this to parse a DDS */
    public void
    Dataset(DDS dds, BaseTypeFactory factory)
	throws ParseException, DDSException
    {
//        setDebugLevel(1);
	dapdebug = getDebugLevel();
	ddsroot = dds;
    this.factory = factory;
	try {
	    Boolean accept = parse();
	    if(!accept) throw new ParseException("Parse failed");
        } finally {
	    cleanup();
	}
	return;
    }

    /* Call this to parse a DAS */
    public void
    Attributes(DAS das)
	throws ParseException
    {
//        setDebugLevel(1);
	dapdebug = getDebugLevel();
	dasroot = das;
	try {
	    Boolean accept = parse();
	    if(!accept) throw new ParseException("Parse failed");
        } finally {
	    cleanup();
	}
	return;
    }

    /* Call this to parse an errorbody */
    public void
    Errorbody()
	throws ParseException
    {
	try {
	    Boolean accept = parse();
	    if(!accept) throw new ParseException("Parse failed");
        } finally {
	    cleanup();
	}
	return;
    }

    /**************************************************/
    /* Parser core */

    /* Switch to das parsing */
    void
    dassetup(Dapparse parsestate) throws ParseException
    {
	lexstate.dassetup();
    }

    Object
    datasetbody(Dapparse state, Object name, Object decls)
	    throws ParseException
    {
     ddsroot.setName((String)name);
	for(Object o: (OClist)decls)
	    ddsroot.addVariable((BaseType)o);
	if(dapdebug > 0) {
	    System.err.println("datasetbody:");
	    ddsroot.print(System.err);
	}
	return null;
    }

    Object
    attributebody(Dapparse state, Object attrlist)
	    throws ParseException
    {
	try {

	for(Object o: (OClist)attrlist) {
	    if(o instanceof Attribute) {
	        Attribute a = (Attribute)o;
	        Iterator it = a.getValuesIterator();
	        while(it.hasNext())
                    dasroot.appendAttribute(a.getName(),
                                        a.getType(),
                                        (String)it.next()); /*UGH*/
	    } else if(o instanceof AttributeTable) {
	        AttributeTable aset = (AttributeTable)o;
		dasroot.addAttributeTable(aset.getName(),aset);
	    } else
		throw new Exception("attribute body: unknown object: "+o);
	}

        } catch (Exception e) {
	    throw new ParseException(e);
	}
        return null;
    }

    Object
    errorbody(Dapparse state,
	      Object code, Object msg, Object ptype, Object prog)
	    throws ParseException
    {
	this.svcerr = true;
        this.code     = (code==null?null:code.toString());
        this.message  = (msg==null?null:msg.toString());
        this.ptype  = (ptype==null?null:ptype.toString());
        this.prog  = (prog==null?null:prog.toString());
        return null;
    }

    Object
    unrecognizedresponse(Dapparse state)
	    throws ParseException
    {
	return errorbody(state,"0",state.lexstate.input,null,null);
    }

    Object
    declarations(Dapparse state, Object decls, Object decl)
	    throws ParseException
    {
	OClist alist = (OClist)decls;
	if(alist == null)
	     alist = new OClist();
	else
	    alist.add((BaseType)decl);
	return alist;
    }

    Object
    arraydecls(Dapparse state, Object arraydecls, Object arraydecl)
	    throws ParseException
    {
	OClist alist = (OClist)arraydecls;
	if(alist == null)
	    alist = new OClist();
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
	    value = Integer.decode((String)size);
	} catch (NumberFormatException nfe) {
	    throw new ParseException("Dimension "+name+" is not an integer: "+size,nfe);
	}
	dim = new DArrayDimension(value,(String)name);
	return dim;
    }

    Object
    attrlist(Dapparse state, Object attrlist, Object attrtuple)
	    throws ParseException
    {
	OClist alist = (OClist)attrlist;
	if(alist == null)
	    alist = new OClist();
	else {
	    String dupname;
	    if(attrtuple != null) {/* null=>alias encountered, ignore */
	        alist.add(attrtuple);
		if((dupname=scopeduplicates(alist))!=null) {
		    dap_parse_error(state,"Duplicate attribute names in same scope: %s",dupname);
		    /* Remove this attribute */
		    alist.pop();
		}
	    }
	}
	return alist;
    }

    Object
    attrvalue(Dapparse state, Object valuelist, Object value, Object etype)
	    throws ParseException
    {
	OClist alist = (OClist)valuelist;
	if(alist == null) alist = new OClist();
	/* Watch out for null values */
	if(value == null) value = "";
	alist.add(value);
	return alist;
    }

    Object
    attribute(Dapparse state, Object name, Object values, Object etype)
	    throws ParseException
    {
	Attribute att;
	att = new Attribute((String)name,attributetypefor((Integer)etype));
	try {
	    for(Object o: (OClist)values) att.appendValue((String)o,true);
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
	attset = new AttributeTable((String)name);
	for(Object o: (OClist)attributes) {
	    if(o instanceof Attribute) {
		/* ugh, Attributetable needs an additional addAttribute fcn*/
		Attribute a = (Attribute)o;
		Iterator it = a.getValuesIterator();
		while(it.hasNext())
	                attset.appendAttribute(a.getName(),a.getType(),(String)it.next());
	    } else if(o instanceof AttributeTable) {
		AttributeTable at = (AttributeTable)o;
	        attset.addContainer(at.getName(),at);
	    } else
		    throw new ParseException("attrset: unexpected object: "+o);
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
	OClist dims = (OClist)dimensions;
	node = basetypefor((Integer)etype,(String)name);
	if(dims.size() > 0) {
	    DArray array = state.factory.newDArray();
	    array.addVariable(node);
	    dimension(array,dims);
	    node = array;
	}
	return node;
    }

    void
    dimension(DArray node, OClist dimensions)
	    throws ParseException
    {
	int i;
	int rank = dimensions.size();
	/* Interface requires rebuilding the dimensions */
	for(Object o: dimensions) {
	    DArrayDimension dim = (DArrayDimension)o;
	    node.appendDim(dim.getSize(),dim.getName());
	}
    }

    Object
    makestructure(Dapparse state, Object name, Object dimensions, Object fields)
	    throws ParseException
    {
	BaseType node;
	String dupname;
	OClist dimset = (OClist)dimensions;
	if((dupname=scopeduplicates((OClist)fields))!= null) {
	    dap_parse_error(state,"Duplicate structure field names in same scope: %s.%s",(String)name,dupname);
	    return (Object)null;
	}
	node = factory.newDStructure((String)name);
	OClist list = (OClist)fields;
	for(Object o: list)
	    ((DStructure)node).addVariable((BaseType)o,NA);
	/* If this is dimensioned, then we need to wrap with DArray */
	if(dimset.size() > 0) {
	    DArray anode = factory.newDArray();
	    anode.addVariable(node);
	    dimension(anode,(OClist)dimensions);
	    node = anode;
	}
	return node;
    }

    Object
    makesequence(Dapparse state,Object name,Object members)
	    throws ParseException
    {
	DSequence node;
	String dupname;
	if((dupname=scopeduplicates((OClist)members)) != null) {
	    dap_parse_error(state,"Duplicate sequence member names in same scope: %s.%s",(String)name,dupname);
	    return (Object)null;
	}
	node = factory.newDSequence((String)name);
	OClist list = (OClist)members;
	for(Object o: list)
	    node.addVariable((BaseType)o,NA);
	return node;
    }

    Object
    makegrid(Dapparse state,Object name,Object arraydecl0,Object mapdecls0)
	    throws ParseException
    {
	DGrid node;
	DArray arraydecl = (DArray)arraydecl0;
	OClist mapdecls = (OClist)mapdecls0;
	/* Check for duplicate map names */
	String dupname;
	if((dupname=scopeduplicates(mapdecls)) != null) {
	    dap_parse_error(state,"Duplicate grid map names in same scope: %s.%s",(String)name,dupname);
	    return (Object)null;
	}
	node = factory.newDGrid((String)name);
        node.addVariable(arraydecl,DGrid.ARRAY);
	for(Object m: mapdecls)
	    node.addVariable((BaseType)m,DGrid.MAPS);
	return node;
    }

    String
    flatten(String s)
	    throws ParseException
    {
	boolean whitespace = false;
	StringBuilder buf = new StringBuilder();
	for(int i=0;i<s.length();i++) {
	    char c = s.charAt(i);
	    switch (c) {
	    case '\r': case '\n': break;
	    case '\t': buf.append(' '); break;
	    default: buf.append(c); break;
	    }
	}
	return buf.toString();
    }

    int
    daperror(Dapparse state,String msg)
	    throws ParseException
    {
	dap_parse_error(state,msg);
	return 0;
    }

    void
    dap_parse_error(Dapparse state,String fmt,Object... args)
	    throws ParseException
    {
	int len;
	System.err.println(String.format(fmt,args));
	String tmp = null;
	len = lexstate.getInput().length();
	tmp = flatten(lexstate.getInput());
	throw new ParseException("context: "+tmp+ "^");
    }

    BaseType
    basetypefor(Integer etype, String name)
	throws ParseException
    {
	switch (etype) {
	case SCAN_BYTE: return factory.newDByte(name);
	case SCAN_INT16: return factory.newDInt16(name);
	case SCAN_UINT16: return factory.newDUInt16(name);
	case SCAN_INT32: return factory.newDInt32(name);
	case SCAN_UINT32: return factory.newDUInt32(name);
	case SCAN_FLOAT32: return factory.newDFloat32(name);
	case SCAN_FLOAT64: return factory.newDFloat64(name);
	case SCAN_URL: return factory.newDURL(name);
	case SCAN_STRING: return factory.newDString(name);
	default:
	    throw new ParseException("basetypefor: illegal type: "+etype);
	}
    }

    int
    attributetypefor(Integer etype)
	throws ParseException
    {
	switch (etype) {
	case SCAN_BYTE: return Attribute.BYTE;
	case SCAN_INT16: return Attribute.INT16;
	case SCAN_UINT16: return Attribute.UINT16;
	case SCAN_INT32: return Attribute.INT32;
	case SCAN_UINT32: return Attribute.UINT32;
	case SCAN_FLOAT32: return Attribute.FLOAT32;
	case SCAN_FLOAT64: return Attribute.FLOAT64;
	case SCAN_URL: return Attribute.URL;
	case SCAN_STRING: return Attribute.STRING;
	default:
	    throw new ParseException("attributetypefor: illegal type: "+etype);
	}
    }

    String
    scopeduplicates(OClist list)
	    throws ParseException
    {
	for(int i=0;i<list.size();i++) {
	    Object io = list.get(i);
	    String iname = extractname(io);
	    for(int j=i+1;j<list.size();j++) {
		Object jo = list.get(j);
	        String jname = extractname(jo);
		if(iname.equals(jname))
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
	if(o instanceof BaseType)
	    return ((BaseType)o).getName();
	if(o instanceof Attribute)
	    return ((Attribute)o).getName();
	if(o instanceof AttributeTable)
	    return ((AttributeTable)o).getName();
	throw new ParseException("extractname: illegal object class: "+o);
    }


}

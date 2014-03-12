/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;

import java.util.*;

public class DapEnum extends DapType
{

//////////////////////////////////////////////////
// Constants

    static public final DapType DEFAULTBASETYPE = DapType.INT32;

//////////////////////////////////////////////////
// Instance Variables

    protected DapType basetype = DEFAULTBASETYPE;

    /**
     * The enumeration constants are represented by
     * a List of names since order is important at least for printing,
     * and a pair of maps.
     */

    protected List<String> names = null; // Keyset in order or original declaration
    protected Map<String, Long> namemap = null; // Map name to value
    protected Map<Long, String> valuemap = null; // Map value to name

    //////////////////////////////////////////////////
    // Constructors

    public DapEnum(String name)
    {
        super(name);
    }

///////////////////////////////////////////////////
// Accessors

    public DapType getBaseType()
    {
        return basetype;
    }

    public void setBaseType(DapType basetype)
	throws DapException
    {
	// validate the base type
	if(!basetype.isIntegerType())
	    throw new DapException("DapEnum: illegal base type: "+basetype);
        this.basetype = basetype;
    }

    public void addEnumConst(String name, Long value)
        throws DapException
    {
        if(names == null) {
            names = new ArrayList<String>();
            namemap = new HashMap<String, Long>();
            valuemap = new HashMap<Long, String>();
        }
        Long oldvalue = namemap.get(name);
        String oldname = valuemap.get(value);
        if(oldname != null)
            throw new DapException("DapEnum: Duplicate enum constant name: " + name);
        else if(oldvalue != null)
            throw new DapException("DapEnum: Duplicate enum constant value: " + value);
        namemap.put(name, value);
        valuemap.put(value, name);
        names.add(name);
    }

    // Not clear which enum const aggregates to export.
    // We currently choose to export an alternate interface
    public List<String> getNames()
    {
        return names;
    }

    public Long lookup(String name)
    {
        return namemap.get(name);
    }

    public String lookup(long value)
    {
        return valuemap.get(value);
    }

} // class DapEnum

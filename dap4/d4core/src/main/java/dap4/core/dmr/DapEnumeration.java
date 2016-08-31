/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;

import java.util.ArrayList;
import java.util.List;

public class DapEnumeration extends DapType
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

    protected List<DapEnumConst> constants = new ArrayList<>();

    //////////////////////////////////////////////////
    // Constructors

    public DapEnumeration(String name)
    {
        super(name);
    }

    public DapEnumeration(String name, DapType basetype)
    {
        this(name);
        setBaseType(basetype);
    }
    ///////////////////////////////////////////////////

    public DapNode
    findByName(String name)
    {
        DapEnumConst dec = lookup(name);
        return dec;
    }

    ///////////////////////////////////////////////////
    // Accessors

    public DapType getBaseType()
    {
        return basetype;
    }

    public void setBaseType(DapType basetype)
    {
        // validate the base type
        if(!basetype.isIntegerType())
            throw new IllegalArgumentException("DapEnumeration: illegal base type: " + basetype);
        this.basetype = basetype;
    }

    public void setEnumConsts(List<DapEnumConst> econsts)
            throws DapException
    {
        for(DapEnumConst dec : econsts) {
            addEnumConst(dec);
        }
    }

    public void addEnumConst(DapEnumConst dec)
            throws DapException
    {
        DapEnumConst nold = lookup(dec.getShortName());
        DapEnumConst vold = lookup(dec.getValue());
        if(nold != null)
            throw new DapException("DapEnumeration: Duplicate enum constant name: " + dec.getShortName());
        else if(vold != null)
            throw new DapException("DapEnumeration: Duplicate enum constant value: " + dec.getValue());
        dec.setParent(this);
        constants.add(dec);
    }

    public List<String> getNames()
    {
        List<String> names = new ArrayList<>();
        for(DapEnumConst dec : constants) {
            names.add(dec.getShortName());
        }
        return names;
    }

    public DapEnumConst lookup(String name)
    {
        for(DapEnumConst dec : constants) {
            if(dec.getShortName().equals(name)) return dec;
        }
        return null;
    }

    public DapEnumConst lookup(long value)
    {
        for(DapEnumConst dec : constants) {
            if(dec.getValue() == value) return dec;
        }
        return null;
    }

} // class DapEnumeration

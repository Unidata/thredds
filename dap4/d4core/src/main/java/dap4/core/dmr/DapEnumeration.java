/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.ConversionException;
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
        super(name, TypeSort.Enum);
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

    /**
     * Convert a string vector of int strings mixed with econst names
     * to a set of longs only
     * @param vec   vector of int strings mixed with econst names
     * @param tolong return a vector of longs else vector of econst names
     * @return
     * @throws ConversionException
     */
    public String[]
    convert(String[] vec)
            throws ConversionException
    {
        int count = vec.length;
        long[] lvalues = new long[count];
        for(int i = 0; i < count; i++) {
            try {// see if this is an integer
                lvalues[i] = Long.parseLong(vec[i]);
                // See if this is a legal value for the enum
                if(lookup(lvalues[i]) == null)
                    throw new ConversionException("Illegal Enum constant: " + vec[i]);
            } catch (NumberFormatException nfe) {// not an integer
                DapEnumConst dec = lookup(vec[i]);
                if(dec == null)
                    throw new ConversionException("Illegal Enum constant: " + vec[i]);
                lvalues[i] = dec.getValue();
            }
        }
        // convert to econst names
        String[] names = new String[count];
        for(int i=0;i<count;i++)
            names[i] = this.lookup(lvalues[i]).getShortName();
        return names;
    }

    public String[]
    convert(long[] lvalues)
            throws ConversionException
    {
        int count = lvalues.length;
        String[] svalues = new String[count];
        for(int i = 0; i < count; i++) {
            DapEnumConst dec = this.lookup(lvalues[i]);
            if(dec == null)
                throw new ConversionException("Illegal Enum constant: " + lvalues[i]);
            svalues[i] = dec.getShortName();
        }
        return svalues;
    }

} // class DapEnumeration

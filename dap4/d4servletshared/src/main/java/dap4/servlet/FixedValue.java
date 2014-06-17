/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.servlet;

import dap4.core.dmr.AtomicType;
import dap4.core.dmr.DapType;
import dap4.core.util.DapException;
import dap4.dap4shared.Dap4Util;

public class FixedValue extends Value
{

    //////////////////////////////////////////////////
    // constants

    // We generate integer values by number of bits

    static protected long[] intvalues = {

    };

    static protected long[] ulongvalues = {

    };

    static protected double[] doublevalues = {

    };

    static protected char[] charvalues = {

    };

    static protected byte[] opaquevalues = {
    };

    static protected String[] stringvalues = {
    };

    static protected String[] urlvalues = {
    };

    //////////////////////////////////////////////////

    protected int intindex = 0;
    protected int longindex = 0;
    protected int doubleindex = 0;
    protected int charindex = 0;
    protected int opaqueindex = 0;
    protected int stringindex = 0;
    protected int urlindex = 0;

    //////////////////////////////////////////////////
    // Constructors

    public FixedValue()
    {
    }

    //////////////////////////////////////////////////
    // Value Interface

    public ValueSource source()
    {
        return ValueSource.FIXED;
    }

    public Object
    nextValue(DapType basetype)
        throws DapException
    {
        AtomicType atomtype = basetype.getAtomicType();
        boolean unsigned = atomtype.isUnsigned();
        long typebits = 8 * Dap4Util.daptypeSize(atomtype);
        switch (atomtype) {
        case Int8:
        case UInt8:
        case Int16:
        case UInt16:
        case Int32:
        case UInt32:
        case Int64:
        case UInt64:
	    return null;

        case Float32:
        case Float64:
            return null;

        case Char:
            return null;

        case String:
            return null;

        case URL:
            return null;

        case Opaque:
	    return null;

        case Enum:
            return null;

        default:
            throw new DapException("Unexpected type: " + basetype);
        }
    }


    public int nextCount(int max)
	throws DapException
    {
	return 0;
    }


}


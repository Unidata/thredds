/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.cdmshared.CDMUtil;
import dap4.core.data.*;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.*;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.util.List;


public class CDMDataAtomic extends AbstractDataVariable
                           implements DataAtomic
{
    //////////////////////////////////////////////////
    // Instance variables

    //COVERITY[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
    protected CDMDSP dsp = null;

    protected long product = 0; // dimension cross product; 0 => undefined; scalar=>1

    protected DapType basetype = null;
    protected AtomicType atomtype = null;

    protected Array data = null;

    //////////////////////////////////////////////////
    // Constructors

    public CDMDataAtomic(CDMDSP dsp, DapAtomicVariable template, Array array)
        throws DataException
    {
        super(template);
        this.basetype = ((DapVariable) template).getBaseType();
        this.atomtype = this.basetype.getPrimitiveType();
        this.product = DapUtil.dimProduct(template.getDimensions());
        this.dsp = dsp;
        this.data = array;
    }

    //////////////////////////////////////////////////
    // DataAtomic Interface

    @Override
    public DapType getType()
    {
        return this.basetype;
    }

    @Override
    public long getCount() // dimension cross-product
    {
        return this.product;
    }

    @Override
    public long getElementSize()
    {
        return Dap4Util.daptypeSize(this.atomtype);
    }

    @Override
    public void
    read(List<Slice> slices, Object data, long offset)
    //read(long start, long count, Object data, long offset)
        throws DataException
    {
        Array array = (Array)this.data;
        // If content.getDataType returns object, then we
        // really do not know its true datatype. So, as a rule,
        // we will rely on this.basetype.
        DataType datatype = CDMUtil.daptype2cdmtype(this.basetype);
        if(datatype == null)
            throw new DataException("Unknown basetype: "+this.basetype);
        Class elementclass = CDMUtil.cdmElementClass(datatype);
        if(elementclass == null)
            throw new DataException("Attempt to read non-atomic value of type: " + datatype);
        Object content = array.get1DJavaArray(elementclass); // not very efficient
        try {
            Odometer odom = Odometer.factory(slices, ((DapVariable) this.getTemplate()).getDimensions(), false);
            while(odom.hasNext()) {
                long index = odom.next();
                System.arraycopy(content, (int)index, data, (int)offset, 1);
                offset++;
            }
        } catch (DapException de) {
            throw new DataException(de);
        }
/*
    switch (datatype) {
        case BOOLEAN:
	    boolean[] bovector = (boolean[])vector;
	
	    break;	
        case BYTE:
	    byte[] byvector = (byte[])vector;
	    break;	
        case CHAR:
	    char[] chvector = (char[])vector;
	    break;	
        case SHORT:
	    short[] shvector = (short[])vector;
	    break;	
        case INT:
	    int[] invector = (int[])vector;
	    break;	
        case LONG:
	    long[] lovector = (long[])vector;
	    break;	
        case FLOAT:
	    float[] flvector = (float[])vector;
	    break;	
        case DOUBLE:
	    double[] dovector = (double[])vector;
	    break;	
        case STRING:
	    string[] stvector = (string[])vector;
	    break;	
        case OBJECT:
	    object[] obvector = (object[])vector;
	    break;	
        case STRUCTURE:
        case SEQUENCE:
        default:
	    throw new DataException("Attempt to read non-atomic value of type: "+datatype);
        }
	return result;
*/
    }

    @Override
    public Object
    read(long index)
        throws DataException
    {
        Object result;
        int i = (int)index;
        Array content = (Array)this.data;
        DataType datatype = content.getDataType();
        switch (datatype) {
        case BOOLEAN:
            result = (Boolean) content.getBoolean(i);
            break;
        case BYTE:
            result = (Byte) content.getByte(i);
            break;
        case CHAR:
            result = (Character) content.getChar(i);
            break;
        case SHORT:
            result = (Short) content.getShort(i);
            break;
        case INT:
            result = (Integer) content.getInt(i);
            break;
        case LONG:
            result = (Long) content.getLong(i);
            break;
        case FLOAT:
            result = (Float) content.getFloat(i);
            break;
        case DOUBLE:
            result = (Double) content.getDouble(i);
            break;
        case STRING:
            result = content.getObject(i).toString();
            break;
        case OBJECT:
            result = content.getObject(i);
            break;
        case STRUCTURE:
        case SEQUENCE:
        default:
            throw new DataException("Attempt to read non-atomic value of type: " + datatype);
        }
        return result;
    }

    //////////////////////////////////////////////////
    // Utilities

    protected DapSort
    computesort(Array array)
        throws DataException
    {
        DapSort sort = null;
        Array content = (Array)this.data;
        switch (content.getDataType()) {
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case STRING:
        case OBJECT:
            return DapSort.ATOMICVARIABLE;
        case STRUCTURE:
            return DapSort.STRUCTURE;
        case SEQUENCE:
            return DapSort.SEQUENCE;
        default:
            break; // sequence is not supported
        }
        throw new DataException("Unsupported datatype: " + content.getDataType());
    }
}

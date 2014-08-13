/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapVariable;
import dap4.core.util.*;

import java.util.ArrayList;
import java.util.List;

public class D4DataCompoundArray extends D4DataVariable implements DataCompoundArray
{
    //////////////////////////////////////////////////
    // Instance variables

    long position = 0;
    DapVariable dapvar = null;

    List<DataCompound> instances = new ArrayList<DataCompound>();

    //////////////////////////////////////////////////
    // Constructors

    public D4DataCompoundArray(D4DSP dsp, DapVariable dv)
        throws DataException
    {
        super(dsp, dv);
        this.dsp = dsp;
        this.dapvar = dv;
    }

    //////////////////////////////////////////////////
    // Accessor(s)

    public D4DSP getDSP()
    {
        return this.dsp;
    }

    public DapVariable getDapVariable()
    {
        return this.dapvar;
    }

    public void addElement(DataCompound di)
    {
        this.instances.add(di);
    }

    //////////////////////////////////////////////////
    // DataVariable Interface

    public DataSort getElementSort()
    {
        if(this.dapvar.getSort() == DapSort.SEQUENCE)
            return DataSort.SEQUENCE;
        else
            return DataSort.STRUCTURE;
    }

    public long getCount()
    {
        return DapUtil.dimProduct(dapvar.getDimensions());
    }

    public void read(List<Slice> slices, DataCompound[] data)
        throws DataException
    {
        Odometer odom;
        try {
            odom = Odometer.factory(slices,
                    ((DapVariable) this.getTemplate()).getDimensions(),
                    false);
        } catch (DapException de) {
            throw new DataException(de);
        }
        assert (odom.index() <= data.length);
        for(int i = 0;odom.hasNext();i++) {
            long offset = odom.next();
            data[i] = read(offset);
        }
    }

    // Provide a read of a single value at a given offset in
    // a dimensioned variable.
    public DataCompound
    read(long index)
        throws DataException
    {
        if(index < 0 || index >= instances.size())
            throw new DataException("D4DataCompoundArray.read(i): index out of range: " + index);
        return instances.get((int) index);
    }

    //////////////////////////////////////////////////
    // Utilities

    /*protected DapSort
    computesort(Array array)
        throws DataException
    {
        DapSort sort = null;
        switch (array.getDataType()) {
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
            return DapSort.COMPOUND;
        default:
            break; // sequence is not supported
        }
        throw new DataException("Unsupported datatype: " + array.getDataType());
    }*/
}

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.data.*;
import dap4.core.dmr.DapStructure;
import dap4.core.dmr.DapVariable;
import dap4.core.util.*;
import dap4.dap4shared.AbstractData;
import dap4.dap4shared.AbstractDataVariable;
import ucar.ma2.*;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.*;

/**
 * Provide DSP support for an
 * array of Structure or Sequence
 * instances.
 */

public class CDMDataCompoundArray extends AbstractDataVariable implements DataCompoundArray
{
    //////////////////////////////////////////////////
    // Instance variables

    protected CDMDSP dsp = null;
    //Coverity[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
    protected Variable cdmvar = null;
    protected DataCompound[] instances = null;
    protected ArrayStructure array = null;
    protected int[] shape = null;
    protected long nelems = 0;

    //////////////////////////////////////////////////
    // Constructors

    public CDMDataCompoundArray(CDMDSP dsp, DapVariable dv, ArrayStructure array)
        throws DataException
    {
        super(dv);
        this.dsp = dsp;
        this.template = dv;
        this.cdmvar = (Variable) dsp.getCDMNode(dv);
        this.array = array;
        this.shape = array.getShape();
        if(this.shape.length == 0) this.shape = null; // uniform scalar mark
        // compute shape cross product
        this.nelems = 1;
        if(this.shape != null) {
            for(int i = 0;i < this.shape.length;i++)
                this.nelems *= this.shape[i];
        }
        instances = new DataCompound[(int) this.nelems];
        Arrays.fill(instances, null);
    }

    //////////////////////////////////////////////////
    // DataCompoundArray Interface

    @Override
    public DataSort
    getElementSort()
    {
        if(getTemplate().getSort() == DapSort.SEQUENCE)
            return DataSort.SEQUENCE;
        else
            return DataSort.STRUCTURE;
    }

    @Override
    public long
    getCount() // dimension cross-product
    {
        return this.nelems;
    }

    // Provide a read of a single value at a given offset in a dimensioned variable.
    @Override
    public DataCompound
    read(long index)
        throws DataException
    {
        if(instances[(int) index] == null)
            instances[(int) index] = new CDMDataStructure(this.dsp, (DapStructure) this.getTemplate(), this, index, array.getStructureData((int) index));
        return instances[(int) index];
    }

    /**
     * For this method, the data will be a list of CDMDataStructure
     * or (eventually) CDMDataSequence objects.
     */
    @Override
    public void read(List<Slice> slices, DataCompound[] result)
        throws DataException
    {
        // Cannot use array.section on ArrayStructure: not implemented.
        // So we need to simulate it
        long count = DapUtil.sliceProduct(slices);
        if(count > result.length)
            throw new DataException("read(slices,result): result is too short");
        Odometer odom;
        try {
            odom = Odometer.factory(slices,((DapVariable)this.getTemplate()).getDimensions(),false);
        } catch (DapException de) {
            throw new DataException(de);
        }
        int i;
        for(i=0;odom.hasNext();i++) {
            long offset = odom.next();
            int ioffset = (int)offset;
            if(instances[ioffset] == null) {
                StructureData data = (StructureData) this.array.getStructureData(ioffset);
                instances[ioffset] = new CDMDataStructure(this.dsp, (DapStructure) this.getTemplate(), this, offset, data);
            }
            result[i] = instances[ioffset];
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    /**
     * Dynamically create a CDMData{Structure,Sequence} object
     * and cache it
     */    


/*
    protected DapSort
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
    }
*/
}

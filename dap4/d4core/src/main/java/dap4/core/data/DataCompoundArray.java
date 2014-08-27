/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapType;
import dap4.core.util.Slice;

import java.io.IOException;
import java.util.List;

/**
DataCompoundArray represents an array of
either DataStructure or DataSequence instances.
Note that is is NOT used for SCALARS.
*/

public interface DataCompoundArray extends DataVariable
{
    /**
     * Get the element sort; currently returns only DataSort.{SEQUENCE,STRUCTURE}
      * @return sort
     */
    public DataSort getElementSort();

    /**
     * Get the total number of elements in the variable array.
     * A scalar is treated as a one element array.
     *
     * @return 1 if the variable is scalar, else the product
     *         of the dimensions of the variable.
     */
    public long getCount();

    /**
     *  Read multiple values at once.
     *  The returned value (parameter "data") is some form of array of DataCompound objects.
     *
     *  @param constraint slices constraining what is to be returned.
     *  @param data the array into which the values are returned
     */
    public void read(List<Slice> constraint, DataCompound[] data) throws DataException;

    /**
     *  Provide a read of a single value at a given index in a variable.
     *  @param index of the value to read.
     */
    public DataCompound read(long index) throws DataException;

}

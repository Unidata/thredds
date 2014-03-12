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
    public DataSort getElementSort(); // return SEQUENCE or STRUCTURE

    public long getCount(); // dimension cross-product

    // Provide a constrained read of multiple values at once.
    public void read(List<Slice> constraint, DataCompound[] data) throws DataException;

    // Provide a read of a single value at a given offset in a dimensioned variable.
    public DataCompound read(long index) throws DataException;

}

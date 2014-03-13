/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapVariable;

import java.util.Iterator;

/**
DataDataset represents the whole dataset
and is the entry point for walking the data.
*/

public interface DataDataset extends Data
{
    // Get the data associated with a top-level variable, by variable
    public DataVariable getVariableData(DapVariable var) throws DataException;
}

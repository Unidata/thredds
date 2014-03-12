/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

/**
DataSequence represents a set of records.
*/

public interface DataSequence extends DataCompound
{
    public long getRecordCount();

    // Read a single record
    public DataRecord readRecord(long recordno) throws DataException;

    // Return a predicate-based subset of the records TBD
    //public List<DataRecord> read() throws DataException;
}

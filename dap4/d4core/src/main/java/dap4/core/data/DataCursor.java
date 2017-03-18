/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

import dap4.core.dmr.DapNode;
import dap4.core.util.DapException;
import dap4.core.util.Index;
import dap4.core.util.Slice;

import java.util.List;

/**
 * For data access, we adopt a cursor model.
 * This comes from database technology where a
 * cursor object is used to walk over the
 * results of a database query.  Here the cursor
 * walks the underlying data and stores enough
 * state to extract data depending on its
 * sort. The cursor may (or may not) contain
 * internal subclasses to track various kinds of
 * state.
 */

public interface DataCursor
{
    //////////////////////////////////////////////////
    // Kinds of Cursor

    static public enum Scheme
    {
        ATOMIC,
        STRUCTARRAY,
        STRUCTURE,
        SEQARRAY,
        SEQUENCE,
        RECORD;

        public boolean isCompoundArray()
        {
            return this == STRUCTARRAY || this == SEQARRAY;
        }
    }

    //////////////////////////////////////////////////
    // API

    public Scheme getScheme();

    public DSP getDSP();

    public DapNode getTemplate();

    public Index getIndex() throws DapException;

    public boolean isScalar();

    public boolean isField();

    // Return null if top-level, else return the struct/seq from which this is derived
    public DataCursor getContainer();

    //////////////////////////////////////////////////
    // Atomic Data Management

    // As a rule, only one will be fully implemented and the other written
    // to use the fully implemented one.
    // Returns:
    // atomic - array of data values
    // structure/sequence - DataCursor[]
    // Even if the result is a scalar,
    // a 1-element array will be returned.

    public Object read(List<Slice> slices) throws DapException;

    public Object read(Index index) throws DapException;

    //////////////////////////////////////////////////
    // Sequence record management
    // assert scheme == SEQUENCE

    public long getRecordCount() throws DapException;

    public DataCursor readRecord(long i) throws DapException;

    public long getRecordIndex() throws DapException; // assert scheme == RECORD

    //////////////////////////////////////////////////
    // field management
    // assert scheme == STRUCTURE | scheme == RECORD

    public int fieldIndex(String name) throws DapException; // Convert a name to an index

    public DataCursor readField(int fieldindex) throws DapException;
}

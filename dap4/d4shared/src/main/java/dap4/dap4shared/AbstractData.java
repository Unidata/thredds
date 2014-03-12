/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapNode;
import dap4.core.dmr.DapVariable;

abstract public class AbstractData implements Data
{

    //////////////////////////////////////////////////
    // Instance variables

    protected DataSort sort = null;
    protected DapNode template = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    protected AbstractData(DapNode template)
        throws DataException
    {
        this.template = template;
        this.sort = computesort();
    }

    //////////////////////////////////////////////////
    // Data Interface

    @Override
    public DataSort
    getSort()
    {
        return sort;
    }

    @Override
    public DapNode
    getTemplate()
    {
        return template;
    }

    //////////////////////////////////////////////////
    // Utilities

    protected DataSort
    computesort()
    {   // order is important
        if(this instanceof DataAtomic) return DataSort.ATOMIC;
        if(this instanceof DataRecord) return DataSort.RECORD;
        if(this instanceof DataSequence) return DataSort.SEQUENCE;
        if(this instanceof DataStructure) return DataSort.STRUCTURE;
        if(this instanceof DataDataset) return DataSort.DATASET;
        if(this instanceof DataCompoundArray) return DataSort.COMPOUNDARRAY;
        assert false : "Cannot compute sort";
        return null;
    }
}

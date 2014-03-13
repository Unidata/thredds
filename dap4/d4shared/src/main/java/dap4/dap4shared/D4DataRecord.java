/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapSequence;
import dap4.core.dmr.DapStructure;

/**
 * DataRecord represents a record from a sequence.
 * It is effectively equivalent to a Structure instance.
 */

public class D4DataRecord extends D4DataVariable implements DataRecord
{

    //////////////////////////////////////////////////
    // Instance variables

    protected D4DataSequence parent = null;

    protected int recno = 0;

    protected D4DSP dsp = null;

    //////////////////////////////////////////////////
    // Constructors

    public D4DataRecord(D4DSP dsp, DapStructure dap, D4DataSequence parent, int recno)
        throws DataException
    {
        super(dsp,dap);
        this.dsp = dsp;
        this.parent = parent;
        this.recno = recno;
    }

    //////////////////////////////////////////////////
    // Accessors

    public void
    addField(int mindex, D4DataVariable ddv)
    {

    }

    //////////////////////////////////////////////////
    // DataStructure Interface

    // Read field by index
    @Override
    public DataVariable readfield(int i) throws DataException
    {
        return null;
    }

    // Read field by name
    @Override
    public DataVariable readfield(String name) throws DataException
    {
        return null;
    }
}

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.core.data.DataException;
import dap4.core.dmr.DapSequence;
import dap4.core.dmr.DapStructure;
import dap4.servlet.CDMDataStructure;
import ucar.ma2.Array;
import ucar.ma2.StructureData;

/**
DataRecord represents a record from a sequence.
It is effectively equivalent to a Structure instance.
*/

public class CDMDataRecord extends CDMDataStructure
{

    //////////////////////////////////////////////////
    // Constructors

    public CDMDataRecord(CDMDSP dsp, DapSequence dap, CDMDataCompoundArray cdv, int index, StructureData data)
        throws DataException
    {
        super(dsp,dap,cdv,index,data);
    }
}

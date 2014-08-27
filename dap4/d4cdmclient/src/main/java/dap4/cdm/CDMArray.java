/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.dap4shared.DSP;
import ucar.ma2.Range;
import ucar.nc2.Dimension;

import java.util.List;

/**
It is convenient to be able to create 
a common "parent" interface for all
the CDM array classes
*/

public interface CDMArray
{
    public DSP getDSP();
    public CDMDataset getRoot();
    public DapVariable getTemplate();
    public long getByteSize();
    public DapType getBaseType();
    public AtomicType getPrimitiveType();
}

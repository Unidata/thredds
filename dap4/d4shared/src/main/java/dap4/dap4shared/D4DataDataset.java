/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.*;

import java.util.ArrayList;
import java.util.List;

public class D4DataDataset extends AbstractData implements DataDataset
{

    //////////////////////////////////////////////////
    // Instance variables

    D4DSP dsp = null;
    List<DataVariable> variables = new ArrayList<DataVariable>();

    //////////////////////////////////////////////////
    // Constructors

    public D4DataDataset(D4DSP dsp, DapDataset dmr)
        throws DataException
    {
        super(dmr);
        this.dsp = dsp;
        dsp.setD4Dataset(this);
    }

    //////////////////////////////////////////////////
    // Accessors

    public void
    addVariable(DataVariable dv)
    {
        variables.add(dv);
    }

    public List<DataVariable>
    getTopVariables()
    {
        return variables;
    }

    //////////////////////////////////////////////////
    // DataDataset Interface

    public DataVariable
    getVariableData(DapVariable var)
        throws DataException
    {
        return null;
    }

}

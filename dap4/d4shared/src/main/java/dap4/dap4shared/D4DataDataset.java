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
    // Constants

    static final public long serialVersionUID = 1L;

    //////////////////////////////////////////////////
    // Instance variables

    //Coverity[FB.URF_UNREAD_FIELD]
    protected D4DSP dsp = null;
    protected List<D4DataVariable> variables = new ArrayList<D4DataVariable>();

    //////////////////////////////////////////////////
    // Constructors

    public D4DataDataset(D4DSP dsp, DapDataset dmr)
        throws DataException
    {
        super(dmr);
        this.dsp = dsp;
    }

    //////////////////////////////////////////////////
    // Accessors

    public void
    addVariable(D4DataVariable dv)
    {
        variables.add(dv);
    }

    public List<D4DataVariable>
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
        for(D4DataVariable dv: variables) {
            if(dv.getVariable() == var)
                return dv;
        }
        return null;
    }

}

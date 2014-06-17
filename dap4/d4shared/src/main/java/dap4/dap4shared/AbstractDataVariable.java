/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapNode;
import dap4.core.dmr.DapVariable;

abstract public class AbstractDataVariable extends AbstractData
    implements DataVariable
{
    //////////////////////////////////////////////////
    // Constructor(s)

    protected AbstractDataVariable(DapNode template)
        throws DataException
    {
        super(template);
    }

    //////////////////////////////////////////////////
    // DataVariable Interface

    public DapVariable getVariable()
    {
        return (DapVariable) getTemplate();
    }

}

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

//////////////////////////////////////////////////

package dap4.core.data;

import dap4.core.util.DapException;

public class DataException extends DapException
{
    int code = 0; // 0=> no code; > 0 => http code; < 0 => dap defined code

    public DataException()
    {
        super();
    }

    public DataException(String msg)
    {
        super(msg);
    }

    public DataException(Throwable e)
    {
        super(e);
    }

    public DataException(String msg, Throwable e)
    {
        super(msg, e);
    }
}

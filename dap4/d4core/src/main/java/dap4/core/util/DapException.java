/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

//////////////////////////////////////////////////

package dap4.core.util;

public class DapException extends java.io.IOException
{
    int code = 0; // 0=> no code; > 0 => http code; < 0 => dap defined code

    public DapException()
    {
    }

    public DapException(String msg)
    {
        super(msg);
    }

    public DapException(Throwable e)
    {
        super(e);
    }

    public DapException(String msg, Throwable e)
    {
        super(msg, e);
    }

    public DapException setCode(int code)
    {
        this.code = code;
        return this;
    }

    public int getCode()
    {
        return this.code;
    }
}

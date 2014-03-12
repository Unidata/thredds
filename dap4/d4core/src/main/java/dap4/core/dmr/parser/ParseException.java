/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


//////////////////////////////////////////////////

package dap4.core.dmr.parser;

import dap4.core.util.DapException;

public class ParseException extends DapException
{
    public ParseException(String msg)
    {
        super(msg);
    }

    public ParseException(Throwable e)
    {
        super(e);
    }

    public ParseException(String msg, Throwable e)
    {
        super(msg, e);
    }

}

/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: dmh
 * Date: May 20, 2010
 * Time: 12:04:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class HTTPException extends IOException
{

    int code = 0; // 0=> no code; > 0 => http code; < 0 => dap defined code

    public HTTPException()
    {
        super();
    }

    public HTTPException(java.lang.String message)
    {
        super(message);
    }

    public HTTPException(java.lang.String message, java.lang.Throwable cause)
    {
        super(message, cause);
    }

    public HTTPException(java.lang.Throwable cause)
    {
        super(cause);
    }

    public HTTPException setCode(int code)
    {
        this.code = code;
        return this;
    }

    public int getCode()
    {
        return this.code;
    }
}

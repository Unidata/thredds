/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

//////////////////////////////////////////////////

/**
 * Define an exception to hold the XML content of an
 *error chunk.
 */
package dap4.dap4lib;

import dap4.core.util.DapException;

public class ErrorException extends DapException
{
    String document = null;

    public ErrorException(String msg)
    {
        super(msg);
    }

    public ErrorException(Throwable e)
    {
        super(e);
    }

    public ErrorException setDocument(String document)
    {
        this.document = document;
        return this;
    }

    public String getDocument()
    {
        return this.document;
    }
}

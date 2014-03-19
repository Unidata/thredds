/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.d4ts;

import dap4.core.util.DapException;

abstract public class URLMap
{
    //////////////////////////////////////////////////
    // Abstract API

    abstract public String mapURL(String url) throws DapException; // url -> path (file or dir)
    abstract public String mapPath(String path) throws DapException; // path -> url
}


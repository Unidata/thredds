/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

/**
 * Provide a general map of Object->Object to serve
 * to pass context/env info into various classes.
 * Note that we cannot use e.g. java.util.Properties
 * because it is a String->String map.
 */

public class DapContext extends java.util.HashMap<Object, Object>
{
    public DapContext()
    {
        super();
    }

}

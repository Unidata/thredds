/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.util;

import java.util.Map;

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

    public String
    toString()
    {
        StringBuilder buf = new StringBuilder("DapContext{");
        boolean first = true;
        for(Map.Entry<Object,Object> entry: super.entrySet()) {
            if(!first) buf.append(",");
            buf.append("|");
            buf.append(entry.getKey().toString());
            buf.append("|");
            buf.append("=");
            buf.append("|");
            buf.append(entry.getValue().toString());
            buf.append("|");
            first = false;
        }
        buf.append("}");
        return buf.toString();
    }

}

/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;


import ucar.httpservices.HTTPUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static thredds.server.reify.LoadCommon.Command;

/**
 * Process an HttpRequest to extract common reification parameters
 */
class Parameters
{
    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // Constructor arguments

    public HttpServletRequest req;

    // computed

    Map<String, String[]> params;

    public Map<String, String> testinfo = new HashMap<>();

    // Public
    public Command command = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public Parameters(HttpServletRequest req)
            throws IOException
    {
        assert req != null;

        this.req = req;
        this.params = new HashMap<String, String[]>();
        if(req.getParameterMap() == null) {
            this.command = Command.NONE;
        } else {
            this.params.putAll(req.getParameterMap());
            String s = getparam("testinfo");
            if(s != null)
                this.testinfo = LoadCommon.parseMap(s, ';', true);
            // Command
            s = getparam("request");
            if(s == null)
                this.command = Command.NONE;
            else {
                this.command = Command.parse(s);
                if(this.command == null)
                    throw new IOException("Unknown request: " + s);
            }
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    protected String[]
    getparamset(String key)
            throws IOException
    {
        String[] values = this.params.get(key);
        return (values == null ? new String[0] : values);
    }

    public String
    getparam(String key)
            throws IOException
    {
        String[] values = getparamset(key);
        if(values.length == 0) return null;
        String value = URLDecoder.decode(values[0], "UTF-8");
        value = HTTPUtil.nullify(value);
        return value;
    }

}

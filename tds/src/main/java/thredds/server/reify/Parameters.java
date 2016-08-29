/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;


import ucar.httpservices.HTTPUtil;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static thredds.server.reify.ReifyUtils.*;

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

    //////////////////////////////////////////////////
    // Known parameters (allow direct access)

    public Command command = null;
    public FileFormat format = null;
    public String url = null;
    public String target = null;
    public String inquire = null;

    public Map<String,String> testinfo = new HashMap<>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public Parameters(HttpServletRequest req)
            throws IOException
    {
        assert req != null;

        this.req = req;
        this.params = new HashMap<String, String[]>();
        if(req.getParameterMap() != null)
	    this.params.putAll(req.getParameterMap());

	String s = getparam("testinfo");
	this.testinfo = ReifyUtils.parseMap(s,';',true);

        // Command
        s = getparam("request");
        this.command = Command.parse(s);
        if(this.command == null)
            throw new IOException("Unknown request: " + s);

        // File Format
        this.format = FileFormat.getformat(getparam("format"));

        // url
        this.url = getparam("url");

        // target
        this.target = getparam("target");

        // inquiry key
        this.inquire = getparam("inquire");
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
        return URLDecoder.decode(values[0],"UTF-8");
    }

}

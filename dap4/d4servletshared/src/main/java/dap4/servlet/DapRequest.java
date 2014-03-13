/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENCE file for more information. */

package dap4.servlet;

import dap4.core.util.*;
import dap4.dap4shared.RequestMode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * User requests get cached here so that downstream code can access
 * the details of the request information.
 * <p/>
 * Modified by Heimbigner for DAP4.
 *
 * @author Nathan Potter
 * @author Dennis Heimbigner
 */

public class DapRequest
{

    //////////////////////////////////////////////////
    // Instance variables

    ServletInfo svcinfo = null;
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    String url = null;  // without any query
    String originalurl = null; // as given
    String contextpath = null;
    String fullpath = null;
    String servletpath = null;
    String datasetpath = null;
    String querystring = null;

    RequestMode mode = null; // .dmr, .dap, or .dsr
    ResponseFormat format = null; // e.g. .xml when given .dmr.xml

    Map<String, String> queries = new HashMap<String, String>();

    //////////////////////////////////////////////////
    // Constructor(s)

    public DapRequest(ServletInfo svcinfo,
                      HttpServletRequest request,
                      HttpServletResponse response)
        throws DapException
    {
        this.svcinfo = svcinfo;
        this.request = request;
        this.response = response;
        try {
            parse();
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
    }

    //////////////////////////////////////////////////
    // Request path parsing

    protected void
    parse()
        throws IOException
    {
        this.url = request.getRequestURL().toString();// does not include query
        this.querystring = request.getQueryString();
        this.originalurl = (this.querystring == null ? this.url : this.url + "?" + this.querystring);
        this.contextpath = relpath(request.getContextPath());

        this.servletpath = relpath(request.getServletPath());
        if(this.servletpath == null) this.servletpath = "";
        // The servlet path may start with the servlet name; the rest is the path to the resource
        String servletname = svcinfo.getServletname();
        if(servletpath.startsWith(servletname)) {
            this.datasetpath = servletpath.substring(servletname.length(), servletpath.length());
            this.servletpath = servletname;
        } else {
            this.datasetpath = servletpath;
            this.servletpath = "";
        }
        this.datasetpath = DapUtil.nullify(DapUtil.canonicalpath(this.datasetpath, true));

        this.mode = null;
        if(this.datasetpath == null) {
            // Presume mode is a capabilities request
            this.mode = RequestMode.CAPABILITIES;
        } else {
            String[] segments = datasetpath.split("/");
            if(segments.length > 0 && segments[0].length() > 0) {
                String endofpath = segments[segments.length - 1];
                // Decompose last element in path by '.'
                String[] pieces = endofpath.split("[.]");
                // Search backward looking for the mode (dmr or databuffer)
                // meanwhile capturing the format extension
                int modepos = 0;
                for(int i = pieces.length - 1;i >= 1;i--) {//ignore first piece
                    String ext = pieces[i];
                    RequestMode mode = RequestMode.modeFor(ext);
                    ResponseFormat format = ResponseFormat.formatFor(ext);
                    if(mode != null) {
                        // Stop here
                        this.mode = mode;
                        modepos = i;
                        break;
                    } else if(format != null) {
                        if(this.format != null)
                            throw new DapException("Multiple response formats specified: " + ext)
                                .setCode(HttpServletResponse.SC_BAD_REQUEST);
                        this.format = format;
                    }
                }
                // Set the databuffer set name to the entire path before the mode
                // defining extension.
                if(modepos > 0) {
                    StringBuilder buf = new StringBuilder();
                    for(int i = 0;i < modepos;i++) {
                        if(i > 0) buf.append(".");
                        buf.append(pieces[i]);
                    }
                    datasetpath = buf.toString();
                } else
                    datasetpath = endofpath;
                // If we did not find a mode, assume DSR
                if(this.mode == null)
                    this.mode = RequestMode.DSR;
            }
        }

        // Parse the query string into a Map
        if(querystring != null && querystring.length() > 0) {
            String[] pieces = querystring.split("&");
            for(String piece : pieces) {
                String[] pair = piece.split("=");
                String name = Escape.urlDecode(pair[0]);
                name = name.toLowerCase(); // for consistent lookup
                String value = (pair.length == 2 ? Escape.urlDecode(pair[1]) : "");
                queries.put(name, value);
            }
        }

        DapLog.debug("DapRequest: resourcedir=" + getResourcePath());
        DapLog.debug("DapRequest: extension=" + (this.mode == null ? "null" : this.mode.extension()));
        DapLog.debug("DapRequest: servletpath=" + this.servletpath);
        DapLog.debug("DapRequest: datasetpath=" + this.datasetpath);

    } // parse()

    //////////////////////////////////////////////////
    // Get/Set

    public HttpServletRequest getRequest()
    {
        return request;
    }

    public HttpServletResponse getResponse()
    {
        return response;
    }

    public OutputStream getOutputStream()
        throws IOException
    {
        return response.getOutputStream();
    }

    public String getURL()
    {
        return this.url;
    }

    public String getOriginalURL()
    {
        return this.originalurl;
    }

    public String getDataset()
    {
        return this.datasetpath;
    }

    public String getServletPath()
    {
        return this.servletpath;
    }

    public RequestMode getMode()
    {
        return this.mode;
    }

    public ResponseFormat getFormat()
    {
        return this.format;
    }


    /**
     * Set a request header
     *
     * @param name  the header name
     * @param value the header value
     */
    public void setResponseHeader(String name, String value)
    {
        this.response.setHeader(name, value);
    }

    public String queryLookup(String name)
    {
        return queries.get(name.toLowerCase());
    }

    /**
     * Return the absolute path for the /WEB-INF/resources directory
     *
     * @return the absolute path for the /WEB-INF/resources directory
     */
    public String getResourcePath()
    {
        return this.svcinfo.getResourcePath();
    }

    //////////////////////////////////////////////////
    // Utility methods

    /**
     * Relativizing a path =>  remove any leading '/' and cleaning it
     *
     * @param path
     * @return
     */
    String
    relpath(String path)
    {
        return DapUtil.canonicalpath(path, true);
    }
}



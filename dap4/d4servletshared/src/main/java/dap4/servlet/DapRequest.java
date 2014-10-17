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

    protected ServletInfo svcinfo = null;
    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected String url = null;  // without any query  and as with any modified dataset path
    protected String servletpath = null;
    protected String contextpath = null;
    protected String datasetpath = null;   // everything after the servletpath
    protected String querystring = null;
    protected String server = null; // scheme + host + port

    protected RequestMode mode = null; // .dmr, .dap, or .dsr
    protected ResponseFormat format = null; // e.g. .xml when given .dmr.xml

    protected Map<String, String> queries = new HashMap<String, String>();

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

    /**
     * The goal of parse() is to extract info
     * from the underlying HttpRequest and cache it
     * in this object.
     * <p/>
     * In particular, the incoming URL needs to be decomposed
     * into multiple pieces. Certain assumptions are made:
     * 1. every incoming url is of the form
     * (a) http(s)://host:port/d4ts/
     * or
     * (b) http(s)://host:port/d4ts/<datasetpath>?query
     * Case a indicates that the front page is to be returned.
     * Case b indicates a request for a dataset (or dsr), and its
     * value is determined by its extensions. The query may be absent.
     * We want to extract the following pieces.
     * 1. (In URI parlance) The scheme plus the authority:
     * http://host:port
     * 2. The servlet path: should always be "d4ts".
     * 3. The return type: depending on the last extension (e.g. ".txt").
     * 3. The requested: depending on the next to last extension (e.g. ".dap").
     * 5. The suffix path specifying the actual dataset: datasetpath
     * with return and request type extensions removed.
     * 6. The url path = servletpath + datasetpath.
     * 7. The query part.
     */

    protected void
    parse()
            throws IOException
    {
        this.url = request.getRequestURL().toString();// does not include query
        this.querystring = request.getQueryString();
        this.servletpath = DapUtil.absolutize(request.getServletPath());
        this.contextpath = DapUtil.nullify(request.getContextPath());
        this.datasetpath = request.getPathInfo();
        this.datasetpath = DapUtil.canonicalpath(this.datasetpath);
        this.datasetpath = DapUtil.absolutize(this.datasetpath);

        // I still do not understand what this is: this.contextpath = relpath(request.getContextPath());

        // It appears that, sometimes, tomcat does not conform to the servlet spec.
        // Specifically, we can see either of the following:
        // 1. getServletPath() can be null.
        // 2. getServletPath may also contain what should be the result
        //    of calling getPathInfo().
        // So we need to be prepared to fix.

        String servletprefix = "/" + svcinfo.getServletname();
        if(this.servletpath != null && !this.servletpath.equals(servletprefix)) {
            if(!this.servletpath.startsWith(servletprefix)) {
                this.servletpath = servletprefix + this.servletpath;
                //throw new IOException("URL does not specify the servlet:" + this.url);
            }
            this.datasetpath = this.servletpath.substring(servletprefix.length(), this.servletpath.length());
            this.datasetpath = DapUtil.canonicalpath(this.datasetpath);
        }
        this.servletpath = servletprefix; // always
        this.datasetpath = DapUtil.nullify(this.datasetpath);

        // Now, construct various items
        StringBuilder buf = new StringBuilder();
        buf.append(request.getScheme());
        buf.append("://");
        buf.append(request.getServerName());
        int port = request.getServerPort();
        if(port > 0) {
            buf.append(":");
            buf.append(port);
        }
        this.server = buf.toString();

/*
        if(this.dataSetName == null) {
            if(servletpath != null) {
                // use servlet path
		if(cxtpath!= null && servletpath.startsWith(cxtpath)) {
		    this.dataSetName = servletpath.substring(cxtpath.length());
		} else {
		    this.dataSetName = servletpath;
		}
	    }
        }
*/

        this.mode = null;
        if(this.datasetpath == null) {
            // Presume mode is a capabilities request
            this.mode = RequestMode.CAPABILITIES;
            this.format = ResponseFormat.HTML;
        } else {
            // Decompose path by '.'
            String[] pieces = this.datasetpath.split("[.]");
            // Search backward looking for the mode (dmr or databuffer)
            // meanwhile capturing the format extension
            int modepos = 0;
            for(int i = pieces.length - 1; i >= 1; i--) {//ignore first piece
                String ext = pieces[i];
                // We assume that the set of response formats does not interset the set of request modes
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
            // Set the datasetpath to the entire path before the mode defining extension.
            if(modepos > 0)
                this.datasetpath = DapUtil.join(pieces, ".", 0, modepos);
        }
        if(this.mode == null)
            this.mode = RequestMode.DSR;
        if(this.format == null)
            this.format = ResponseFormat.NONE;

        //Reassemble the url minus the query
        buf.setLength(0);
        buf.append(this.server);
        if(this.servletpath != null)
            buf.append(this.servletpath);
        if(this.datasetpath != null) {
            buf.append(this.datasetpath);
        }
        this.url = buf.toString();

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

        DapLog.debug("DapRequest: realrootdir=" + getRealPath(""));
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
        return (this.querystring == null ? this.url
                : this.url + "?" + this.querystring);
    }

    public String getDataset()
    {
        return this.datasetpath;
    }

    public String getServletPath()
    {
        return this.servletpath;
    }

    public String getURLPath()
    {
        return this.servletpath + (this.datasetpath == null ? "" : this.datasetpath);
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
     * Return the absolute path for the /resources directory
     *
     * @return the absolute path for the /resources directory
     */
    public String getRealPath(String virtual)
    {
        return this.svcinfo.getRealPath(virtual);
    }

    /**
     * Return the path info from the request url past the servlet name
     *
     * @return the path
     */
    public String getPathInfo()
    {
        String path = this.request.getPathInfo();
        if(path == null)
            path = "";
        return path;
    }

}


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
     * into multiple pieces. Specifically, given, for exampe,
     * http://host.edu:8081/a/b/c/dap4/x/y/dataset.nc.dmr.txt?<query>
     * we want to extract the following pieces.
     * 1. (In URI parlance) The scheme plus the authority:
     * http://host.edu:8081
     * in our example.
     * 2. The servlet path: a/b/c/<servletname>
     * In our case, the servletname is "dap4".
     * Note that as a rule, the "/a/b/c" part will
     * not be present, so normally, the servlet path
     * will just be "<servletname>".
     * 3. The return type: ".txt" in this case.
     * 4. The requested object: ".dmr" in this case.
     * 5. The suffix path specifying the actual dataset:
     * x/y/dataset.nc
     * Note that the return type and request type are removed
     * leaving, one hopes, the bare path to the dataset.
     * Of course the x/y part may be empty.
     * 6. The query part.
     */

    protected void
    parse()
            throws IOException
    {
        this.url = request.getRequestURL().toString();// does not include query
        this.querystring = request.getQueryString();
        // I still do not understand what this is: this.contextpath = relpath(request.getContextPath());

        String servletname = svcinfo.getServletname();
        this.datasetpath = DapUtil.canonicalpath(DapUtil.nullify(request.getPathInfo()));
        if(this.datasetpath != null)
            this.datasetpath = DapUtil.relativize(this.datasetpath.substring(1));

        // It appears that, sometimes, tomcat does not conform to the servlet spec.
        // Specifically, getServletPath may also contain what whould be the result
        // of calling getPathInfo().
        // Solution: look for this case and split the path
        this.datasetpath = DapUtil.canonicalpath(DapUtil.nullify(request.getPathInfo()));
        if(this.datasetpath != null)
            this.datasetpath = DapUtil.relativize(this.datasetpath.substring(1));
        this.servletpath = request.getServletPath();
        // Assume that the servletpath may have leading segment (e.g. /x/dap4).
        String[] segments = this.servletpath.split("[/]");
        // Locate the servletname segment
        // Note that it is possible that it may not exist
        // if this servlet is being run as ROOT under tomcat.
        int pos = -1;
        for(int i = 0; i < segments.length; i++) {
            if(segments[i].equals(servletname)) {
                pos = i;
                break;
            }
        }
        if(pos < 0) {
            // Looks like we are running as ROOT
            this.datasetpath = this.servletpath;
            this.servletpath = "";
        } else if(pos < (segments.length - 1)) {
            // Split the servlet path at pos
            this.servletpath = DapUtil.join(segments, "/", 0, pos + 1);
            this.datasetpath = DapUtil.join(segments, "/", pos + 1, segments.length);
        } //else looks like a proper servlet path was returned, so do nothing

        this.datasetpath = DapUtil.nullify(DapUtil.canonicalpath(this.datasetpath));

        // Now, construct #1 (scheme + authority)
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

        this.mode = null;
        if(DapUtil.nullify(this.datasetpath) == null) {
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
        this.datasetpath = DapUtil.relativize(this.datasetpath);
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
            buf.append(DapUtil.absolutize(this.datasetpath));
            this.url = buf.toString();
        }

        // Parse the query string into a Map
        if(querystring != null && querystring.length() > 0)

        {
            String[] pieces = querystring.split("&");
            for(String piece : pieces) {
                String[] pair = piece.split("=");
                String name = Escape.urlDecode(pair[0]);
                name = name.toLowerCase(); // for consistent lookup
                String value = (pair.length == 2 ? Escape.urlDecode(pair[1]) : "");
                queries.put(name, value);
            }
        }

        DapLog.debug("DapRequest: resourcedir=" +

                getResourcePath()

        );
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


}



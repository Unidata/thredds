/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENCE file for more information. */

package dap4.servlet;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.core.util.ResponseFormat;
import dap4.dap4lib.DapLog;
import dap4.dap4lib.RequestMode;
import dap4.dap4lib.XURI;
import ucar.httpservices.HTTPUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * User requests get cached here so that downstream code can access
 * the details of the request information.
 * <p>
 * Modified by Heimbigner for DAP4.
 *
 * @author Nathan Potter
 * @author Dennis Heimbigner
 */

public class DapRequest
{
    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static public final String WEBINFPATH = "/WEB-INF";
    static public final String RESOURCEDIRNAME = "resources";

    static public final String CONSTRAINTTAG = "dap4.ce";

    //////////////////////////////////////////////////
    // Instance variables

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected String url = null;  // without any query  and as with any modified dataset path
    protected String querystring = null;
    protected String server = null; // scheme + host + port
    protected String controllerpath = null; // scheme + host + port + prefix of path thru servlet Notes
    protected String datasetpath = null; // past controller path; char(0) != '/'

    protected RequestMode mode = null; // .dmr, .dap, or .dsr
    protected ResponseFormat format = null; // e.g. .xml when given .dmr.xml

    protected Map<String, String> queries = new HashMap<String, String>();
    protected DapController controller = null;
    protected ServletContext servletcontext = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public DapRequest(DapController controller,
                      HttpServletRequest request,
                      HttpServletResponse response)
            throws DapException
    {
        this.controller = controller;
        this.request = request;
        this.response = response;
        this.servletcontext = request.getServletContext();
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
     * <p>
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
     * 3. The return type: depending on the last extension (e.g. ".txt").
     * 4. The requested value: depending on the next to last extension (e.g. ".dap").
     * 5. The suffix path specifying the actual dataset: datasetpath
     * with return and request type extensions removed.
     * 6. The url path = servletpath + datasetpath.
     * 7. The query part.
     */

    protected void
    parse()
            throws IOException
    {
        this.url = request.getRequestURL().toString(); // does not include query
        this.querystring = request.getQueryString();    // raw (undecoded)
        // When using Spring Mock, the query is part of the parameters
        if(this.controller.TESTING && this.querystring == null) {
            String param = request.getParameter(CONSTRAINTTAG); // raw?
            if(param != null) {
                StringBuilder buf = new StringBuilder();
                buf.append(CONSTRAINTTAG);
                buf.append('=');
                buf.append(param);
                this.querystring = buf.toString();
            }
        }
        XURI xuri;
        try {
            xuri = new XURI(this.url).parseQuery(this.querystring);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
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

        // There appears to be some inconsistency in how the url path is divided up
        // depending on if this is a spring controller vs a raw servlet.
        // Try to canonicalize so that context path always ends with id
        // and servletpath does not start with it.
        String id = controller.getServletID();
        String sp = DapUtil.canonicalpath(request.getServletPath());
        String cp = DapUtil.canonicalpath(request.getContextPath());
        if(!cp.endsWith(id)) // probably spring ; contextpath does not ends with id
            cp = cp + "/" + id;
        buf.append(cp);
        this.controllerpath = buf.toString();
        sp = HTTPUtil.relpath(sp);
        if(sp.startsWith(id)) // probably spring also
            sp = sp.substring(id.length());

        this.datasetpath = HTTPUtil.relpath(sp);
        this.datasetpath = DapUtil.nullify(this.datasetpath);

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

        // Parse the query string into a Map
        if(querystring != null && querystring.length() > 0)
            this.queries = xuri.getQueryFields();

        if(DEBUG) {
            DapLog.debug("DapRequest: controllerpath =" + this.controllerpath);
            DapLog.debug("DapRequest: extension=" + (this.mode == null ? "null" : this.mode.extension()));
            DapLog.debug("DapRequest: datasetpath=" + this.datasetpath);
        }
    }

    //////////////////////////////////////////////////
    // Accessor(s)

    public ServletContext getContext()
    {
        return this.servletcontext;
    }

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

    public String getServer()
    {
        return this.server;
    }

    public String getControllerPath()
    {
        return this.controllerpath;
    }

    public String getURLPath()
    {
        return this.controllerpath + (this.datasetpath == null ? "" : this.datasetpath);
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

    public String getResourcePath()
            throws IOException
    {
        return getResourcePath(getDatasetPath());
    }

    public String getResourcePath(String relpath)
            throws IOException
    {
        return controller.getResourcePath(this, relpath);
    }

    public String getDatasetPath()
    {
        return this.datasetpath;
    }
}


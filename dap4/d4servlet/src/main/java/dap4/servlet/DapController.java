/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.servlet;

import dap4.core.ce.CEConstraint;
import dap4.core.data.DSP;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.ErrorResponse;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.core.util.ResponseFormat;
import dap4.dap4lib.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

abstract public class DapController extends HttpServlet
{
    // Provide a way for test programs to pass info into the controller
    static public boolean TESTING = false;

    //////////////////////////////////////////////////
    // Constants

    static public final boolean DEBUG = false;

    static public final boolean PARSEDEBUG = false;

    static protected final String BIG_ENDIAN = "Big-Endian";
    static protected final String LITTLE_ENDIAN = "Little-Endian";

    // Is this machine big endian?
    static protected boolean IS_BIG_ENDIAN = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);

    static protected final String DMREXT = ".dmr";
    static protected final String DATAEXT = ".dap";
    static protected final String DSREXT = ".dsr";
    static protected final String[] ENDINGS = {DMREXT, DATAEXT, DSREXT};

    static protected final String FAVICON = "favicon.ico"; // relative to resource dir

    static public final long DEFAULTBINARYWRITELIMIT = 100 * 1000000; // in bytes

    //////////////////////////////////////////////////
    // static variables

    // We need a singleton instance of a DapCache in order
    // To avoid re-opening the same NetcdfFile instance.
    // Assume:
    // 1. This is subclassed only once and that class will fill in
    //    this DapCache instance.

    static protected DapCache cache = null;

    //////////////////////////////////////////////////
    // Static accessors

    static protected void
    setCache(DapCache cache)
    {
        DapController.cache = cache;
    }

    static protected DapCache
    getCache()
    {
        return DapController.cache;
    }

    //////////////////////////////////////////////////
    // Instance variables

    transient protected DapContext dapcxt = new DapContext();

    protected boolean compress = true;

    transient protected ByteOrder byteorder = ByteOrder.nativeOrder();

    transient protected DapDSR dsrbuilder = new DapDSR();


    //////////////////////////////////////////////////
    // ServletContextAware

    transient protected ServletContext servletcontext = null;

    public void setServletContext(ServletContext servletContext)
    {
        this.servletcontext = servletcontext;
    }

    //////////////////////////////////////////////////
    // Constructor(s)

    public DapController()
    {
    }

    //////////////////////////////////////////////////////////
    // Abstract methods

    /**
     * Process a favicon request.
     *
     * @param drq The merged dap state
     */

    abstract protected void doFavicon(DapRequest drq, String icopath, DapContext cxt) throws IOException;

    /**
     * Process a capabilities request.
     * Currently, does nothing (but see D4TSServlet.doCapabilities).
     *
     * @param drq The merged dap state
     */

    abstract protected void doCapabilities(DapRequest drq, DapContext cxt) throws IOException;

    /**
     * Convert a URL path into an absolute file path
     * Note that it is assumed than any leading servlet prefix has been removed.
     *
     * @param drq      for context
     * @param location suffix of url path
     * @return
     * @throws IOException
     */

    abstract public String getResourcePath(DapRequest drq, String location) throws IOException;

    /**
     * Get the maximum # of bytes per request
     *
     * @return size
     */
    abstract public long getBinaryWriteLimit();

    /**
     * Get the servlet name (with no leading or trailing slashes)
     *
     * @return name
     */
    abstract public String getServletID();

    /**
     * Initialize servlet
     */
    abstract public void initialize();

    //////////////////////////////////////////////////////////

    public void init()
            throws ServletException
    {
        org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
        logServerStartup.info(getClass().getName() + " initialization start");
        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
            initialize();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    //////////////////////////////////////////////////////////
    // Accessors

    public DapController setControllerPath(String controllerpath)
    {
        this.dapcxt.put("controllerpath", DapUtil.canonjoin("", controllerpath));
        return this;
    }

    public DapContext getContext()
    {
        return this.dapcxt;
    }

    //////////////////////////////////////////////////////////
    // Primary Controller Entry Point

    public void
    handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws IOException
    {
        DapLog.debug("doGet(): User-Agent = " + req.getHeader("User-Agent"));
        if(TESTING) {
            String resourcedir = (String) req.getAttribute("RESOURCEDIR");
            this.dapcxt.put("RESOURCEDIR", resourcedir);
        }
        DapRequest drq = getRequestState(req, res);
        String url = drq.getOriginalURL();
        StringBuilder info = new StringBuilder("doGet():");
        info.append(" dataset = ");
        info.append(" url = ");
        info.append(url);
        if(DEBUG) {
            System.err.println("DAP4 Servlet: processing url: " + drq.getOriginalURL());
        }
        assert (this.dapcxt != null);
        this.dapcxt.put(HttpServletRequest.class, req);
        this.dapcxt.put(HttpServletResponse.class, res);
        if(url.endsWith(FAVICON)) {
            doFavicon(drq, FAVICON, this.dapcxt);
            return;
        }

        String datasetpath = DapUtil.nullify(drq.getDataset());
        try {
            if(datasetpath == null) {
                // This is the case where a request was made without a dataset;
                // According to the spec, I think we should return the
                // services/capabilities document
                doCapabilities(drq, this.dapcxt);
            } else {
                RequestMode mode = drq.getMode();
                if(mode == null)
                    throw new DapException("Unrecognized request extension")
                            .setCode(HttpServletResponse.SC_BAD_REQUEST);
                switch (mode) {
                case DMR:
                    doDMR(drq, this.dapcxt);
                    break;
                case DAP:
                    doData(drq, this.dapcxt);
                    break;
                case DSR:
                    doDSR(drq, this.dapcxt);
                    break;
                default:
                    throw new DapException("Unrecognized request extension")
                            .setCode(HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            int code = HttpServletResponse.SC_BAD_REQUEST;
            if(t instanceof DapException) {
                DapException e = (DapException) t;
                code = e.getCode();
                if(code <= 0)
                    code = DapCodes.SC_BAD_REQUEST;
                e.setCode(code);
            } else if(t instanceof FileNotFoundException)
                code = DapCodes.SC_NOT_FOUND;
            else if(t instanceof UnsupportedOperationException)
                code = DapCodes.SC_FORBIDDEN;
            else if(t instanceof MalformedURLException)
                code = DapCodes.SC_NOT_FOUND;
            else if(t instanceof IOException)
                code = DapCodes.SC_BAD_REQUEST;
            else
                code = DapCodes.SC_INTERNAL_SERVER_ERROR;
            senderror(drq, code, t);
        }//catch
    }

    //////////////////////////////////////////////////////////
    // Extension processors

    /**
     * Process a DSR request.
     */

    protected void
    doDSR(DapRequest drq, DapContext cxt)
            throws IOException
    {
        try {
            String dsr = dsrbuilder.generate(drq.getURL());
            OutputStream out = drq.getOutputStream();
            addCommonHeaders(drq);// Add relevant headers
            // Wrap the outputstream with a Chunk writer
            ChunkWriter cw = new ChunkWriter(out, RequestMode.DSR, this.byteorder);
            cw.writeDSR(dsr);
            cw.close();
        } catch (IOException ioe) {
            throw new DapException("DSR generation error", ioe)
                    .setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Process a DMR request.
     *
     * @param drq The merged dap state
     */

    protected void
    doDMR(DapRequest drq, DapContext cxt)
            throws IOException
    {
        // Convert the url to an absolute path
        String realpath = getResourcePath(drq, drq.getDatasetPath());

        DSP dsp = DapCache.open(realpath, cxt);
        DapDataset dmr = dsp.getDMR();

        // Process any constraint view
        CEConstraint ce = null;
        String sce = drq.queryLookup(DapProtocol.CONSTRAINTTAG);
        ce = CEConstraint.compile(sce, dmr);

        // Provide a PrintWriter for capturing the DMR.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Get the DMR as a string
        DMRPrinter dapprinter = new DMRPrinter(dmr,ce,pw);
        dapprinter.print();
        pw.close();
        sw.close();

        String sdmr = sw.toString();
        if(DEBUG)
            System.err.println("Sending: DMR:\n" + sdmr);

        addCommonHeaders(drq);// Add relevant headers

        // Wrap the outputstream with a Chunk writer
        OutputStream out = drq.getOutputStream();
        ChunkWriter cw = new ChunkWriter(out, RequestMode.DMR, this.byteorder);
        cw.writeDMR(sdmr);
        cw.close();
    }

    /**
     * Process a DataDMR request.
     * Note that if this throws an exception,
     * then it has not yet started to output
     * a response. It a response had been initiated,
     * then the exception would produce an error chunk.
     *
     * @param drq The merged dap state
     */

    protected void
    doData(DapRequest drq, DapContext cxt)
            throws IOException
    {
        // Convert the url to an absolute path
        String realpath = getResourcePath(drq, drq.getDatasetPath());

        DSP dsp = DapCache.open(realpath, cxt);
        if(dsp == null)
            throw new IOException("No such file: " + drq.getResourcePath());
        DapDataset dmr = dsp.getDMR();

        // Process any constraint
        CEConstraint ce = null;
        String sce = drq.queryLookup(DapProtocol.CONSTRAINTTAG);
        ce = CEConstraint.compile(sce, dmr);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Get the DMR as a string
        DMRPrinter dapprinter = new DMRPrinter(dmr,ce,pw);
        dapprinter.print();
        pw.close();
        sw.close();

        String sdmr = sw.toString();
        if(DEBUG)
            System.err.println("Sending: Data DMR:\n" + sdmr);

        // Wrap the outputstream with a Chunk writer
        OutputStream out = drq.getOutputStream();
        ChunkWriter cw = new ChunkWriter(out, RequestMode.DAP, this.byteorder);
        cw.setWriteLimit(getBinaryWriteLimit());
        cw.writeDMR(sdmr);
        cw.flush();

        addCommonHeaders(drq);

        // Dump the databuffer part
        switch (drq.getFormat()) {
        case TEXT:
        case XML:
        case HTML:
            throw new IOException("Unsupported return format: " + drq.getFormat());
            /*
            sw = new StringWriter();
            DAPPrint dp = new DAPPrint(sw);
            dp.print(dsp.getDataset(), ce);
            break;
                */
        case NONE:
        default:
            DapSerializer writer = new DapSerializer(dsp, ce, cw, byteorder);
            writer.write(dsp.getDMR());
            cw.flush();
            cw.close();
            break;
        }
    }

    //////////////////////////////////////////////////////////
    // Utility Methods

    protected void
    addCommonHeaders(DapRequest drq)
            throws IOException
    {
        // Add relevant headers
        ResponseFormat format = drq.getFormat();
        if(format == null)
            format = ResponseFormat.NONE;
        DapProtocol.ContentType contentheaders = DapProtocol.contenttypes.get(drq.getMode());
        String header = contentheaders.getFormat(format);
        if(header != null) {
            header = header + "; charset=utf-8";
            drq.setResponseHeader("Content-Type", header);
        } else
            DapLog.error("Cannot determine response Content-Type");

        // Not sure what this should be yet
        //setHeader("Content-Description","?");

        // Again, not sure what value to use
        //setHeader("Content-Disposition","?");

        //not legal drq.setResponseHeader("Content-Encoding", IS_BIG_ENDIAN ? BIG_ENDIAN : LITTLE_ENDIAN);
    }

    /**
     * Merge the servlet inputs into a single object
     * for easier transport as well as adding value.
     *
     * @param rq  A Servlet request object
     * @param rsp A Servlet response object
     * @return the union of the
     * servlet request and servlet response arguments
     * from the servlet engine.
     */

    protected DapRequest
    getRequestState(HttpServletRequest rq, HttpServletResponse rsp)
            throws IOException
    {
        return new DapRequest(this, rq, rsp);
    }

    //////////////////////////////////////////////////////////
    // Error Methods

    /* Note that these error returns are assumed to be before
       any DAP4 response has been generated. So they will
       set the header return code and an Error Response as body.
       Error chunks are handled elsewhere.
     */

    /**
     * Generate an error based on the parameters
     *
     * @param drq      DapRequest
     * @param httpcode 0=>no code specified
     * @param t        exception that caused the error; may be null
     * @throws IOException
     */
    protected void
    senderror(DapRequest drq, int httpcode, Throwable t)
            throws IOException
    {
        if(httpcode == 0) httpcode = HttpServletResponse.SC_BAD_REQUEST;
        ErrorResponse err = new ErrorResponse();
        err.setCode(httpcode);
        if(t == null) {
            err.setMessage("Servlet error: " + drq.getURL());
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter p = new PrintWriter(sw);
            t.printStackTrace(p);
            p.close();
            sw.close();
            err.setMessage(sw.toString());
        }
        err.setContext(drq.getURL());
        String errormsg = err.buildXML();
        drq.getResponse().sendError(httpcode, errormsg);
    }

}



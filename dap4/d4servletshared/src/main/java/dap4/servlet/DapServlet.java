/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.servlet;

import dap4.ce.*;
import dap4.ce.parser.*;
import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;
import dap4.dap4shared.*;
import dap4.servlet.*;
import net.jcip.annotations.NotThreadSafe;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

@NotThreadSafe
abstract public class DapServlet extends javax.servlet.http.HttpServlet
{

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

    static protected final String FAVICON = "favicon.ico"; // relative to resource dir

    //////////////////////////////////////////////////
    // Instance variables

    protected boolean compress = true;

    // Define the path prefix for finding a dataset
    // given a url file spec

    transient protected URLMap urlmap = new URLMapDefault();

    transient protected ByteOrder byteorder = ByteOrder.nativeOrder();

    transient protected DapDSR dsrbuilder = new DapDSR();

    transient protected ServletInfo svcinfo;

    //////////////////////////////////////////////////
    // Constructor(s)

    public DapServlet()
    {
    }

    //////////////////////////////////////////////////////////
    // Accessors

    public ServletInfo
    getInfo()
    {
        return this.svcinfo;
    }

    //////////////////////////////////////////////////////////
    // Non-doXXX Servlet Methods

    @Override
    public void init()
            throws ServletException
    {
        super.init();
        DapLog.info(getClass().getName() + " initialization start");
        //String initParam  = config.getInitParameter("InitParam");
        try {
            this.svcinfo = new ServletInfo(this);
        } catch (Exception ioe) {
            throw new ServletException(ioe);
        }
        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
        }
    }

    //////////////////////////////////////////////////////////
    // doXXX Methods

    public void  // Make public so TestServlet can access
    doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        DapLog.debug("doGet(): User-Agent = " + req.getHeader("User-Agent"));
        String url = req.getRequestURL().toString();

        // This may be a tomcat under intellij thing,
        // but for some reason, tomcat invokes this servlet
        // both with and without the d4ts path
        // E.g. it gets invokes with url=http://localhost:8080/
        // and with url=http://localhost:8080/d4ts.
        //
        // In any case, any request whose path does not start with svcinfo.servletname
        // will be ignored.

        try {
            URI uri = new URI(url);
            if(!uri.getPath().startsWith("/" + svcinfo.getServletname()))
                return;
        } catch (URISyntaxException use) {
            return;
        }

        synchronized (this) {
            this.svcinfo.setServer(url);
        }
        String query = req.getQueryString();
        DapLog.debug("doGet(): url = " + url + (query == null || query.length() == 0 ? "" : "?" + query));

        DapRequest drq = getRequestState(svcinfo, req, resp);

        if(DEBUG) {
            System.err.println("DAP4 Servlet: processing url: " + drq.getOriginalURL());
        }

        if(url.endsWith(FAVICON)) {
            doFavicon(drq);
            return;
        }

        String datasetpath = DapUtil.nullify(drq.getDataset());
        try {
            if(datasetpath == null) {
                // This is the case where a request was made without a dataset;
                // According to the spec, I think we should return the
                // services/capabilities document
                doCapabilities(drq);
            } else {
                RequestMode mode = drq.getMode();
                if(mode == null)
                    throw new DapException("Unrecognized request extension")
                            .setCode(HttpServletResponse.SC_BAD_REQUEST);
                switch (mode) {
                case DMR:
                    doDMR(drq);
                    break;
                case DAP:
                    doData(drq);
                    break;
                case DSR:
                    doDSR(drq);
                    break;
                default:
                    throw new DapException("Unrecognized request extension")
                            .setCode(HttpServletResponse.SC_BAD_REQUEST);
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
            int code = HttpServletResponse.SC_BAD_REQUEST;
            if(t instanceof FileNotFoundException)
                code = HttpServletResponse.SC_NOT_FOUND;
            else if(t instanceof UnsupportedOperationException)
                code = HttpServletResponse.SC_FORBIDDEN;
            else if(t instanceof MalformedURLException)
                code = HttpServletResponse.SC_NOT_FOUND;
            else if(t instanceof IOException) {
                code = HttpServletResponse.SC_BAD_REQUEST;
            } else if(t instanceof DapException) {
                code = ((DapException) t).getCode();
                if(code <= 0) // not http code
                    code = HttpServletResponse.SC_BAD_REQUEST;
            } else
                code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            senderror(drq, code, t);
        }//catch
    } // doGet

    //////////////////////////////////////////////////////////
    // Capabilities processors

    /**
     * Process a capabilities request.
     * Currently, does nothing (but see D4TSServlet.doCapabilities).
     *
     * @param drq The merged dap state
     */

    protected void
    doCapabilities(DapRequest drq)
            throws IOException
    {
        throw new IOException("Unsupported operation: get capabilities");
    }

    //////////////////////////////////////////////////////////
    // Extension processors

    /**
     * Process a favicon request.
     *
     * @param drq The merged dap state
     */

    protected void
    doFavicon(DapRequest drq)
            throws IOException
    {
        String favfile = getResourceFile(drq, false);
        if(favfile != null) {
            try (FileInputStream fav = new FileInputStream(favfile);) {
                byte[] content = DapUtil.readbinaryfile(fav);
                OutputStream out = drq.getOutputStream();
                out.write(content);
            }
        }
    }

    /**
     * Process a DSR request.
     *
     * @param drq The merged dap state
     */

    protected void
    doDSR(DapRequest drq)
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
    doDMR(DapRequest drq)
            throws IOException
    {

        String datasetpath = getResourceFile(drq, false);
        DSP dsp = DapCache.open(datasetpath);
        DapDataset dmr = dsp.getDMR();

        // Process any constraint view
        CEConstraint ce = null;
        String sce = drq.queryLookup(DapProtocol.CONSTRAINTTAG);
        ce = buildconstraint(drq, sce, dmr);

        // Provide a PrintWriter for capturing the DMR.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Get the DMR as a string
        DMRPrint dapprinter = new DMRPrint(pw);
        dapprinter.printDMR(ce, dmr);
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
    doData(DapRequest drq)
            throws IOException
    {
        String datasetpath = getResourceFile(drq, false); // dataset path is relative to resource path
        DSP dsp = DapCache.open(datasetpath);
        if(dsp == null)
            throw new IOException("No such file: " + datasetpath);
        DapDataset dmr = dsp.getDMR();

        // Process any constraint
        CEConstraint ce = null;
        String sce = drq.queryLookup(DapProtocol.CONSTRAINTTAG);
        ce = buildconstraint(drq, sce, dmr);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Get the DMR as a string
        DMRPrint dapprinter = new DMRPrint(pw);
        dapprinter.printDMR(ce, dmr);
        pw.close();
        sw.close();

        String sdmr = sw.toString();
        if(DEBUG)
            System.err.println("Sending: Data DMR:\n" + sdmr);

        // Wrap the outputstream with a Chunk writer
        OutputStream out = drq.getOutputStream();
        ChunkWriter cw = new ChunkWriter(out, RequestMode.DAP, this.byteorder);
        cw.writeDMR(sdmr);
        cw.flush();

        addCommonHeaders(drq);

        // Dump the databuffer part
        DapSerializer writer = new DapSerializer(dsp, ce, cw, byteorder);
        writer.write(dsp.getDMR());
        cw.flush();
        cw.close();
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
     * Extract the servlet specific info
     *
     * @param svc A Servlet object
     * @return the extracted servlet info
     */
/*
    protected ServletInfo
    getRequestState(HttpServlet svc)
        throws IOException
    {
        return new ServletInfo(svc);
    }
    */

    /**
     * Merge the servlet inputs into a single object
     * for easier transport as well as adding value.
     *
     * @param rq  A Servlet request object
     * @param rsp A Servlet response object
     * @return the union of the
     *         servlet request and servlet response arguments
     *         from the servlet engine.
     */

    protected DapRequest
    getRequestState(ServletInfo info, HttpServletRequest rq, HttpServletResponse rsp)
            throws IOException
    {
        return new DapRequest(info, rq, rsp);
    }

    /**
     * Return the full file path with
     * respect to the resource directory.
     *
     * @param drq   The request info
     * @param isdir do we want a file or a directory
     */

    protected String
    getResourceFile(DapRequest drq, boolean isdir)
            throws IOException
    {
        // Using context information, we need to
        // construct a file path to the specified dataset

        String urlpath = drq.getURLPath();
        URLMap.Result mappath = null;
        String datasetfilepath = null;
        mappath = urlmap.mapURL(urlpath);
        if(mappath == null)
            throw new IOException("Unknown dataset: " + drq.getOriginalURL());

        // Locate the dataset file
        datasetfilepath = mappath.prefix + mappath.suffix;
        // See if it really exists and is readable and of proper type
        File dataset = new File(datasetfilepath);
        if(!dataset.exists())
            throw new DapException("Requested file does not exist: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_NOT_FOUND);

        if(!dataset.canRead())
            throw new DapException("Requested file not readable: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_FORBIDDEN);

        if(isdir && !dataset.isDirectory())
            throw new DapException("Requested file not a directory: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_FORBIDDEN);
        return datasetfilepath;
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
        err.setMessage(t == null ? "Servlet Error" : t.getMessage());
        err.setContext(drq.getURL());
        String errormsg = err.buildXML();
        drq.getResponse().sendError(httpcode, errormsg);
    }


    /**
     * If the request has a constraint, then parse it
     * else use the universal constraint
     *
     * @param drq
     * @param sce string of the constraint
     * @return parsed constraint
     */
    protected CEConstraint
    buildconstraint(DapRequest drq, String sce, DapDataset dmr)
            throws IOException
    {
        // Process any constraint
        if(sce == null || sce.length() == 0)
            return CEConstraint.getUniversal(dmr);
        CEParser ceparser = new CEParser(dmr);
        if(PARSEDEBUG)
            ceparser.setDebugLevel(1);
        if(DEBUG) {
            System.err.println("Dap4Servlet: parsing constraint: |" + sce + "|");
        }
        boolean parseok = ceparser.parse(sce);
        if(!parseok)
            throw new IOException("Constraint Parse failed: " + sce);
        CEAST root = ceparser.getConstraint();
        CECompiler compiler = new CECompiler();
        CEConstraint ce = compiler.compile(dmr, root);
        ce.expand();
        ce.finish();
        return ce;
    }

}


// Obtain the dataset as a NetcdfFile. Wrap in a DSP.
//NetcdfFile ncd = DatasetHandler.getNetcdfFile(req.getRequest(), req.getResponse(), datasetpath);

/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.d4ts;

import dap4.core.data.DSPRegistry;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.DapCodes;
import dap4.dap4lib.DapLog;
import dap4.dap4lib.FileDSP;
import dap4.dap4lib.netcdf.Nc4DSP;
import dap4.servlet.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class D4TSServlet extends DapController
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    static protected final String RESOURCEPATH = "WEB-INF/resources/testfiles";

    //////////////////////////////////////////////////
    // Type Decls

    static class D4TSFactory extends DSPFactory
    {

        public D4TSFactory()
        {
            // Register known DSP classes: order is important
            // in event that two or more dsps can match a given file
            // (e.q. FileDSP vs Nc4DSP).
            // Only used in server
            DapCache.dspregistry.register(Nc4DSP.class, DSPRegistry.LAST);
            DapCache.dspregistry.register(SynDSP.class, DSPRegistry.LAST);
            DapCache.dspregistry.register(FileDSP.class, DSPRegistry.LAST);
        }

    }

    //////////////////////////////////////////////////

    static {
        DapCache.setFactory(new D4TSFactory());
    }

    //////////////////////////////////////////////////
    // Instance variables

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4TSServlet()
    {
        super();
    }

    @Override
    public void initialize()
    {
        DapLog.info("Initializing d4ts servlet");
        ServletContext svccxt = getServletContext();
        // Construct the resource dir path
        String svcroot = svccxt.getRealPath("");
        String resourcedir = DapUtil.canonjoin(svcroot, RESOURCEPATH);
        this.dapcxt.put("RESOURCEDIR",resourcedir);
    }

    //////////////////////////////////////////////////

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException,
            java.io.IOException
    {
        super.handleRequest(req, resp);
    }

    //////////////////////////////////////////////////////////
    // Capabilities processors

    @Override
    protected void
    doFavicon(DapRequest drq, String icopath, DapContext cxt)
            throws IOException
    {
        String favfile = getResourcePath(drq, icopath);
        if(favfile != null) {
            try (FileInputStream fav = new FileInputStream(favfile);) {
                byte[] content = DapUtil.readbinaryfile(fav);
                OutputStream out = drq.getOutputStream();
                out.write(content);
            }
        }
    }

    @Override
    protected void
    doCapabilities(DapRequest drq, DapContext cxt)
            throws IOException
    {
        addCommonHeaders(drq);

        // Figure out the directory containing
        // the files to display.
        String dir = getResourcePath(drq, "");
        if(dir == null)
            throw new DapException("Cannot locate resources directory");

        // Generate the front page
        FrontPage front = new FrontPage(dir, drq);
        String frontpage = front.buildPage();

        if(frontpage == null)
            throw new DapException("Cannot create front page")
                    .setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // // Convert to UTF-8 and then to byte[]
        byte[] frontpage8 = DapUtil.extract(DapUtil.UTF8.encode(frontpage));
        OutputStream out = drq.getOutputStream();
        out.write(frontpage8);

    }

    @Override
    public String
    getResourcePath(DapRequest drq, String location)
            throws IOException
    {
        String prefix = (String) this.dapcxt.get("RESOURCEDIR");
        if(prefix == null)
            throw new DapException("Cannot find location resource: " + location)
                    .setCode(DapCodes.SC_NOT_FOUND);
        location = DapUtil.canonicalpath(location);
        String datasetfilepath = DapUtil.canonjoin(prefix, location);
        // See if it really exists and is readable and of proper type
        File dataset = new File(datasetfilepath);
        if(!dataset.exists()) {
            String msg = String.format("Requested file does not exist: prefix=%s location=%s datasetfilepath=%s",
                    prefix,location,datasetfilepath);
            throw new DapException(msg)
                    .setCode(HttpServletResponse.SC_NOT_FOUND);
        }
        if(!dataset.canRead())
            throw new DapException("Requested file not readable: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_FORBIDDEN);
        return datasetfilepath;
    }

    @Override
    public long getBinaryWriteLimit()
    {
        return DEFAULTBINARYWRITELIMIT;
    }

    @Override
    public String
    getServletID()
    {
        return "d4ts";
    }

    @Override
    public ServletContext
    getServletContext()
    {
        if(this.servletcontext == null)
            this.servletcontext = super.getServletContext();
        assert (this.servletcontext != null);
        return this.servletcontext;
    }
}

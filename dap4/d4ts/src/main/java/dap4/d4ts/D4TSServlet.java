/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.d4ts;

import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.DapCodes;
import dap4.dap4lib.DapLog;
import dap4.dap4lib.FileDSP;
import dap4.dap4lib.netcdf.Nc4DSP;
import dap4.servlet.*;
import ucar.nc2.NetcdfFile;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static dap4.d4ts.FrontPage.Root;


public class D4TSServlet extends DapController
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    static protected final String RESOURCEPATH = "WEB-INF/resources";

    //////////////////////////////////////////////////
    // Type Decls

    //////////////////////////////////////////////////
    // Instance variables

    protected List<Root> defaultroots = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4TSServlet()
    {
        super();
    }

    @Override
    public void initialize()
    {
        super.initialize();
        DapLog.info("Initializing d4ts servlet");
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
    doFavicon(String icopath, DapContext cxt)
            throws IOException
    {
        DapRequest drq = (DapRequest)cxt.get(DapRequest.class);
        String prefix = getResourceRoot(drq);
        String favfile = DapUtil.canonjoin(prefix, icopath);
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

        // Generate the front page
        FrontPage front = getFrontPage(drq, cxt);
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
    public NetcdfFile
    getNetcdfFile(DapRequest drq, String location)
            throws DapException
    {
        NetcdfFile ncfile = null;
        String prefix = getResourceRoot(drq);
        location = DapUtil.canonicalpath(location);
        String datasetfilepath = DapUtil.canonjoin(prefix, location);
        // See if it really exists and is readable and of proper type
        File dataset = new File(datasetfilepath);
        if(!dataset.exists()) {
            String msg = String.format("Requested file does not exist: prefix=%s location=%s datasetfilepath=%s",
                    prefix, location, datasetfilepath);
            throw new DapException(msg)
                    .setCode(HttpServletResponse.SC_NOT_FOUND);
        }
        if(!dataset.canRead())
            throw new DapException("Requested file not readable: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_FORBIDDEN);
        try {
            ncfile = NetcdfFile.open(datasetfilepath);
        } catch (IOException ioe) {
            ncfile = null;
        }
        return ncfile;
    }

    @Override
    public String
    getResourceRoot(DapRequest drq)
            throws DapException
    {
        String rootpath;
        rootpath = drq.getResourceRoot();
        File f = (rootpath == null ? null : new File(rootpath));
        if(f == null || !f.exists() || !f.canRead() || !f.isDirectory())
            throw new DapException("Resource root path not found")
                    .setCode(DapCodes.SC_NOT_FOUND);
        return rootpath;
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

    /**
     * Isolate front page builder so we can override if desired for testing.
     *
     * @param drq
     * @param cxt
     * @return  FrontPage object
     */
    protected FrontPage
    getFrontPage(DapRequest drq, DapContext cxt)
            throws DapException
    {
        if(this.defaultroots == null) {
            // Figure out the directory containing
            // the files to display.
            String pageroot;
            pageroot = getResourceRoot(drq);
            if(pageroot == null)
                throw new DapException("Cannot locate resources directory");
            this.defaultroots = new ArrayList<>();
            this.defaultroots.add(
                    new Root("testfiles",pageroot));
        }
        return new FrontPage(this.defaultroots, drq);
    }

}

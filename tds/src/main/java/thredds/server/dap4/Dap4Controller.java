/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.server.dap4;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.servlet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.server.dataset.TdsRequestedDataset;
import thredds.servlet.DatasetHandler;
import thredds.servlet.ThreddsConfig;
import thredds.util.TdsPathUtils;
import ucar.nc2.constants.CDM;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;

@Controller
@RequestMapping("/dap4")
public class Dap4Controller extends DapController
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    // NetcdfDataset enhancement to use: need only coord systems
    //static Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    //////////////////////////////////////////////////
    // Type Decls

    static class Dap4Factory extends DSPFactory
    {

        public Dap4Factory()
        {
            // Register known DSP classes: order is important.
            registerDSP(ThreddsDSP.class, true);
        }

    }

    static {
        DapCache.setFactory(new Dap4Factory());
    }

    //////////////////////////////////////////////////
    // Spring Elements

    @RequestMapping("**")
    public void handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws IOException
    {
        super.handleRequest(req, res);
    }

    //////////////////////////////////////////////////
    // Constructor(s)

    public Dap4Controller()
    {
        super("dap4");
    }

    //////////////////////////////////////////////////////////

    @Override
    protected void
    doFavicon(DapRequest drq, String icopath)
            throws IOException
    {
        throw new UnsupportedOperationException("Favicon");
    }

    @Override
    protected void
    doCapabilities(DapRequest drq)
            throws IOException
    {
        addCommonHeaders(drq);
        OutputStream out = drq.getOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, DapUtil.UTF8));
        pw.println("Capabilities page not supported");
        pw.flush();
    }

    @Override
    protected long
    getBinaryWriteLimit()
    {
        return DEFAULTBINARYWRITELIMIT;
    }

    @Override
    protected String
    getResourcePath(DapRequest drq, String relpath)
            throws IOException
    {
        // Using context information, we need to
        // construct a file path to the specified dataset
        URL realpathurl = servletcontext.getResource(relpath);
        String realpath = null;
        if(realpathurl.getProtocol().equalsIgnoreCase("file"))
            realpath = realpathurl.getPath();
        else
            throw new DapException("Requested file not found " + realpathurl)
                                .setCode(HttpServletResponse.SC_NOT_FOUND);

        // See if it really exists and is readable and of proper type
        File dataset = new File(realpath);
        if(!dataset.exists())
            throw new DapException("Requested file does not exist: " + realpath)
                    .setCode(HttpServletResponse.SC_NOT_FOUND);

        if(!dataset.canRead())
            throw new DapException("Requested file not readable: " + realpath)
                    .setCode(HttpServletResponse.SC_FORBIDDEN);
        return realpath;
    }

}



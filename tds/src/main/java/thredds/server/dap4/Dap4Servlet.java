/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.server.dap4;

import dap4.ce.CEConstraint;
import dap4.ce.parser.*;
import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;
import dap4.dap4shared.*;
import dap4.servlet.*;
import org.xml.sax.SAXException;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DatasetHandler;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class Dap4Servlet extends DapServlet
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
    // Constructor(s)

    public Dap4Servlet()
    {
        super();
    }

    //////////////////////////////////////////////////////////

    @Override
    protected void
    doFavicon(DapRequest drq)
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
        PrintWriter pw = new PrintWriter(out);
        pw.println("Capabilities page not yet supported");
        pw.flush();
    }

    @Override
    protected String
    getResourcePath(DapRequest drq)
            throws IOException
    {
        // Using context information, we need to
        // construct a file path to the specified dataset
        String datasetpath = drq.getDataset();
        if(datasetpath.startsWith("/"))
            datasetpath = datasetpath.substring(1);
        datasetpath = DatasetHandler.getNetcdfFilePath(drq.getRequest(), datasetpath);
        return datasetpath;
    }

}



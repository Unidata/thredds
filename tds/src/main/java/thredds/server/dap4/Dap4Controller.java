/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.server.dap4;

import dap4.core.data.DSPRegistry;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.core.util.IndentWriter;
import dap4.dap4lib.*;
import dap4.servlet.DSPFactory;
import dap4.servlet.DapCache;
import dap4.servlet.DapController;
import dap4.servlet.DapRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.core.TdsRequestedDataset;
import ucar.nc2.NetcdfFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller
@RequestMapping("/dap4")
public class Dap4Controller extends DapController
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    static protected final ContentType CAPTYPE = new ContentType(RequestMode.CAPABILITIES, ResponseFormat.XML);


    // NetcdfDataset enhancement to use: need only coord systems
    //static Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    //////////////////////////////////////////////////
    // Type Decls

    static class Dap4Factory extends DSPFactory
    {

        public Dap4Factory()
        {
            // For TDS, we only need to register one DSP type: ThreddsDSP.
            // This is because we will always serve only NetcdfFile objects.
            // See D4TSServlet for a multiple registration case.
            DapCache.dspregistry.register(ThreddsDSP.class, DSPRegistry.LAST);
        }

    }

    static {
        DapCache.setFactory(new Dap4Factory());
    }

    //////////////////////////////////////////////////
    // Spring Elements

    @Autowired
    private ServletContext servletContext;

    @RequestMapping("**")
    public void handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws IOException
    {
        super.handleRequest(req, res);
    }

    /**
     * Initialize servlet/controller
     */
    @Override
    public void
    initialize()
    {
        super.initialize();
        try {
            // Always prefer Nc4Iosp over HDF5
            NetcdfFile.iospDeRegister(ucar.nc2.jni.netcdf.Nc4Iosp.class);
            NetcdfFile.registerIOProviderPreferred(ucar.nc2.jni.netcdf.Nc4Iosp.class,
                    ucar.nc2.iosp.hdf5.H5iosp.class
            );
        } catch (Exception e) {
            DapLog.warn("Cannot load ucar.nc2.jni.netcdf.Nc4Iosp");
        }
    }

    //////////////////////////////////////////////////
    // Constructor(s)

    public Dap4Controller()
    {
        super();
    }

    //////////////////////////////////////////////////////////

    @Override
    protected void
    doFavicon(String icopath, DapContext cxt)
            throws IOException
    {
        throw new UnsupportedOperationException("Favicon");
    }

    @Override
    protected void
    doCapabilities(DapRequest drq, DapContext cxt)
            throws IOException
    {
        addCommonHeaders(drq,CAPTYPE);
        OutputStream out = drq.getOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, DapUtil.UTF8));
        pw.println(generate());
        pw.flush();
    }

    @Override
    public long
    getBinaryWriteLimit()
    {
        return DEFAULTBINARYWRITELIMIT;
    }

    @Override
    public String
    getServletID()
    {
        return "dap4";
    }

    @Override
    public String
    getResourcePath(DapRequest drq, String location)
            throws DapException
    {
        String prefix = drq.getResourceRoot();
        String realpath;
        if(prefix != null) {
            realpath = DapUtil.canonjoin(prefix, location);
        } else
            realpath = TdsRequestedDataset.getLocationFromRequestPath(location);

        if(!TESTING) {
            if(!TdsRequestedDataset.resourceControlOk(drq.getRequest(), drq.getResponse(), realpath))
                throw new DapException("Not authorized: " + location)
                        .setCode(DapCodes.SC_FORBIDDEN);
        }
        File f = new File(realpath);
        if(!f.exists() || !f.canRead())
            throw new DapException("Not found: " + location)
                    .setCode(DapCodes.SC_NOT_FOUND);
        //ncfile = TdsRequestedDataset.getNetcdfFile(this.request, this.response, path);
        return realpath;
    }

    /**
     * generate the capabilities document (currently subset of DSR).
     * @return the capabilities page
     * @throws IOException
     */
    protected String generate()
               throws IOException
       {
           StringWriter sw = new StringWriter();
           IndentWriter printer = new IndentWriter(sw);
           printer.marginPrintln("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
           printer.marginPrintln("<Capabilities");
           printer.indent(2);
           printer.marginPrintln("xmlns=\"http://xml.opendap.org/ns/DAP/4.0/dataset-capabilities#\">");
           printer.outdent();
           printer.marginPrint("<DapVersion>");
           printer.print(DapProtocol.X_DAP_VERSION);
           printer.println("</DapVersion>");
           printer.marginPrint("<ServerSoftwareVersion>");
           printer.print(DapProtocol.X_DAP_SERVER);
           printer.println("</ServerSoftwareVersion>");

           printer.marginPrintln("<Service title=\"DAP4 Dataset Services\"");
           printer.indent(3);
           printer.marginPrintln("role=\"http://services.opendap.org/dap4/dataset-services\">");
           printer.outdent(3);
           printer.indent();
           printer.marginPrint("<link type=\"");
           printer.print(DapProtocol.defaultmimetype(RequestMode.DSR));
           printer.println("\">");
           printer.indent();
           printer.marginPrintln("<alt type=\"text/xml\"/>");
           printer.outdent();
           printer.marginPrintln("</link>");
           printer.marginPrintln("<link type=\"text/xml\">");
           printer.outdent();
           printer.marginPrintln("</Service>");

           printer.marginPrintln("<Service title=\"DAP4 Dataset Metadata\"");
           printer.indent(3);
           printer.marginPrintln("role=\"http://services.opendap.org/dap4/dataset-metadata\">");
           printer.outdent(3);
           printer.indent();
           printer.marginPrint("<link type=\"");
           printer.print(DapProtocol.defaultmimetype(RequestMode.DMR));
           printer.println("\">");
           printer.indent();
           printer.marginPrintln("<alt type=\"text/xml\"/>");
           printer.outdent();
           printer.marginPrintln("</link>");
           printer.marginPrintln("<link type=\"text/xml\">");
           printer.outdent();
           printer.marginPrintln("</Service>");

           printer.marginPrintln("<Service title=\"DAP4 Dataset Data\"");
           printer.indent(2);
           printer.marginPrintln("role=\"http://services.opendap.org/dap4/data\">");
           printer.outdent(2);
           printer.indent();
           printer.marginPrint("<link type=\"");
           printer.print(DapProtocol.defaultmimetype(RequestMode.DAP));
           printer.println("\"/>");
           printer.outdent();
           printer.marginPrintln("</Service>");
           printer.outdent();
           printer.marginPrintln("</Capabilities>");

           printer.flush();
           printer.close();
           sw.close();
           return sw.toString();
       }

}



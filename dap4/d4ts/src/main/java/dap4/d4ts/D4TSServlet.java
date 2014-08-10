/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.d4ts;

import dap4.ce.CEConstraint;
import dap4.ce.parser.*;
import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;
import dap4.dap4shared.*;
import dap4.servlet.*;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class D4TSServlet extends DapServlet
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    static final String TESTDATADIR = "testfiles"; // relative to resource path

    //////////////////////////////////////////////////
    // Instance variables

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4TSServlet()
    {
        super();
    }

    //////////////////////////////////////////////////////////
     // Non-doXXX Servlet Methods

     @Override
     public void init()
     throws ServletException
     {
         super.init(); // define svcinfo
         // Construct a simple URLmap
         try {
             String urlprefix = "/" + DapUtil.canonicalpath(this.svcinfo.getServletname());
             String fileprefix = DapUtil.canonicalpath(svcinfo.getResourcePath()) + "/" + TESTDATADIR;
             this.urlmap.addEntry(urlprefix,fileprefix);
         } catch (DapException de) {
             throw new ServletException(de);
         }
     }

    //////////////////////////////////////////////////////////
    // Capabilities processors

    /**
     * Process a capabilities request.
     * Currently, generate the front page.
     *
     * @param drq The merged dap state
     */

    protected void
    doCapabilities(DapRequest drq)
        throws IOException
    {
        addCommonHeaders(drq);

        // Figure out the directory containing
        // the files to display.
        String dir = getResourceFile(drq, true);
        if(dir == null)
            throw new DapException("Cannot locate resources directory");

        // Generate the front page
        FrontPage front = new FrontPage(dir,this.urlmap,this.svcinfo);
        String frontpage = front.buildPage();

        if(frontpage == null)
            throw new DapException("Cannot create front page")
                .setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // // Convert to UTF-8 and then to byte[]
        byte[] frontpage8 = DapUtil.extract(DapUtil.UTF8.encode(frontpage));

        OutputStream out = drq.getOutputStream();
        out.write(frontpage8);

    }

}



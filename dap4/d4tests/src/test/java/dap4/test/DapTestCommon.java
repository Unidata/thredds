/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.test;

import dap4.core.util.DapException;
import dap4.servlet.DapController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import thredds.server.dap4.Dap4Controller;
import ucar.httpservices.HTTPUtil;
import ucar.nc2.util.CommonTestUtils;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@ContextConfiguration
@WebAppConfiguration("file:src/test/data")
public class DapTestCommon extends CommonTestUtils
{
    //////////////////////////////////////////////////
    // Constants

    static final String DEFAULTTREEROOT = "dap4";

    static public final String FILESERVER = "file://localhost:8080";

    static public final String CONSTRAINTTAG = "dap4.ce";

    // Equivalent to the path to the webapp/d4ts for testing purposes
    static protected final String RESOURCEPATH = "/d4tests/src/test/data/resources";
    static protected final String TESTFILES = "/d4tests/src/test/data/resources/testfiles";

    //////////////////////////////////////////////////
    // Type decls

    static public class Mocker
    {
        public MockHttpServletRequest req = null;
        public MockHttpServletResponse resp = null;
        public MockServletContext context = null;
        public DapController controller = null;
        public String url = null;
        public String servletname = null;
        public DapTestCommon parent = null;

        public Mocker(String servletname, String url, DapTestCommon parent)
                throws Exception
        {
            this.parent = parent;
            this.url = url;
            this.servletname = servletname;
            String resdir = parent.getResourceDir();
            // There appears to be bug in the spring core.io code
            // such that it assumes absolute paths start with '/'.
            // So, check for windows drive and prepend 'file:/' as a hack.
            if(System.getProperty("os.name").toLowerCase().startsWith("windows")
                    && resdir.matches("[a-zA-Z][:].*"))
                resdir = "/" + resdir;
            resdir = "file:" + resdir;
            this.context = new MockServletContext(resdir);
            this.req = new MockHttpServletRequest(this.context, "GET", url);
            this.resp = new MockHttpServletResponse();
            req.setMethod("GET");
            setup();
            this.controller = new Dap4Controller();
            controller.init();
        }

        /**
         * The spring mocker is not very smart.
         * Given the url it should be possible
         * to initialize a lot of its fields.
         * Instead, it requires the user to so do.
         */
        protected void setup()
                throws URISyntaxException
        {
            this.req.setCharacterEncoding("UTF-8");
            this.req.setServletPath("/" + this.servletname);
            URI url = HTTPUtil.parseToURI(this.url);
            this.req.setProtocol(url.getScheme());
            this.req.setQueryString(url.getQuery());
            this.req.setServerName(url.getHost());
            this.req.setServerPort(url.getPort());
            String path = url.getPath();
            if(path != null) {// probably more complex than it needs to be
                String prefix = null;
                String suffix = null;
                String spiece = "/" + servletname;
                if(path.equals(spiece) || path.equals(spiece + "/")) {
                    // path is just spiece
                    prefix = spiece;
                    suffix = "/";
                } else {

                    String[] pieces = path.split(spiece + "/"); // try this first
                    if(pieces.length == 1 && path.endsWith(spiece))
                        pieces = path.split(spiece);  // try this
                    switch (pieces.length) {
                    case 0:
                        throw new IllegalArgumentException("CommonTestUtils");
                    case 1:
                        prefix = pieces[0] + spiece;
                        suffix = "";
                        break;
                    default: // > 1
                        prefix = pieces[0] + spiece;
                        suffix = path.substring(prefix.length());
                        break;
                    }
                }
                this.req.setContextPath(prefix);
                this.req.setPathInfo(suffix);
            }
        }

        public byte[] execute()
                throws IOException
        {
            this.controller.handleRequest(this.req, this.resp);
            return this.resp.getContentAsByteArray();
        }
    }

    //////////////////////////////////////////////////
    // Static methods

    static String
    locateDAP4Root(String threddsroot)
    {
        String root = threddsroot;
        if(root != null)
            root = root + "/" + DEFAULTTREEROOT;
        // See if it exists
        File f = new File(root);
        if(!f.exists() || !f.isDirectory())
            root = null;
        return root;
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected String dap4root = null;
    protected String d4tsServer = null;
    protected String resourcedir = null;

    protected String title = "Testing";

    public DapTestCommon()
    {
        this("DapTest");
    }

    public DapTestCommon(String name)
    {
        super(name);
        this.dap4root = locateDAP4Root(this.threddsroot);
        if(this.dap4root == null)
            System.err.println("Cannot locate /dap4 parent dir");
        this.resourcedir = this.dap4root + RESOURCEPATH;
        // Compute the set of SOURCES
        this.d4tsServer = TestDir.dap4TestServer;
        if(DEBUG)
            System.err.println("CommonTestUtils: d4tsServer=" + d4tsServer);
    }

    protected void
    findServer(String path)
            throws DapException
    {
        if(d4tsServer.startsWith("file:")) {
            d4tsServer = FILESERVER + "/" + path;
        } else {
            String svc = "http://" + d4tsServer + "/d4ts";
            if(!checkServer(svc))
                log.warn("D4TS Server not reachable: " + svc);
            // Since we will be accessing it thru NetcdfDataset, we need to change the schema.
            d4tsServer = "dap4://" + d4tsServer + "/d4ts";
        }
    }

    //////////////////////////////////////////////////
    // Accessor

    public String getDAP4Root()
    {
        return this.dap4root;
    }

    @Override
    public String getResourceDir()
    {
        return this.resourcedir;
    }

}


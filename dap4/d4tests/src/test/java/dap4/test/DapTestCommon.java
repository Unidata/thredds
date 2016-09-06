/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.servlet.DapController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import thredds.core.DatasetManager;
import thredds.core.TdsRequestedDataset;
import thredds.server.dap4.Dap4Controller;
import ucar.httpservices.HTTPUtil;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@ContextConfiguration
@WebAppConfiguration("file:src/test/data")
abstract public class DapTestCommon extends UnitTestCommon
{
    //////////////////////////////////////////////////
    // Constants

    static final String DEFAULTTREEROOT = "dap4";

    static public final String FILESERVER = "file://localhost:8080";

    static public final String CONSTRAINTTAG = "dap4.ce";

    static final String D4TESTDIRNAME = "d4tests";

    // Equivalent to the path to the webapp/d4ts for testing purposes
    static protected final String DFALTRESOURCEPATH = "/src/test/data/resources";
    //////////////////////////////////////////////////
    // Type decls

    static class Mocker
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
            this(servletname, url, new Dap4Controller(), parent);
        }

        public Mocker(String servletname, String url, DapController controller, DapTestCommon parent)
                throws Exception
        {
            this.parent = parent;
            this.url = url;
            this.servletname = servletname;
            if(controller != null)
                setController(controller);
            String testdir = parent.getResourceRoot();
            // There appears to be bug in the spring core.io code
            // such that it assumes absolute paths start with '/'.
            // So, check for windows drive and prepend 'file:/' as a hack.
            if(DapUtil.hasDriveLetter(testdir))
                testdir = "/" + testdir;
            testdir = "file:" + testdir;
            this.context = new MockServletContext(testdir);
            URI u = HTTPUtil.parseToURI(url);
            this.req = new MockHttpServletRequest(this.context, "GET", u.getPath());
            this.resp = new MockHttpServletResponse();
            req.setMethod("GET");
            setup();
        }

        protected void
        setController(DapController ct)
                throws ServletException
        {
            this.controller = ct;
        }

        /**
         * The spring mocker is not very smart.
         * Given the url it should be possible
         * to initialize a lot of its fields.
         * Instead, it requires the user to so do.
         * The request elements to set are:
         * - servletpath
         * - protocol
         * - querystring
         * - servername
         * - serverport
         * - contextpath
         * - pathinfo
         * - servletpath
         */
        protected void setup()
                throws Exception
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
                if(path.equals("/" + this.servletname) || path.equals("/" + this.servletname + "/")) {
                    // path is just servlet tag
                    prefix = "/" + this.servletname;
                    suffix = "/";
                } else {
                    int i;
                    String[] pieces = path.split("[/]");
                    for(i = 0; i < pieces.length; i++) { // find servletname piece
                        if(pieces[i].equals(this.servletname)) break;
                    }
                    if(i >= pieces.length) // not found
                        throw new IllegalArgumentException("DapTestCommon");
                    prefix = DapUtil.join(pieces, "/", 0, i);
                    suffix = DapUtil.join(pieces, "/", i + 1, pieces.length);
                }
                this.req.setContextPath(DapUtil.absolutize(prefix));
                this.req.setPathInfo(suffix);
                this.req.setServletPath(DapUtil.absolutize(suffix));
            }
        }

        public byte[] execute()
                throws IOException
        {
            if(this.controller == null)
                throw new DapException("Mocker: no controller");
            this.controller.handleRequest(this.req, this.resp);
            return this.resp.getContentAsByteArray();
        }
    }

    static class TestFilter implements FileFilter
    {
        boolean debug;
        boolean strip;
        String[] extensions;

        public TestFilter(boolean debug, String[] extensions)
        {
            this.debug = debug;
            this.strip = strip;
            this.extensions = extensions;
        }

        public boolean accept(File file)
        {
            boolean ok = false;
            if(file.isFile() && file.canRead()) {
                // Check for proper extension
                String name = file.getName();
                if(name != null) {
                    for(String ext : extensions) {
                        if(name.endsWith(ext))
                            ok = true;
                    }
                }
                if(!ok && debug)
                    System.err.println("Ignoring: " + file.toString());
            }
            return ok;
        }

        static void
        filterfiles(String path,List<String> matches, String... extensions)
        {
            File testdirf = new File(path);
            assert (testdirf.canRead());
            TestFilter tf = new TestFilter(DEBUG, extensions);
            File[] filelist = testdirf.listFiles(tf);
            for(int i = 0; i < filelist.length; i++) {
                File file = filelist[i];
                if(file.isDirectory()) continue;
                String fname = DapUtil.canonicalpath(file.getAbsolutePath());
                matches.add(fname);
            }
        }
    }

    //////////////////////////////////////////////////
    // Static variables

    static protected String dap4root = null;
    static protected String dap4testroot = null;
    static protected String dap4resourcedir = null;

    static {
        dap4root = locateDAP4Root(threddsroot);
        if(dap4root == null)
            System.err.println("Cannot locate /dap4 parent dir");
        dap4testroot = canonjoin(dap4root, D4TESTDIRNAME);
        dap4resourcedir = canonjoin(dap4testroot, DFALTRESOURCEPATH);
    }

    //////////////////////////////////////////////////
    // Static methods


    static protected String getD4TestsRoot()
    {
        return dap4testroot;
    }

    static protected String getResourceRoot()
    {
        return dap4resourcedir;
    }

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


    protected String d4tsserver = null;

    protected String title = "Dap4 Testing";

    public DapTestCommon()
    {
        this("DapTest");
    }

    public DapTestCommon(String name)
    {
        super(name);

        this.d4tsserver = TestDir.dap4TestServer;
        if(DEBUG)
            System.err.println("DapTestCommon: d4tsServer=" + d4tsserver);
    }

    /**
     * Try to get the system properties
     */
    protected void setSystemProperties()
    {
        String testargs = System.getProperty("testargs");
        if(testargs != null && testargs.length() > 0) {
            String[] pairs = testargs.split("[  ]*[,][  ]*");
            for(String pair : pairs) {
                String[] tuple = pair.split("[  ]*[=][  ]*");
                String value = (tuple.length == 1 ? "" : tuple[1]);
                if(tuple[0].length() > 0)
                    System.setProperty(tuple[0], value);
            }
        }
        if(System.getProperty("nodiff") != null)
            prop_diff = false;
        if(System.getProperty("baseline") != null)
            prop_baseline = true;
        if(System.getProperty("nogenerate") != null)
            prop_generate = false;
        if(System.getProperty("debug") != null)
            prop_debug = true;
        if(System.getProperty("visual") != null)
            prop_visual = true;
        if(System.getProperty("ascii") != null)
            prop_ascii = true;
        if(System.getProperty("utf8") != null)
            prop_ascii = false;
        if(prop_baseline && prop_diff)
            prop_diff = false;
        prop_controls = System.getProperty("controls", "");
    }

    //////////////////////////////////////////////////
    // Overrideable methods

    //////////////////////////////////////////////////
    // Accessor

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return this.title;
    }

    //////////////////////////////////////////////////
    // Instance Utilities

    public void
    visual(String header, String captured)
    {
        if(!captured.endsWith("\n"))
            captured = captured + "\n";
        // Dump the output for visual comparison
        System.err.println("Testing " + title + ": " + header + ":");
        System.err.println("---------------");
        System.err.print(captured);
        System.err.println("---------------");
    }

    protected void
    findServer(String path)
            throws DapException
    {
        String svc = "http://" + this.d4tsserver + "/d4ts";
        if(!checkServer(svc))
            log.warn("D4TS Server not reachable: " + svc);
        // Since we will be accessing it thru NetcdfDataset, we need to change the schema.
        d4tsserver = "dap4://" + d4tsserver + "/d4ts";
    }

    //////////////////////////////////////////////////

    public String getDAP4Root()
    {
        return this.dap4root;
    }

    @Override
    public String getResourceDir()
    {
        return this.dap4resourcedir;
    }

    /**
     * Unfortunately, mock does not appear to always
     * do proper initialization
     */
    static protected void
    mockSetup()
    {
        TdsRequestedDataset.setDatasetManager(new DatasetManager());
    }


    static protected void
    testSetup()
    {
        DapController.TESTING = true;
    }

    static protected MvcResult
    perform(String url, String respath, String query,
            MockMvc mockMvc)
            throws Exception
    {
        MockHttpServletRequestBuilder rb = MockMvcRequestBuilders
                .get(url)
                .servletPath(url);
        if(query != null)
            rb.param(CONSTRAINTTAG, query);
        String realdir = canonjoin(dap4testroot, respath);
        rb.requestAttr("RESOURCEDIR", realdir);
        MvcResult result = mockMvc.perform(rb).andReturn();
        return result;
    }
}


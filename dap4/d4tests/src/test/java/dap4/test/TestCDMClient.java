package dap4.test;

import dap4.test.util.UnitTestCommon;
import ucar.httpclient.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.net.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Test at the NetcdfDataset level
 */
public class TestCDMClient extends UnitTestCommon
{
    static final boolean DEBUG = true;

    static final boolean NCDUMP = true; // Use NcDumpW instead of NCPrint

    static final String EXTENSION = (NCDUMP ? "ncdump" : "dmp");

    static final String TESTEXTENSION = "raw";

    //////////////////////////////////////////////////
    // Constants

    static final String DATADIR = "tests/src/test/data"; // relative to opuls root
    static final String TESTDATADIR = DATADIR + "/resources/TestCDMClient";
    static final String BASELINEDIR = TESTDATADIR + "/baseline";
    static final String TESTINPUTDIR = TESTDATADIR + "/testinput";

    //Define the names of the xfail tests
    static final String[] XFAIL_TESTS = {"test_struct_array.nc"};

    static boolean isXfailTest(String t)
    {
        for(String s : XFAIL_TESTS) {
            if(s.equals(t)) return true;
        }
        return false;
    }

    //////////////////////////////////////////////////
    // Type Declarations

    static class ClientTest
    {
        static String root = null;
        static String server = null;

        String title;
        String dataset;
        boolean checksumming;
        boolean xfail;
        String testinputpath;
        String baselinepath;

        ClientTest(String dataset, boolean checksumming)
        {
            this(dataset, checksumming, false);
        }

        ClientTest(String dataset, boolean checksumming, boolean xfail)
        {
            this.title = dataset;
            this.dataset = dataset;
            this.checksumming = checksumming;
            this.xfail = xfail;
            this.testinputpath
                = root + "/" + TESTINPUTDIR + "/" + dataset;
            this.baselinepath
                = root + "/" + BASELINEDIR + "/" + dataset;
        }

        String makeurl()
        {
            String url = url = server + "/" + dataset;
            if(server.startsWith(FILESERVER))
                url += ".raw";
            return url;
        }

        public String toString()
        {
            return dataset;
        }
    }

    static class TestFilter implements FileFilter
    {
        boolean debug;

        public TestFilter(boolean debug)
        {
            this.debug = debug;
        }

        public boolean accept(File file)
        {
            boolean ok = false;
            if(file.isFile() && file.canRead()) {
                // Check for proper extension
                String name = file.getName();
                if(name != null && name.endsWith(TESTEXTENSION))
                    ok = true;
            }
            if(!ok && debug) {
                report("Ignoring: " + file.toString());
            }
            return ok;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    // System properties

    boolean prop_diff = true;
    boolean prop_baseline = false;
    boolean prop_visual = false;
    boolean prop_debug = DEBUG;
    String prop_server = null;


    // Test cases

    List<ClientTest> alltestcases = new ArrayList<ClientTest>();
    List<ClientTest> chosentests = new ArrayList<ClientTest>();

    String root = null;
    String datasetpath = null;

    String sourceurl = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestCDMClient()
        throws Exception
    {
        this("TestCDMClient");
    }

    public TestCDMClient(String name)
        throws Exception
    {
        this(name, null);
    }

    public TestCDMClient(String name, String[] argv)
        throws Exception
    {
        super(name);
        setSystemProperties();
        this.root = getRoot();
        if(this.root == null)
            throw new Exception("Opuls root cannot be located");
        if(this.root.charAt(0) != '/')
            this.root = "/" + this.root; // handle problem of windows paths
        this.datasetpath = this.root + "/" + TESTINPUTDIR;
        makefilesource(this.datasetpath);
        this.sourceurl = getSourceURL();
        System.out.println("Using source url " + this.sourceurl);
        defineAllTestcases(this.root, this.sourceurl);
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(true) {
            chosentests = locate("test_struct_array.syn");
        } else {
            for(ClientTest tc : alltestcases)
                chosentests.add(tc);
        }
    }

    boolean
    defineAllTestcases(String root, String server)
    {
        ClientTest.root = root;
        ClientTest.server = server;
        File testpath = new File(root + "/" + TESTINPUTDIR);
        File basepath = new File(root + "/" + BASELINEDIR);
        if(!basepath.exists() || !basepath.isDirectory() || !basepath.canRead())
            return report("Base directory not readable: " + basepath.toString());
        if(!testpath.exists() || !testpath.isDirectory() || !testpath.canRead())
            return report("Test directory not readable: " + testpath.toString());
        // Generate the testcases from the located .dmr files
        // This assumes that the set of files and the set of
        // files thru the sourceurl are the same
        File[] filelist = testpath.listFiles(new TestFilter(prop_debug));
        for(int i = 0;i < filelist.length;i++) {
            File file = filelist[i];
            // remove the extension
            String name = file.getName();
            if(name == null) continue;
            int index = name.lastIndexOf("." + TESTEXTENSION);
            assert (index > 0);
            name = name.substring(0, index);
            ClientTest ct = new ClientTest(name, true, isXfailTest(name));
            this.alltestcases.add(ct);
        }

        return true;
    }

    //////////////////////////////////////////////////
    // Junit test method

    public void testCDMClient()
        throws Exception
    {
        boolean pass = true;
        for(ClientTest testcase : chosentests) {
            if(!doOneTest(testcase)) pass = false;
        }
        assertTrue("*** Fail: TestCDMClient", pass);
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(ClientTest testcase)
        throws Exception
    {
        boolean pass = true;
        System.out.println("Testcase: " + testcase.testinputpath);
        String url = testcase.makeurl();
        NetcdfDataset ncfile = null;
        try {
            ncfile = openDataset(url);
        } catch (Exception e) {
            System.err.println(testcase.xfail ? "XFail" : "Fail");
            e.printStackTrace();
            return testcase.xfail;
        }

        String metadata = (NCDUMP ? ncdumpmetadata(ncfile) : null);
        if(prop_visual) {
            visual(testcase.title + ".dmr", metadata);
        }

        String data = (NCDUMP ? ncdumpdata(ncfile) : null);
        if(prop_visual) {
            visual(testcase.title + ".dap", data);
        }

        String testoutput = (NCDUMP ? data : metadata + data);

        String baselinefile = testcase.baselinepath + "." + EXTENSION;

        if(prop_baseline)
            writefile(baselinefile, testoutput);

        if(prop_diff) { //compare with baseline
            // Read the baseline file(s)
            String baselinecontent = readfile(baselinefile);
            System.out.println("Comparison: vs " + baselinefile);
            pass = pass && compare(baselinecontent, testoutput);
            System.out.println(pass ? "Pass" : "Fail");
        }
        return pass;
    }


    String ncdumpmetadata(NetcdfDataset ncfile)
        throws Exception
    {
        StringWriter sw = new StringWriter();
        // Print the meta-databuffer using these args to NcdumpW
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, "-unsigned", sw, null))
                throw new Exception("NcdumpW failed");
        } catch (IOException ioe) {
            throw new Exception("NcdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    String ncdumpdata(NetcdfDataset ncfile)
        throws Exception
    {
        StringWriter sw = new StringWriter();
        // Dump the databuffer
        sw = new StringWriter();
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, "-vall -unsigned", sw, null))
                throw new Exception("NCdumpW failed");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Exception("NCdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    //////////////////////////////////////////////////
    // Utility methods

    /**
     * Try to get the system properties
     */
    void setSystemProperties()
    {
        prop_diff = (System.getProperty("nodiff") == null);
        prop_baseline = (System.getProperty("baseline") != null);
        prop_visual = (System.getProperty("visual") != null);
        if(System.getProperty("debug") != null)
            prop_debug = true;
        prop_server = System.getProperty("server");
        if(prop_diff && prop_baseline)
            prop_diff = false;
    }

    //Locate the test cases with given prefix
    List<ClientTest>
    locate(String prefix)
    {
        List<ClientTest> results = new ArrayList<ClientTest>();
        for(ClientTest ct : this.alltestcases) {
            if(!ct.dataset.startsWith(prefix))
                continue;
            results.add(ct);
        }
        return results;
    }

    static boolean
    report(String msg)
    {
        System.err.println(msg);
        return false;
    }

    void
    makefilesource(String path)
    {
        for(Source s : SOURCES) {
            if(s.name.equals("file"))
                s.prefix = s.prefix + path;
        }
    }

    String
    getSourceURL()
    {
        Source chosen = null;
        if(prop_server != null) {
            for(int i = 0;i < SOURCES.length;i++) {
                if(SOURCES[i].name.equals(prop_server)) {
                    chosen = SOURCES[i];
                    break;
                }
            }
            if(chosen == null) {
                System.err.println("-Dserver argument unknown: " + prop_server);
                return null;
            }
            if(!chosen.isfile && !checkServer(chosen)) {
                System.err.println("-Dserver unreachable: " + prop_server);
                return null;
            }
            return chosen.prefix;
        }
        // Look for a sourceurl in order of appearance in SOURCES
        for(int i = 0;i < SOURCES.length;i++) {
            chosen = SOURCES[i];
            if(!chosen.isfile)
                break;
            if(checkServer(chosen))
                break;
        }
        // Could not find working sourceurl
        return chosen.prefix;
    }

    boolean
    checkServer(Source candidate)
    {
        if(candidate == null) return false;
/* requires httpclient4
        int savecount = HTTPSession.getRetryCount();
        HTTPSession.setRetryCount(1);
*/
        // See if the sourceurl is available by trying to get the DSR
        System.err.print("Checking for sourceurl: " + candidate.prefix);
        try {
            HTTPSession session = new HTTPSession(candidate.testurl);
            HTTPMethod method = HTTPFactory.Get(session);
            method.execute();
            String s = method.getResponseAsString();
            session.close();
            System.err.println(" ; found");
            return true;
        } catch (IOException ie) {
            System.err.println(" ; fail");
            return false;
        } finally {
// requires httpclient4            HTTPSession.setRetryCount(savecount);
        }
    }


    //////////////////////////////////////////////////
    // Stand alone

    static public void
    main(String[] argv)
    {
        try {
            new TestCDMClient().testCDMClient();
        } catch (Exception e) {
            System.err.println("*** FAIL");
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("*** PASS");
        System.exit(0);
    }// main

} // class TestCDMClient


package dap4.test;

import dap4.test.util.UnitTestCommon;
import ucar.httpclient.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.net.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Test OpenDap Server at the NetcdfDataset level
 */
public class TestHyrax extends UnitTestCommon
{
    static final boolean DEBUG = true;

    static final boolean NCDUMP = true; // Use NcDumpW instead of NCPrint

    static final String EXTENSION = (NCDUMP ? "ncdump" : "dmp");

    static final String TESTEXTENSION = "dmr";

    // Mnemonic
    static final boolean HEADERONLY = false;

    static final String IP = "ec2-54-204-231-163";
    //////////////////////////////////////////////////
    // Constants

    static final String DATADIR = "tests/src/test/data"; // relative to opuls root
    static final String TESTDATADIR = DATADIR + "/resources/TestHyrax";
    static final String BASELINEDIR = TESTDATADIR + "/baseline";

    //Define the names of the xfail tests
    static final String[] XFAIL_TESTS = {"test_struct_array.nc"};

    // Order is important; testing reachability is in the order
    // listed
    static final Source[] SOURCES = new Source[]{
        new Source("hyrax",
            "http://" + IP + ".compute-1.amazonaws.com:8080/opendap/data/reader/dap4/dap4.html",
            "dap4://" + IP + ".compute-1.amazonaws.com:8080/opendap/data/reader/dap4"),
    };

    static boolean isXfailTest(String t)
    {
        for(String s : XFAIL_TESTS) {
            if(s.equals(t)) return true;
        }
        return false;
    }

    //////////////////////////////////////////////////
    // Type Declarations

    static class Source
    {
        public String name;
        public String testurl;
        public String prefix;

        public Source(String name, String testurl, String prefix)
        {
            this.name = name;
            this.prefix = prefix;
            this.testurl = testurl;
        }
    }

    static class ClientTest
    {
        static String root = null;
        static String server = null;
        static int counter = 0;

        boolean checksumming = true;
        boolean xfail = false;
        boolean headeronly = false;

        String title;
        String dataset; // path minus the server url part.
        String datasetpath; // Hyrax test databuffer is segregated into multiple directories
        String baselinepath;
        String constraint;
        int id;

        ClientTest(String dataset)
        {
            this(0, dataset, null);
        }

        ClientTest(int id, String datasetpath, String constraint)
        {
            // Break off the final file set name
            int index = datasetpath.lastIndexOf('/');
            this.dataset = datasetpath.substring(index + 1, datasetpath.length());
            this.datasetpath = datasetpath;
            this.title = this.dataset;
            this.id = id;
            this.constraint = (constraint.length() == 0 ? null : constraint);
            this.baselinepath
                = root + "/" + BASELINEDIR + "/" + dataset;
            if(this.constraint != null)
                this.baselinepath += ("." + String.valueOf(this.id));
        }

        public ClientTest nochecksum()
        {
            this.checksumming = false;
            return this;
        }

        public ClientTest xfail()
        {
            this.xfail = true;
            return this;
        }

        public ClientTest headeronly()
        {
            this.headeronly = true;
            return this;
        }

        String makeurl()
        {
            String url = url = server + "/" + datasetpath;
            if(constraint != null)
                url += ("?" + constraint);
            return url;
        }

        public String toString()
        {
            return dataset;
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

    public TestHyrax()
        throws Exception
    {
        this("TestCDMClient");
    }

    public TestHyrax(String name)
        throws Exception
    {
        this(name, null);
    }

    public TestHyrax(String name, String[] argv)
        throws Exception
    {
        super(name);
        setSystemProperties();
        this.root = getRoot();
        if(this.root == null)
            throw new Exception("Opuls root cannot be located");
        if(this.root.charAt(0) != '/')
            this.root = "/" + this.root; // handle problem of windows paths
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
            chosentests = locate("dmr-testsuite/test_array_7.xml");
        } else {
            for(ClientTest tc : alltestcases)
                chosentests.add(tc);
        }
    }

    boolean
    defineAllTestcases(String root, String server)
    {

        boolean what = HEADERONLY;

        ClientTest.root = root;
        ClientTest.server = server;
        if(true) {
            alltestcases.add(new ClientTest(1, "D4-xml/DMR_4.xml", "b1"));
        }if(false) {
            alltestcases.add(new ClientTest("test_simple_1.dmr"));
            //deleted: alltestcases.add(new ClientTest("dmr-testsuite/testall.xml"));
        }
        if(false) {
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_1.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_2.xml"));
            strings:
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_3.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_4.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_5.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_6.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_7.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_8.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_10.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_array_11.xml"));

        }
        if(false) {
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_1.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_2.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_3.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_4.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_5.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_6.xml"));
            //sequence: alltestcases.add(new ClientTest("dmr-testsuite/test_simple_7.xml"));
            //sequence: alltestcases.add(new ClientTest("dmr-testsuite/test_simple_8.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_9.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_9.1.xml"));
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_10.xml"));
        }
        if(false) {
            // alltestcases.add(new ClientTest("D4-xml/DMR_0.1.xml"));  needs fixing
            alltestcases.add(new ClientTest("D4-xml/DMR_0.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_1.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_2.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_2.1.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_3.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_3.1.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_3.2.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_3.3.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_3.4.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_3.5.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_4.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_4.1.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_5.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_5.1.xml"));
            //serial:  alltestcases.add(new ClientTest("D4-xml/DMR_6.xml"));
            //serial: alltestcases.add(new ClientTest("D4-xml/DMR_6.1.xml"));
            //serial: alltestcases.add(new ClientTest("D4-xml/DMR_6.2.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_7.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_7.1.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_7.2.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_7.3.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_7.4.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_7.5.xml"));
            alltestcases.add(new ClientTest("D4-xml/DMR_8.xml"));
        }

        if(false) {
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_3_error_1.xml").xfail());
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_3_error_2.xml").xfail());
            alltestcases.add(new ClientTest("dmr-testsuite/test_simple_3_error_3.xml").xfail());
        }
        for(ClientTest test : alltestcases) {
            if(what == HEADERONLY) test.headeronly();
        }
        return true;
    }

    //////////////////////////////////////////////////
    // Junit test method

    public void testHyrax()
        throws Exception
    {
        boolean pass = true;
        for(ClientTest testcase : chosentests) {
            if(!doOneTest(testcase)) pass = false;
        }
        assertTrue("*** Fail: TestHyrax", pass);
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(ClientTest testcase)
        throws Exception
    {
        boolean pass = true;
        System.out.println("Testcase: " + testcase.dataset);
        String url = testcase.makeurl();
        NetcdfDataset ncfile = null;
        try {
            ncfile = openDataset(url);
        } catch (Exception e) {
            System.err.println(testcase.xfail ? "XFail" : "Fail");
            e.printStackTrace();
            return testcase.xfail;
        }

        String metadata = (NCDUMP ? ncdumpmetadata(ncfile):null);
        if(prop_visual) {
            visual(testcase.title + ".dmr", metadata);
        }

        String data = null;
        if(!testcase.headeronly) {
            data = (NCDUMP ? ncdumpdata(ncfile):null);
            if(prop_visual) {
                visual(testcase.title + ".dap", data);
            }
        }

        String testoutput = (testcase.headeronly ? metadata : (NCDUMP ? data : metadata + data));

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
            if(!ct.datasetpath.startsWith(prefix))
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
            if(!checkServer(chosen)) {
                System.err.println("-Dserver unreachable: " + prop_server);
                return null;
            }
            return chosen.prefix;
        }
        // Look for a sourceurl in order of appearance in SOURCES
        for(int i = 0;i < SOURCES.length;i++) {
            chosen = SOURCES[i];
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
            new TestHyrax().testHyrax();
        } catch (Exception e) {
            System.err.println("*** FAIL");
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("*** PASS");
        System.exit(0);
    }// main

} // class TestHyrax


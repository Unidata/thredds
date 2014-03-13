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
public class TestConstraints extends UnitTestCommon
{
    static final boolean DEBUG = false;

    static final boolean NCDUMP = true; // Use NcDumpW instead of D4Print

    static final String EXTENSION = (NCDUMP ? "ncdump" : "dmp");

    //////////////////////////////////////////////////
    // Constants

    static final String DATADIR = "tests/src/test/data"; // relative to opuls root
    static final String TESTDATADIR = DATADIR + "/resources/TestCDMClient";
    static final String BASELINEDIR = TESTDATADIR + "/baseline";
    static final String TESTINPUTDIR = TESTDATADIR + "/testinput";

    static final String alpha = "abcdefghijklmnopqrstuvwxyz"
        + "abcdefghijklmnopqrstuvwxyz".toUpperCase();

    static class ClientTest
    {
        static String root = null;
        static String server = null;
        static int counter = 0;

        String title;
        String dataset;
        String testinputpath;
        String baselinepath;
        String constraint;
        int id;

        ClientTest(String dataset, String constraint)
        {
            if(constraint != null && constraint.length() == 0)
                constraint = null;
            this.title = dataset + (constraint == null ? "" : "?" + constraint);
            this.dataset = dataset;
            this.id = ++counter;
            this.testinputpath
                = root + "/" + TESTINPUTDIR + "/" + dataset;
            this.baselinepath
                = root + "/" + BASELINEDIR + "/" + dataset + "." + String.valueOf(this.id);
            this.constraint = constraint;
        }

        String makeurl()
        {
            String url = url = server + "/" + dataset;
            if(constraint != null) url += "?"+UnitTestCommon.CONSTRAINTTAG+"=" + constraint;
            return url;
        }

        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(dataset);
            buf.append("{");
            if(constraint != null)
                buf.append("?"+UnitTestCommon.CONSTRAINTTAG+"=" + constraint);
            return buf.toString();
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

    public TestConstraints()
        throws Exception
    {
        this("TestConstraints");
    }

    public TestConstraints(String name)
        throws Exception
    {
        this(name, null);
    }

    public TestConstraints(String name, String[] argv)
        throws Exception
    {
        super(name);
        setSystemProperties();
        this.root = getRoot();
        if(this.root == null)
            throw new Exception("Opuls root cannot be located");
        // Check for windows path
        if(alpha.indexOf(this.root.charAt(0)) >= 0 && this.root.charAt(1) == ':') {
        } else if(this.root.charAt(0) != '/')
            this.root = "/" + this.root;
        this.datasetpath = this.root + "/" + TESTINPUTDIR;
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
        if(false) {
            chosentests = locate("test_struct_array.nc");
        } else {
            for(ClientTest tc : alltestcases)
                chosentests.add(tc);
        }
    }

    void
    defineAllTestcases(String root, String server)
    {
        ClientTest.root = root;
        ClientTest.server = server;
        //alltestcases.add(new ClientTest("test_one_vararray.nc", null));
        alltestcases.add(new ClientTest("test_one_vararray.nc", "t"));
        alltestcases.add(new ClientTest("test_one_vararray.nc", "t[1]"));
        // alltestcases.add(new ClientTest("test_enum_array.nc", null));
        alltestcases.add(new ClientTest("test_enum_array.nc", "primary_cloud[1:2:4]"));
        //alltestcases.add(new ClientTest("test_atomic_array.nc", null));
        alltestcases.add(new ClientTest("test_atomic_array.nc", "vu8[1][0:2:2];vd[1];vs[1][0];vo[0][1]"));
        //alltestcases.add(new ClientTest("test_struct_array.nc", null));
        alltestcases.add(new ClientTest("test_struct_array.nc", "s[0:2:3][0:1]"));
    }

    //////////////////////////////////////////////////
    // Junit test method

    public void testConstraints()
        throws Exception
    {
        for(ClientTest testcase : chosentests) {
            if(!doOneTest(testcase)) {
                assertTrue(false);
            }
        }
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(ClientTest testcase)
        throws Exception
    {
        boolean pass = true;
        int testcounter = 0;

        System.out.println("Testcase: " + testcase.testinputpath);

        String url = testcase.makeurl();
        NetcdfDataset ncfile = null;
        try {
	    ncfile = openDataset(url);
        } catch (Exception e) {
            throw e;
        }

        String metadata = (NCDUMP ? ncdumpmetadata(ncfile) : null);
        String data = (NCDUMP ? ncdumpdata(ncfile) : null);

        if(prop_visual) {
            visual("DMR: " + url, metadata);
            visual("DAP: " + url, data);
        }

        String testoutput = (NCDUMP ? data : metadata + data);

        if(prop_baseline)
            writefile(testcase.baselinepath, testoutput);

        if(prop_diff) { //compare with baseline
            // Read the baseline file(s)
            String baselinecontent = readfile(testcase.baselinepath);
            System.out.println("Comparison:");
            pass = pass && compare(baselinecontent, testoutput);
            System.out.println(pass ? "Pass" : "Fail");
        }
        return pass;
    }

    //////////////////////////////////////////////////
    // Dump methods

    String ncdumpmetadata(NetcdfDataset ncfile)
    {
        boolean ok = false;
        String metadata = null;
        StringWriter sw = new StringWriter();

        // Print the meta-databuffer using these args to NcdumpW
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, "-unsigned", sw, null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ok = false;
        }
        try {
            sw.close();
        } catch (IOException e) {
        }
        ;
        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
        return sw.toString();
    }

    String ncdumpdata(NetcdfDataset ncfile)
    {
        boolean ok = false;
        StringWriter sw = new StringWriter();

        // Dump the databuffer
        sw = new StringWriter();
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, "-vall -unsigned", sw, null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ok = false;
        }
        try {
            sw.close();
        } catch (IOException e) {
        }
        ;
        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
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

    // Locate the test cases with given prefix
    ClientTest
    locate1(String prefix)
    {
        List<ClientTest> tests = locate(prefix);
        assert tests.size() > 0;
        return tests.get(0);
    }

    //Locate the test cases with given prefix and optional constraint
    List<ClientTest>
    locate(String prefix)
    {
        List<ClientTest> results = new ArrayList<ClientTest>();
        for(ClientTest ct : this.alltestcases) {
            if(!ct.title.equals(prefix))
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

    String
    getSourceURL()
    {
        Source chosen = null;
        if(prop_server != null) {
            for(int i = 0;i < SOURCES.length;i++) {
		if(SOURCES[i].isfile) continue;
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
            new TestConstraints().testConstraints();
        } catch (Exception e) {
            System.err.println("*** FAIL");
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("*** PASS");
        System.exit(0);
    }// main

} // class TestConstraints

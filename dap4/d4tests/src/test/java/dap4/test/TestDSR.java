package dap4.test;

import dap4.test.servlet.*;
import dap4.test.util.UnitTestCommon;

import java.nio.charset.Charset;

/**
 * TestFrontPage verifies the front page
 * generation code
 */

public class TestDSR extends UnitTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected String DATADIR = "tests/src/test/data"; // relative to opuls root
    static protected String TESTDATADIR = DATADIR + "/resources/";
    static protected String BASELINEDIR = DATADIR + "/resources/TestDSR/baseline";

    // constants for Fake Request
    static protected final String FAKEDATASET = "test1"; 
    static protected String FAKEURL = "http://localhost:8080/d4ts/" + FAKEDATASET;

    //////////////////////////////////////////////////
    // Instance variables

    // System properties

    protected boolean prop_diff = true;
    protected boolean prop_baseline = false;
    protected boolean prop_visual = false;
    protected boolean prop_debug = DEBUG;

    protected String datasetpath = null;

    protected String root = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestDSR()
        throws Exception
    {
        this("TestDSR");
    }

    public TestDSR(String name)
        throws Exception
    {
        this(name, null);
    }

    public TestDSR(String name, String[] argv)
        throws Exception
    {
        super(name);
        setSystemProperties();
        this.root = getRoot();
        if(this.root == null)
            throw new Exception("Opuls root not found");
        this.datasetpath = this.root + "/" + DATADIR;
    }

    //////////////////////////////////////////////////
    // Junit test methods

    public void testDSR()
        throws Exception
    {
        boolean pass = true;

        // Create request and response objects
        FakeServlet servlet = new FakeServlet(this.datasetpath);
        String url = FAKEURL; // no file specified
        FakeServletRequest req = new FakeServletRequest(url, servlet);
        FakeServletResponse resp = new FakeServletResponse();

        // See if the servlet can process this
        try {
            servlet.init();
            servlet.doGet(req, resp);
        } catch (Throwable t) {
            t.printStackTrace();
            assertTrue(false);
        }
        // Collect the output
        FakeServletOutputStream fakestream = (FakeServletOutputStream) resp.getOutputStream();
        byte[] byteresult = fakestream.toArray();

        // Convert the raw output to a string
        String dsr = new String(byteresult,UTF8);

        if(prop_visual)
            visual("TestDSR", dsr);

        // Figure out the baseline
        String baselinepath = this.root + "/" + BASELINEDIR + "/" + FAKEDATASET + ".dsr";
	
        if(prop_baseline) {
            writefile(baselinepath, dsr);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baselinepath);
            System.out.println("DSR Comparison:");
            pass = compare(baselinecontent, dsr);
            System.out.println(pass ? "Pass" : "Fail");
        }
        assertTrue(pass);
    }

    //////////////////////////////////////////////////
    // Utility methods

    /**
     * Try to get the system properties
     */
    void setSystemProperties()
    {
        if(System.getProperty("nodiff") != null)
            prop_diff = false;
        String value = System.getProperty("baseline");
	if(value != null) prop_baseline = true;
        value = System.getProperty("debug");
	if(value != null) prop_debug = true;
        if(System.getProperty("visual") != null)
            prop_visual = true;
        if(prop_baseline && prop_diff)
            prop_diff = false;
    }

    //////////////////////////////////////////////////
    // Stand alone

    static public void
    main(String[] argv)
    {
        try {
            new TestFrontPage().testFrontPage();
        } catch (Exception e) {
            System.err.println("*** FAIL");
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("*** PASS");
        System.exit(0);
    }// main

}


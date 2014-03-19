package dap4.test;

import dap4.test.servlet.*;
import dap4.test.util.UnitTestCommon;

import java.nio.charset.Charset;

/**
 * TestFrontPage verifies the front page
 * generation code
 */

public class TestFrontPage extends UnitTestCommon
{
    static final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected String DATADIR = "tests/src/test/data"; // relative to opuls root
    static protected String TESTDATADIR = DATADIR + "/resources/";
    static protected String BASELINEDIR = DATADIR + "/resources/TestServlet/baseline";
    static protected String TESTINPUTDIR = DATADIR + "/resources/testfiles";

    // constants for Fake Request
    static protected String FAKEURLPREFIX = "http://localhost:8080/d4ts";

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

    public TestFrontPage()
        throws Exception
    {
        this("TestFrontPage");
    }

    public TestFrontPage(String name)
        throws Exception
    {
        this(name, null);
    }

    public TestFrontPage(String name, String[] argv)
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

    public void testFrontPage()
        throws Exception
    {
        boolean pass = true;

        // Create request and response objects
        FakeServlet servlet = new FakeServlet(this.datasetpath);
        String url = FAKEURLPREFIX; // no file specified
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
        String html = new String(byteresult,UTF8);

        if(prop_visual)
            visual("Front Page", html);

	    // Figure out the baseline
        String baselinepath = this.root + "/" + BASELINEDIR + "/index.html";
	
        if(prop_baseline) {
            writefile(baselinepath, html);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baselinepath);
            System.out.println("HTML Comparison:");
            pass = compare(baselinecontent, html);
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

} // class TestFrontPage

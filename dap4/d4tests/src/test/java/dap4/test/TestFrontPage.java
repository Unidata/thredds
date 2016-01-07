package dap4.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TestFrontPage verifies the front page
 * generation code
 */

public class TestFrontPage extends DapTestCommon
{
    static final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected String DATADIR = "d4tests/src/test/data"; // relative to dap4 root
    static protected String TESTDATADIR = DATADIR + "/resources/";
    static protected String BASELINEDIR = DATADIR + "/resources/TestServlet/baseline";
    static protected String TESTINPUTDIR = DATADIR + "/resources/testfiles";

    static protected String TESTFILE = "test_frontpage.html";

    // constants for Fake Request
    static protected String FAKEURLPREFIX = "http://localhost:8080/thredds/d4ts";

    //////////////////////////////////////////////////
    // Instance variables

    protected String datasetpath = null;

    protected String root = null;

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception {
        this.root = getDAP4Root();
        if(this.root == null)
            throw new Exception("dap4 root not found");
        this.datasetpath = this.root + "/" + DATADIR;
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void testFrontPage()
        throws Exception
    {
	org.junit.Assume.assumeTrue(usingIntellij);

        boolean pass = true;
        String url = FAKEURLPREFIX; // no file specified

        // Create request and response objects
	    Mocker mocker = new Mocker("d4ts",url,this);
        byte[] byteresult = null;

        try {
            byteresult = mocker.execute();
        } catch (Throwable t) {
            t.printStackTrace();
            Assert.assertTrue(false);
        }

        // Convert the raw output to a string
        String html = new String(byteresult,UTF8);

        if(prop_visual)
            visual("Front Page", html);

	    // Figure out the baseline
        String baselinepath = this.root + "/" + BASELINEDIR + "/" + TESTFILE;
	
        if(prop_baseline) {
            writefile(baselinepath, html);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baselinepath);
            System.out.println("HTML Comparison:");
            pass = compare(baselinecontent, html);
            System.out.println(pass ? "Pass" : "Fail");
        }
        Assert.assertTrue(pass);
    }

    //////////////////////////////////////////////////
    // Utility methods

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

package dap4.test;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import thredds.server.dap4.Dap4Controller;

/**
 * TestFrontPage verifies the front page
 * generation code
 */

public class TestDSR extends DapTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected String BASELINEDIR = "/TestDSR/baseline";

    // constants for Fake Request
    static protected final String FAKEDATASET = "test1";
    static protected String FAKEURL = "http://localhost:8080/d4ts/" + FAKEDATASET;

    //////////////////////////////////////////////////
    // Instance variables

    protected String datasetpath = null;

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception {
        this.datasetpath = getResourceRoot();
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void testDSR()
        throws Exception
    {
        boolean pass = true;
        String url = FAKEURL; // no file specified

        Mocker mocker = new Mocker("d4ts", url, this);
        byte[] byteresult = null;

        try {
            byteresult = mocker.execute();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

        // Convert the raw output to a string
        String dsr =
                new String(byteresult, UTF8);

        if(prop_visual)
            visual("TestDSR", dsr);

        // Figure out the baseline
        String baselinepath = getResourceRoot() + BASELINEDIR + "/" + FAKEDATASET + ".dsr";

        if(prop_baseline) {
            writefile(baselinepath, dsr);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baselinepath);
            System.out.println("DSR Comparison:");
            pass = same(getTitle(),baselinecontent, dsr);
            System.out.println(pass ? "Pass" : "Fail");
        }
        Assert.assertTrue(pass);
    }

    //////////////////////////////////////////////////
    // Utility methods

}


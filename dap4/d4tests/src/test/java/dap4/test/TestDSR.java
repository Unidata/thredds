package dap4.test;


import dap4.core.data.DSPRegistry;
import dap4.dap4lib.FileDSP;
import dap4.servlet.DapCache;
import dap4.servlet.Generator;
import dap4.servlet.SynDSP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import thredds.server.dap4.Dap4Controller;

/**
 * TestDSR verifies the DSR page
 * generation code
 */

public class TestDSR extends DapTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDir
    static protected final String TESTINPUTDIR = "/testfiles";
    static protected final String BASELINEDIR = "/TestDSR/baseline";

    // constants for Fake Request
    static protected final String FAKEDATASET = "test1";
    static protected String FAKEURLPREFIX = "/d4ts";
    static protected String FAKEURLPATH = FAKEURLPREFIX + "/" + FAKEDATASET;

    //////////////////////////////////////////////////
    // Instance variables

    //////////////////////////////////////////////////
    // Instance variables
    MockMvc mockMvc = null;
    protected String resourceroot = null;

    //////////////////////////////////////////////////

    @Before
    public void setup()
	throws Exception
    {
        StandaloneMockMvcBuilder mvcbuilder =
                MockMvcBuilders.standaloneSetup(new D4TSController());
        mvcbuilder.setValidator(new TestServlet.NullValidator());
        this.mockMvc = mvcbuilder.build();
        testSetup();
        DapCache.dspregistry.register(FileDSP.class, DSPRegistry.FIRST);
        DapCache.dspregistry.register(SynDSP.class, DSPRegistry.FIRST);
        if(prop_ascii)
            Generator.setASCII(true);
        this.resourceroot = getResourceRoot();
//        this.datasetpath = getResourceRoot();
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void testDSR()
        throws Exception
    {
        String url = FAKEURLPATH; // no file specified

        // Figure out the baseline
        String baselinepath = canonjoin(this.resourceroot, BASELINEDIR, FAKEDATASET) + ".dsr";

        MvcResult result = perform(url, this.mockMvc, RESOURCEPATH);

        // Collect the output
        MockHttpServletResponse res = result.getResponse();
        byte[] byteresult = res.getContentAsByteArray();

        // Convert the raw output to a string
        String dsr = new String(byteresult, UTF8);

        if(prop_visual)
            visual("TestDSR", dsr);

        if(prop_baseline) {
            writefile(baselinepath, dsr);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baselinepath);
            System.out.println("DSR Comparison:");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, dsr));
        }
    }
}


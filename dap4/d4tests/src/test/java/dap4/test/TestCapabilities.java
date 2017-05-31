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
 * TestCapabilities verifies the Capabilities page
 */

public class TestCapabilities extends DapTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDir
    static protected final String TESTINPUTDIR = "/testfiles";
    // Use TestDSR to store the Capabilities page
    static protected final String BASELINEDIR = "/TestDSR/baseline";
    static protected final String BASELINEPATH= "/TestDSR/baseline/capabilities.xml";

    // constants for Fake Request
    static protected String FAKEURLPREFIX = "/dap4";
    static protected String FAKEURLPATH = FAKEURLPREFIX;

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
                MockMvcBuilders.standaloneSetup(new Dap4Controller());
        mvcbuilder.setValidator(new TestServlet.NullValidator());
        this.mockMvc = mvcbuilder.build();
        testSetup();
        DapCache.dspregistry.register(FileDSP.class, DSPRegistry.FIRST);
        DapCache.dspregistry.register(SynDSP.class, DSPRegistry.FIRST);
        if(prop_ascii)
            Generator.setASCII(true);
        this.resourceroot = getResourceRoot();
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void testCapabilities()
        throws Exception
    {
        String url = FAKEURLPATH; // no file specified

        // Figure out the baseline
        String baselinepath = canonjoin(this.resourceroot, BASELINEPATH);

        MvcResult result = perform(url, this.mockMvc, RESOURCEPATH);

        // Collect the output
        MockHttpServletResponse res = result.getResponse();
        byte[] byteresult = res.getContentAsByteArray();

        // Convert the raw output to a string
        String cap = new String(byteresult, UTF8);

        if(prop_visual)
            visual("TestCapabilities", cap);

        if(prop_baseline) {
            writefile(baselinepath, cap);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baselinepath);
            System.out.println("Capabilities Comparison:");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, cap));
        }
    }








}


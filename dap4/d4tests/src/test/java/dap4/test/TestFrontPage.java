package dap4.test;

import dap4.core.data.DSPRegistry;
import dap4.core.util.DapUtil;
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

/**
 * TestFrontPage verifies the front page
 * generation code
 */

public class TestFrontPage extends DapTestCommon
{
    static final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String RESOURCEPATH = "/src/test/data/resources"; // wrt getTestInputFilesDir
    static protected final String TESTINPUTDIR = "/testfiles";
    static protected final String BASELINEDIR = "/TestFrontPage/baseline";
/*
    static protected String DATADIR = "src/test/data"; // relative to dap4 root
    static protected String TESTDATADIR = DATADIR + "/resources/";
    static protected String BASELINEDIR = "/TestServlet/baseline";
    static protected String TESTINPUTDIR = DATADIR + "/resources/testfiles";
*/

    static protected String TESTFILE = "test_frontpage.html";

    // constants for Fake Request
    static protected String FAKEURLPREFIX = "/d4ts";

    //////////////////////////////////////////////////
    // Instance variables
    MockMvc mockMvc = null;
    protected String resourceroot = null;

    //////////////////////////////////////////////////

    @Before
    public void setup() throws Exception
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
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Test
    public void testFrontPage()
            throws Exception
    {
        String url = FAKEURLPREFIX; // no file specified

        // Figure out the baseline
        String baselinepath = canonjoin(this.resourceroot, BASELINEDIR, TESTFILE);

        MvcResult result = perform(url, this.mockMvc, RESOURCEPATH);

        // Collect the output
        MockHttpServletResponse res = result.getResponse();
        byte[] byteresult = res.getContentAsByteArray();

        // Convert the raw output to a string
        String html = new String(byteresult, UTF8);

        if(DEBUG || prop_visual)
            visual("Front Page", html);

        if(prop_baseline) {
            writefile(baselinepath, html);
        } else if(prop_diff) { //compare with baseline
            // Read the baseline file
            String baselinecontent = readfile(baselinepath);
            System.out.println("HTML Comparison:");
            Assert.assertTrue("***Fail", same(getTitle(), baselinecontent, html));
        }
    }

} // class TestFrontPage
